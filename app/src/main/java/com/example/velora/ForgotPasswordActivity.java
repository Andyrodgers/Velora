package com.example.velora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import kotlin.Unit;

public class ForgotPasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        EditText etEmail = findViewById(R.id.etForgotEmail);
        Button btnSend = findViewById(R.id.btnSendResetLink);
        TextView tvBack = findViewById(R.id.tvBackToLogin);

        btnSend.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSend.setEnabled(false);
            SupabaseHelper.resetPassword(email, (success, error) -> {
                btnSend.setEnabled(true);
                if (success) {
                    Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
                return Unit.INSTANCE;
            });
        });

        tvBack.setOnClickListener(v -> finish());
    }
}
