@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.velora

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseSessionFromFragment
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

@Serializable
data class UserProfile(
    val id: String = "",
    val phone: String? = null,
    val name: String? = null,
    val email: String? = null,
    val age: Int? = null,
    @SerialName("photo_uri")
    val photoUri: String? = null,
    val status: String? = "offline",
    @SerialName("is_typing")
    val isTyping: Boolean? = false,
    @SerialName("last_seen")
    val lastSeen: String? = null,
    @SerialName("partner_id")
    val partnerId: String? = null,
    @SerialName("app_theme")
    val appTheme: Int? = 0,
    @SerialName("user_notes")
    val userNotes: String? = null
)

@Serializable
data class ChatMessageSupabase(
    val id: String? = null,
    @SerialName("sender_id")
    val senderId: String? = null,
    @SerialName("receiver_id")
    val receiverId: String? = null,
    val message: String? = null,
    val timestamp: Long? = null,
    @SerialName("chat_type")
    val chatType: String? = null, // "community" or "partner"
    @SerialName("is_seen")
    val isSeen: Boolean? = null,
    @SerialName("is_delivered")
    val isReceived: Boolean? = null,
    val status: Int? = 0, // 0:PENDING, 1:SENT, 2:DELIVERED, 3:READ
    @SerialName("replied_message_id")
    val repliedMessageId: String? = null,
    @SerialName("replied_text")
    val repliedText: String? = null,
    @SerialName("replied_sender_name")
    val repliedSenderName: String? = null
)

@Serializable
data class UnreadCount(
    @SerialName("sender_id")
    val senderId: String = "",
    @SerialName("receiver_id")
    val receiverId: String = "",
    val count: Long = 0
)

object SupabaseHelper {

    interface OnUploadCompleteListener {
        fun onSuccess(downloadUrl: String)
        fun onFailure(error: String)
    }

