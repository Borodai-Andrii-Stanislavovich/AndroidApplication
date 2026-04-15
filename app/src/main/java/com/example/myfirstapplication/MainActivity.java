package com.example.myfirstapplication;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor linearAccelSensor;

    private float vx = 0, vy = 0;
    private float sx = 0, sy = 0;
    private float sTotal = 0;
    private long lastTimestamp = 0;

    private static final float NOISE_THRESHOLD = 0.15f;

    private LineChart chartX, chartY, chartTotal;
    private Spinner spinnerT, spinnerPrecision;
    private File csvFile;

    private Handler handler = new Handler();
    private Runnable logRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        csvFile = new File(getFilesDir(), "displacement_2d_data.csv");
        initUI();
        initSensor();
        startLogging();
    }

    private void initUI() {
        chartX = findViewById(R.id.chartX);
        chartY = findViewById(R.id.chartY);
        chartTotal = findViewById(R.id.chartTotal);
        spinnerT = findViewById(R.id.spinnerT);
        spinnerPrecision = findViewById(R.id.spinnerPrecision);

        setupChart(chartX, "Переміщення X (м)");
        setupChart(chartY, "Переміщення Y (м)");
        setupChart(chartTotal, "Модуль переміщення 2D (м)");

        findViewById(R.id.btnClear).setOnClickListener(v -> clearAll());
    }

    private void setupChart(LineChart chart, String label) {
        chart.getDescription().setEnabled(false);
        chart.setData(new LineData(new LineDataSet(new ArrayList<>(), label)));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new TimeAxisValueFormatter());
        xAxis.setGranularity(120000f);

        chart.setDragEnabled(true);
        chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(false);
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        if (linearAccelSensor == null) {
            Toast.makeText(this, "Лінійний акселерометр відсутній!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LINEAR_ACCELERATION) return;

        if (lastTimestamp != 0) {
            float dt = (event.timestamp - lastTimestamp) * 1.0f / 1000000000.0f;

            float ax = event.values[0];
            float ay = event.values[1];

            if (Math.abs(ax) < NOISE_THRESHOLD) { ax = 0; vx = 0; }
            if (Math.abs(ay) < NOISE_THRESHOLD) { ay = 0; vy = 0; }

            sx += vx * dt + 0.5f * ax * dt * dt;
            sy += vy * dt + 0.5f * ay * dt * dt;

            vx += ax * dt;
            vy += ay * dt;

            sTotal = (float) Math.sqrt(sx * sx + sy * sy);
        }
        lastTimestamp = event.timestamp;
    }

    private void startLogging() {
        logRunnable = new Runnable() {
            @Override
            public void run() {
                processAndLogData();
                int tSeconds = Integer.parseInt(spinnerT.getSelectedItem().toString());
                handler.postDelayed(this, tSeconds * 1000);
            }
        };
        handler.post(logRunnable);
    }

    private void processAndLogData() {
        long now = System.currentTimeMillis();
        int precision = Integer.parseInt(spinnerPrecision.getSelectedItem().toString());
        String format = "%." + precision + "f";

        saveToCsv(now, sx, sy, sTotal, format);

        addEntry(chartX, now, sx);
        addEntry(chartY, now, sy);
        addEntry(chartTotal, now, sTotal);

        checkThresholds(sTotal);
    }

    private void addEntry(LineChart chart, long x, float y) {
        LineData data = chart.getData();
        if (data != null) {
            data.addEntry(new Entry(x, y), 0);
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(600000);
            chart.moveViewToX(x);
        }
    }

    private void saveToCsv(long time, float x, float y, float total, String fmt) {
        try (FileWriter writer = new FileWriter(csvFile, true)) {
            writer.append(String.format(Locale.US, "%d," + fmt + "," + fmt + "," + fmt + "\n",
                    time, x, y, total));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void checkThresholds(float totalDisplacement) {
        if (totalDisplacement > 5.0f) {
            Toast.makeText(this, "Перевищено ліміт переміщення (> 5м)!", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAll() {
        sx = 0; sy = 0; sTotal = 0;
        vx = 0; vy = 0;
        lastTimestamp = 0;

        if (csvFile.exists()) csvFile.delete();
        chartX.getData().clearValues();
        chartY.getData().clearValues();
        chartTotal.getData().clearValues();

        chartX.invalidate();
        chartY.invalidate();
        chartTotal.invalidate();
        Toast.makeText(this, "Дані та координати очищено", Toast.LENGTH_SHORT).show();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (linearAccelSensor != null) {
            sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        lastTimestamp = 0;
        vx = 0; vy = 0;
    }
}