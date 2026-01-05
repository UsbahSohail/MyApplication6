package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private ListView listViewMessages;
    private EditText editTextMessage;
    private Button buttonSend;
    
    private ArrayAdapter<String> adapter;
    private List<String> messageList;
    
    private DatabaseReference messagesRef;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    
    private String receiverId;
    private String receiverName;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get receiver info from intent
        receiverId = getIntent().getStringExtra("receiverId");
        receiverName = getIntent().getStringExtra("receiverName");

        if (receiverId == null || receiverId.isEmpty()) {
            Toast.makeText(this, "Receiver ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(receiverName != null ? receiverName : "Chat");
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

        // Initialize views
        listViewMessages = findViewById(R.id.listViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        messageList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageList);
        listViewMessages.setAdapter(adapter);

        // Generate chat ID (sorted to ensure same chat for both users)
        String senderId = currentUser.getUid();
        chatId = senderId.compareTo(receiverId) < 0 
                ? senderId + "_" + receiverId 
                : receiverId + "_" + senderId;

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("chats").child(chatId).child("messages");

        // Load messages
        loadMessages();

        // Send button click
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Message message = dataSnapshot.getValue(Message.class);
                if (message != null) {
                    addMessageToView(message);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle message updates if needed
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle message removal if needed
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle message moves if needed
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatActivity.this, "Error loading messages: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMessageToView(Message message) {
        String senderId = currentUser.getUid();
        boolean isSender = message.getSenderId().equals(senderId);
        
        String senderName = isSender ? "You" : (receiverName != null ? receiverName : "User");
        
        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        String timestamp = sdf.format(new Date(message.getTimestamp()));
        
        // Format message display
        String messageText = senderName + ": " + message.getMessage() + "\n" + timestamp;
        
        messageList.add(messageText);
        adapter.notifyDataSetChanged();
        listViewMessages.setSelection(adapter.getCount() - 1);
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        String senderId = currentUser.getUid();
        long timestamp = System.currentTimeMillis();

        // Create message
        Message message = new Message(senderId, receiverId, messageText, timestamp);
        
        // Push message to database
        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            message.setMessageId(messageId);
            messagesRef.child(messageId).setValue(message)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            editTextMessage.setText("");
                            // Update last message in user chats
                            updateUserChats(messageText, timestamp);
                        } else {
                            Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateUserChats(String lastMessage, long timestamp) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        String senderId = currentUser.getUid();
        
        // Update sender's chat list
        DatabaseReference senderChatRef = database.getReference("userChats").child(senderId).child(chatId);
        Map<String, Object> senderChatData = new HashMap<>();
        senderChatData.put("lastMessage", lastMessage);
        senderChatData.put("timestamp", timestamp);
        senderChatData.put("receiverId", receiverId);
        senderChatRef.setValue(senderChatData);
        
        // Update receiver's chat list
        DatabaseReference receiverChatRef = database.getReference("userChats").child(receiverId).child(chatId);
        Map<String, Object> receiverChatData = new HashMap<>();
        receiverChatData.put("lastMessage", lastMessage);
        receiverChatData.put("timestamp", timestamp);
        receiverChatData.put("receiverId", senderId);
        receiverChatRef.setValue(receiverChatData);
    }
}

