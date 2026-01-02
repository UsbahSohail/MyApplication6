package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        SharedPreferences preferences = getSharedPreferences(SignupActivity.PREF_NAME, MODE_PRIVATE);
        String savedEmail = preferences.getString(SignupActivity.KEY_EMAIL, "");
        if (!TextUtils.isEmpty(savedEmail)) {
            etEmail.setText(savedEmail.trim());
        }

        btnLogin.setOnClickListener(v -> {
            String inputEmail = etEmail.getText().toString().trim();
            String inputPassword = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(inputEmail)) {
                etEmail.setError("Enter registered email");
                etEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(inputPassword)) {
                etPassword.setError("Enter password");
                etPassword.requestFocus();
                return;
            }

            String storedEmail = preferences.getString(SignupActivity.KEY_EMAIL, "");
            String storedPassword = preferences.getString(SignupActivity.KEY_PASSWORD, "");
            String storedName = preferences.getString(SignupActivity.KEY_NAME, "");

            if (TextUtils.isEmpty(storedEmail) || TextUtils.isEmpty(storedPassword)) {
                Toast.makeText(this, "No account found. Please sign up first.", Toast.LENGTH_SHORT).show();
                return;
            }

            storedEmail = storedEmail.trim();
            storedPassword = storedPassword.trim();
            storedName = storedName.trim();

            if (inputEmail.equalsIgnoreCase(storedEmail) && inputPassword.equals(storedPassword)) {
                String greetingName = TextUtils.isEmpty(storedName) ? "Prime Member" : storedName;
                Toast.makeText(this, "Welcome back, " + greetingName + "!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ProductActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Incorrect email or password.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
