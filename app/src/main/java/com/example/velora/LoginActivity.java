// LoginActivity.java
package com.example.velora;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import kotlin.Unit;

public class LoginActivity extends BaseActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check for Deep Link (Option 3)
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            handleDeepLink(intent.getData());
        }

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        SignInButton btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnLogin.setEnabled(false);

            // Login with Supabase
            SupabaseHelper.signIn(email, pass, (success, error) -> {
                if (success) {
                    fetchUserDataAndFinish(email, pass);
                } else {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
                }
                return Unit.INSTANCE;
            });
        });

        tvForgotPassword.setOnClickListener(v -> {
            Log.d("Velora", "Navigating to ForgotPasswordActivity");
            Toast.makeText(LoginActivity.this, "Loading Reset Screen...", Toast.LENGTH_SHORT).show();
            Intent intentReset = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intentReset);
        });

        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        tvCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Request notification permission for Android 13+
        requestNotificationPermission();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private void handleDeepLink(Uri data) {
        String url = data.toString();
        Log.d("Velora", "Handling deep link: " + url);
        
        SupabaseHelper.handleAuthRedirect(url, (success, error) -> {
            if (success) {
                Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_LONG).show();
                String email = SupabaseHelper.getCurrentUserEmail();
                if (email != null) {
                    fetchUserDataAndFinish(email, ""); 
                }
            } else {
                Log.e("Velora", "Auth redirect error: " + error);
                Toast.makeText(this, "Verification failed or link expired.", Toast.LENGTH_LONG).show();
            }
            return Unit.INSTANCE;
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Toast.makeText(this, "Google Sign-In successful. Fetching data...", Toast.LENGTH_SHORT).show();
                    fetchUserDataAndFinish(account.getEmail(), "");
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchUserDataAndFinish(String email, String password) {
        SupabaseHelper.getUserByEmail(email, (user) -> {
            if (user != null) {
                String phone = user.getPhone();
                String name = user.getName();
                int age = user.getAge();
                String photoUri = user.getPhotoUri();
                
                // Save all user data including profile photo
                dataManager.registerUser(email, phone, password, name, age);
                dataManager.saveProfile(name, age, email, phone, photoUri);
            } else {
                dataManager.registerUser(email, "", password, "User", 0);
            }

            dataManager.setLoggedIn(true);
            
            // Schedule reminders upon successful login
            schedulePeriodReminders();

            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return Unit.INSTANCE;
        });
    }
}
