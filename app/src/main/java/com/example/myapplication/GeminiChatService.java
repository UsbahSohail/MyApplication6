package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.TextPart;

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
    private GenerativeModelFutures modelFutures;
    private StringBuilder conversationContext;
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
        // Context parameter kept for future use
        initializeModel();
    }
    
    private void initializeModel() {
        try {
            // Validate API key first
            if (API_KEY_PLACEHOLDER == null || API_KEY_PLACEHOLDER.length() == 0 || 
                API_KEY_PLACEHOLDER.equals("YOUR_API_KEY_HERE")) {
                Log.e(TAG, "API key is not configured!");
                return;
            }
            
            // Initialize Gemini AI model
            try {
                model = new GenerativeModel(
                    "gemini-1.5-flash",
                    API_KEY_PLACEHOLDER
                );
                Log.d(TAG, "Using gemini-1.5-flash model");
            } catch (Exception e) {
                Log.w(TAG, "gemini-1.5-flash failed, trying gemini-pro. Error: " + e.getMessage());
                try {
                    model = new GenerativeModel(
                        "gemini-pro",
                        API_KEY_PLACEHOLDER
                    );
                    Log.d(TAG, "Using gemini-pro model");
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to initialize model with both attempts", e2);
                    String errorMsg = e2.getMessage();
                    if (errorMsg != null && (errorMsg.contains("API key") || errorMsg.contains("invalid") || 
                        errorMsg.contains("INVALID_ARGUMENT") || errorMsg.contains("PERMISSION_DENIED"))) {
                        Log.e(TAG, "Invalid API key or permission denied. Please check your API key at https://aistudio.google.com/app/apikey");
                    }
                    return;
                }
            }
            
            if (model == null) {
                Log.e(TAG, "Model is null after initialization");
                return;
            }
            
            modelFutures = GenerativeModelFutures.from(model);
            conversationContext = new StringBuilder();
            conversationContext.append(APP_CONTEXT).append("\n\n");
            
            Log.d(TAG, "Gemini model initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Gemini model: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send a message to the AI chatbot
     * @param message User's message
     * @param callback Callback for response or error
     */
    public void sendMessage(String message, ChatCallback callback) {
        // Fix: Make message final for lambda usage
        final String userMessage = message;
        
        // Check API key first
        if (API_KEY_PLACEHOLDER == null || API_KEY_PLACEHOLDER.length() == 0 || 
            API_KEY_PLACEHOLDER.equals("YOUR_API_KEY_HERE")) {
            callback.onError("API key not configured. Please set your Gemini API key in GeminiChatService.java");
            return;
        }
        
        if (model == null || modelFutures == null) {
            // Try to reinitialize if not initialized
            Log.d(TAG, "Model is null, attempting to reinitialize...");
            initializeModel();
            if (model == null || modelFutures == null) {
                callback.onError("AI service not initialized. Please check API key configuration and internet connection.");
                return;
            }
        }
        
        // Fix: Make modelFutures and conversationContext final for lambda
        final GenerativeModelFutures finalModelFutures = modelFutures;
        final StringBuilder finalConversationContext = conversationContext;
        
        executor.execute(() -> {
            try {
                String processedMessage = processMessage(userMessage);
                String prompt = finalConversationContext.toString() + 
                    "User: " + processedMessage + "\n\nAssistant:";
                
                // Fix: Use Content.Builder() with TextPart - the correct method for older Java SDK
                // This SDK version requires: Content.Builder().addPart(new TextPart(prompt)).build()
                Content content = new Content.Builder()
                    .addPart(new TextPart(prompt))
                    .build();
                
                // Generate response using Gemini AI
                GenerateContentResponse response = finalModelFutures.generateContent(content).get();
                
                // Extract the text response from the generated content
                String responseText = null;
                if (response != null && response.getText() != null) {
                    responseText = response.getText();
                }
                
                if (responseText != null && !responseText.isEmpty()) {
                    // Update conversation context
                    finalConversationContext.append("User: ").append(processedMessage).append("\n\n");
                    finalConversationContext.append("Assistant: ").append(responseText).append("\n\n");
                    
                    // Fix: Make responseText final for lambda
                    final String finalResponseText = responseText;
                    
                    // Return response on main thread
                    mainHandler.post(() -> callback.onResponse(finalResponseText));
                } else {
                    mainHandler.post(() -> callback.onError("No response received from AI. Please try again."));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message to Gemini", e);
                String actualError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                
                // Provide user-friendly error messages
                String userErrorMessage;
                if (actualError != null) {
                    if (actualError.contains("API key") || actualError.contains("invalid") || 
                        actualError.contains("INVALID_ARGUMENT") || actualError.contains("PERMISSION_DENIED")) {
                        userErrorMessage = "Invalid API key. Please check your Gemini API key in GeminiChatService.java";
                    } else if (actualError.contains("network") || actualError.contains("timeout")) {
                        userErrorMessage = "Network error. Please check your internet connection and try again.";
                    } else if (actualError.contains("quota") || actualError.contains("QUOTA_EXCEEDED")) {
                        userErrorMessage = "API quota exceeded. Please check your Gemini API quota.";
                    } else {
                        userErrorMessage = "Error: " + actualError;
                    }
                } else {
                    userErrorMessage = "Error communicating with AI service.";
                }
                
                // Fix: Make error message final for lambda
                final String finalErrorMessage = userErrorMessage;
                mainHandler.post(() -> callback.onError(finalErrorMessage));
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
        conversationContext.setLength(0);
        conversationContext.append(APP_CONTEXT).append("\n\n");
    }
    
    /**
     * Check if API key is configured
     */
    public boolean isApiKeyConfigured() {
        return model != null && API_KEY_PLACEHOLDER != null && 
               !API_KEY_PLACEHOLDER.equals("YOUR_API_KEY_HERE") && 
               API_KEY_PLACEHOLDER.trim().length() > 0;
    }
}
