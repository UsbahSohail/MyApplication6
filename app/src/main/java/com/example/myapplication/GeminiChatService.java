package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

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
        initializeModel();
    }
    
    private void initializeModel() {
        try {
            // Validate API key first
            if (API_KEY_PLACEHOLDER == null || API_KEY_PLACEHOLDER.isEmpty() || 
                API_KEY_PLACEHOLDER.equals("YOUR_API_KEY_HERE")) {
                Log.e(TAG, "API key is not configured!");
                return;
            }
            
            // Initialize Generative Model with Gemini Pro
            // Try gemini-1.5-flash first (faster and free tier friendly)
            try {
                model = new GenerativeModel(
                    "gemini-1.5-flash",
                    API_KEY_PLACEHOLDER
                );
                Log.d(TAG, "Using gemini-1.5-flash model");
            } catch (Exception e) {
                Log.d(TAG, "gemini-1.5-flash failed, trying gemini-pro");
                try {
                    model = new GenerativeModel(
                        "gemini-pro",
                        API_KEY_PLACEHOLDER
                    );
                    Log.d(TAG, "Using gemini-pro model");
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to initialize model with both attempts", e2);
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
        // Check API key first
        if (API_KEY_PLACEHOLDER == null || API_KEY_PLACEHOLDER.isEmpty() || 
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
        
        executor.execute(() -> {
            try {
                // Tokenize and process the message for better context understanding
                String processedMessage = processMessage(message);
                
                // Build prompt with conversation context
                String prompt = conversationContext.toString() + 
                    "User: " + processedMessage + "\n\nAssistant:";
                
                // Create Content object from prompt
                Content.Builder promptBuilder = new Content.Builder();
                promptBuilder.addText(prompt);
                Content promptContent = promptBuilder.build();
                
                // Generate response using modelFutures.generateContent
                // This returns a ListenableFuture<GenerateContentResponse>
                ListenableFuture<GenerateContentResponse> futureResponse = 
                    modelFutures.generateContent(promptContent);
                
                // Get the response from the ListenableFuture
                GenerateContentResponse response = futureResponse.get();
                
                // Extract response text
                String responseText = response != null ? response.getText() : null;
                
                if (responseText != null && !responseText.isEmpty()) {
                    // Update conversation context with user message and AI response
                    conversationContext.append("User: ").append(processedMessage).append("\n\n");
                    conversationContext.append("Assistant: ").append(responseText).append("\n\n");
                    
                    mainHandler.post(() -> callback.onResponse(responseText));
                } else {
                    mainHandler.post(() -> callback.onError("No response from AI"));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending message to Gemini", e);
                String actualError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Log.e(TAG, "Actual error: " + actualError);
                
                mainHandler.post(() -> {
                    String errorMsg;
                    if (actualError.contains("API") || actualError.contains("key") || 
                        actualError.contains("401") || actualError.contains("403") ||
                        actualError.contains("PERMISSION_DENIED") || actualError.contains("INVALID_ARGUMENT")) {
                        errorMsg = "API key error: " + actualError + "\n\nPlease verify your API key is valid and has proper permissions.\nGet your API key from: https://aistudio.google.com/app/apikey";
                    } else {
                        errorMsg = "Error: " + actualError;
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
        conversationContext.setLength(0);
        conversationContext.append(APP_CONTEXT).append("\n\n");
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
