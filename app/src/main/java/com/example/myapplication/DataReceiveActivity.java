package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DataReceiveActivity extends AppCompatActivity {

    private static final String TAG = "DataReceiveActivity";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_AGE = "extra_age";

    private TextView tvNameValue;
    private TextView tvAgeValue;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_data_receive);

            tvNameValue = findViewById(R.id.tvNameValue);
            tvAgeValue = findViewById(R.id.tvAgeValue);
            tvEmptyState = findViewById(R.id.tvEmptyState);
            Button btnBackToSender = findViewById(R.id.btnBackToSender);
            Button btnBackToMenu = findViewById(R.id.btnBackToMenu);

            if (tvNameValue == null || tvAgeValue == null || tvEmptyState == null) {
                Log.e(TAG, "One or more views are null");
                Toast.makeText(this, "Error loading screen. Please try again.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String name = null;
            int age = -1;
            Intent intent = getIntent();
            if (intent != null) {
                name = intent.getStringExtra(EXTRA_NAME);
                age = intent.getIntExtra(EXTRA_AGE, -1);
            }

            Log.d(TAG, "Received - Name: " + name + ", Age: " + age);

            if (TextUtils.isEmpty(name) || age < 0) {
                showEmptyState();
            } else {
                showProfile(name.trim(), age);
            }

            if (btnBackToSender != null) {
                btnBackToSender.setOnClickListener(v -> {
                    Intent backIntent = new Intent(this, DataSendActivity.class);
                    startActivity(backIntent);
                    finish();
                });
            }

            if (btnBackToMenu != null) {
                btnBackToMenu.setOnClickListener(v -> {
                    startActivity(new Intent(this, ProductActivity.class));
                    finish();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading screen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showProfile(String name, int age) {
        try {
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.GONE);
            }
            if (tvNameValue != null) {
                tvNameValue.setVisibility(View.VISIBLE);
                tvNameValue.setText("Name: " + name);
            }
            if (tvAgeValue != null) {
                tvAgeValue.setVisibility(View.VISIBLE);
                tvAgeValue.setText("Age: " + age + " years");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in showProfile: " + e.getMessage(), e);
        }
    }

    private void showEmptyState() {
        try {
            if (tvNameValue != null) {
                tvNameValue.setVisibility(View.GONE);
            }
            if (tvAgeValue != null) {
                tvAgeValue.setVisibility(View.GONE);
            }
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in showEmptyState: " + e.getMessage(), e);
        }
    }
}
