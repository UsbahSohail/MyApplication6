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
            
            // If email field is empty, show dialog to enter email
            if (TextUtils.isEmpty(email)) {
                showEmailInputDialog();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            // Show dialog to confirm password reset
            showPasswordResetConfirmation(email);
        });

        // Sign Up link - navigate to SignupActivity
        tvSignupLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });
    }

    /**
     * Show dialog to input email if email field is empty
     */
    private void showEmailInputDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter your email address");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your registered email address to receive a password reset link.")
                .setView(input)
                .setPositiveButton("Send Reset Link", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showPasswordResetConfirmation(email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show confirmation dialog before sending password reset email
     */
    private void showPasswordResetConfirmation(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("We will send a password reset link to:\n\n" + email + "\n\nPlease check your inbox and spam folder.")
                .setPositiveButton("Send Link", (dialog, which) -> {
                    sendPasswordResetEmail(email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Send password reset email using Firebase Authentication
     * This will send a REAL email with a link that allows the user to reset their password
     * The email is sent by Firebase Authentication service automatically
     */
    private void sendPasswordResetEmail(String email) {
        Log.d("LoginActivity", "Sending password reset email to: " + email);
        
        // Show loading indicator
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Sending password reset link...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Send password reset email via Firebase Authentication
        // Firebase sends REAL emails automatically - no additional setup needed in code
        // The email is sent from Firebase's email service (noreply@<project-id>.firebaseapp.com)
        // 
        // IMPORTANT: To ensure emails are sent:
        // 1. Go to Firebase Console > Authentication > Templates > Password reset
        // 2. Make sure the email template is enabled
        // 3. Verify your Firebase project has email sending enabled
        // 4. Customize the email template if needed (optional)
        //
        // The email will contain a reset link that expires in 1 hour
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    
                    if (task.isSuccessful()) {
                        Log.d("LoginActivity", "Password reset email sent successfully to: " + email);
                        Log.d("LoginActivity", "Email sent from: Firebase Authentication Service");
                        Log.d("LoginActivity", "Check email inbox for password reset link");
                        
                        // Show success dialog with detailed instructions
                        new AlertDialog.Builder(this)
                                .setTitle("✓ Reset Link Sent!")
                                .setMessage("A password reset email has been sent to:\n\n" + 
                                           email + 
                                           "\n\n📧 WHAT TO DO:\n" +
                                           "1. Check your email inbox (it may take 1-2 minutes)\n" +
                                           "2. Look for an email from Firebase Authentication\n" +
                                           "3. Check your spam/junk folder if you don't see it\n" +
                                           "4. Click the 'Reset Password' link in the email\n" +
                                           "5. Create your new password in the browser\n" +
                                           "6. Return to the app and login with your new password\n\n" +
                                           "⚠️ NOTE:\n" +
                                           "• The reset link expires in 1 hour\n" +
                                           "• The link will open in your web browser\n" +
                                           "• If you don't receive the email, check your spam folder")
                                .setPositiveButton("Got it!", null)
                                .setIcon(android.R.drawable.ic_dialog_email)
                                .show();
                        
                        Toast.makeText(LoginActivity.this,
                                "✓ Password reset email sent! Check your inbox.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Get the actual error for debugging
                        String errorMessage = "Failed to send password reset email.";
                        Exception exception = task.getException();
                        
                        if (exception != null) {
                            String exceptionMessage = exception.getMessage();
                            String exceptionClass = exception.getClass().getSimpleName();
                            
                            // Log the full error for debugging
                            Log.e("LoginActivity", "Password reset error: " + exceptionClass + " - " + exceptionMessage, exception);
                            exception.printStackTrace();
                            
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("user record does not exist") ||
                                    exceptionMessage.contains("There is no user record") ||
                                    exceptionMessage.contains("USER_NOT_FOUND")) {
                                    errorMessage = "No account found with this email address.\n\n" +
                                                  "Please verify the email address or sign up first.";
                                } else if (exceptionMessage.contains("invalid email") ||
                                           exceptionMessage.contains("INVALID_EMAIL")) {
                                    errorMessage = "Invalid email address format.\n\nPlease check and try again.";
                                } else if (exceptionMessage.contains("network") || 
                                           exceptionMessage.contains("Network") ||
                                           exceptionMessage.contains("timeout") ||
                                           exceptionMessage.contains("NETWORK_ERROR")) {
                                    errorMessage = "Network error. Please check your internet connection and try again.";
                                } else if (exceptionMessage.contains("too many requests") ||
                                           exceptionMessage.contains("TOO_MANY_ATTEMPTS")) {
                                    errorMessage = "Too many password reset attempts.\n\nPlease wait a few minutes before trying again.";
                                } else {
                                    // Show detailed error for debugging
                                    errorMessage = "Error: " + exceptionMessage + 
                                                  "\n\nPlease check:\n" +
                                                  "1. Your internet connection\n" +
                                                  "2. That the email address is correct\n" +
                                                  "3. Your Firebase project email settings\n" +
                                                  "\nFull error: " + exceptionClass;
                                }
                            } else {
                                errorMessage = "Unknown error occurred. Please try again later.\n\nError type: " + exceptionClass;
                            }
                        }
                        
                        new AlertDialog.Builder(this)
                                .setTitle("❌ Error Sending Email")
                                .setMessage(errorMessage + 
                                           "\n\n💡 TROUBLESHOOTING:\n" +
                                           "• Verify your internet connection\n" +
                                           "• Make sure the email address is correct\n" +
                                           "• Check Firebase Console email settings\n" +
                                           "• Ensure Firebase Authentication is enabled")
                                .setPositiveButton("OK", null)
                                .setNeutralButton("Retry", (dialog, which) -> sendPasswordResetEmail(email))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("LoginActivity", "Failed to send password reset email", e);
                    Toast.makeText(LoginActivity.this,
                            "Failed to send email. Please check your connection.",
                            Toast.LENGTH_SHORT).show();
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
