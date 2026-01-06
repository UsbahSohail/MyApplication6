package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service class for handling Google Gemini AI chatbot interactions
 * Provides natural language responses with app-specific context
 */
public class GeminiChatService {
    private static final String TAG = "GeminiChatService";
    // IMPORTANT: Replace "YOUR_API_KEY_HERE" below with your actual Gemini API key
    // Get your free API key from: https://aistudio.google.com/app/apikey
    // Example: private static final String API_KEY_PLACEHOLDER = "AIzaSyAbCdEfGhIjKlMnOpQrStUvWxYz";
    private static final String API_KEY_PLACEHOLDER = "AIzaSyBKzNu_MNDcWJRZeKsDsci-yM4ZSDxMsac";
    
    private GenerativeModel model;
    private ChatFutures chat;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // App context for tokenization and better responses
    private static final String APP_CONTEXT = 
        "You are a helpful AI assistant for an e-commerce mobile application similar to Amazon. " +
        "The app has the following features:\n" +
        "- Product Catalog: Users can browse products like Echo Dot (₹4,499), Fire TV Stick (₹3,999), " +
        "Kindle Paperwhite (₹13,999), Amazon Basics Mouse (₹699), Alexa Smart Plug (₹1,999), " +
        "Amazon Gift Cards (₹500-₹5,000), Wireless Chargers (₹1,299), Amazon Hoodies (₹2,299), " +
        "Bluetooth Speakers (₹1,799), and USB-C Cables (₹499).\n" +
        "- User Authentication: Login and Signup functionality with Firebase.\n" +
        "- Chat System: Users can chat with each other.\n" +
        "- QR Code Scanner: For scanning QR codes.\n" +
        "- Navigation Components: Drawer navigation, fragments, and various app screens.\n" +
        "- User Profile: User information forms and data management.\n\n" +
        "When users ask questions about the app, provide helpful, concise answers. " +
        "For product-related queries, mention available products and their prices. " +
        "For app navigation questions, explain how to access different features. " +
        "Keep responses natural and conversational. " +
        "If asked about something not in the app, politely redirect to app features.";
    
    public interface ChatCallback {
        void onResponse(String response);
        void onError(String error);
    }
    
    public GeminiChatService(Context context) {
        initializeModel();
    }
    
    private void initializeModel() {
        try {
            // Initialize Generative Model with Gemini Pro
            model = new GenerativeModel(
                "gemini-pro",
                API_KEY_PLACEHOLDER
            );
            
            GenerativeModelFutures modelFutures = GenerativeModelFutures.from(model);
            
            // Start chat session
            chat = modelFutures.startChat();
            
            // Send initial context as first message
            if (chat != null) {
                String initialPrompt = APP_CONTEXT + "\n\nPlease acknowledge that you understand your role as an AI assistant for this e-commerce app.";
                chat.sendMessage(initialPrompt);
            }
            
            Log.d(TAG, "Gemini model initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Gemini model", e);
        }
    }
    
    /**
     * Send a message to the AI chatbot
     * @param message User's message
     * @param callback Callback for response or error
     */
    public void sendMessage(String message, ChatCallback callback) {
        if (model == null || chat == null) {
            callback.onError("AI service not initialized. Please check API key configuration.");
            return;
        }
        
        executor.execute(() -> {
            try {
                // Tokenize and process the message for better context understanding
                String processedMessage = processMessage(message);
                
                // Send message to Gemini
                GenerateContentResponse response = chat.sendMessage(processedMessage).get();
                
                // Extract response text
                String responseText = response != null ? response.getText() : null;
                
                if (responseText != null && !responseText.isEmpty()) {
                    mainHandler.post(() -> callback.onResponse(responseText));
                } else {
                    mainHandler.post(() -> callback.onError("No response from AI"));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message to Gemini", e);
                mainHandler.post(() -> {
                    String errorMsg = "Error: " + (e.getMessage() != null ? e.getMessage() : "Unknown error");
                    if (errorMsg.contains("API") || errorMsg.contains("key") || 
                        errorMsg.contains("401") || errorMsg.contains("403")) {
                        errorMsg = "API key not configured or invalid. Please set your Gemini API key in GeminiChatService.java. " +
                                   "Get your API key from: https://makersuite.google.com/app/apikey";
                    }
                    callback.onError(errorMsg);
                });
            }
        });
    }
    
    /**
     * Process and tokenize the message for better context understanding
     * This helps the AI understand app-specific queries
     */
    private String processMessage(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Build enhanced message with context hints (tokenization)
        StringBuilder processed = new StringBuilder(message);
        
        // Product-related keywords
        if (lowerMessage.contains("product") || lowerMessage.contains("item") || 
            lowerMessage.contains("buy") || lowerMessage.contains("purchase") ||
            lowerMessage.contains("price") || lowerMessage.contains("cost")) {
            processed.append(" [Context: Product Catalog Query]");
        }
        
        // Navigation-related keywords
        if (lowerMessage.contains("navigate") || lowerMessage.contains("go to") || 
            lowerMessage.contains("screen") || lowerMessage.contains("menu") ||
            lowerMessage.contains("how to") || lowerMessage.contains("where")) {
            processed.append(" [Context: App Navigation Query]");
        }
        
        // Chat-related keywords
        if (lowerMessage.contains("chat") || lowerMessage.contains("message") || 
            lowerMessage.contains("talk") || lowerMessage.contains("contact")) {
            processed.append(" [Context: Chat Feature Query]");
        }
        
        // Authentication-related keywords
        if (lowerMessage.contains("login") || lowerMessage.contains("signup") || 
            lowerMessage.contains("register") || lowerMessage.contains("account") ||
            lowerMessage.contains("sign in") || lowerMessage.contains("sign out")) {
            processed.append(" [Context: Authentication Query]");
        }
        
        // Feature-related keywords
        if (lowerMessage.contains("feature") || lowerMessage.contains("function") ||
            lowerMessage.contains("what can") || lowerMessage.contains("help")) {
            processed.append(" [Context: App Features Query]");
        }
        
        return processed.toString();
    }
    
    /**
     * Reset the chat session
     */
    public void resetChat() {
        initializeModel();
    }
    
    /**
     * Check if API key is configured
     */
    public boolean isApiKeyConfigured() {
        return model != null && API_KEY_PLACEHOLDER != null && 
               !API_KEY_PLACEHOLDER.equals("YOUR_API_KEY_HERE") && 
               !API_KEY_PLACEHOLDER.trim().isEmpty();
    }
}
