package com.example.velora;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import kotlin.Unit;

public class PartnerChatActivity extends BaseActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ChatAdapter adapter;
    private List<ChatMessage> chatList;
    private ImageView ivPartnerPhoto;
    private TextView tvPartnerName, tvPartnerStatus;
    private String partnerId, partnerPhone;
    private boolean isPartnerOnline = false;

    private LinearLayout chatLayout, pinOverlay;
    private TextInputEditText etPinInput;
    private Button btnUnlockChat;
    private TextView btnBackFromChat;

    // Reply feature views
    private LinearLayout llReplyContainer;
    private TextView tvReplyName, tvReplyText;
    private ChatMessage replyingToMessage = null;

    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingRunnable;
    private boolean isTyping = false;

    private MessageDao messageDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partner_chat);

        messageDao = VeloraDatabase.getInstance(this).messageDao();

        chatLayout = findViewById(R.id.chatLayout);
        ivPartnerPhoto = findViewById(R.id.ivPartnerPhoto);
        tvPartnerName = findViewById(R.id.tvPartnerName);
        tvPartnerStatus = findViewById(R.id.tvPartnerStatus);
        rvMessages = findViewById(R.id.rvPartnerMessages);
        etMessage = findViewById(R.id.etPartnerMessage);
        ImageButton btnSend = findViewById(R.id.btnSendPartnerMessage);

        pinOverlay = findViewById(R.id.pinOverlay);
        etPinInput = findViewById(R.id.etPinInput);
        btnUnlockChat = findViewById(R.id.btnUnlockChat);
        btnBackFromChat = findViewById(R.id.btnBackFromChat);

        llReplyContainer = findViewById(R.id.llReplyContainer);
        tvReplyName = findViewById(R.id.tvReplyName);
        tvReplyText = findViewById(R.id.tvReplyText);
        findViewById(R.id.btnCancelReply).setOnClickListener(v -> cancelReply());

        View.OnClickListener openProfileListener = v -> {
            Intent intent = new Intent(this, OtherUserProfileActivity.class);
            intent.putExtra("otherUserId", partnerId);
            intent.putExtra("otherUserPhone", partnerPhone);
            intent.putExtra("otherUserName", dataManager.getPartnerName());
            intent.putExtra("otherUserPhoto", dataManager.getPartnerPhotoUri());
            intent.putExtra("isPartner", true);
            startActivity(intent);
        };
        tvPartnerName.setOnClickListener(openProfileListener);
        ivPartnerPhoto.setOnClickListener(openProfileListener);

        chatList = new ArrayList<>();
        adapter = new ChatAdapter(chatList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        loadPartnerDetails();

        String savedPin = dataManager.getPartnerChatPin();
        if (savedPin.isEmpty()) {
            pinOverlay.setVisibility(View.GONE);
            chatLayout.setVisibility(View.VISIBLE);
        } else {
            chatLayout.setVisibility(View.GONE);
            pinOverlay.setVisibility(View.VISIBLE);
            btnUnlockChat.setOnClickListener(v -> {
                if (etPinInput.getText() != null && etPinInput.getText().toString().equals(savedPin)) {
                    pinOverlay.setVisibility(View.GONE);
                    chatLayout.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
                    etPinInput.setText("");
                }
            });
        }

        btnBackFromChat.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());
        findViewById(R.id.btnChatSettings).setOnClickListener(v -> showSetPinDialog());

        setupTypingListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (partnerId != null) {
            SupabaseHelper.resetUnreadCount(SupabaseHelper.getCurrentUserId(), partnerId);
            // Read transition: mark messages as read when entering the chat
            SupabaseHelper.markMessagesAsRead(SupabaseHelper.getCurrentUserId(), partnerId, "partner");
        }
    }

    private void setupTypingListener() {
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isTyping && s.length() > 0) {
                    isTyping = true;
                    SupabaseHelper.setTypingStatus(SupabaseHelper.getCurrentUserId(), true);
                }
                typingHandler.removeCallbacks(typingRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                typingRunnable = () -> {
                    isTyping = false;
                    SupabaseHelper.setTypingStatus(SupabaseHelper.getCurrentUserId(), false);
                };
                typingHandler.postDelayed(typingRunnable, 2000);
            }
        });
    }

    private void cancelReply() {
        replyingToMessage = null;
        llReplyContainer.setVisibility(View.GONE);
    }

    private void startReply(ChatMessage message) {
        replyingToMessage = message;
        llReplyContainer.setVisibility(View.VISIBLE);
        String myId = SupabaseHelper.getCurrentUserId();
        String senderName = (myId != null && myId.equals(message.getSenderId())) ? "You" : (dataManager.getPartnerName().isEmpty() ? "Partner" : dataManager.getPartnerName());
        tvReplyName.setText(senderName);
        tvReplyText.setText(message.getMessage());
        etMessage.requestFocus();
    }

    private void loadPartnerDetails() {
        partnerId = dataManager.getPartnerId();
        partnerPhone = dataManager.getPartnerPhone();
        tvPartnerName.setText(dataManager.getPartnerName().isEmpty() ? "Partner" : dataManager.getPartnerName());
        
        String photoUri = dataManager.getPartnerPhotoUri();
        Glide.with(this).load(photoUri).placeholder(R.drawable.user_account_icon).error(R.drawable.user_account_icon).into(ivPartnerPhoto);

        if (partnerId != null && !partnerId.isEmpty()) {
            setupSupabaseChat();
            observePartnerDetails();
        } else if (partnerPhone != null && !partnerPhone.isEmpty()) {
            // If we only have phone, try to fetch ID
            SupabaseHelper.getUserByPhone(partnerPhone, user -> {
                if (user != null) {
                    partnerId = user.getId();
                    setupSupabaseChat();
                    observePartnerDetails();
                }
                return Unit.INSTANCE;
            });
        }
    }

    private void setupSupabaseChat() {
        String myId = SupabaseHelper.getCurrentUserId();
        if (myId == null || partnerId == null) return;

        // Observe Local Database (UI updates automatically)
        messageDao.getMessagesForConversation(myId, partnerId, "partner").observe(this, messages -> {
            chatList.clear();
            chatList.addAll(messages);
            adapter.notifyDataSetChanged();
            if (!chatList.isEmpty()) rvMessages.scrollToPosition(chatList.size() - 1);
        });

        // Fetch History from Supabase and sync with Room
        SupabaseHelper.getChatHistory(myId, partnerId, "partner", (messages) -> {
            new Thread(() -> {
                for (ChatMessageSupabase m : messages) {
                    ChatMessage local = mapToChatMessage(m);
                    local.setSynced(true);
                    messageDao.insertMessage(local);
                }
            }).start();

            // Mark incoming messages as read
            SupabaseHelper.markMessagesAsRead(myId, partnerId, "partner");
            return Unit.INSTANCE;
        });

        // Observe Realtime and save to Room
        SupabaseHelper.observeMessages(myId, partnerId, "partner", (m) -> {
            new Thread(() -> {
                ChatMessage local = mapToChatMessage(m);
                local.setSynced(true);
                messageDao.insertMessage(local);
            }).start();
            
            // Delivered transition: If I'm receiver, update server status to 2
            if (m.getReceiverId() != null && m.getReceiverId().equals(myId) && (m.getStatus() == null || m.getStatus() < 2)) {
                SupabaseHelper.updateMessageStatus(m.getId(), 2);
            }

            // Read transition: If I'm receiver, update to 3 and reset unread
            if (m.getReceiverId() != null && m.getReceiverId().equals(myId)) {
                SupabaseHelper.markMessageAsSeen(m.getId());
                SupabaseHelper.resetUnreadCount(myId, partnerId);
            }
            return Unit.INSTANCE;
        });
    }

    private ChatMessage mapToChatMessage(ChatMessageSupabase m) {
        ChatMessage cm = new ChatMessage(
            m.getId(), 
            m.getSenderId() != null ? m.getSenderId() : "", 
            m.getReceiverId() != null ? m.getReceiverId() : "", 
            m.getMessage() != null ? m.getMessage() : "", 
            m.getTimestamp() != null ? m.getTimestamp() : 0,
            "partner"
        );
        cm.setStatus(m.getStatus() != null ? m.getStatus() : (Boolean.TRUE.equals(m.isSeen()) ? 3 : (Boolean.TRUE.equals(m.isReceived()) ? 2 : 1)));
        cm.setSeen(Boolean.TRUE.equals(m.isSeen()) || cm.getStatus() == 3);
        cm.setReceived(Boolean.TRUE.equals(m.isReceived()) || cm.getStatus() >= 2);
        cm.setRepliedMessageId(m.getRepliedMessageId());
        cm.setRepliedText(m.getRepliedText());
        cm.setRepliedSenderName(m.getRepliedSenderName());
        return cm;
    }

    private void observePartnerDetails() {
        if (partnerId == null) return;
        SupabaseHelper.observeUserDetails(partnerId, (user) -> {
            if (user != null) {
                isPartnerOnline = "online".equalsIgnoreCase(user.getStatus());
                if (Boolean.TRUE.equals(user.isTyping())) {
                    tvPartnerStatus.setText("typing...");
                    tvPartnerStatus.setTextColor(0xFF4CAF50); // Green
                } else if (isPartnerOnline) {
                    tvPartnerStatus.setText("Online");
                    tvPartnerStatus.setTextColor(0xFF4CAF50); // Green
                } else {
                    tvPartnerStatus.setText("Offline");
                    tvPartnerStatus.setTextColor(0xFFE0E0E0);
                }

                // Update photo if changed in Supabase
                if (user.getPhotoUri() != null) {
                    Glide.with(this)
                            .load(user.getPhotoUri())
                            .placeholder(R.drawable.user_account_icon)
                            .error(R.drawable.user_account_icon)
                            .into(ivPartnerPhoto);
                }
            }
            return Unit.INSTANCE;
        });
    }

    private void sendMessage() {
        String msgText = etMessage.getText().toString().trim();
        if (!msgText.isEmpty()) {
            String myId = SupabaseHelper.getCurrentUserId();
            if (myId == null || partnerId == null) return;

            // Offline-First: status = 0 (PENDING), isSynced = false
            ChatMessage localMsg = new ChatMessage(
                    null, myId, partnerId, msgText, System.currentTimeMillis(), "partner"
            );
            localMsg.setSenderEmail(SupabaseHelper.getCurrentUserEmail());
            localMsg.setStatus(0);
            localMsg.setSynced(false);
            
            if (replyingToMessage != null) {
                String senderName = myId.equals(replyingToMessage.getSenderId()) ? "You" : (dataManager.getPartnerName().isEmpty() ? "Partner" : dataManager.getPartnerName());
                localMsg.setRepliedMessageId(replyingToMessage.getMessageId());
                localMsg.setRepliedText(replyingToMessage.getMessage());
                localMsg.setRepliedSenderName(senderName);
                cancelReply();
            }

            // Save to Room first
            new Thread(() -> {
                messageDao.insertMessage(localMsg);
                // Trigger Sync Worker
                triggerOfflineSync();
            }).start();
            
            SupabaseHelper.incrementUnreadCount(partnerId, myId);
            etMessage.setText("");
            
            isTyping = false;
            SupabaseHelper.setTypingStatus(myId, false);
        }
    }

    private void showSetPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Privacy PIN");
        final EditText input = new EditText(this);
        input.setHint("4-digit PIN");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Save", (d, w) -> {
            String pin = input.getText().toString();
            if (pin.length() >= 4) {
                dataManager.savePartnerChatPin(pin);
                Toast.makeText(this, "PIN Saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isTyping) {
            isTyping = false;
            SupabaseHelper.setTypingStatus(SupabaseHelper.getCurrentUserId(), false);
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private final List<ChatMessage> messages;
        ChatAdapter(List<ChatMessage> messages) { this.messages = messages; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_partner_message, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatMessage m = messages.get(position);
            String myId = SupabaseHelper.getCurrentUserId();
            boolean isMe = myId != null && m.getSenderId() != null && m.getSenderId().equals(myId);

            holder.tvText.setText(m.getMessage());
            holder.tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(m.getTimestamp())));

            if (m.getRepliedMessageId() != null) {
                holder.llReplyPreview.setVisibility(View.VISIBLE);
                holder.tvRepliedName.setText(m.getRepliedSenderName());
                holder.tvRepliedText.setText(m.getRepliedText());
            } else {
                holder.llReplyPreview.setVisibility(View.GONE);
            }

            TypedValue typedValue = new TypedValue();
            holder.itemView.getContext().getTheme().resolveAttribute(R.attr.appPrimaryColor, typedValue, true);
            int primaryColor = typedValue.data;

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.container.getLayoutParams();
            if (isMe) {
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                holder.container.setBackgroundResource(R.drawable.chat_bubble_sent);
                holder.container.getBackground().setTint(primaryColor);
                
                holder.tvText.setTextColor(0xFFFFFFFF);
                holder.tvTime.setTextColor(0xFFEEEEEE);
                holder.ivTicks.setVisibility(View.VISIBLE);
                
                // Tick Logic based on status
                int status = m.getStatus();
                if (status == 3) { // READ
                    holder.ivTicks.setImageResource(R.drawable.ic_double_tick);
                    holder.ivTicks.setColorFilter(0xFF00B0FF); // Blue tick
                } else if (status == 2) { // DELIVERED
                    holder.ivTicks.setImageResource(R.drawable.ic_double_tick);
                    holder.ivTicks.setColorFilter(0xFFEEEEEE); // Grey double tick
                } else if (status == 1 || m.isSynced()) { // SENT
                    holder.ivTicks.setImageResource(R.drawable.ic_single_tick);
                    holder.ivTicks.setColorFilter(0xFFEEEEEE); // Grey single tick
                } else { // PENDING
                    holder.ivTicks.setImageResource(R.drawable.ic_clock);
                    holder.ivTicks.setColorFilter(0xFFEEEEEE);
                }
                
                holder.replySideBar.setBackgroundColor(0x80FFFFFF);
                holder.tvRepliedName.setTextColor(0xFFFFFFFF);
                holder.tvRepliedText.setTextColor(0xFFEEEEEE);
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.removeRule(RelativeLayout.ALIGN_PARENT_END);
                holder.container.setBackgroundResource(R.drawable.chat_bubble_received);
                holder.container.getBackground().setTint(0xFFFFFFFF);
                holder.tvText.setTextColor(0xFF000000);
                holder.tvTime.setTextColor(0xFF888888);
                holder.ivTicks.setVisibility(View.GONE);
                
                holder.replySideBar.setBackgroundColor(primaryColor);
                holder.tvRepliedName.setTextColor(primaryColor);
                holder.tvRepliedText.setTextColor(0xFF666666);
            }
            holder.container.setLayoutParams(params);

            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                private float startX;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: startX = event.getX(); return true;
                        case MotionEvent.ACTION_MOVE:
                            float diff = event.getX() - startX;
                            if (diff > 150) { // Swipe Right
                                startReply(m);
                                v.performClick();
                                return true;
                            }
                            break;
                    }
                    return false;
                }
            });
        }

        @Override public int getItemCount() { return messages.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvText, tvTime, tvRepliedName, tvRepliedText;
            ImageView ivTicks;
            View container, llReplyPreview, replySideBar;
            ViewHolder(View v) {
                super(v);
                tvText = v.findViewById(R.id.tvMessageText);
                tvTime = v.findViewById(R.id.tvMessageTime);
                ivTicks = v.findViewById(R.id.ivTicks);
                container = v.findViewById(R.id.llMessageContainer);
                llReplyPreview = v.findViewById(R.id.llReplyPreview);
                tvRepliedName = v.findViewById(R.id.tvRepliedName);
                tvRepliedText = v.findViewById(R.id.tvRepliedText);
                replySideBar = v.findViewById(R.id.replySideBar);
            }
        }
    }
}
