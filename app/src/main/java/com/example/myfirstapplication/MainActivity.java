package com.example.myfirstapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private View bubble;
    private TextView tvAngles;

    // Чутливість (на скільки пікселів зсувається бульбашка при нахилі)
    private static final int SENSITIVITY = 50;

    // Гравітаційна стала для розрахунку кутів
    private static final float GRAVITY = SensorManager.GRAVITY_EARTH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bubble = findViewById(R.id.bubble);
        tvAngles = findViewById(R.id.tvAngles);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];

            // 1. Розрахунок кутів нахилу в градусах
            // Обмежуємо значення від -1 до 1, щоб уникнути помилок математики
            float normalizedX = Math.max(-1.0f, Math.min(1.0f, x / GRAVITY));
            float normalizedY = Math.max(-1.0f, Math.min(1.0f, y / GRAVITY));

            int angleX = (int) Math.toDegrees(Math.asin(normalizedX));
            int angleY = (int) Math.toDegrees(Math.asin(normalizedY));

            // 2. Оновлення тексту на екрані
            tvAngles.setText("Нахил X: " + angleX + "°\nНахил Y: " + angleY + "°");

            // 3. Зсув бульбашки по екрану
            // Знак мінус для осі X, щоб бульбашка "спливала" вгору проти гравітації, як справжня
            bubble.setTranslationX(x * SENSITIVITY);
            bubble.setTranslationY(-y * SENSITIVITY);

            // 4. Візуалізація: зміна кольору, якщо телефон лежить рівно (похибка 2 градуси)
            if (Math.abs(angleX) <= 2 && Math.abs(angleY) <= 2) {
                bubble.setBackgroundResource(R.drawable.bubble_green);
            } else {
                bubble.setBackgroundResource(R.drawable.bubble_red);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Залишаємо порожнім, для цієї задачі зміна точності не критична
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            // Використовуємо SENSOR_DELAY_GAME для плавної анімації (приблизно 50 кадрів на секунду)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}