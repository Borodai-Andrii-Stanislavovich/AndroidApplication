package com.example.myfirstapplication;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText etFlowerName;
    RadioGroup rgColor, rgPrice;
    TextView tvResult;
    Button btnOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etFlowerName = findViewById(R.id.etFlowerName);
        rgColor = findViewById(R.id.rgColor);
        rgPrice = findViewById(R.id.rgPrice);
        tvResult = findViewById(R.id.tvResult);
        btnOk = findViewById(R.id.btnOk);

        btnOk.setOnClickListener(v -> {
            String flower = etFlowerName.getText().toString().trim();

            if (flower.isEmpty()
                    || rgColor.getCheckedRadioButtonId() == -1
                    || rgPrice.getCheckedRadioButtonId() == -1) {

                Toast.makeText(this,
                        "Заповніть всі поля",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton colorBtn =
                    findViewById(rgColor.getCheckedRadioButtonId());
            RadioButton priceBtn =
                    findViewById(rgPrice.getCheckedRadioButtonId());

            String result = "Замовлення:\n"
                    + "Квіти: " + flower + "\n"
                    + "Колір: " + colorBtn.getText() + "\n"
                    + "Ціна: " + priceBtn.getText();

            tvResult.setText(result);
        });
    }
}
