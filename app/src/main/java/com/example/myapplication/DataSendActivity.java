package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DataSendActivity extends AppCompatActivity {

    private Calendar selectedDate;
    private TextView tvSelectedDate;

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_data_send);

        EditText etName = findViewById(R.id.etName);
        Button btnDatePicker = findViewById(R.id.btnDatePicker);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        Button btnSend = findViewById(R.id.btnSend);
        Button btnBackToMenu = findViewById(R.id.btnBackToMenu);

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        btnDatePicker.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate = Calendar.getInstance();
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                        String formattedDate = sdf.format(selectedDate.getTime());
                        tvSelectedDate.setText(formattedDate);
                        tvSelectedDate.setVisibility(View.VISIBLE);
                    },
                    currentYear - 25,
                    currentMonth,
                    currentDay
            );

            datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
            datePickerDialog.show();
        });

        btnSend.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Please enter name");
                etName.requestFocus();
                return;
            }

            if (selectedDate == null) {
                Toast.makeText(this, "Please select birth date", Toast.LENGTH_SHORT).show();
                btnDatePicker.requestFocus();
                return;
            }

            int age = calculateAge(selectedDate);
            if (age < 0) {
                Toast.makeText(this, "Invalid birth date", Toast.LENGTH_SHORT).show();
                btnDatePicker.requestFocus();
                return;
            }

            Intent i = new Intent(DataSendActivity.this, DataReceiveActivity.class);
            i.putExtra(DataReceiveActivity.EXTRA_NAME, name);
            i.putExtra(DataReceiveActivity.EXTRA_AGE, age);
            startActivity(i);
        });

        btnBackToMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, ProductActivity.class));
            finish();
        });
    }

    private int calculateAge(Calendar birthDate) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        return age >= 0 ? age : -1;
    }
}
