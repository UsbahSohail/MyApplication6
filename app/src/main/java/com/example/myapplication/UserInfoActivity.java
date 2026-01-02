package com.example.myapplication;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class UserInfoActivity extends AppCompatActivity {
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_user_info);

        EditText name = findViewById(R.id.etUsername);
        DatePicker dp = findViewById(R.id.datePicker);
        Button show = findViewById(R.id.btnShow);
        TextView result = findViewById(R.id.tvResult);

        show.setOnClickListener(v -> {
            int age = 2025 - dp.getYear(); // simple age calc
            result.setText("Hello " + name.getText() + ", Age: " + age);
        });
    }
}
