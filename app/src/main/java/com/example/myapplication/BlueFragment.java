package com.example.myapplication;

import android.os.Bundle;
import android.view.*;
import androidx.fragment.app.Fragment;

public class BlueFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = new View(getContext());
        v.setBackgroundColor(0xFF0000FF); // Blue background
        return v;
    }
}
