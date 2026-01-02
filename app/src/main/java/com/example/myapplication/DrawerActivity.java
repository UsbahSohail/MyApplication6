package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class DrawerActivity extends AppCompatActivity {
    private final String[] menu = {
            "Product Catalog",
            "Fragments Demo",
            "Send Data",
            "User Info Form",
            "Navigation Drawer App",
            "Sign Out"
    };

    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_drawer);

        DrawerLayout dl = findViewById(R.id.drawerLayout);
        ListView list = findViewById(R.id.drawerList);

        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menu));
        list.setOnItemClickListener((parent, view, pos, id) -> {
            Intent intent = null;
            switch (pos) {
                case 0:
                    intent = new Intent(this, ProductActivity.class);
                    break;
                case 1:
                    intent = new Intent(this, FragmentsActivity.class);
                    break;
                case 2:
                    intent = new Intent(this, DataSendActivity.class);
                    break;
                case 3:
                    intent = new Intent(this, UserInfoActivity.class);
                    break;
                case 4:
                    intent = new Intent(this, MainActivity.class);
                    break;
                case 5:
                    Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();
                    intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finishAffinity();
                    dl.closeDrawers();
                    return;
                default:
                    break;
            }

            if (intent != null) {
                startActivity(intent);
            }

            dl.closeDrawers();
        });
    }
}
