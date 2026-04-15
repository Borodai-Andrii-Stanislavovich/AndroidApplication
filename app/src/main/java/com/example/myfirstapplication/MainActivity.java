package com.example.myfirstapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private View bubble;
    private TextView tvAngles, tvTrigX, tvTrigY;
    private Button btnMark, btnViewMarks;

    private DatabaseHelper dbHelper;

    private static final int SENSITIVITY = 50;
    private static final float GRAVITY = SensorManager.GRAVITY_EARTH;

    private int currentAngleX = 0;
    private int currentAngleY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        bubble = findViewById(R.id.bubble);
        tvAngles = findViewById(R.id.tvAngles);
        tvTrigX = findViewById(R.id.tvTrigX);
        tvTrigY = findViewById(R.id.tvTrigY);
        btnMark = findViewById(R.id.btnMark);
        btnViewMarks = findViewById(R.id.btnViewMarks);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        btnMark.setOnClickListener(v -> showMarkDialog());
        btnViewMarks.setOnClickListener(v -> showMarksListDialog());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];

            float normalizedX = Math.max(-1.0f, Math.min(1.0f, x / GRAVITY));
            float normalizedY = Math.max(-1.0f, Math.min(1.0f, y / GRAVITY));

            currentAngleX = (int) Math.toDegrees(Math.asin(normalizedX));
            currentAngleY = (int) Math.toDegrees(Math.asin(normalizedY));

            tvAngles.setText("Нахил X: " + currentAngleX + "°\nНахил Y: " + currentAngleY + "°");

            double radX = Math.toRadians(currentAngleX);
            double radY = Math.toRadians(currentAngleY);

            String trigXStr = String.format(Locale.US, "X\nsin: %.3f\ncos: %.3f\ntan: %.3f",
                    Math.sin(radX), Math.cos(radX), Math.tan(radX));
            String trigYStr = String.format(Locale.US, "Y\nsin: %.3f\ncos: %.3f\ntan: %.3f",
                    Math.sin(radY), Math.cos(radY), Math.tan(radY));

            tvTrigX.setText(trigXStr);
            tvTrigY.setText(trigYStr);

            bubble.setTranslationX(x * SENSITIVITY);
            bubble.setTranslationY(-y * SENSITIVITY);

            if (Math.abs(currentAngleX) <= 2 && Math.abs(currentAngleY) <= 2) {
                bubble.setBackgroundResource(R.drawable.bubble_green);
            } else {
                bubble.setBackgroundResource(R.drawable.bubble_red);
            }
        }
    }

    private void showMarkDialog() {

        final int capturedAngleX = currentAngleX;
        final int capturedAngleY = currentAngleY;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Створити мітку");
        builder.setMessage("Кут X: " + capturedAngleX + "°, Кут Y: " + capturedAngleY + "°\nВведіть опис мітки:");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Зберегти", (dialog, which) -> {
            String description = input.getText().toString();

            dbHelper.addMark(capturedAngleX, capturedAngleY, description);

            Toast.makeText(this, "Мітку збережено!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Скасувати", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showMarksListDialog() {
        Cursor cursor = dbHelper.getAllMarks();

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "База даних порожня", Toast.LENGTH_SHORT).show();
            if (cursor != null) cursor.close();
            return;
        }

        ArrayList<String> displayList = new ArrayList<>();
        ArrayList<Long> idList = new ArrayList<>();

        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
            int ax = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ANGLE_X));
            int ay = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ANGLE_Y));
            String desc = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESC));

            displayList.add("X: " + ax + "° | Y: " + ay + "°\nОпис: " + desc);
            idList.add(id);
        }
        cursor.close();

        String[] items = displayList.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Збережені мітки");

        builder.setItems(items, (dialog, which) -> {
            long selectedId = idList.get(which);
            String currentDesc = items[which].split("Опис: ")[1];
            showEditDeleteDialog(selectedId, currentDesc);
        });

        builder.setPositiveButton("Закрити", null);
        builder.show();
    }

    private void showEditDeleteDialog(long markId, String currentDescription) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Керування міткою");

        final EditText input = new EditText(this);
        input.setText(currentDescription);
        builder.setView(input);

        builder.setPositiveButton("Оновити", (dialog, which) -> {
            String newDesc = input.getText().toString();
            dbHelper.updateMarkDescription(markId, newDesc);
            Toast.makeText(this, "Мітку оновлено", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Видалити", (dialog, which) -> {
            dbHelper.deleteMark(markId);
            Toast.makeText(this, "Мітку видалено", Toast.LENGTH_SHORT).show();
        });

        builder.setNeutralButton("Скасувати", null);
        builder.show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}