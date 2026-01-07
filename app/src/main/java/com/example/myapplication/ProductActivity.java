package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity {
    private AdManager adManager;
    private LinearLayout bannerAdContainer;
    private LinearLayout topBannerAdContainer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        // Initialize AdMob
        adManager = AdManager.getInstance();
        
        RecyclerView recyclerView = findViewById(R.id.recyclerProducts);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(new ProductAdapter(buildProducts()));
        
        // Setup Banner Ad Containers
        bannerAdContainer = findViewById(R.id.bannerAdContainer);
        topBannerAdContainer = findViewById(R.id.topBannerAdContainer);
        
        // Initialize AdMob and load banner ads after initialization
        adManager.initialize(this, () -> {
            // Load banner ads after AdMob is initialized
            if (topBannerAdContainer != null) {
                adManager.loadBannerAd(ProductActivity.this, topBannerAdContainer);
            }
            if (bannerAdContainer != null) {
                adManager.loadBannerAd(ProductActivity.this, bannerAdContainer);
            }
        });

        Button btnFragments = findViewById(R.id.btnFragments);
        Button btnDrawer = findViewById(R.id.btnDrawerDemo);
        Button btnUserInfo = findViewById(R.id.btnUserInfo);
        Button btnDataSend = findViewById(R.id.btnDataSend);
        Button btnNavigationView = findViewById(R.id.btnNavigationView);
        Button btnQRScanner = findViewById(R.id.btnQRScanner);
        Button btnChat = findViewById(R.id.btnChat);
        Button btnAIChatbot = findViewById(R.id.btnAIChatbot);
        Button btnSignOut = findViewById(R.id.btnSignOut);

        btnFragments.setOnClickListener(v -> {
            showInterstitialAdThenNavigate(FragmentsActivity.class);
        });
        btnDrawer.setOnClickListener(v -> {
            showInterstitialAdThenNavigate(DrawerActivity.class);
        });
        btnUserInfo.setOnClickListener(v -> {
            showInterstitialAdThenNavigate(UserInfoActivity.class);
        });
        btnDataSend.setOnClickListener(v -> {
            showInterstitialAdThenNavigate(DataSendActivity.class);
        });
        btnNavigationView.setOnClickListener(v -> {
            showInterstitialAdThenNavigate(MainActivity.class);
        });
        btnQRScanner.setOnClickListener(v -> {
            showInterstitialAdThenNavigate(QRScannerActivity.class);
        });
        btnChat.setOnClickListener(v -> {
            showInterstitialAdThenNavigate(ChatListActivity.class);
        });
        btnAIChatbot.setOnClickListener(v -> {
            showInterstitialAdThenNavigate(AIChatbotActivity.class);
        });
        btnSignOut.setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();
            Intent signOutIntent = new Intent(this, LoginActivity.class);
            signOutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(signOutIntent);
            finishAffinity();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Session handling - Check if user is authenticated
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    /**
     * Show interstitial ad before navigating to another activity
     */
    private void showInterstitialAdThenNavigate(Class<?> targetActivity) {
        if (adManager != null) {
            adManager.showInterstitialAd(this, () -> {
                // Navigate after ad is closed
                startActivity(new Intent(ProductActivity.this, targetActivity));
            });
        } else {
            // AdManager not initialized, navigate immediately
            startActivity(new Intent(this, targetActivity));
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload banner ads if needed
        if (adManager != null) {
            if (topBannerAdContainer != null) {
                adManager.loadBannerAd(this, topBannerAdContainer);
            }
            if (bannerAdContainer != null) {
                adManager.loadBannerAd(this, bannerAdContainer);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up if needed
    }

    private List<Product> buildProducts() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("Echo Dot (5th Gen)", "₹4,499", R.drawable.echo_dot));
        products.add(new Product("Fire TV Stick", "₹3,999", R.drawable.fire_tv_stick));
        products.add(new Product("Kindle Paperwhite", "₹13,999", R.drawable.amazon_logo));
        products.add(new Product("Amazon Basics Mouse", "₹699", R.drawable.amazon_basic_mouse));
        products.add(new Product("Alexa Smart Plug", "₹1,999", R.drawable.amazon_smart_plug));
        products.add(new Product("Amazon Gift Card", "₹500 - ₹5,000", R.drawable.amazon_gift));
        products.add(new Product("Wireless Charger", "₹1,299", R.drawable.amazon_logo));
        products.add(new Product("Amazon Hoodie", "₹2,299", R.drawable.hoodie));
        products.add(new Product("Bluetooth Speaker", "₹1,799", R.drawable.bluetooth_speaker));
        products.add(new Product("USB-C Cable", "₹499", R.drawable.c_cable));
        return products;
    }
}
