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
    private Sensor tempSensor, humSensor, pressSensor;
    private float lastTemp = 1.0f, lastHum = 1.0f, lastPress = 1.0f;
    private boolean hasTemp, hasHum, hasPress;

    private LineChart chartTemp, chartHum, chartPress, chartGcc;
    private Spinner spinnerT, spinnerPrecision;
    private File csvFile;

    private Handler handler = new Handler();
    private Runnable logRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        csvFile = new File(getFilesDir(), "weather_data.csv");
        initUI();
        initSensors();
        startLogging();
    }

    private void initUI() {
        chartTemp = findViewById(R.id.chartTemp);
        chartHum = findViewById(R.id.chartHum);
        chartPress = findViewById(R.id.chartPress);
        chartGcc = findViewById(R.id.chartGcc);
        spinnerT = findViewById(R.id.spinnerT);
        spinnerPrecision = findViewById(R.id.spinnerPrecision);

        setupChart(chartTemp, "Температура (°C)");
        setupChart(chartHum, "Вологість (%)");
        setupChart(chartPress, "Тиск (hPa)");
        setupChart(chartGcc, "GCC (T*H*P)");

        findViewById(R.id.btnClear).setOnClickListener(v -> clearAll());
    }

    private void setupChart(LineChart chart, String label) {
        chart.getDescription().setEnabled(false);
        chart.setData(new LineData(new LineDataSet(new ArrayList<>(), label)));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new TimeAxisValueFormatter());
        xAxis.setGranularity(120000f); // 2 хвилини у мілісекундах (120 * 1000)

        chart.setDragEnabled(true);
        chart.setScaleXEnabled(true);
        chart.setScaleYEnabled(false);
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        humSensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        pressSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        hasTemp = (tempSensor != null);
        hasHum = (humSensor != null);
        hasPress = (pressSensor != null);

        if (!hasTemp || !hasHum || !hasPress) {
            Toast.makeText(this, "Деякі датчики відсутні. Значення замінено на 1.", Toast.LENGTH_LONG).show();
        }
    }

    private void startLogging() {
        logRunnable = new Runnable() {
            @Override
            public void run() {
                processData();
                int tSeconds = Integer.parseInt(spinnerT.getSelectedItem().toString());
                handler.postDelayed(this, tSeconds * 1000);
            }
        };
        handler.post(logRunnable);
    }

    private void processData() {
        long now = System.currentTimeMillis();
        int precision = Integer.parseInt(spinnerPrecision.getSelectedItem().toString());
        String format = "%." + precision + "f";

        float gcc = lastTemp * lastHum * lastPress;

        saveToCsv(now, lastTemp, lastHum, lastPress, gcc, format);

        if (hasTemp) addEntry(chartTemp, now, lastTemp);
        if (hasHum) addEntry(chartHum, now, lastHum);
        if (hasPress) addEntry(chartPress, now, lastPress);
        addEntry(chartGcc, now, gcc);

        checkThresholds(lastTemp, lastHum, lastPress);
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

    private void saveToCsv(long time, float t, float h, float p, float gcc, String fmt) {
        try (FileWriter writer = new FileWriter(csvFile, true)) {
            writer.append(String.format(Locale.US, "%d," + fmt + "," + fmt + "," + fmt + "," + fmt + "\n",
                    time, t, h, p, gcc));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void clearAll() {
        if (csvFile.exists()) csvFile.delete();
        chartTemp.getData().clearValues();
        chartHum.getData().clearValues();
        chartPress.getData().clearValues();
        chartGcc.getData().clearValues();

        chartTemp.invalidate();
        chartHum.invalidate();
        chartPress.invalidate();
        chartGcc.invalidate();
        Toast.makeText(this, "Дані очищено", Toast.LENGTH_SHORT).show();
    }

    private void checkThresholds(float t, float h, float p) {
        if (t > 40) Toast.makeText(this, "Критична температура!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) lastTemp = event.values[0];
        if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) lastHum = event.values[0];
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) lastPress = event.values[0];
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (hasTemp) sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_UI);
        if (hasHum) sensorManager.registerListener(this, humSensor, SensorManager.SENSOR_DELAY_UI);
        if (hasPress) sensorManager.registerListener(this, pressSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}