package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service for YOLO object detection using Hugging Face Inference API
 * Uses API-based inference (no local model training)
 */
public class HuggingFaceYOLOService {
    private static final String TAG = "HuggingFaceYOLO";
    
    // Hugging Face Inference API endpoint for YOLO models
    // Using ultralytics/yolov8 which is a popular YOLO model on Hugging Face
    private static final String API_URL = "https://api-inference.huggingface.co/models/ultralytics/yolov8n";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Executor executor;
    private final Gson gson;
    private String apiToken;
    
    /**
     * Represents a detected object with bounding box and confidence
     */
    public static class DetectedObject {
        private final String label;
        private final float confidence;
        private final float xMin;
        private final float yMin;
        private final float xMax;
        private final float yMax;
        
        public DetectedObject(String label, float confidence, float xMin, float yMin, float xMax, float yMax) {
            this.label = label;
            this.confidence = confidence;
            this.xMin = xMin;
            this.yMin = yMin;
            this.xMax = xMax;
            this.yMax = yMax;
        }
        
        public String getLabel() { return label; }
        public float getConfidence() { return confidence; }
        public float getXMin() { return xMin; }
        public float getYMin() { return yMin; }
        public float getXMax() { return xMax; }
        public float getYMax() { return yMax; }
    }
    
    public interface DetectionCallback {
        void onSuccess(List<DetectedObject> detections);
        void onError(String error);
    }
    
    public HuggingFaceYOLOService(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
        this.apiToken = getApiToken();
    }
    
    private String getApiToken() {
        try {
            Class<?> buildConfigClass = Class.forName("com.example.myapplication.BuildConfig");
            java.lang.reflect.Field field = buildConfigClass.getField("HUGGINGFACE_API_TOKEN");
            String token = (String) field.get(null);
            
            // Remove quotes if present
            if (token != null && token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1);
            }
            
            // Clean and trim
            token = token != null ? token.trim() : "";
            
            // Log for debugging (first 10 chars only for security)
            if (!token.isEmpty()) {
                String preview = token.length() > 10 ? token.substring(0, 10) + "..." : token.substring(0, Math.min(token.length(), 10));
                Log.d(TAG, "Hugging Face API token loaded: " + preview + " (length: " + token.length() + ")");
            } else {
                Log.w(TAG, "Hugging Face API token is empty or not configured");
            }
            
            return token;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "BuildConfig class not found. Please sync Gradle: File → Sync Project with Gradle Files");
            return "";
        } catch (Exception e) {
            Log.w(TAG, "Could not read Hugging Face API token from BuildConfig: " + e.getMessage());
            Log.w(TAG, "Please ensure: 1) local.properties has HUGGINGFACE_API_TOKEN, 2) Gradle is synced, 3) Project is rebuilt");
            return "";
        }
    }
    
    /**
     * Convert bitmap to base64 string for API request
     */
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
    
    /**
     * Detect objects in an image using YOLO via Hugging Face Inference API
     */
    public void detectObjects(Bitmap image, DetectionCallback callback) {
        // Refresh token in case BuildConfig was updated
        apiToken = getApiToken();
        
        if (apiToken == null || apiToken.isEmpty() || apiToken.equals("\"\"")) {
            callback.onError("Hugging Face API token not configured. Please:\n1. Add HUGGINGFACE_API_TOKEN to local.properties\n2. Sync Gradle\n3. Rebuild project");
            return;
        }
        
        // Validate token format (should start with hf_)
        if (!apiToken.startsWith("hf_")) {
            Log.w(TAG, "Warning: API token doesn't start with 'hf_'. Make sure it's a valid Hugging Face token.");
        }
        
        executor.execute(() -> {
            try {
                String base64Image = bitmapToBase64(image);
                
                // Create JSON request body
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("inputs", base64Image);
                
                // Create HTTP request
                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                Request request = new Request.Builder()
                        .url(API_URL)
                        .header("Authorization", "Bearer " + apiToken)
                        .header("Content-Type", "application/json")
                        .post(body)
                        .build();
                
                Log.d(TAG, "Sending request to Hugging Face API...");
                
                // Execute request
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API request failed: " + response.code() + " - " + errorBody);
                        callback.onError("API request failed: " + response.code() + ". " + errorBody);
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    Log.d(TAG, "API response received");
                    
                    // Parse response
                    List<DetectedObject> detections = parseResponse(responseBody);
                    callback.onSuccess(detections);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error during API call", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error processing detection", e);
                callback.onError("Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Parse Hugging Face API response to extract detected objects
     */
    private List<DetectedObject> parseResponse(String responseBody) {
        List<DetectedObject> detections = new ArrayList<>();
        
        try {
            // Hugging Face returns an array of detection results
            JsonArray jsonArray = gson.fromJson(responseBody, JsonArray.class);
            
            if (jsonArray != null && jsonArray.size() > 0) {
                JsonArray detectionsArray = jsonArray.get(0).getAsJsonObject().getAsJsonArray("predictions");
                
                if (detectionsArray != null) {
                    for (JsonElement element : detectionsArray) {
                        JsonObject detection = element.getAsJsonObject();
                        
                        String label = detection.has("label") ? detection.get("label").getAsString() : "Unknown";
                        float score = detection.has("score") ? detection.get("score").getAsFloat() : 0f;
                        
                        // Get bounding box coordinates
                        JsonObject box = detection.getAsJsonObject("box");
                        if (box != null) {
                            float xMin = box.has("xmin") ? box.get("xmin").getAsFloat() : 0f;
                            float yMin = box.has("ymin") ? box.get("ymin").getAsFloat() : 0f;
                            float xMax = box.has("xmax") ? box.get("xmax").getAsFloat() : 0f;
                            float yMax = box.has("ymax") ? box.get("ymax").getAsFloat() : 0f;
                            
                            // Filter out low confidence detections
                            if (score > 0.3f) {
                                detections.add(new DetectedObject(label, score, xMin, yMin, xMax, yMax));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing API response: " + e.getMessage());
            // If parsing fails, try alternative format
            Log.d(TAG, "Response body: " + responseBody);
        }
        
        return detections;
    }
    
    /**
     * Check if API token is configured
     */
    public boolean isApiTokenConfigured() {
        return apiToken != null && !apiToken.isEmpty() && !apiToken.equals("\"\"");
    }
}

