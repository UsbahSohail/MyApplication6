package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private TextView tvSignupLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignupLink = findViewById(R.id.tvSignupLink);

        btnLogin.setOnClickListener(v -> {
            String inputEmail = etEmail.getText().toString().trim();
            String inputPassword = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(inputEmail)) {
                etEmail.setError("Enter registered email");
                etEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(inputPassword)) {
                etPassword.setError("Enter password");
                etPassword.requestFocus();
                return;
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            // Sign in with Firebase Authentication
            btnLogin.setEnabled(false);
            mAuth.signInWithEmailAndPassword(inputEmail, inputPassword)
                    .addOnCompleteListener(this, task -> {
                        btnLogin.setEnabled(true);
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String displayName = user.getDisplayName();
                                String greetingName = TextUtils.isEmpty(displayName) ? "Prime Member" : displayName;
                                Toast.makeText(LoginActivity.this, 
                                    "Welcome back, " + greetingName + "!", 
                                    Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, ProductActivity.class));
                                finish();
                            }
                        } else {
                            // Sign in failed
                            String errorMessage = "Login failed. Please check your credentials.";
                            if (task.getException() != null) {
                                String exceptionMessage = task.getException().getMessage();
                                if (exceptionMessage != null) {
                                    if (exceptionMessage.contains("user record does not exist") ||
                                        exceptionMessage.contains("There is no user record")) {
                                        errorMessage = "No account found with this email. Please sign up first.";
                                    } else if (exceptionMessage.contains("password is invalid") ||
                                               exceptionMessage.contains("wrong password")) {
                                        errorMessage = "Incorrect password. Please try again.";
                                    } else if (exceptionMessage.contains("invalid email")) {
                                        errorMessage = "Invalid email address. Please check and try again.";
                                    }
                                }
                            }
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Forgot Password functionality
        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter your email address");
                etEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            // Show dialog to confirm password reset
            new AlertDialog.Builder(this)
                    .setTitle("Reset Password")
                    .setMessage("We will send a password reset link to:\n" + email)
                    .setPositiveButton("Send", (dialog, which) -> {
                        sendPasswordResetEmail(email);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Sign Up link - navigate to SignupActivity
        tvSignupLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });
    }

    private void sendPasswordResetEmail(String email) {
        Log.d("LoginActivity", "Sending password reset email to: " + email);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("LoginActivity", "Password reset email sent successfully");
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent! Please check your inbox (and spam folder).",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Get the actual error for debugging
                        String errorMessage = "Failed to send password reset email.";
                        if (task.getException() != null) {
                            Exception exception = task.getException();
                            String exceptionMessage = exception.getMessage();
                            
                            // Log the full error for debugging
                            Log.e("LoginActivity", "Password reset error: " + exceptionMessage, exception);
                            exception.printStackTrace();
                            
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("user record does not exist") ||
                                    exceptionMessage.contains("There is no user record")) {
                                    errorMessage = "No account found with this email address.";
                                } else if (exceptionMessage.contains("invalid email")) {
                                    errorMessage = "Invalid email address. Please check and try again.";
                                } else if (exceptionMessage.contains("network") || 
                                           exceptionMessage.contains("Network")) {
                                    errorMessage = "Network error. Please check your internet connection.";
                                } else {
                                    // Show actual error message for debugging
                                    errorMessage = "Error: " + exceptionMessage;
                                }
                            }
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Don't auto-redirect - always show login screen
        // User must login even if previously logged in
        // This ensures: Splash → Login → (after login) → Home
    }
}
