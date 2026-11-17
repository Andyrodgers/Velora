package com.example.velora;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import java.util.Calendar;

public class ProfileActivity extends BaseActivity {

    private ImageView ivProfilePhoto;
    private EditText etName, etAge, etEmail, etPhone;
    private Uri selectedPhotoUri;
    private TextView tvSettings, tvPersonalInfo, tvCurrentStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        etName = findViewById(R.id.etProfileName);
        etAge = findViewById(R.id.etProfileAge);
        etEmail = findViewById(R.id.etProfileEmail);
        etPhone = findViewById(R.id.etProfilePhone);
        
        tvSettings = findViewById(R.id.tvSettings);
        tvPersonalInfo = findViewById(R.id.tvPersonalInfo);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);

        Button btnUpload = findViewById(R.id.btnUploadPhoto);
        Button btnSave = findViewById(R.id.btnUpdateProfile);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Load current data initially
        loadUserData();

        updateCurrentStatus();

        // Click listeners for reactive sections
        tvSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        tvPersonalInfo.setOnClickListener(v -> {
            startActivity(new Intent(this, PersonalInfoActivity.class));
        });

        tvCurrentStatus.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryDetailActivity.class));
        });

        // Photo Picker Launcher
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        Log.d("PhotoPicker", "Selected URI: " + uri);
                        selectedPhotoUri = uri;
                        // Display newly selected image immediately
                        ivProfilePhoto.setImageURI(uri);

                        // Grant persistable permission
                        try {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (Exception e) {
                            Log.e("PhotoPicker", "Error taking persistable permission", e);
                        }
                    }
                });

        btnUpload.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnSave.setOnClickListener(v -> {
            saveUserDataWithUpload();
        });

        btnLogout.setOnClickListener(v -> {
            SupabaseHelper.signOut();
            dataManager.setLoggedIn(false);
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTextFields();
        updateCurrentStatus();
    }

    private void loadUserData() {
        refreshTextFields();

        if (selectedPhotoUri == null) {
            String photoUriStr = dataManager.getProfilePhotoUri();
            if (photoUriStr != null) {
                Glide.with(this)
                        .load(photoUriStr)
                        .placeholder(R.drawable.user_account_icon)
                        .error(R.drawable.user_account_icon)
                        .into(ivProfilePhoto);
            }
        }
    }

    private void refreshTextFields() {
        etName.setText(dataManager.getUserName());
        etAge.setText(String.valueOf(dataManager.getUserAge()));
        etEmail.setText(dataManager.getUserEmail());
        etPhone.setText(dataManager.getUserPhone());
    }

    private void saveUserDataWithUpload() {
        String name = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || ageStr.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show();
            return;
        }


        // Use correct Supabase User ID (UUID) instead of phone number
        String userId = SupabaseHelper.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedPhotoUri != null) {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage("Uploading photo...");
            pd.setCancelable(false);
            pd.show();

            SupabaseHelper.uploadProfilePhoto(userId, selectedPhotoUri, this, new SupabaseHelper.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String downloadUrl) {
                    pd.dismiss();
                    dataManager.saveProfile(name, age, email, phone, downloadUrl);
                    SupabaseHelper.updateUserInfo(phone, name, email, age, downloadUrl);
                    Toast.makeText(ProfileActivity.this, "Profile updated with photo!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    pd.dismiss();
                    Toast.makeText(ProfileActivity.this, "Photo upload failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            String photoUri = dataManager.getProfilePhotoUri();
            dataManager.saveProfile(name, age, email, phone, photoUri);
            SupabaseHelper.updateUserInfo(phone, name, email, age, photoUri);
            Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateCurrentStatus() {
        Calendar lastPeriod = dataManager.getLastPeriod();
        if (lastPeriod == null) {
            tvCurrentStatus.setText("Current Status: Cycle data not set");
            return;
        }

        int cycle = dataManager.getCycleLength();
        int duration = dataManager.getPeriodDuration();
        Calendar today = Calendar.getInstance();

        Calendar nextPeriod = CycleHelper.calculateNextPeriod(lastPeriod, cycle);
        int daysToPeriod = CycleHelper.daysUntil(today, nextPeriod);

        long diff = today.getTimeInMillis() - lastPeriod.getTimeInMillis();
        int totalDaysSinceStart = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
        int currentDay = ((totalDaysSinceStart - 1) % cycle + cycle) % cycle + 1;

        int ovulationDay = cycle - 14;

        String phase;
        if (currentDay <= duration) {
            phase = "Menstruation";
        } else if (currentDay == ovulationDay) {
            phase = "Ovulation";
        } else if (currentDay < ovulationDay) {
            phase = "Follicular Phase";
        } else {
            if (daysToPeriod <= 3 && daysToPeriod >= 0) {
                phase = "Safe Day (Luteal)";
            } else {
                phase = "Luteal Phase";
            }
        }

        tvCurrentStatus.setText("Current Status: Day " + currentDay + " (" + phase + ")");
    }
}
