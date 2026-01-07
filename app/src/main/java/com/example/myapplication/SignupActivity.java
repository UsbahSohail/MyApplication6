package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnSignup;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvLogin = findViewById(R.id.tvLoginLink);

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Enter your name");
                etName.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter email");
                etEmail.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Enter a password");
                etPassword.requestFocus();
                return;
            }

            // Validate password length (Firebase requires minimum 6 characters)
            if (password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return;
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            // Create user with Firebase Authentication
            btnSignup.setEnabled(false);
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        btnSignup.setEnabled(true);
                        if (task.isSuccessful()) {
                            // Sign up success, update user profile with name
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(updateTask -> {
                                            // Save user to Firebase Realtime Database
                                            saveUserToDatabase(user, name, email);
                                            
                                            Toast.makeText(SignupActivity.this, 
                                                "Account created! Welcome, " + name, 
                                                Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignupActivity.this, ProductActivity.class));
                                            finish();
                                        });
                            }
                        } else {
                            // Sign up failed
                            String errorMessage = "Signup failed. Please try again.";
                            if (task.getException() != null) {
                                String exceptionMessage = task.getException().getMessage();
                                if (exceptionMessage != null) {
                                    if (exceptionMessage.contains("email address is already in use")) {
                                        errorMessage = "This email is already registered. Please login.";
                                    } else if (exceptionMessage.contains("invalid email")) {
                                        errorMessage = "Invalid email address. Please check and try again.";
                                    } else if (exceptionMessage.contains("weak password")) {
                                        errorMessage = "Password is too weak. Please use a stronger password.";
                                    }
                                }
                            }
                            Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
    
    /**
     * Save user to Firebase Realtime Database
     */
    private void saveUserToDatabase(FirebaseUser firebaseUser, String name, String email) {
        if (firebaseUser == null) {
            android.util.Log.e("SignupActivity", "FirebaseUser is null, cannot save to database");
            return;
        }
        
        String userId = firebaseUser.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference usersRef = database.getReference("users");
        
        // Create User object
        User user = new User(userId, name, email);
        
        android.util.Log.d("SignupActivity", "Saving user to database - ID: " + userId + ", Name: " + name + ", Email: " + email);
        
        // Save to database
        usersRef.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("SignupActivity", "User saved successfully to database: " + userId);
                    } else {
                        android.util.Log.e("SignupActivity", "Failed to save user to database", task.getException());
                        if (task.getException() != null) {
                            android.util.Log.e("SignupActivity", "Error details: " + task.getException().getMessage());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SignupActivity", "Error saving user to database", e);
                });
    }
}
