package com.example.myfirstapplication;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TableLayout table = findViewById(R.id.tableOrders);
        DatabaseHelper db = new DatabaseHelper(this);

        addHeader(table);

        Cursor cursor = db.getOrdersWithDetails();
        while (cursor.moveToNext()) {
            TableRow row = new TableRow(this);
            row.addView(makeCell(String.valueOf(cursor.getInt(0))));
            row.addView(makeCell(cursor.getString(1)));
            row.addView(makeCell(cursor.getString(2)));
            row.addView(makeCell(cursor.getString(3)));
            table.addView(row);
        }
        cursor.close();
    }

    private void addHeader(TableLayout table) {
        TableRow header = new TableRow(this);
        header.setBackgroundColor(Color.DKGRAY);
        String[] titles = {"ID", "Назва", "Колір", "Ціна"};
        for (String s : titles) {
            TextView tv = makeCell(s);
            tv.setTextColor(Color.WHITE);
            header.addView(tv);
        }
        table.addView(header);
    }

    private TextView makeCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(15, 15, 15, 15);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }
}