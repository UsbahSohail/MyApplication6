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
        // Hugging Face Object Detection Card
        if (binding.cardObjectDetection != null) {
            binding.cardObjectDetection.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ObjectDetectionActivity.class);
                startActivity(intent);
            });
        }
        
        // AI Chatbot Card
        if (binding.cardAIChatbot != null) {
            binding.cardAIChatbot.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AIChatbotActivity.class);
                startActivity(intent);
            });
        }
        
        // Chat with Users Card
        if (binding.cardChatUsers != null) {
            binding.cardChatUsers.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), UsersListActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}