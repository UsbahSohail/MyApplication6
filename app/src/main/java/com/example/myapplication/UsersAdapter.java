package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    
    private List<User> users;
    private OnUserClickListener listener;
    
    public interface OnUserClickListener {
        void onUserClick(User user);
    }
    
    public UsersAdapter(OnUserClickListener listener) {
        this.users = new ArrayList<>();
        this.listener = listener;
    }
    
    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, listener);
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }
    
    static class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewUserName;
        private TextView textViewUserEmail;
        
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUserName = itemView.findViewById(R.id.textViewUserName);
            textViewUserEmail = itemView.findViewById(R.id.textViewUserEmail);
        }
        
        public void bind(User user, OnUserClickListener listener) {
            // Display user name
            String name = user.getName();
            if (name == null || name.isEmpty()) {
                name = "Unknown User";
            }
            textViewUserName.setText(name);
            
            // Display user email
            String email = user.getEmail();
            if (email == null || email.isEmpty()) {
                // If no email, show user ID or placeholder
                String userId = user.getUserId();
                if (userId != null && !userId.isEmpty()) {
                    email = "ID: " + userId.substring(0, Math.min(12, userId.length())) + "...";
                } else {
                    email = "No email";
                }
            }
            textViewUserEmail.setText(email);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}

