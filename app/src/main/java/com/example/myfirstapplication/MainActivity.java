package com.example.myfirstapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView sensorListTextView;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorListTextView = findViewById(R.id.sensorListTextView);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        StringBuilder sensorInfo = new StringBuilder();
        sensorInfo.append("Всього знайдено датчиків: ").append(deviceSensors.size()).append("\n\n");

        for (Sensor s : deviceSensors) {
            sensorInfo.append("Назва: ").append(s.getName()).append("\n");
            sensorInfo.append("Тип: ").append(s.getStringType()).append("\n");
            sensorInfo.append("Виробник: ").append(s.getVendor()).append("\n");
            sensorInfo.append("----------------------------\n");
        }

        sensorListTextView.setText(sensorInfo.toString());
    }
}