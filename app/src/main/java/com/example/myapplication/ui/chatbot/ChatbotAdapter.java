package com.example.myapplication.ui.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying chatbot messages
 * Uses the same layout pattern as the chat feature
 */
public class ChatbotAdapter extends RecyclerView.Adapter<ChatbotAdapter.MessageViewHolder> {
    
    private static final int MESSAGE_TYPE_SENT = 1;
    private static final int MESSAGE_TYPE_RECEIVED = 2;
    
    private List<ChatbotMessage> messages;
    
    public ChatbotAdapter() {
        this.messages = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MESSAGE_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatbotMessage message = messages.get(position);
        holder.bind(message);
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        ChatbotMessage message = messages.get(position);
        return message.isUser() ? MESSAGE_TYPE_SENT : MESSAGE_TYPE_RECEIVED;
    }
    
    public void addMessage(ChatbotMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void setMessages(List<ChatbotMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }
    
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timestampText;
        TextView senderNameText;
        
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
            timestampText = itemView.findViewById(R.id.textTimestamp);
            senderNameText = itemView.findViewById(R.id.textSenderName);
        }
        
        void bind(ChatbotMessage message) {
            messageText.setText(message.getMessage());
            
            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String time = sdf.format(new Date(message.getTimestamp()));
            timestampText.setText(time);
            
            // Show sender name only for received messages (AI Assistant)
            if (senderNameText != null) {
                if (message.isUser()) {
                    senderNameText.setVisibility(View.GONE);
                } else {
                    senderNameText.setVisibility(View.VISIBLE);
                    senderNameText.setText("AI Assistant");
                }
            }
        }
    }
}

