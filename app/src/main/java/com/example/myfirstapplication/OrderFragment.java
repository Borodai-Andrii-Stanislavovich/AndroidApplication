package com.example.myfirstapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OrderFragment extends Fragment {

    private EditText etFlowerName;
    private RadioGroup rgColor, rgPrice;
    private DatabaseHelper dbHelper;
    private Button btnOpen;

    public OrderFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_order, container, false);

        etFlowerName = view.findViewById(R.id.etFlowerName);
        rgColor = view.findViewById(R.id.rgColor);
        rgPrice = view.findViewById(R.id.rgPrice);
        Button btnOk = view.findViewById(R.id.btnOk);

        btnOk.setOnClickListener(v -> handleOkClick());

        dbHelper = new DatabaseHelper(getActivity());
        btnOpen = view.findViewById(R.id.btnOpen);

        btnOpen.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), HistoryActivity.class));
        });

        return view;
    }

    private void handleOkClick() {
        if (etFlowerName.getText().toString().trim().isEmpty()
                || rgColor.getCheckedRadioButtonId() == -1
                || rgPrice.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getActivity(), "Заповніть всі поля", Toast.LENGTH_SHORT).show();
            return;
        }

        View colorRB = rgColor.findViewById(rgColor.getCheckedRadioButtonId());
        int colorIdx = rgColor.indexOfChild(colorRB);

        View priceRB = rgPrice.findViewById(rgPrice.getCheckedRadioButtonId());
        int priceIdx = rgPrice.indexOfChild(priceRB);

        dbHelper.insertOrder(etFlowerName.getText().toString(), colorIdx, priceIdx);
        Toast.makeText(getActivity(), "Збережено в базу!", Toast.LENGTH_SHORT).show();

        String resultText = buildResultText();
        Bundle bundle = new Bundle();
        bundle.putString("result", resultText);
        ResultFragment rf = new ResultFragment();
        rf.setArguments(bundle);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, rf).addToBackStack(null).commit();
    }

    private String buildResultText() {
        RadioButton color =
                requireView().findViewById(rgColor.getCheckedRadioButtonId());
        RadioButton price =
                requireView().findViewById(rgPrice.getCheckedRadioButtonId());

        return "Замовлення квітів:\n"
                + "Назва: " + etFlowerName.getText().toString() + "\n"
                + "Колір: " + color.getText() + "\n"
                + "Ціна: " + price.getText();
    }

}
