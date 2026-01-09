package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service class for handling Google Gemini AI chatbot interactions
 * Uses REST API (allows gemini-2.5-flash support)
 */
public class GeminiChatService {
    private static final String TAG = "GeminiChatService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    /**
     * Get API key from BuildConfig
     */
    private static String getApiKey() {
        try {
            String key = BuildConfig.GEMINI_API_KEY;
            
            // Remove quotes if present (from buildConfigField)
            if (key != null && key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            
            // Clean and trim
            if (key != null) {
                key = key.trim().replaceAll("\\s+", "");
            } else {
                key = "";
            }
            
            // Log for debugging (first 10 chars only for security)
            if (!key.isEmpty()) {
                String preview = key.length() > 10 ? key.substring(0, 10) + "..." : key;
                Log.d(TAG, "Gemini API key loaded: " + preview + " (length: " + key.length() + ")");
                
                // Validate format (Gemini keys start with AIza)
                if (!key.startsWith("AIza")) {
                    Log.w(TAG, "⚠️ API key doesn't start with 'AIza'. Make sure it's a valid Gemini API key.");
                }
            } else {
                Log.w(TAG, "⚠️ Gemini API key is empty or not configured");
            }
            
            return key;
        } catch (Exception e) {
            Log.e(TAG, "❌ Could not read API key from BuildConfig: " + e.getMessage());
            Log.e(TAG, "Please ensure: 1) local.properties has GEMINI_API_KEY, 2) Gradle is synced, 3) Project is rebuilt");
            return "";
        }
    }
    
    /**
     * Get the Gemini API URL with API key
     */
    private static String getGeminiApiUrl() {
        String apiKey = getApiKey();
        return BASE_URL + "?key=" + apiKey;
    }
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private StringBuilder conversationContext;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // App context for better responses
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
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.conversationContext = new StringBuilder();
        this.conversationContext.append(APP_CONTEXT).append("\n\n");
        
        // Validate API key on initialization
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty() || 
            apiKey.equals("\"\"") || apiKey.equals("") ||
            apiKey.equals("YOUR_API_KEY_HERE") || apiKey.equals("YOUR_KEY_HERE")) {
            Log.e(TAG, "❌ API key is NOT configured!");
            Log.e(TAG, "Please add GEMINI_API_KEY=YOUR_KEY to local.properties file");
            Log.e(TAG, "Get your key from: https://aistudio.google.com/app/apikey");
        } else {
            Log.d(TAG, "✅ Gemini REST API service initialized (using gemini-2.5-flash)");
        }
    }
    
    /**
     * Send a message to the AI chatbot using REST API
     * @param message User's message
     * @param callback Callback for response or error
     */
    public void sendMessage(String message, ChatCallback callback) {
        final String userMessage = message;
        
        // Check API key
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty() || 
            apiKey.equals("\"\"") || apiKey.equals("") ||
            apiKey.equals("YOUR_API_KEY_HERE") || apiKey.equals("YOUR_KEY_HERE")) {
            String errorMsg = "API key not configured.\n\n" +
                "Please:\n" +
                "1. Open local.properties file\n" +
                "2. Add: GEMINI_API_KEY=YOUR_KEY\n" +
                "3. Get key from: https://aistudio.google.com/app/apikey\n" +
                "4. Sync Gradle and rebuild";
            Log.e(TAG, errorMsg);
            callback.onError(errorMsg);
            return;
        }
        
        final StringBuilder finalConversationContext = conversationContext;
        
        executor.execute(() -> {
            try {
                String processedMessage = processMessage(userMessage);
                String prompt = finalConversationContext.toString() + 
                    "User: " + processedMessage + "\n\nAssistant:";
                
                // Build JSON request body
                JsonObject requestBody = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject content = new JsonObject();
                JsonArray requestParts = new JsonArray();
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", prompt);
                requestParts.add(textPart);
                content.add("parts", requestParts);
                contents.add(content);
                requestBody.add("contents", contents);
                
                String jsonBody = gson.toJson(requestBody);
                
                Log.d(TAG, "Sending request to Gemini API...");
                
                // Create HTTP request
                RequestBody body = RequestBody.create(jsonBody, JSON);
                Request request = new Request.Builder()
                    .url(getGeminiApiUrl())
                    .post(body)
                    .build();
                
                // Execute request
                Response response = httpClient.newCall(request).execute();
                
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API request failed: " + response.code() + " - " + errorBody);
                    
                    String userErrorMessage = "Error: " + response.code();
                    if (errorBody.contains("API key") || errorBody.contains("invalid") || 
                        errorBody.contains("INVALID_ARGUMENT") || errorBody.contains("PERMISSION_DENIED")) {
                        userErrorMessage = "Invalid API key.\n\n" +
                            "Fixes:\n" +
                            "1. Get key from: https://aistudio.google.com/app/apikey\n" +
                            "2. Enable 'Generative Language API'\n" +
                            "3. Add to local.properties: GEMINI_API_KEY=YOUR_KEY\n" +
                            "4. Sync Gradle and rebuild\n" +
                            "5. Check for spaces/newlines in key";
                    } else if (errorBody.contains("quota") || errorBody.contains("QUOTA_EXCEEDED")) {
                        userErrorMessage = "API quota exceeded. Please check your Gemini API quota.";
                    }
                    
                    final String finalErrorMessage = userErrorMessage;
                    mainHandler.post(() -> callback.onError(finalErrorMessage));
                    return;
                }
                
                // Parse response
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                // Extract text from response
                String responseText = null;
                try {
                    JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                    if (candidates != null && candidates.size() > 0) {
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        JsonObject contentObj = candidate.getAsJsonObject("content");
                        if (contentObj != null) {
                            JsonArray parts = contentObj.getAsJsonArray("parts");
                            if (parts != null && parts.size() > 0) {
                                JsonObject part = parts.get(0).getAsJsonObject();
                                responseText = part.get("text").getAsString();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing response: " + e.getMessage());
                    Log.e(TAG, "Response body: " + responseBody);
                }
                
                if (responseText != null && !responseText.isEmpty()) {
                    // Update conversation context
                    finalConversationContext.append("User: ").append(processedMessage).append("\n\n");
                    finalConversationContext.append("Assistant: ").append(responseText).append("\n\n");
                    
                    final String finalResponseText = responseText;
                    
                    // Return response on main thread
                    mainHandler.post(() -> callback.onResponse(finalResponseText));
                } else {
                    mainHandler.post(() -> callback.onError("No response received from AI. Please try again."));
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Network error sending message to Gemini", e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("timeout") || errorMsg.contains("network"))) {
                    errorMsg = "Network error. Please check your internet connection and try again.";
                } else {
                    errorMsg = "Error communicating with AI service: " + errorMsg;
                }
                final String finalErrorMessage = errorMsg;
                mainHandler.post(() -> callback.onError(finalErrorMessage));
            } catch (Exception e) {
                Log.e(TAG, "Error sending message to Gemini", e);
                String actualError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                
                // Provide user-friendly error messages
                String userErrorMessage;
                if (actualError != null) {
                    if (actualError.contains("API key") || actualError.contains("invalid") || 
                        actualError.contains("INVALID_ARGUMENT") || actualError.contains("PERMISSION_DENIED")) {
                        userErrorMessage = "Invalid API key.\n\n" +
                            "Fixes:\n" +
                            "1. Get key from: https://aistudio.google.com/app/apikey\n" +
                            "2. Enable 'Generative Language API'\n" +
                            "3. Add to local.properties: GEMINI_API_KEY=YOUR_KEY\n" +
                            "4. Sync Gradle and rebuild\n" +
                            "5. Check for spaces/newlines in key";
                    } else {
                        userErrorMessage = "Error: " + actualError;
                    }
                } else {
                    userErrorMessage = "Error communicating with AI service.";
                }
                
                final String finalErrorMessage = userErrorMessage;
                mainHandler.post(() -> callback.onError(finalErrorMessage));
            }
        });
    }
    
    /**
     * Process and enhance the message for better context understanding
     */
    private String processMessage(String message) {
        String lowerMessage = message.toLowerCase();
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
        String apiKey = getApiKey();
        return apiKey != null && 
               !apiKey.equals("\"\"") && 
               !apiKey.equals("YOUR_API_KEY_HERE") && 
               !apiKey.equals("YOUR_KEY_HERE") &&
               apiKey.trim().length() > 0;
    }
}
