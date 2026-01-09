package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivityResetPasswordBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Activity for resetting password directly in the app
 * User enters email and new password, app handles the reset without leaving the app
 */
public class ResetPasswordActivity extends AppCompatActivity {
    
    private static final String TAG = "ResetPasswordActivity";
    private ActivityResetPasswordBinding binding;
    private FirebaseAuth firebaseAuth;
    private String emailFromIntent;
    private String actionCode;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Get email from intent (if coming from LoginActivity)
        emailFromIntent = getIntent().getStringExtra("email");
        if (emailFromIntent != null && !emailFromIntent.isEmpty()) {
            binding.editTextEmail.setText(emailFromIntent);
            binding.editTextEmail.setEnabled(false); // Disable if email is pre-filled
        }
        
        // Check if this is opened from a password reset link (deep link)
        handleIntent(getIntent());
        
        setupTextWatchers();
        setupButtons();
    }
    
    /**
     * Handle deep link from password reset email
     */
    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        Log.d(TAG, "Intent data: " + data);
        
        if (data != null) {
            String uriString = data.toString();
            Log.d(TAG, "Full URI: " + uriString);
            
            // Firebase reset links can be in format:
            // https://PROJECT.firebaseapp.com/__/auth/action?mode=resetPassword&oobCode=CODE&apiKey=KEY&continueUrl=...
            
            // Try to extract from query parameters first
            actionCode = data.getQueryParameter("oobCode");
            String mode = data.getQueryParameter("mode");
            
            // If not in query params, try extracting from the URL string directly
            if (actionCode == null && uriString.contains("oobCode=")) {
                int oobCodeIndex = uriString.indexOf("oobCode=");
                int endIndex = uriString.indexOf("&", oobCodeIndex);
                if (endIndex == -1) endIndex = uriString.length();
                actionCode = uriString.substring(oobCodeIndex + 8, endIndex);
            }
            
            if (mode == null && uriString.contains("mode=")) {
                int modeIndex = uriString.indexOf("mode=");
                int endIndex = uriString.indexOf("&", modeIndex);
                if (endIndex == -1) endIndex = uriString.length();
                mode = uriString.substring(modeIndex + 5, endIndex);
            }
            
            Log.d(TAG, "Extracted - mode: " + mode + ", actionCode: " + (actionCode != null ? "present" : "null"));
            
            if ("resetPassword".equals(mode) && actionCode != null && !actionCode.isEmpty()) {
                // This is a password reset link - redirect to Firebase web page to handle password reset
                Log.d(TAG, "Password reset link detected, opening in browser");
                
                // Open the reset link in browser (Firebase will handle password entry)
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, data);
                startActivity(browserIntent);
                
                // Show message
                Toast.makeText(this, "Opening reset page in browser. Complete the password reset there.", 
                        Toast.LENGTH_LONG).show();
                
                // Navigate back to login after showing message
                finish();
            } else if (uriString.contains("auth/action") && uriString.contains("resetPassword")) {
                // Open in browser if it's a reset password link
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, data);
                startActivity(browserIntent);
                finish();
            }
        } else {
            Log.d(TAG, "No intent data found - opened normally");
        }
    }
    
    private void setupTextWatchers() {
        binding.editTextEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                validateEmail();
            }
        });
    }
    
    private void setupButtons() {
        binding.buttonResetPassword.setOnClickListener(v -> handleResetPassword());
        binding.textBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    /**
     * Handle password reset - just send reset email
     * When user clicks link in email, Firebase's web page will handle password reset
     */
    private void handleResetPassword() {
        boolean isEmailValid = validateEmail();
        if (!isEmailValid) {
            return;
        }
        
        String email = binding.editTextEmail.getText() != null
                ? binding.editTextEmail.getText().toString().trim()
                : "";
        
        binding.emailInputLayout.setError(null);
        
        // Show loading state
        binding.buttonResetPassword.setEnabled(false);
        binding.buttonResetPassword.setText("Sending...");
        
        // Send password reset email
        sendPasswordResetEmail(email);
    }
    
    /**
     * Confirm password reset using action code from email link
     * NOTE: This method is no longer used - password reset happens in browser
     */
    @Deprecated
    private void confirmPasswordReset(String code, String newPassword) {
        firebaseAuth.confirmPasswordReset(code, newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        binding.buttonResetPassword.setEnabled(true);
                        binding.buttonResetPassword.setText(R.string.reset_password_button);
                        
                        if (task.isSuccessful()) {
                            // Password reset successful
                            Toast.makeText(ResetPasswordActivity.this,
                                    "Password reset successful!", Toast.LENGTH_SHORT).show();
                            
                            // Show notification
                            NotificationHelper.showNotification(
                                    ResetPasswordActivity.this,
                                    "Password Reset Successful",
                                    "You can now log in with your new password",
                                    new Intent(ResetPasswordActivity.this, LoginActivity.class),
                                    2004
                            );
                            
                            // Navigate to login
                            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            // Password reset failed
                            Exception exception = task.getException();
                            String errorMessage = "Failed to reset password. Please try again.";
                            if (exception != null) {
                                errorMessage = exception.getMessage();
                                if (errorMessage != null && errorMessage.contains("expired")) {
                                    errorMessage = "Reset link has expired. Please request a new one.";
                                } else if (errorMessage != null && errorMessage.contains("invalid")) {
                                    errorMessage = "Invalid reset code. Please request a new reset email.";
                                }
                            }
                            Toast.makeText(ResetPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    
    /**
     * Send password reset email
     * After clicking the link in email, user will come back here with actionCode
     */
    private void sendPasswordResetEmail(String email) {
        Log.d(TAG, "Attempting to send password reset email to: " + email);
        
        // Use Firebase default domain (no custom domain needed)
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        binding.buttonResetPassword.setEnabled(true);
                        binding.buttonResetPassword.setText(R.string.reset_password_button);
                        
                        if (task.isSuccessful()) {
                            // Email sent successfully
                            Log.d(TAG, "Password reset email sent successfully to: " + email);
                            Toast.makeText(ResetPasswordActivity.this,
                                    "Reset link sent to " + email + ". Please check your email (including spam folder) and click the link.",
                                    Toast.LENGTH_LONG).show();
                            
                            NotificationHelper.showNotification(
                                    ResetPasswordActivity.this,
                                    "Check Your Email",
                                    "Click the reset link in your email to continue. It will open the app automatically.",
                                    new Intent(ResetPasswordActivity.this, LoginActivity.class),
                                    2005
                            );
                            
                            // Update UI to show next steps
                            binding.textViewSubtitle.setText("Reset link sent to " + email + 
                                    "\n\nPlease check your inbox\n" +
                                    "Click the link in the email to continue.");
                            
                            
                        } else {
                            // Failed to send email - show detailed error
                            Exception exception = task.getException();
                            String errorMessage = "Failed to send reset email.";
                            String detailedError = "";
                            
                            if (exception != null) {
                                String exceptionMessage = exception.getMessage();
                                String exceptionClass = exception.getClass().getSimpleName();
                                
                                Log.e(TAG, "Failed to send password reset email", exception);
                                Log.e(TAG, "Exception class: " + exceptionClass);
                                Log.e(TAG, "Exception message: " + exceptionMessage);
                                
                                if (exceptionMessage != null) {
                                    detailedError = exceptionMessage;
                                    
                                    // Check for specific error types
                                    if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                        errorMessage = "No account found with this email address.";
                                        detailedError = "The email " + email + " is not registered in our system. Please sign up first or use a different email.";
                                    } else if (exceptionMessage.contains("domain") || exceptionMessage.contains("allowlist")) {
                                        errorMessage = "Firebase configuration issue.";
                                        detailedError = "Please check Firebase Console -> Authentication -> Settings -> Authorized domains";
                                    } else if (exceptionMessage.contains("network") || exceptionMessage.contains("timeout")) {
                                        errorMessage = "Network error.";
                                        detailedError = "Please check your internet connection and try again.";
                                    } else if (exceptionMessage.contains("too-many-requests")) {
                                        errorMessage = "Too many requests.";
                                        detailedError = "Please wait a few minutes before requesting another reset email.";
                                    } else {
                                        errorMessage = "Error sending reset email.";
                                        detailedError = exceptionMessage;
                                    }
                                }
                            }
                            
                            // Show detailed error in both UI and log
                            Log.e(TAG, "Error: " + errorMessage + " - " + detailedError);
                            binding.emailInputLayout.setError(errorMessage);
                            
                            // Show detailed error in toast
                            Toast.makeText(ResetPasswordActivity.this, 
                                    errorMessage + "\n" + detailedError, 
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    
    private boolean validateEmail() {
        // Skip email validation if it's hidden (when actionCode exists)
        if (binding.editTextEmail.getVisibility() != android.view.View.VISIBLE) {
            return true;
        }
        
        String email = binding.editTextEmail.getText() != null
                ? binding.editTextEmail.getText().toString().trim()
                : "";
        
        if (TextUtils.isEmpty(email)) {
            binding.emailInputLayout.setError(getString(R.string.error_email_required));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError(getString(R.string.error_email_invalid));
            return false;
        } else {
            binding.emailInputLayout.setError(null);
            binding.emailInputLayout.setErrorEnabled(false);
            return true;
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
    
    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // no-op
        }
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // no-op
        }
    }
}

