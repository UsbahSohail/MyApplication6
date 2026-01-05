package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ProductActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        RecyclerView recyclerView = findViewById(R.id.recyclerProducts);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(new ProductAdapter(buildProducts()));

        Button btnFragments = findViewById(R.id.btnFragments);
        Button btnDrawer = findViewById(R.id.btnDrawerDemo);
        Button btnUserInfo = findViewById(R.id.btnUserInfo);
        Button btnDataSend = findViewById(R.id.btnDataSend);
        Button btnNavigationView = findViewById(R.id.btnNavigationView);
        Button btnQRScanner = findViewById(R.id.btnQRScanner);
        Button btnChat = findViewById(R.id.btnChat);
        Button btnSignOut = findViewById(R.id.btnSignOut);

        btnFragments.setOnClickListener(v -> startActivity(new Intent(this, FragmentsActivity.class)));
        btnDrawer.setOnClickListener(v -> startActivity(new Intent(this, DrawerActivity.class)));
        btnUserInfo.setOnClickListener(v -> startActivity(new Intent(this, UserInfoActivity.class)));
        btnDataSend.setOnClickListener(v -> startActivity(new Intent(this, DataSendActivity.class)));
        btnNavigationView.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        btnQRScanner.setOnClickListener(v -> startActivity(new Intent(this, QRScannerActivity.class)));
        btnChat.setOnClickListener(v -> startActivity(new Intent(this, ChatListActivity.class)));
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
