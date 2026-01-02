package com.example.myapplication;

import android.os.Bundle;
import android.view.*;
import androidx.fragment.app.Fragment;

public class GreenFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = new View(getContext());
        v.setBackgroundColor(0xFF00FF00); // Green background
        return v;
    }
}
