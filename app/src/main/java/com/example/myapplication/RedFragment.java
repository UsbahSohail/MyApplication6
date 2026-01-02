package com.example.myapplication;
import android.os.Bundle;
import android.view.*;
import androidx.fragment.app.Fragment;

public class RedFragment extends Fragment {
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle b){
        View v = new View(getContext());
        v.setBackgroundColor(0xFFFF0000);
        return v;
    }
}
