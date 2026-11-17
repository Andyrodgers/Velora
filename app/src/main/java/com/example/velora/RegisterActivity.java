package com.example.velora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.common.SignInButton;
import java.util.HashMap;
import java.util.Map;
import kotlin.Unit;

public class RegisterActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText etFirstName = findViewById(R.id.etFirstName);
        EditText etLastName = findViewById(R.id.etLastName);
        EditText etEmail = findViewById(R.id.etRegEmail);
        EditText etPhoneNumber = findViewById(R.id.etPhoneNumber);
        EditText etAge = findViewById(R.id.etAge);
        EditText etPassword = findViewById(R.id.etRegPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnCreate = findViewById(R.id.btnCreateAccount);
        
        Button btnSendOtp = findViewById(R.id.btnSendOtp);
        Button btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        EditText etOtpCode = findViewById(R.id.etOtpCode);
        View layoutOtp = findViewById(R.id.layoutOtp);
        SignInButton btnRegGoogleLogin = findViewById(R.id.btnRegGoogleLogin);

        btnSendOtp.setOnClickListener(v -> {
            String phone = etPhoneNumber.getText().toString().trim();
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            SupabaseHelper.sendOtp(phone, (success, error) -> {
                if (success) {
                    layoutOtp.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error sending OTP: " + error, Toast.LENGTH_LONG).show();
                }
                return Unit.INSTANCE;
            });
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String phone = etPhoneNumber.getText().toString().trim();
            String code = etOtpCode.getText().toString().trim();
            if (code.isEmpty()) return;

            SupabaseHelper.verifyOtp(phone, code, (success, error) -> {
                if (success) {
                    btnCreate.setEnabled(true);
                    Toast.makeText(this, "Phone verified! You can now complete registration.", Toast.LENGTH_SHORT).show();
                    layoutOtp.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, "Verification failed: " + error, Toast.LENGTH_LONG).show();
                }
                return Unit.INSTANCE;
            });
        });

        btnCreate.setOnClickListener(v -> {
            String fName = etFirstName.getText().toString().trim();
            String lName = etLastName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String phone = etPhoneNumber.getText().toString().trim();
            String ageStr = etAge.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (fName.isEmpty() || lName.isEmpty() || email.isEmpty() || phone.isEmpty() || ageStr.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!SecurityUtils.isValidPassword(pass)) {
                Toast.makeText(this, "Password must be at least 8 characters and contain both letters and numbers", Toast.LENGTH_LONG).show();
                return;
            }

            int age;
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show();
                return;
            }

            btnCreate.setEnabled(false);

            // Prepare metadata for Supabase Trigger
            String fullName = fName + " " + lName;
            Map<String, String> metadata = new HashMap<>();
            metadata.put("name", fullName);
            metadata.put("phone", phone);

            // Register with Supabase
            SupabaseHelper.signUp(email, pass, metadata, (success, error) -> {
                if (success) {
                    String hashedPass = SecurityUtils.hashPassword(pass);
                    
                    // Save additional info to Supabase Database (will upsert correctly now)
                    SupabaseHelper.updateUserInfo(phone, fullName, email, age, null);
                    
                    // Save to local storage
                    dataManager.registerUser(email, phone, hashedPass, fullName, age);
                    
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                } else {
                    btnCreate.setEnabled(true);
                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
                return Unit.INSTANCE;
            });
        });

        btnRegGoogleLogin.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In is available on the Login screen.", Toast.LENGTH_SHORT).show();
        });
    }
}
