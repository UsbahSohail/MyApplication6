package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * AI Chatbot Activity using Google Gemini
 * Provides user assistance with app-related queries
 */
public class AIChatbotActivity extends AppCompatActivity {
    
    private TextView chatHistoryTextView;
    private EditText messageEditText;
    private Button sendButton;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    
    private GeminiChatService chatService;
    private List<ChatMessage> chatMessages;
    private FirebaseUser currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chatbot);
        
        // Initialize Firebase user
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AI Assistant");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Initialize views
        chatHistoryTextView = findViewById(R.id.chatHistoryTextView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);
        scrollView = findViewById(R.id.scrollView);
        
        chatMessages = new ArrayList<>();
        
        // Initialize Gemini Chat Service
        chatService = new GeminiChatService(this);
        
        // Check API key configuration
        if (!chatService.isApiKeyConfigured()) {
            showMessage("AI Assistant", "Please configure your Gemini API key in GeminiChatService.java to use this feature.", false);
            Toast.makeText(this, "API key not configured. Check GeminiChatService.java", Toast.LENGTH_LONG).show();
        } else {
            // Show welcome message
            showWelcomeMessage();
        }
        
        // Send button click listener
        sendButton.setOnClickListener(v -> sendMessage());
        
        // Enter key listener for message input
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }
    
    private void showWelcomeMessage() {
        String welcomeMessage = "Hello! I'm your AI assistant. I can help you with:\n\n" +
            "• Product information and recommendations\n" +
            "• App navigation and features\n" +
            "• How to use different screens\n" +
            "• General questions about the app\n\n" +
            "How can I assist you today?";
        
        showMessage("AI Assistant", welcomeMessage, false);
    }
    
    private void sendMessage() {
        String message = messageEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(message)) {
            return;
        }
        
        // Add user message to chat
        showMessage("You", message, true);
        messageEditText.setText("");
        
        // Show loading indicator
        setLoading(true);
        
        // Disable send button while processing
        sendButton.setEnabled(false);
        
        // Send message to AI
        chatService.sendMessage(message, new GeminiChatService.ChatCallback() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    sendButton.setEnabled(true);
                    showMessage("AI Assistant", response, false);
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    sendButton.setEnabled(true);
                    showMessage("Error", error, false);
                    Toast.makeText(AIChatbotActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showMessage(String sender, String message, boolean isUser) {
        ChatMessage chatMessage = new ChatMessage(sender, message, isUser, System.currentTimeMillis());
        chatMessages.add(chatMessage);
        
        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timestamp = sdf.format(new Date(chatMessage.getTimestamp()));
        
        // Build chat history
        StringBuilder chatHistory = new StringBuilder();
        for (ChatMessage msg : chatMessages) {
            String time = sdf.format(new Date(msg.getTimestamp()));
            if (msg.isUser()) {
                chatHistory.append("[").append(time).append("] ").append(msg.getSender())
                    .append(": ").append(msg.getMessage()).append("\n\n");
            } else {
                chatHistory.append("[").append(time).append("] ").append(msg.getSender())
                    .append(":\n").append(msg.getMessage()).append("\n\n");
            }
        }
        
        chatHistoryTextView.setText(chatHistory.toString());
        
        // Scroll to bottom
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
    
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!loading);
    }
    
    /**
     * Inner class to represent a chat message
     */
    private static class ChatMessage {
        private final String sender;
        private final String message;
        private final boolean isUser;
        private final long timestamp;
        
        public ChatMessage(String sender, String message, boolean isUser, long timestamp) {
            this.sender = sender;
            this.message = message;
            this.isUser = isUser;
            this.timestamp = timestamp;
        }
        
        public String getSender() { return sender; }
        public String getMessage() { return message; }
        public boolean isUser() { return isUser; }
        public long getTimestamp() { return timestamp; }
    }
}

