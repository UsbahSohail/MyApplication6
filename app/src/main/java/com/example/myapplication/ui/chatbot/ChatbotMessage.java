package com.example.myapplication.ui.chatbot;

/**
 * Represents a message in the chatbot conversation
 */
public class ChatbotMessage {
    private final String message;
    private final boolean isUser;
    private final long timestamp;
    
    public ChatbotMessage(String message, boolean isUser, long timestamp) {
        this.message = message;
        this.isUser = isUser;
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public boolean isUser() {
        return isUser;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}

