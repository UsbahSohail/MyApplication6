package com.example.myapplication.ui.chatbot;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.GeminiChatService;
import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentChatbotBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Chatbot Fragment using Google Gemini
 * Provides user assistance with app-related queries
 */
public class ChatbotFragment extends Fragment {
    
    private FragmentChatbotBinding binding;
    private ChatbotAdapter chatbotAdapter;
    private GeminiChatService chatService;
    private List<ChatbotMessage> messages;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatbotBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        messages = new ArrayList<>();
        
        // Initialize Gemini Chat Service
        chatService = new GeminiChatService(requireContext());
        
        // Setup RecyclerView
        chatbotAdapter = new ChatbotAdapter();
        binding.recyclerViewMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewMessages.setAdapter(chatbotAdapter);
        
        // Auto-scroll to bottom when new message is added
        chatbotAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int messageCount = chatbotAdapter.getItemCount();
                if (messageCount > 0) {
                    binding.recyclerViewMessages.smoothScrollToPosition(messageCount - 1);
                }
            }
        });
        
        // Check API key configuration
        if (!chatService.isApiKeyConfigured()) {
            showMessage("Error: Please configure your Gemini API key in local.properties to use this feature.", false);
            Toast.makeText(requireContext(), "API key not configured. Check local.properties", Toast.LENGTH_LONG).show();
        } else {
            // Show welcome message
            showWelcomeMessage();
        }
        
        // Send button click listener
        binding.buttonSend.setOnClickListener(v -> sendMessage());
        
        // Enter key listener for message input
        binding.editTextMessage.setOnEditorActionListener((v, actionId, event) -> {
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
        
        showMessage(welcomeMessage, false);
    }
    
    private void sendMessage() {
        String message = binding.editTextMessage.getText().toString().trim();
        
        if (TextUtils.isEmpty(message)) {
            return;
        }
        
        // Add user message to chat
        showMessage(message, true);
        binding.editTextMessage.setText("");
        
        // Show loading indicator
        setLoading(true);
        
        // Disable send button while processing
        binding.buttonSend.setEnabled(false);
        
        // Send message to AI
        chatService.sendMessage(message, new GeminiChatService.ChatCallback() {
            @Override
            public void onResponse(String response) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    binding.buttonSend.setEnabled(true);
                    showMessage(response, false);
                });
            }
            
            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    binding.buttonSend.setEnabled(true);
                    showMessage("Error: " + error, false);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showMessage(String message, boolean isUser) {
        ChatbotMessage chatMessage = new ChatbotMessage(message, isUser, System.currentTimeMillis());
        messages.add(chatMessage);
        chatbotAdapter.setMessages(messages);
    }
    
    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonSend.setEnabled(!loading);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

