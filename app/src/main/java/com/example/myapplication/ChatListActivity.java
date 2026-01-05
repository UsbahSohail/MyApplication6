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

        // Load users from database
        loadUsers();

        // Handle user click - open chat
        listViewUsers.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = userList.get(position);
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("receiverId", selectedUser.getUserId());
            intent.putExtra("receiverName", selectedUser.getName());
            startActivity(intent);
        });
    }

    private void loadUsers() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();
                String currentUserId = currentUser.getUid();
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getUserId() != null && !user.getUserId().equals(currentUserId)) {
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatListActivity.this, "Error loading users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
            usersRef.child(userId).setValue(user);
        }
    }
}

