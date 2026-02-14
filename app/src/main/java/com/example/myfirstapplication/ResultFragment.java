package com.example.myfirstapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ResultFragment extends Fragment {

    public ResultFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_result, container, false);

        TextView tvResult = view.findViewById(R.id.tvResult);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        if (getArguments() != null) {
            tvResult.setText(getArguments().getString("result"));
        }

        btnCancel.setOnClickListener(v -> handleCancel());

        return view;
    }

    private void handleCancel() {
        OrderFragment orderFragment = new OrderFragment();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, orderFragment)
                .commit();
    }

}
