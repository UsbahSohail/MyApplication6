package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private ListView listViewUsers;
    private ArrayAdapter<User> adapter;
    private List<User> userList;
    private DatabaseReference usersRef;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chat with Users");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Add refresh icon to toolbar
        toolbar.inflateMenu(R.menu.chat_list_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_refresh) {
                Toast.makeText(this, "Refreshing user list...", Toast.LENGTH_SHORT).show();
                saveCurrentUserToDatabase();
                // Force reload by removing and re-adding listener
                if (valueEventListener != null) {
                    usersRef.removeEventListener(valueEventListener);
                }
                loadUsers();
                return true;
            }
            return false;
        });

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listViewUsers = findViewById(R.id.listViewUsers);
        userList = new ArrayList<>();
        adapter = new ArrayAdapter<User>(this, R.layout.item_user, R.id.textViewUserName, userList) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                User user = userList.get(position);
                
                android.widget.TextView textViewName = view.findViewById(R.id.textViewUserName);
                android.widget.TextView textViewEmail = view.findViewById(R.id.textViewUserEmail);
                
                textViewName.setText(user.getName() != null ? user.getName() : "Unknown User");
                textViewEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                
                return view;
            }
        };
        listViewUsers.setAdapter(adapter);

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");

        // Save current user first, then load all users
        saveCurrentUserToDatabase();
        
        // Load users from database (with a small delay to ensure current user is saved)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            loadUsers();
        }, 500);

        // Handle user click - open chat
        listViewUsers.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userList.get(position);
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("receiverId", selectedUser.getUserId());
            intent.putExtra("receiverName", selectedUser.getName());
            startActivity(intent);
        });
    }
    
    private ValueEventListener valueEventListener;

    private void loadUsers() {
        android.util.Log.d("ChatListActivity", "Loading users from database...");
        android.util.Log.d("ChatListActivity", "Current user ID: " + (currentUser != null ? currentUser.getUid() : "null"));
        
        // Remove existing listener if any
        if (valueEventListener != null) {
            usersRef.removeEventListener(valueEventListener);
        }
        
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                android.util.Log.d("ChatListActivity", "onDataChange called. Has data: " + dataSnapshot.exists());
                userList.clear();
                String currentUserId = currentUser.getUid();
                
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    android.util.Log.d("ChatListActivity", "Found " + dataSnapshot.getChildrenCount() + " users in database");
                    int addedCount = 0;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);
                        android.util.Log.d("ChatListActivity", "Processing user: " + (user != null ? user.getUserId() : "null"));
                        if (user != null && user.getUserId() != null && !user.getUserId().equals(currentUserId)) {
                            // Ensure user has a name, use email if name is missing
                            if (user.getName() == null || user.getName().isEmpty()) {
                                String email = user.getEmail();
                                if (email != null && !email.isEmpty()) {
                                    user.setName(email.split("@")[0]);
                                } else {
                                    user.setName("User");
                                }
                            }
                            userList.add(user);
                            addedCount++;
                            android.util.Log.d("ChatListActivity", "Added user: " + user.getName() + " (" + user.getEmail() + ")");
                        }
                    }
                    android.util.Log.d("ChatListActivity", "Total users added to list: " + addedCount);
                } else {
                    android.util.Log.d("ChatListActivity", "No users found in database or database is empty");
                }
                
                adapter.notifyDataSetChanged();
                
                if (userList.isEmpty()) {
                    android.util.Log.d("ChatListActivity", "User list is empty after loading");
                    Toast.makeText(ChatListActivity.this, "No other users found. Make sure other users have signed up and logged in.", Toast.LENGTH_LONG).show();
                } else {
                    android.util.Log.d("ChatListActivity", "Successfully loaded " + userList.size() + " users");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                android.util.Log.e("ChatListActivity", "Error loading users. Code: " + databaseError.getCode() + ", Message: " + databaseError.getMessage());
                Toast.makeText(ChatListActivity.this, "Error loading users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        
        usersRef.addValueEventListener(valueEventListener);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener to prevent memory leaks
        if (valueEventListener != null && usersRef != null) {
            usersRef.removeEventListener(valueEventListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Ensure user data is saved to database
        saveCurrentUserToDatabase();
    }

    private void saveCurrentUserToDatabase() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String name = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            if (name == null || name.isEmpty()) {
                name = email != null ? email.split("@")[0] : "User";
            }

            User user = new User(userId, name, email);
            android.util.Log.d("ChatListActivity", "Saving current user to database: " + userId + ", Name: " + name + ", Email: " + email);
            
            usersRef.child(userId).setValue(user)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            android.util.Log.d("ChatListActivity", "Current user saved successfully to database");
                        } else {
                            android.util.Log.e("ChatListActivity", "Failed to save current user to database", task.getException());
                        }
                    });
        } else {
            android.util.Log.e("ChatListActivity", "Current user is null, cannot save to database");
        }
    }
}

