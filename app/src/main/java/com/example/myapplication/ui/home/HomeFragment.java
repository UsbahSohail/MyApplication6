package com.example.myapplication.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.AIChatbotActivity;
import com.example.myapplication.ObjectDetectionActivity;
import com.example.myapplication.UsersListActivity;
import com.example.myapplication.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        
        // Setup click listeners for feature cards
        setupCardClickListeners();
        
        return root;
    }
    
    private void setupCardClickListeners() {
        android.util.Log.d("HomeFragment", "Setting up card click listeners...");
        
        // Hugging Face Object Detection Card
        if (binding.cardObjectDetection != null) {
            android.util.Log.d("HomeFragment", "✓ Found Hugging Face card");
            binding.cardObjectDetection.setOnClickListener(v -> {
                android.util.Log.d("HomeFragment", "Hugging Face card clicked!");
                Intent intent = new Intent(getContext(), ObjectDetectionActivity.class);
                startActivity(intent);
            });
        } else {
            android.util.Log.e("HomeFragment", "❌ Hugging Face card is NULL!");
        }
        
        // AI Chatbot Card
        if (binding.cardAIChatbot != null) {
            android.util.Log.d("HomeFragment", "✓ Found AI Chatbot card");
            binding.cardAIChatbot.setOnClickListener(v -> {
                android.util.Log.d("HomeFragment", "AI Chatbot card clicked!");
                Intent intent = new Intent(getContext(), AIChatbotActivity.class);
                startActivity(intent);
            });
        } else {
            android.util.Log.e("HomeFragment", "❌ AI Chatbot card is NULL!");
        }
        
        // Chat with Users Card
        if (binding.cardChatUsers != null) {
            android.util.Log.d("HomeFragment", "✓ Found Chat Users card");
            binding.cardChatUsers.setOnClickListener(v -> {
                android.util.Log.d("HomeFragment", "Chat Users card clicked!");
                Intent intent = new Intent(getContext(), UsersListActivity.class);
                startActivity(intent);
            });
        } else {
            android.util.Log.e("HomeFragment", "❌ Chat Users card is NULL!");
        }
        
        android.util.Log.d("HomeFragment", "Finished setting up card click listeners");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}