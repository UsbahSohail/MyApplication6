package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ActivityUsersListBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UsersListActivity extends AppCompatActivity {
    
    private ActivityUsersListBinding binding;
    private UsersAdapter usersAdapter;
    private DatabaseReference usersRef;
    private ValueEventListener usersListener;
    private String currentUserId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get current user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();
        
        // Save current user to database first (if not already saved)
        saveCurrentUserToDatabase(currentUser);
        
        // IMPORTANT: Users must exist in Realtime Database to show in list
        // If users are missing, login with each account to trigger saveUserToDatabase()
        // This happens automatically on login/signup
        
        // Set toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.chat_users_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        setupRecyclerView();
        
        // Show loading state initially
        binding.textNoUsers.setVisibility(View.VISIBLE);
        binding.recyclerViewUsers.setVisibility(View.GONE);
        binding.textNoUsers.setText("Loading users from Firebase...");
        
        // Refresh button
        if (binding.buttonRefresh != null) {
            binding.buttonRefresh.setOnClickListener(v -> {
                Toast.makeText(this, "Refreshing users...", Toast.LENGTH_SHORT).show();
                binding.textNoUsers.setVisibility(View.VISIBLE);
                binding.recyclerViewUsers.setVisibility(View.GONE);
                binding.textNoUsers.setText("Refreshing...");
                loadUsers();
            });
        }
        
        loadUsers();
    }
    
    private void setupRecyclerView() {
        usersAdapter = new UsersAdapter(new UsersAdapter.OnUserClickListener() {
            @Override
            public void onUserClick(User user) {
                openChat(user);
            }
        });
        binding.recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewUsers.setAdapter(usersAdapter);
    }
    
    private void loadUsers() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        android.util.Log.d("UsersListActivity", "Loading users from Firebase... Current user ID: " + currentUserId);
        
        // Remove existing listener if any
        if (usersListener != null && usersRef != null) {
            usersRef.removeEventListener(usersListener);
        }
        
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("USERS_DEBUG", "=== onDataChange called ===");
                android.util.Log.d("USERS_DEBUG", "Snapshot exists: " + snapshot.exists());
                android.util.Log.d("USERS_DEBUG", "Children count: " + snapshot.getChildrenCount());
                android.util.Log.d("USERS_DEBUG", "Current user ID: " + currentUserId);
                
                // Log raw snapshot data for debugging
                if (snapshot.exists()) {
                    android.util.Log.d("USERS_DEBUG", "Raw snapshot value: " + snapshot.getValue());
                }
                
                List<User> users = new ArrayList<>();
                
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    android.util.Log.w("UsersListActivity", "No users found in Firebase database");
                    runOnUiThread(() -> {
                        binding.textNoUsers.setVisibility(View.VISIBLE);
                        binding.recyclerViewUsers.setVisibility(View.GONE);
                        binding.textNoUsers.setText("No other users found.\n\nUsers will appear here when:\n• Other users sign up\n• Other users log in\n• They're saved to the Firebase database\n\nCreate multiple accounts to test!");
                    });
                    return;
                }
                
                int totalUsersFound = 0;
                int currentUserSkipped = 0;
                
                android.util.Log.d("USERS_DEBUG", "=== Processing users ===");
                
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    totalUsersFound++;
                    android.util.Log.d("USERS_DEBUG", "User #" + totalUsersFound + " - ID: " + userId);
                    android.util.Log.d("USERS_DEBUG", "Raw user data: " + userSnapshot.getValue());
                    
                    // Skip current user
                    if (userId != null && userId.equals(currentUserId)) {
                        currentUserSkipped++;
                        android.util.Log.d("UsersListActivity", "Skipped current user: " + userId);
                        continue;
                    }
                    
                    // Try to get User object directly first
                    String name = "Unknown";
                    String email = "";
                    
                    try {
                        User userObj = userSnapshot.getValue(User.class);
                        if (userObj != null) {
                            name = userObj.getName() != null ? userObj.getName() : "Unknown";
                            email = userObj.getEmail() != null ? userObj.getEmail() : "";
                            android.util.Log.d("UsersListActivity", "Got User object - name: " + name + ", email: " + email);
                        }
                    } catch (Exception e) {
                        android.util.Log.w("UsersListActivity", "Could not parse User object, trying direct access: " + e.getMessage());
                    }
                    
                    // If User object parsing failed, get fields directly
                    if (name.equals("Unknown") || email.isEmpty()) {
                        Object nameObj = userSnapshot.child("name").getValue();
                        Object emailObj = userSnapshot.child("email").getValue();
                        
                        if (nameObj != null) {
                            name = nameObj.toString();
                        }
                        if (emailObj != null) {
                            email = emailObj.toString();
                        }
                        android.util.Log.d("UsersListActivity", "Got direct fields - name: " + name + ", email: " + email);
                    }
                    
                    // If still no name, use email prefix
                    if ((name == null || name.isEmpty() || name.equals("Unknown")) && !email.isEmpty()) {
                        name = email.split("@")[0];
                    }
                    
                    if (name == null || name.isEmpty()) {
                        name = "User " + userId.substring(0, Math.min(8, userId.length()));
                    }
                    
                    User user = new User(userId, name, email);
                    users.add(user);
                    android.util.Log.d("UsersListActivity", "✓ Added user: " + name + " (" + email + ")");
                }
                
                android.util.Log.d("USERS_DEBUG", "=== Summary ===");
                android.util.Log.d("USERS_DEBUG", "Total users in Firebase: " + totalUsersFound);
                android.util.Log.d("USERS_DEBUG", "Current user skipped: " + currentUserSkipped);
                android.util.Log.d("USERS_DEBUG", "Other users to display: " + users.size());
                android.util.Log.d("USERS_DEBUG", "=== End Summary ===");
                
                // Build message outside lambda to make it final
                final int finalTotalUsersFound = totalUsersFound;
                final List<User> finalUsers = users;
                
                StringBuilder messageBuilder = new StringBuilder("No other users found.\n\n");
                if (finalTotalUsersFound == 0) {
                    messageBuilder.append("⚠️ No users in Firebase database.\n\n");
                    messageBuilder.append("Possible causes:\n");
                    messageBuilder.append("• Firebase rules blocking read\n");
                    messageBuilder.append("• Users not saved to database\n");
                    messageBuilder.append("• Wrong database path\n\n");
                } else if (finalTotalUsersFound == 1) {
                    messageBuilder.append("✅ Found 1 user (you).\n\n");
                } else {
                    messageBuilder.append("Found ").append(finalTotalUsersFound).append(" users, but all are filtered out.\n\n");
                }
                messageBuilder.append("To see users:\n");
                messageBuilder.append("• Create at least 2 accounts\n");
                messageBuilder.append("• Sign up with different emails\n");
                messageBuilder.append("• Check Firebase Console → Realtime Database");
                
                final String finalMessage = messageBuilder.toString();
                
                runOnUiThread(() -> {
                    usersAdapter.setUsers(finalUsers);
                    
                    if (finalUsers.isEmpty()) {
                        binding.textNoUsers.setVisibility(View.VISIBLE);
                        binding.recyclerViewUsers.setVisibility(View.GONE);
                        binding.textNoUsers.setText(finalMessage);
                        android.util.Log.d("USERS_DEBUG", "UI updated - showing empty state");
                    } else {
                        binding.textNoUsers.setVisibility(View.GONE);
                        binding.recyclerViewUsers.setVisibility(View.VISIBLE);
                        android.util.Log.d("USERS_DEBUG", "✅ UI updated - showing " + finalUsers.size() + " users");
                        Toast.makeText(UsersListActivity.this, "Loaded " + finalUsers.size() + " user(s)", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("USERS_DEBUG", "❌ onCancelled called!");
                android.util.Log.e("USERS_DEBUG", "Error code: " + error.getCode());
                android.util.Log.e("USERS_DEBUG", "Error message: " + error.getMessage());
                android.util.Log.e("USERS_DEBUG", "Error details: " + error.getDetails());
                
                runOnUiThread(() -> {
                    Toast.makeText(UsersListActivity.this, "Failed to load users: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    binding.textNoUsers.setVisibility(View.VISIBLE);
                    binding.recyclerViewUsers.setVisibility(View.GONE);
                    
                    String errorMessage = "Error loading users.\n\n";
                    errorMessage += "Code: " + error.getCode() + "\n";
                    errorMessage += "Message: " + error.getMessage() + "\n\n";
                    errorMessage += "Common fixes:\n";
                    errorMessage += "• Check Firebase database rules\n";
                    errorMessage += "• Verify internet connection\n";
                    errorMessage += "• Check Firebase Console → Realtime Database\n";
                    errorMessage += "• Ensure node name is 'users' (lowercase)";
                    
                    binding.textNoUsers.setText(errorMessage);
                });
            }
        };
        
        try {
            usersRef.addValueEventListener(usersListener);
            android.util.Log.d("UsersListActivity", "Firebase listener added successfully");
        } catch (Exception e) {
            android.util.Log.e("UsersListActivity", "Error adding Firebase listener: " + e.getMessage(), e);
            runOnUiThread(() -> {
                binding.textNoUsers.setVisibility(View.VISIBLE);
                binding.recyclerViewUsers.setVisibility(View.GONE);
                binding.textNoUsers.setText("Error connecting to Firebase.\n\n" + e.getMessage());
            });
        }
    }
    
    /**
     * Save current user to Firebase database to ensure they're in the users list
     */
    private void saveCurrentUserToDatabase(FirebaseUser currentUser) {
        if (currentUser == null) {
            android.util.Log.w("UsersListActivity", "Current user is null, cannot save to database");
            return;
        }
        
        String userId = currentUser.getUid();
        String name = currentUser.getDisplayName();
        String email = currentUser.getEmail();
        
        // If display name is not set, use email username
        if (name == null || name.isEmpty()) {
            name = email != null ? email.split("@")[0] : "User";
        }
        
        // Ensure email is set
        if (email == null || email.isEmpty()) {
            email = currentUser.getEmail();
            if (email == null) {
                email = "";
            }
        }
        
        User user = new User(userId, name, email);
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        android.util.Log.d("USERS_DEBUG", "=== Saving current user to database ===");
        android.util.Log.d("USERS_DEBUG", "User ID: " + userId);
        android.util.Log.d("USERS_DEBUG", "Name: " + name);
        android.util.Log.d("USERS_DEBUG", "Email: " + email);
        android.util.Log.d("USERS_DEBUG", "Database path: users/" + userId);
        
        usersRef.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("USERS_DEBUG", "✅ User saved successfully to Firebase!");
                    runOnUiThread(() -> {
                        Toast.makeText(UsersListActivity.this, "User saved to database", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("USERS_DEBUG", "❌ Save failed: " + e.getMessage());
                    android.util.Log.e("USERS_DEBUG", "Error class: " + e.getClass().getSimpleName());
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(UsersListActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                });
    }
    
    private void openChat(User user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("receiverId", user.getUserId());
        intent.putExtra("receiverName", user.getName());
        startActivity(intent);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
    }
}
