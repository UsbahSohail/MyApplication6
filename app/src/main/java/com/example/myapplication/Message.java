package com.example.myapplication;

public class Message {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;

    @SuppressWarnings("unused")
    public Message() {
        // Default constructor required for Firebase Realtime Database deserialization
        // Firebase uses reflection to instantiate objects when reading from database
    }

    public Message(String senderId, String receiverId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
    }

    @SuppressWarnings("unused")
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    @SuppressWarnings("unused")
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    @SuppressWarnings("unused")
    public String getReceiverId() {
        return receiverId;
    }

    @SuppressWarnings("unused")
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    @SuppressWarnings("unused")
    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @SuppressWarnings("unused")
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
