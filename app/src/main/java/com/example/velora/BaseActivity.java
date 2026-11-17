package com.example.velora;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class BaseActivity extends AppCompatActivity {

    protected DataManager dataManager;
    private int appliedThemeId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataManager = new DataManager(this);

        // Persistent login logic: trust dataManager.isLoggedIn() to keep users in the app.
        // This ensures they stay logged in until they manually logout.
        boolean hasLocalSession = dataManager.isLoggedIn();
        boolean hasSupabaseSession = SupabaseHelper.isLoggedIn();

        if (!hasLocalSession && !hasSupabaseSession
                && !(this instanceof LoginActivity)
                && !(this instanceof RegisterActivity)
                && !(this instanceof ForgotPasswordActivity)
                && !(this instanceof SplashActivity)) {

            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        appliedThemeId = dataManager.getTheme();
        applyTheme(appliedThemeId);
        
        // Offline Sync: Trigger sync worker on startup
        triggerOfflineSync();
    }

    protected void triggerOfflineSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(MessageSyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "MessageSyncWork",
                ExistingWorkPolicy.KEEP,
                syncRequest
        );
    }

    protected void schedulePeriodReminders() {
        PeriodicWorkRequest reminderRequest =
                new PeriodicWorkRequest.Builder(PeriodReminderWorker.class, 24, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PeriodReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
        );
    }

    private void applyTheme(int theme) {
        switch (theme) {
            case 1: setTheme(R.style.Theme_Velora_Blue); break;
            case 2: setTheme(R.style.Theme_Velora_LightGreen); break;
            case 3: setTheme(R.style.Theme_Velora_LightYellow); break;
            case 4: setTheme(R.style.Theme_Velora_Purple); break;
            case 5: setTheme(R.style.Theme_Velora_LightMaroon); break;
            default: setTheme(R.style.Theme_Velora); break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appliedThemeId != dataManager.getTheme()) {
            recreate();
        }

        // Update presence in Supabase if a session is available
        if (SupabaseHelper.isLoggedIn()) {
            String userId = SupabaseHelper.getCurrentUserId();
            if (userId != null) {
                SupabaseHelper.setPresence(userId, "online");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (SupabaseHelper.isLoggedIn()) {
            String userId = SupabaseHelper.getCurrentUserId();
            if (userId != null) {
                SupabaseHelper.setPresence(userId, "offline");
            }
        }
    }

    protected int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
