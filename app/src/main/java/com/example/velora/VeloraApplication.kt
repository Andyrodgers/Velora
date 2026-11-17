package com.example.velora

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

class VeloraApplication : Application() {

    companion object {
        lateinit var supabase: SupabaseClient
    }

    override fun onCreate() {
        super.onCreate()

        // Keys are now retrieved from BuildConfig for security
        // Explicitly using the full package path to avoid resolution issues
        val supabaseUrl = com.example.velora.BuildConfig.SUPABASE_URL
        val supabaseKey = com.example.velora.BuildConfig.SUPABASE_KEY

        if (supabaseUrl.isNotEmpty() && supabaseKey.isNotEmpty()) {
            supabase = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
                install(Realtime)
                
                defaultSerializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    encodeDefaults = true
                })
            }
        }
    }
}
