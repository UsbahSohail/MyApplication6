package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
        
        // Request notification permission for Android 13+
        requestNotificationPermission();

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
                                // Save/update user to Firebase Realtime Database
                                saveUserToDatabase(user);
                                
                                String displayName = user.getDisplayName();
                                String greetingName = TextUtils.isEmpty(displayName) ? "Prime Member" : displayName;
                                Toast.makeText(LoginActivity.this, 
                                    "Welcome back, " + greetingName + "!", 
                                    Toast.LENGTH_SHORT).show();
                                
                                // Show a welcome notification
                                Intent productIntent = new Intent(LoginActivity.this, ProductActivity.class);
                                LocalNotificationHelper.showNotification(
                                    LoginActivity.this,
                                    "Welcome back!",
                                    "Hello " + greetingName + "! Check out our latest products.",
                                    productIntent
                                );
                                
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
    
    /**
     * Save/update user to Firebase Realtime Database
     */
    private void saveUserToDatabase(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            android.util.Log.e("LoginActivity", "FirebaseUser is null, cannot save to database");
            return;
        }
        
        String userId = firebaseUser.getUid();
        String name = firebaseUser.getDisplayName();
        String email = firebaseUser.getEmail();
        
        // If display name is not set, use email username
        if (name == null || name.isEmpty()) {
            name = email != null ? email.split("@")[0] : "User";
        }
        
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");
        
        // Create User object
        User user = new User(userId, name, email);
        
        android.util.Log.d("LoginActivity", "Saving/updating user in database - ID: " + userId + ", Name: " + name + ", Email: " + email);
        
        // Save/update to database
        usersRef.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("LoginActivity", "User saved/updated successfully in database: " + userId);
                    } else {
                        android.util.Log.e("LoginActivity", "Failed to save user to database", task.getException());
                        if (task.getException() != null) {
                            android.util.Log.e("LoginActivity", "Error details: " + task.getException().getMessage());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LoginActivity", "Error saving user to database", e);
                });
    }

    /**
     * Request notification permission for Android 13+ (API 33+)
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }
    }
}
