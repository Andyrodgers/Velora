package com.example.velora;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;

public class PersonalInfoActivity extends BaseActivity {

    private ImageView ivInfoPhoto;
    private EditText etName, etAge, etEmail, etPhone;
    private Uri selectedPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        ivInfoPhoto = findViewById(R.id.ivInfoPhoto);
        etName = findViewById(R.id.etInfoName);
        etAge = findViewById(R.id.etInfoAge);
        etEmail = findViewById(R.id.etInfoEmail);
        etPhone = findViewById(R.id.etInfoPhone);
        Button btnChangePhoto = findViewById(R.id.btnChangePhoto);
        Button btnSave = findViewById(R.id.btnSaveInfo);

        // Load current data
        loadUserData();

        // Photo Picker Launcher
        ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
                registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        Log.d("PhotoPicker", "Selected URI: " + uri);
                        selectedPhotoUri = uri;
                        ivInfoPhoto.setImageURI(uri);
                        
                        try {
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        } catch (Exception e) {
                            Log.e("PersonalInfo", "Error taking persistable permission", e);
                        }
                    }
                });

        btnChangePhoto.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (name.isEmpty() || ageStr.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int age = Integer.parseInt(ageStr);
                String photoUri = (selectedPhotoUri != null) ? selectedPhotoUri.toString() : dataManager.getProfilePhotoUri();

                dataManager.saveProfile(name, age, email, phone, photoUri);
                Toast.makeText(this, "Personal Information Updated!", Toast.LENGTH_SHORT).show();
                finish();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUserData() {
        etName.setText(dataManager.getUserName());
        etAge.setText(String.valueOf(dataManager.getUserAge()));
        etEmail.setText(dataManager.getUserEmail());
        etPhone.setText(dataManager.getUserPhone());
        
        String photoUriStr = dataManager.getProfilePhotoUri();
        if (photoUriStr != null) {
            try {
                ivInfoPhoto.setImageURI(Uri.parse(photoUriStr));
            } catch (Exception e) {
                Log.e("PersonalInfo", "Error loading photo", e);
                ivInfoPhoto.setImageResource(R.drawable.user_account_icon);
            }
        }
    }
}
