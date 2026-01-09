package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.databinding.ActivityObjectDetectionBinding;

import java.io.IOException;
import java.util.List;

/**
 * Activity for object detection using Hugging Face YOLO model
 * Allows users to take a photo or select an image from gallery
 */
public class ObjectDetectionActivity extends AppCompatActivity {
    
    private static final String TAG = "ObjectDetectionActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int STORAGE_PERMISSION_REQUEST = 101;
    
    private ActivityObjectDetectionBinding binding;
    private HuggingFaceYOLOService yoloService;
    private Bitmap currentImage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityObjectDetectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize YOLO service
        yoloService = new HuggingFaceYOLOService(this);
        
        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Object Detection (YOLO)");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // Check API token
        if (!yoloService.isApiTokenConfigured()) {
            binding.textViewStatus.setText("Hugging Face API token not configured.\n\nPlease add HUGGINGFACE_API_TOKEN to local.properties");
            binding.buttonDetect.setEnabled(false);
        }
        
        // Button click listeners
        binding.buttonTakePhoto.setOnClickListener(v -> takePhoto());
        binding.buttonSelectImage.setOnClickListener(v -> selectImage());
        binding.buttonDetect.setOnClickListener(v -> detectObjects());
    }
    
    private void takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    CAMERA_PERMISSION_REQUEST);
            return;
        }
        
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void selectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 
                    STORAGE_PERMISSION_REQUEST);
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_IMAGE_PICK);
    }
    
    private void detectObjects() {
        if (currentImage == null) {
            Toast.makeText(this, "Please select or take an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonDetect.setEnabled(false);
        binding.textViewStatus.setText("Detecting objects...");
        
        yoloService.detectObjects(currentImage, new HuggingFaceYOLOService.DetectionCallback() {
            @Override
            public void onSuccess(List<HuggingFaceYOLOService.DetectedObject> detections) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonDetect.setEnabled(true);
                    
                    if (detections.isEmpty()) {
                        binding.textViewStatus.setText("No objects detected");
                        binding.imageViewResult.setImageBitmap(currentImage);
                    } else {
                        // Draw bounding boxes on image
                        Bitmap annotatedImage = drawDetections(currentImage, detections);
                        binding.imageViewResult.setImageBitmap(annotatedImage);
                        
                        // Display detection results
                        StringBuilder statusText = new StringBuilder();
                        statusText.append("Detected ").append(detections.size()).append(" object(s):\n\n");
                        for (HuggingFaceYOLOService.DetectedObject obj : detections) {
                            statusText.append(String.format("• %s (%.1f%% confidence)\n", 
                                    obj.getLabel(), obj.getConfidence() * 100));
                        }
                        binding.textViewStatus.setText(statusText.toString());
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonDetect.setEnabled(true);
                    binding.textViewStatus.setText("Error: " + error);
                    Toast.makeText(ObjectDetectionActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * Draw bounding boxes and labels on the image
     */
    private Bitmap drawDetections(Bitmap original, List<HuggingFaceYOLOService.DetectedObject> detections) {
        Bitmap mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(30f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setFakeBoldText(true);
        
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAlpha(200);
        
        for (HuggingFaceYOLOService.DetectedObject detection : detections) {
            float xMin = detection.getXMin();
            float yMin = detection.getYMin();
            float xMax = detection.getXMax();
            float yMax = detection.getYMax();
            
            // Draw bounding box
            RectF rect = new RectF(xMin, yMin, xMax, yMax);
            canvas.drawRect(rect, boxPaint);
            
            // Draw label with confidence
            String label = detection.getLabel() + " " + String.format("%.1f%%", detection.getConfidence() * 100);
            
            // Draw background for text
            float textWidth = textPaint.measureText(label);
            canvas.drawRect(xMin, yMin - 40, xMin + textWidth + 10, yMin, bgPaint);
            
            // Draw text
            canvas.drawText(label, xMin + 5, yMin - 10, textPaint);
        }
        
        return mutableBitmap;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    currentImage = imageBitmap;
                    binding.imageViewResult.setImageBitmap(imageBitmap);
                    binding.buttonDetect.setEnabled(true);
                    binding.textViewStatus.setText("Image loaded. Click 'Detect Objects' to analyze.");
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri imageUri = data.getData();
                try {
                    currentImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    binding.imageViewResult.setImageBitmap(currentImage);
                    binding.buttonDetect.setEnabled(true);
                    binding.textViewStatus.setText("Image loaded. Click 'Detect Objects' to analyze.");
                } catch (IOException e) {
                    Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectImage();
            } else {
                Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentImage != null && !currentImage.isRecycled()) {
            currentImage.recycle();
        }
    }
}

