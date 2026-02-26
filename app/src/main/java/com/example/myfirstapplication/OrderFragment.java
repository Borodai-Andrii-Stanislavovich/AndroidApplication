package com.example.myfirstapplication;

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

        return view;
    }

    private void handleOkClick() {
        if (etFlowerName.getText().toString().trim().isEmpty()
                || rgColor.getCheckedRadioButtonId() == -1
                || rgPrice.getCheckedRadioButtonId() == -1) {

            Toast.makeText(getActivity(),
                    "Заповніть всі поля",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String resultText = buildResultText();

        Bundle bundle = new Bundle();
        bundle.putString("result", resultText);

        ResultFragment resultFragment = new ResultFragment();
        resultFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, resultFragment)
                .addToBackStack(null)
                .commit();
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

//    public void clearForm() {
//        etFlowerName.setText("");
//        rgColor.clearCheck();
//        rgPrice.clearCheck();
//    }
}