    private val supabase = VeloraApplication.supabase
    private val scope = CoroutineScope(Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @JvmStatic
    fun cleanPhone(phone: String?): String = phone?.replace("[^0-9]".toRegex(), "") ?: ""

    @JvmStatic
    fun uploadProfilePhoto(userId: String, fileUri: Uri, context: Context, listener: OnUploadCompleteListener) {
        scope.launch {
            try {
                val fileName = "profiles/profile_$userId.jpg"
                val bucket = supabase.storage.from("profiles")

                val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                    ?: throw Exception("Could not read file")

                bucket.upload(fileName, bytes) {
                    upsert = true
                }
                val url = bucket.publicUrl(fileName)

                withContext(Dispatchers.Main) {
                    listener.onSuccess(url)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    listener.onFailure(e.message ?: "Unknown error")
                }
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun signUp(email: String, pass: String, metadata: Map<String, String>? = null, onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                    if (metadata != null) {
                        data = buildJsonObject {
                            metadata.forEach { (k, v) -> put(k, v) }
                        }
                    }
                }
                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message) }
            }
        }
    }

    @JvmStatic
    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    @JvmStatic
    fun getCurrentUserEmail(): String? = supabase.auth.currentUserOrNull()?.email

    @JvmStatic
    @JvmOverloads
    fun updateUserInfo(phone: String, name: String, email: String, age: Int, photoUri: String? = null, about: String? = null) {
        scope.launch {
            try {
                val currentId = getCurrentUserId() ?: return@launch
                val user = UserProfile(
                    id = currentId,
                    phone = cleanPhone(phone),
                    name = name,
                    email = email,
                    age = age,
                    photoUri = photoUri,
                    userNotes = about
                )
                supabase.postgrest.from("users").upsert(user)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @JvmStatic
    fun setPresence(userId: String, status: String) {
        scope.launch {
            try {
                supabase.postgrest.from("users").update({
                    set("status", status)
                }) {
                    filter { eq("id", userId) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun getCurrentUserProfile(onResult: (UserProfile?) -> Unit) {
        scope.launch {
            try {
                val id = getCurrentUserId() ?: return@launch
                val profile = supabase.postgrest.from("users").select {
                    filter { eq("id", id) }
                }.decodeSingleOrNull<UserProfile>()
                withContext(Dispatchers.Main) { onResult(profile) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    @JvmStatic
    fun getUserById(id: String, onResult: (UserProfile?) -> Unit) {
        scope.launch {
            try {
                val result = supabase.postgrest.from("users").select {
                    filter { eq("id", id) }
                }.decodeSingleOrNull<UserProfile>()
                withContext(Dispatchers.Main) { onResult(result) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    @JvmStatic
    fun getUserByEmail(email: String, onResult: (UserProfile?) -> Unit) {
        scope.launch {
            try {
                val result = supabase.postgrest.from("users").select {
                    filter { eq("email", email) }
                }.decodeSingleOrNull<UserProfile>()
                withContext(Dispatchers.Main) { onResult(result) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    @JvmStatic
    fun getUserByPhone(phone: String, onResult: (UserProfile?) -> Unit) {
        scope.launch {
            try {
                val cleanedPhone = cleanPhone(phone)
                val result = supabase.postgrest.from("users").select {
                    filter { eq("phone", cleanedPhone) }
                }.decodeSingleOrNull<UserProfile>()
                withContext(Dispatchers.Main) { onResult(result) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    @JvmStatic
    fun observeProfileById(id: String, callback: (UserProfile) -> Unit) {
        scope.launch {
            val channel = supabase.realtime.channel("profile_$id")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "users"
                filter("id", FilterOperator.EQ, id)
            }
            flow.onEach { action ->
                val record = when (action) {
                    is PostgresAction.Insert -> action.record
                    is PostgresAction.Update -> action.record
                    else -> null
                }
                record?.let {
                    try {
                        val user = json.decodeFromJsonElement<UserProfile>(it)
                        withContext(Dispatchers.Main) { callback(user) }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }.launchIn(this)
            channel.subscribe()
        }
    }

    @JvmStatic
    fun searchAndLinkPartner(partnerEmail: String, onResult: (UserProfile?, String?) -> Unit) {
        scope.launch {
            try {
                val currentId = getCurrentUserId() ?: throw Exception("Not logged in")

                val partner = supabase.postgrest.from("users").select {
                    filter { eq("email", partnerEmail) }
                }.decodeSingleOrNull<UserProfile>()

                if (partner == null) {
                    withContext(Dispatchers.Main) { onResult(null, "User not found") }
                    return@launch
                }

                if (partner.id == currentId) {
                    withContext(Dispatchers.Main) { onResult(null, "You cannot link to yourself") }
                    return@launch
                }

                val partnerId = partner.id

                // Mutual Link
                supabase.postgrest.from("users").update({ set("partner_id", partnerId) }) { filter { eq("id", currentId) } }
                supabase.postgrest.from("users").update({ set("partner_id", currentId) }) { filter { eq("id", partnerId) } }

                withContext(Dispatchers.Main) { onResult(partner, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(null, e.message) }
            }
        }
    }

    @JvmStatic
    fun signIn(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                supabase.auth.signInWith(Email) { this.email = email; this.password = pass }
                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message) }
            }
        }
    }

    @JvmStatic
    fun sendOtp(phone: String, onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                supabase.auth.signInWith(OTP) {
                    this.phone = cleanPhone(phone)
                }
                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message) }
            }
        }
    }

    @JvmStatic
    fun verifyOtp(phone: String, code: String, onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                supabase.auth.verifyPhoneOtp(
                    type = OtpType.Phone.SMS,
                    phone = cleanPhone(phone),
                    token = code
                )
                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message) }
            }
        }
    }

    @JvmStatic
    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                supabase.auth.resetPasswordForEmail(email)
                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message) }
            }
        }
    }

    @JvmStatic
    fun signOut() {
        scope.launch { try { supabase.auth.signOut() } catch (e: Exception) { e.printStackTrace() } }
    }

    @JvmStatic
    fun isLoggedIn(): Boolean = supabase.auth.currentSessionOrNull() != null

    @JvmStatic
    fun setTypingStatus(userId: String, isTyping: Boolean) {
        scope.launch {
            try {
                supabase.postgrest.from("users").update({
                    set("is_typing", isTyping)
                }) {
                    filter { eq("id", userId) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @JvmStatic
    fun observeUserDetails(userId: String, callback: (UserProfile?) -> Unit) {
        scope.launch {
            getUserById(userId) { callback(it) }

            val channel = supabase.realtime.channel("user_details_$userId")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "users"
                filter("id", FilterOperator.EQ, userId)
            }
            flow.onEach { action ->
                val record = when (action) {
                    is PostgresAction.Insert -> action.record
                    is PostgresAction.Update -> action.record
                    else -> null
                }
                record?.let {
                    try {
                        val user = json.decodeFromJsonElement<UserProfile>(it)
                        withContext(Dispatchers.Main) { callback(user) }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }.launchIn(this)
            channel.subscribe()
        }
    }

    @JvmStatic
    fun getChatHistory(myId: String, otherId: String, chatType: String, onResult: (List<ChatMessageSupabase>) -> Unit) {
        scope.launch {
            try {
                val results = supabase.postgrest.from("messages").select {
                    filter {
                        eq("chat_type", chatType)
                        or {
                            and {
                                eq("sender_id", myId)
                                eq("receiver_id", otherId)
                            }
                            and {
                                eq("sender_id", otherId)
                                eq("receiver_id", myId)
                            }
                        }
                    }
                    order("timestamp", Order.ASCENDING)
                }.decodeList<ChatMessageSupabase>()
                withContext(Dispatchers.Main) { onResult(results) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(emptyList()) }
            }
        }
    }

    @JvmStatic
    fun updateMessageStatus(messageId: String?, status: Int) {
        if (messageId == null) return
        scope.launch {
            try {
                supabase.postgrest.from("messages").update({
                    set("status", status)
                    if (status == 3) set("is_seen", true)
                    if (status >= 2) set("is_delivered", true)
                }) {
                    filter { eq("id", messageId) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @JvmStatic
    fun markMessageAsSeen(messageId: String?) {
        updateMessageStatus(messageId, 3)
    }

    @JvmStatic
    fun markMessagesAsRead(myId: String, otherId: String, chatType: String) {
        scope.launch {
            try {
                supabase.postgrest.from("messages").update({
                    set("status", 3)
                    set("is_seen", true)
                }) {
                    filter {
                        eq("chat_type", chatType)
                        eq("sender_id", otherId)
                        eq("receiver_id", myId)
                        lt("status", 3)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @JvmStatic
    fun observeMessages(myId: String, otherId: String, chatType: String, callback: (ChatMessageSupabase) -> Unit) {
        scope.launch {
            val channel = supabase.realtime.channel("chat_${myId.take(8)}_${otherId.take(8)}")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
                filter("chat_type", FilterOperator.EQ, chatType)
            }
            flow.onEach { action ->
                val record = when (action) {
                    is PostgresAction.Insert -> action.record
                    is PostgresAction.Update -> action.record
                    else -> null
                }
                record?.let {
                    try {
                        val msg = json.decodeFromJsonElement<ChatMessageSupabase>(it)
                        if ((msg.senderId == myId && msg.receiverId == otherId) ||
                            (msg.senderId == otherId && msg.receiverId == myId)) {
                            withContext(Dispatchers.Main) { callback(msg) }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }.launchIn(this)
            channel.subscribe()
        }
    }

    @JvmStatic
    fun sendMessage(msg: ChatMessageSupabase, onResult: (ChatMessageSupabase?) -> Unit) {
        scope.launch {
            try {
                val result = supabase.postgrest.from("messages").insert(msg) {
                    select()
                }.decodeSingle<ChatMessageSupabase>()
                withContext(Dispatchers.Main) { onResult(result) }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    @JvmStatic
    fun incrementUnreadCount(receiverId: String, senderId: String) {
        scope.launch {
            try {
                val current = supabase.postgrest.from("unread_counts").select {
                    filter {
                        eq("receiver_id", receiverId)
                        eq("sender_id", senderId)
                    }
                }.decodeSingleOrNull<UnreadCount>()

                val newCount = (current?.count ?: 0L) + 1L
                supabase.postgrest.from("unread_counts").upsert(UnreadCount(senderId = senderId, receiverId = receiverId, count = newCount))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @JvmStatic
    fun resetUnreadCount(myId: String, senderId: String) {
        scope.launch {
            try {
                supabase.postgrest.from("unread_counts").update({
                    set("count", 0)
                }) {
                    filter {
                        eq("receiver_id", myId)
                        eq("sender_id", senderId)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @JvmStatic
    fun observeUnreadCounts(myId: String, callback: (List<UnreadCount>?) -> Unit) {
        scope.launch {
            try {
                val initial = supabase.postgrest.from("unread_counts").select {
                    filter { eq("receiver_id", myId) }
                }.decodeList<UnreadCount>()
                withContext(Dispatchers.Main) { callback(initial) }
            } catch (e: Exception) { e.printStackTrace() }

            val channel = supabase.realtime.channel("unread_$myId")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "unread_counts"
                filter("receiver_id", FilterOperator.EQ, myId)
            }
            flow.onEach {
                try {
                    val all = supabase.postgrest.from("unread_counts").select {
                        filter { eq("receiver_id", myId) }
                    }.decodeList<UnreadCount>()
                    withContext(Dispatchers.Main) { callback(all) }
                } catch (e: Exception) { e.printStackTrace() }
            }.launchIn(this)
            channel.subscribe()
        }
    }

    @JvmStatic
    fun getAllUsers(onResult: (List<UserProfile>?) -> Unit) {
        scope.launch {
            try {
                val currentId = getCurrentUserId()
                val response = supabase.postgrest.from("users").select()
                val users = response.decodeList<UserProfile>()
                val filteredUsers = if (currentId != null) {
                    users.filter { it.id != currentId }
                } else {
                    users
                }
                withContext(Dispatchers.Main) { onResult(filteredUsers) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    @JvmStatic
    fun getLastMessage(myId: String, otherId: String, onResult: (ChatMessageSupabase?) -> Unit) {
        scope.launch {
            try {
                val msg = supabase.postgrest.from("messages").select {
                    filter {
                        or {
                            and {
                                eq("sender_id", myId)
                                eq("receiver_id", otherId)
                            }
                            and {
                                eq("sender_id", otherId)
                                eq("receiver_id", myId)
                            }
                        }
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(1)
                }.decodeSingleOrNull<ChatMessageSupabase>()
                withContext(Dispatchers.Main) { onResult(msg) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    @JvmStatic
    fun handleAuthRedirect(url: String, onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                val fragment = url.substringAfter('#', "")
                if (fragment.contains("access_token=")) {
                    supabase.auth.parseSessionFromFragment(fragment)
                    withContext(Dispatchers.Main) { onResult(true, null) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "No session info in URL") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message) }
            }
        }
    }
}
