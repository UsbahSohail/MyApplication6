package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    public static final String PREF_NAME = "amazon_user_prefs";
    public static final String KEY_NAME = "key_name";
    public static final String KEY_EMAIL = "key_email";
    public static final String KEY_PASSWORD = "key_password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        EditText etName = findViewById(R.id.etUsername);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnSignup = findViewById(R.id.btnSignup);
        TextView tvLogin = findViewById(R.id.tvLoginLink);

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Enter your name");
                etName.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Enter email or mobile");
                etEmail.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Enter a password");
                etPassword.requestFocus();
                return;
            }

            SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            preferences.edit()
                    .putString(KEY_NAME, name)
                    .putString(KEY_EMAIL, email)
                    .putString(KEY_PASSWORD, password)
                    .apply();

            Toast.makeText(this, "Account created! Welcome, " + name, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProductActivity.class));
            finish();
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
