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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        
        // Set toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.chat_users_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        setupRecyclerView();
        
        // Add test users directly first (for immediate display)
        List<User> testUsers = new ArrayList<>();
        testUsers.add(new User("user1", "John Smith", "john.smith@example.com"));
        testUsers.add(new User("user2", "Sarah Johnson", "sarah.johnson@example.com"));
        testUsers.add(new User("user3", "Michael Brown", "michael.brown@example.com"));
        testUsers.add(new User("user4", "Emily Davis", "emily.davis@example.com"));
        testUsers.add(new User("user5", "David Wilson", "david.wilson@example.com"));
        
        // Filter out current user
        List<User> filteredTestUsers = new ArrayList<>();
        for (User user : testUsers) {
            if (currentUserId == null || !user.getUserId().equals(currentUserId)) {
                filteredTestUsers.add(user);
            }
        }
        
        if (!filteredTestUsers.isEmpty()) {
            usersAdapter.setUsers(filteredTestUsers);
            binding.textNoUsers.setVisibility(View.GONE);
            binding.recyclerViewUsers.setVisibility(View.VISIBLE);
            android.util.Log.d("UsersListActivity", "✓ Displayed " + filteredTestUsers.size() + " test users");
        }
        
        // Also try loading XML users (may merge with test users later)
        loadAndDisplayXmlUsers();
        
        // Refresh button
        if (binding.buttonRefresh != null) {
            binding.buttonRefresh.setOnClickListener(v -> {
                Toast.makeText(this, "Refreshing users...", Toast.LENGTH_SHORT).show();
                loadUsers(); // This will save current user first, then load all users
            });
        }
        
        // Then load from Firebase and merge with XML users (this happens in background)
        loadUsers();
    }
    
    /**
     * Load users from XML immediately and merge with existing users
     */
    private void loadAndDisplayXmlUsers() {
        android.util.Log.d("UsersListActivity", "=== Loading XML users and merging ===");
        try {
            List<User> xmlUsers = loadUsersFromXml();
            android.util.Log.d("UsersListActivity", "XML users loaded: " + (xmlUsers != null ? xmlUsers.size() : 0));
            
            if (xmlUsers != null && !xmlUsers.isEmpty()) {
                // Get existing users from adapter
                List<User> existingUsers = new ArrayList<>();
                int currentCount = usersAdapter.getItemCount();
                android.util.Log.d("UsersListActivity", "Current users in adapter: " + currentCount);
                
                // Filter out current user from XML users
                List<User> filteredXmlUsers = new ArrayList<>();
                for (User user : xmlUsers) {
                    if (currentUserId == null || !user.getUserId().equals(currentUserId)) {
                        filteredXmlUsers.add(user);
                        android.util.Log.d("UsersListActivity", "Added XML user: " + user.getName());
                    }
                }
                
                // Merge: add XML users that don't already exist
                Map<String, User> userMap = new HashMap<>();
                
                // Add existing users first (if any)
                for (int i = 0; i < currentCount; i++) {
                    // We can't get user from adapter directly, so we'll just merge XML users
                }
                
                // Add all XML users (they'll replace any duplicates by userId)
                for (User user : filteredXmlUsers) {
                    userMap.put(user.getUserId(), user);
                }
                
                List<User> mergedUsers = new ArrayList<>(userMap.values());
                
                if (!mergedUsers.isEmpty()) {
                    // Set merged users to adapter
                    usersAdapter.setUsers(mergedUsers);
                    
                    // Make sure RecyclerView is visible
                    binding.textNoUsers.setVisibility(View.GONE);
                    binding.recyclerViewUsers.setVisibility(View.VISIBLE);
                    
                    android.util.Log.d("UsersListActivity", "✓ SUCCESS: Displayed " + mergedUsers.size() + " users (merged)");
                    android.util.Log.d("UsersListActivity", "RecyclerView visibility: " + binding.recyclerViewUsers.getVisibility());
                    android.util.Log.d("UsersListActivity", "Adapter item count: " + usersAdapter.getItemCount());
                } else {
                    android.util.Log.w("UsersListActivity", "No users after merge - keeping existing users");
                    // Don't hide RecyclerView if we already have users showing
                }
            } else {
                android.util.Log.w("UsersListActivity", "No XML users loaded - keeping existing users");
                // Don't change visibility if we already have users
            }
        } catch (Exception e) {
            android.util.Log.e("UsersListActivity", "ERROR loading XML users: " + e.getMessage(), e);
            // Don't hide users if XML loading fails - keep existing users
        }
        android.util.Log.d("UsersListActivity", "=== End XML loading ===");
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
        
        // RecyclerView is visible by default in XML, will show users when loaded
        binding.textNoUsers.setVisibility(View.GONE);
    }
    
    private void loadUsers() {
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        // Remove existing listener if any
        if (usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
        
        android.util.Log.d("UsersListActivity", "Starting to load users from Firebase...");
        
        // Ensure current user is saved to database first, THEN load users
        saveCurrentUserToDatabase(() -> {
            // Callback - after current user is saved, load all users
            android.util.Log.d("UsersListActivity", "Current user saved, now loading all users...");
            loadUsersFromFirebase();
        });
    }
    
    private void loadUsersFromFirebase() {
        if (usersRef == null) {
            usersRef = FirebaseDatabase.getInstance().getReference("users");
        }
        
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                
                android.util.Log.d("UsersListActivity", "=== Loading Users from Firebase ===");
                android.util.Log.d("UsersListActivity", "Snapshot exists: " + snapshot.exists());
                android.util.Log.d("UsersListActivity", "Children count: " + snapshot.getChildrenCount());
                android.util.Log.d("UsersListActivity", "Current user ID: " + currentUserId);
                
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    android.util.Log.d("UsersListActivity", "No users found in Firebase database, will try XML fallback");
                    // Continue to load XML users below
                }
                
                // Log all user keys for debugging
                android.util.Log.d("UsersListActivity", "=== All Users in Database ===");
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    android.util.Log.d("UsersListActivity", "User ID: " + userId);
                    android.util.Log.d("UsersListActivity", "User data: " + userSnapshot.getValue());
                }
                android.util.Log.d("UsersListActivity", "=== End User List ===");
                
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    android.util.Log.d("UsersListActivity", "Processing user: " + userId);
                    
                    // Skip current user
                    if (userId != null && userId.equals(currentUserId)) {
                        android.util.Log.d("UsersListActivity", "Skipped current user: " + userId);
                        continue;
                    }
                    
                    // Get user data - try multiple methods
                    String name = "Unknown User";
                    String email = "";
                    
                    // Method 1: Try to get User object
                    try {
                        User userObj = userSnapshot.getValue(User.class);
                        if (userObj != null) {
                            if (userObj.getName() != null && !userObj.getName().isEmpty()) {
                                name = userObj.getName();
                            }
                            if (userObj.getEmail() != null && !userObj.getEmail().isEmpty()) {
                                email = userObj.getEmail();
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.w("UsersListActivity", "Could not parse User object: " + e.getMessage());
                    }
                    
                    // Method 2: Get fields directly from snapshot
                    if (name.equals("Unknown User") || email.isEmpty()) {
                        // Try to get name
                        DataSnapshot nameSnapshot = userSnapshot.child("name");
                        if (nameSnapshot.exists() && nameSnapshot.getValue() != null) {
                            name = nameSnapshot.getValue().toString();
                        }
                        
                        // Try to get email
                        DataSnapshot emailSnapshot = userSnapshot.child("email");
                        if (emailSnapshot.exists() && emailSnapshot.getValue() != null) {
                            email = emailSnapshot.getValue().toString();
                        }
                    }
                    
                    // Method 3: If still no name, use email prefix or default
                    if (name.equals("Unknown User")) {
                        if (!email.isEmpty()) {
                            name = email.split("@")[0]; // Use email prefix as name
                        } else {
                            name = "User " + userId.substring(0, Math.min(8, userId.length())); // Use part of user ID
                        }
                    }
                    
                    // Create user object
                    User user = new User(userId, name, email);
                    users.add(user);
                    android.util.Log.d("UsersListActivity", "✓ Added user: " + name + " (" + email + ")");
                }
                
                android.util.Log.d("UsersListActivity", "=== Summary ===");
                android.util.Log.d("UsersListActivity", "Total users found: " + users.size());
                android.util.Log.d("UsersListActivity", "=== End Summary ===");
                
                // Always load XML users and merge with Firebase users
                android.util.Log.d("UsersListActivity", "Loading XML users and merging...");
                List<User> xmlUsers = loadUsersFromXml();
                
                // Create a map to avoid duplicates
                Map<String, User> userMap = new HashMap<>();
                
                // Add Firebase users first (they take priority)
                for (User user : users) {
                    userMap.put(user.getUserId(), user);
                }
                
                // Add XML users if not already present and not current user
                for (User xmlUser : xmlUsers) {
                    if (!userMap.containsKey(xmlUser.getUserId()) && !xmlUser.getUserId().equals(currentUserId)) {
                        userMap.put(xmlUser.getUserId(), xmlUser);
                        android.util.Log.d("UsersListActivity", "Added XML user: " + xmlUser.getName());
                    }
                }
                
                // Convert back to list
                users = new ArrayList<>(userMap.values());
                
                android.util.Log.d("UsersListActivity", "=== Final Summary ===");
                android.util.Log.d("UsersListActivity", "Total users after merge: " + users.size());
                android.util.Log.d("UsersListActivity", "=== End Summary ===");
                
                // Update adapter with merged users
                usersAdapter.setUsers(users);
                
                if (users.isEmpty()) {
                    binding.textNoUsers.setVisibility(View.VISIBLE);
                    binding.recyclerViewUsers.setVisibility(View.GONE);
                    binding.textNoUsers.setText("No other users found.\n\nTo see users:\n1. Create multiple accounts by signing up\n2. Or login with different accounts\n3. Each user will appear here automatically");
                } else {
                    binding.textNoUsers.setVisibility(View.GONE);
                    binding.recyclerViewUsers.setVisibility(View.VISIBLE);
                    String message = "Found " + users.size() + " user(s)";
                    Toast.makeText(UsersListActivity.this, message, Toast.LENGTH_SHORT).show();
                    android.util.Log.d("UsersListActivity", "Users displayed successfully");
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("UsersListActivity", "Error loading users from Firebase: " + error.getMessage());
                
                // Try loading from XML as fallback
                android.util.Log.d("UsersListActivity", "Loading users from XML as fallback...");
                List<User> xmlUsers = loadUsersFromXml();
                
                if (!xmlUsers.isEmpty()) {
                    // Filter out current user from XML users
                    List<User> filteredUsers = new ArrayList<>();
                    for (User user : xmlUsers) {
                        if (!user.getUserId().equals(currentUserId)) {
                            filteredUsers.add(user);
                        }
                    }
                    
                    usersAdapter.setUsers(filteredUsers);
                    binding.textNoUsers.setVisibility(View.GONE);
                    binding.recyclerViewUsers.setVisibility(View.VISIBLE);
                    Toast.makeText(UsersListActivity.this, "Loaded " + filteredUsers.size() + " user(s) from local storage (Firebase unavailable)", Toast.LENGTH_LONG).show();
                    android.util.Log.d("UsersListActivity", "Loaded " + filteredUsers.size() + " users from XML fallback");
                } else {
                    Toast.makeText(UsersListActivity.this, "Failed to load users: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    binding.textNoUsers.setVisibility(View.VISIBLE);
                    binding.recyclerViewUsers.setVisibility(View.GONE);
                    binding.textNoUsers.setText("Error loading users.\n\n" + error.getMessage() + "\n\nCheck your internet connection and Firebase database rules.");
                }
            }
        };
        
        usersRef.addValueEventListener(usersListener);
    }
    
    /**
     * Save current user to Firebase database to ensure they're in the users list
     * @param onComplete Callback to execute after save completes (or immediately if user is null)
     */
    private void saveCurrentUserToDatabase(Runnable onComplete) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        if (currentUser == null) {
            android.util.Log.e("UsersListActivity", "Current user is null, cannot save to database");
            if (onComplete != null) {
                onComplete.run();
            }
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
            email = currentUser.getEmail(); // Try again
            if (email == null) {
                email = "";
            }
        }
        
        User user = new User(userId, name, email);
        android.util.Log.d("UsersListActivity", "Saving current user to database: " + userId + ", Name: " + name + ", Email: " + email);
        
        if (usersRef == null) {
            usersRef = FirebaseDatabase.getInstance().getReference("users");
        }
        
        usersRef.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("UsersListActivity", "Current user saved successfully to database");
                    } else {
                        android.util.Log.e("UsersListActivity", "Failed to save current user", task.getException());
                        if (task.getException() != null) {
                            android.util.Log.e("UsersListActivity", "Error: " + task.getException().getMessage());
                        }
                    }
                    // Always call onComplete, even if save failed (users might still exist in DB)
                    if (onComplete != null) {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UsersListActivity", "Error saving current user", e);
                    // Still call onComplete even on failure (users might still exist in DB)
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }
    
    /**
     * Load users from XML resource file (res/xml/users.xml)
     * Used as fallback when Firebase is unavailable or empty
     */
    private List<User> loadUsersFromXml() {
        List<User> users = new ArrayList<>();
        
        try {
            android.util.Log.d("UsersListActivity", "Starting XML parsing...");
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            
            android.util.Log.d("UsersListActivity", "Opening XML resource: R.xml.users");
            InputStream inputStream = getResources().openRawResource(R.xml.users);
            if (inputStream == null) {
                android.util.Log.e("UsersListActivity", "ERROR: inputStream is null!");
                return users;
            }
            
            parser.setInput(inputStream, "UTF-8");
            
            String userId = null;
            String name = null;
            String email = null;
            String currentTag = null;
            int eventType = parser.getEventType();
            int userCount = 0;
            
            android.util.Log.d("UsersListActivity", "Starting to parse XML...");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("user".equals(tagName)) {
                            // Reset for new user
                            userId = null;
                            name = null;
                            email = null;
                            userCount++;
                            android.util.Log.d("UsersListActivity", "Found user tag #" + userCount);
                        } else if (tagName != null && !tagName.isEmpty()) {
                            currentTag = tagName;
                            android.util.Log.d("UsersListActivity", "Found tag: " + tagName);
                        }
                        break;
                        
                    case XmlPullParser.TEXT:
                        String text = parser.getText();
                        if (text != null) {
                            text = text.trim();
                            if (!text.isEmpty() && currentTag != null) {
                                android.util.Log.d("UsersListActivity", "Tag: " + currentTag + ", Value: " + text);
                                if ("userId".equals(currentTag)) {
                                    userId = text;
                                } else if ("name".equals(currentTag)) {
                                    name = text;
                                } else if ("email".equals(currentTag)) {
                                    email = text;
                                }
                            }
                        }
                        break;
                        
                    case XmlPullParser.END_TAG:
                        if ("user".equals(tagName)) {
                            // Create user if all fields are present
                            android.util.Log.d("UsersListActivity", "End user tag - userId: " + userId + ", name: " + name + ", email: " + email);
                            if (userId != null && name != null && email != null) {
                                User user = new User(userId, name, email);
                                users.add(user);
                                android.util.Log.d("UsersListActivity", "✓ Added user: " + name + " (" + email + ")");
                            } else {
                                android.util.Log.w("UsersListActivity", "⚠ Incomplete user data - userId: " + userId + ", name: " + name + ", email: " + email);
                            }
                            // Reset fields
                            userId = null;
                            name = null;
                            email = null;
                        }
                        currentTag = null;
                        break;
                }
                
                eventType = parser.next();
            }
            
            inputStream.close();
            android.util.Log.d("UsersListActivity", "✓ XML parsing complete. Total users loaded: " + users.size());
            
        } catch (XmlPullParserException e) {
            android.util.Log.e("UsersListActivity", "❌ XML Parse Error: " + e.getMessage(), e);
            e.printStackTrace();
        } catch (IOException e) {
            android.util.Log.e("UsersListActivity", "❌ IO Error reading XML: " + e.getMessage(), e);
            e.printStackTrace();
        } catch (Exception e) {
            android.util.Log.e("UsersListActivity", "❌ Unexpected error loading XML: " + e.getMessage(), e);
            e.printStackTrace();
        }
        
        return users;
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

