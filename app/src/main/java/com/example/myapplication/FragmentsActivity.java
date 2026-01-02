package com.example.myapplication;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class FragmentsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_fragments);

        Button r = findViewById(R.id.btnRed), g = findViewById(R.id.btnGreen), bl = findViewById(R.id.btnBlue);

        r.setOnClickListener(v -> load(new RedFragment()));
        g.setOnClickListener(v -> load(new GreenFragment()));
        bl.setOnClickListener(v -> load(new BlueFragment()));
    }

    void load(Fragment f){
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, f).commit();
    }
}
