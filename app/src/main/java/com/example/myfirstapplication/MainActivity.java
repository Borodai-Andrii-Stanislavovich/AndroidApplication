package com.example.myfirstapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView tvTime, tvMetadata;
    private Uri currentUri;
    private boolean isVideo = false;
    private Handler handler = new Handler();
    private DatabaseHelper dbHelper;

    private View audioContainer;
    private View rewindForwardContainer;
    private String currentStationName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        seekBar = findViewById(R.id.seekBar);
        tvTime = findViewById(R.id.tvTime);
        tvMetadata = findViewById(R.id.tvMetadata);
        audioContainer = findViewById(R.id.audioContainer);
        rewindForwardContainer = findViewById(R.id.rewindForwardContainer);

        dbHelper = new DatabaseHelper(this);

        // Кнопка: Обрати файл з пристрою
        findViewById(R.id.btnPickLocal).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {"audio/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(intent, 101);
        });

        // Кнопка: Радіо Android++
        findViewById(R.id.btnRadioList).setOnClickListener(v -> showRadioListDialog());

        // Кнопка: Ввести URI Інтернет (тільки для аудіо/радіо)
        findViewById(R.id.btnDirectUrl).setOnClickListener(v -> showUrlDialog());

        // Керування відтворенням
        findViewById(R.id.btnPlay).setOnClickListener(v -> playMedia());
        findViewById(R.id.btnPause).setOnClickListener(v -> pauseMedia());
        findViewById(R.id.btnStop).setOnClickListener(v -> stopMedia());

        // Кнопки перемотування
        findViewById(R.id.btnRewind).setOnClickListener(v -> seekRelative(-15000));
        findViewById(R.id.btnForward).setOnClickListener(v -> seekRelative(15000));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (isVideo) videoView.seekTo(progress);
                    else if (mediaPlayer != null) mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void showRadioListDialog() {
        Cursor cursor = dbHelper.getAllStations();
        final String[] names = new String[cursor.getCount() + 1];
        final String[] urls = new String[cursor.getCount() + 1];

        int i = 0;
        while (cursor.moveToNext()) {
            names[i] = cursor.getString(1);
            urls[i] = cursor.getString(2);
            i++;
        }
        cursor.close();
        names[i] = "[ + Додати нову станцію ]";

        new AlertDialog.Builder(this)
                .setTitle("Радіо Android++")
                .setItems(names, (dialog, clicked) -> {
                    if (clicked == names.length - 1) {
                        showAddStationDialog();
                    } else {
                        currentUri = Uri.parse(urls[clicked]);
                        currentStationName = names[clicked];
                        isVideo = false; // Радіо — це завжди аудіо
                        prepareMedia();
                    }
                }).show();
    }

    private void showAddStationDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        final EditText nameInput = new EditText(this); nameInput.setHint("Назва станції");
        final EditText urlInput = new EditText(this); urlInput.setHint("URL (http/https)");
        layout.addView(nameInput); layout.addView(urlInput);

        new AlertDialog.Builder(this)
                .setTitle("Нова станція")
                .setView(layout)
                .setPositiveButton("Зберегти", (d, w) -> {
                    dbHelper.addStation(nameInput.getText().toString(), urlInput.getText().toString());
                    showRadioListDialog();
                })
                .setNegativeButton("Скасувати", (d, w) -> showRadioListDialog()).show();
    }

    private void showUrlDialog() {
        EditText input = new EditText(this);
        input.setHint("Вставте посилання з Google Диску");

        new AlertDialog.Builder(this)
                .setTitle("Медіа з Google Диску")
                .setView(input)
                .setPositiveButton("Грати", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (url.contains("drive.google.com")) {
                        // Конвертуємо посилання у пряме (uc?export=download)
                        url = convertDriveLink(url);
                        currentUri = Uri.parse(url);
                        currentStationName = "Файл з Хмари";

                        // Для Google Диску ми не завжди знаємо розширення
                        isVideo = true;
                        prepareMedia();
                    } else {
                        Toast.makeText(this, "Це не посилання на Google Диск", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Скасувати", null)
                .show();
    }

    // Допоміжний метод для конвертації посилань
    private String convertDriveLink(String url) {
        if (url.contains("/file/d/")) {
            String id = url.split("/file/d/")[1].split("/")[0];
            return "https://drive.google.com/uc?export=download&id=" + id;
        }
        return url;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            currentUri = data.getData();
            String mime = getContentResolver().getType(currentUri);
            if (mime != null && (mime.startsWith("audio/") || mime.startsWith("video/"))) {
                isVideo = mime.startsWith("video/");
                prepareMedia();
                loadMetadata(currentUri);
            }
        }
    }

    private void prepareMedia() {
        stopMedia();

        String uriString = (currentUri != null) ? currentUri.toString() : "";
        boolean isLocalFile = (currentUri != null && "content".equals(currentUri.getScheme()));
        // Перевіряємо, чи це наше конвертоване посилання з Диску
        boolean isGoogleDrive = uriString.contains("drive.google.com");

        // Показуємо інтерфейс керування для локальних файлів та для файлів з Google Диску
        if (isVideo || isLocalFile || isGoogleDrive) {
            audioContainer.setVisibility(View.VISIBLE);
            if (rewindForwardContainer != null) {
                rewindForwardContainer.setVisibility(View.VISIBLE);
            }
        } else {
            // Ховаємо тільки для Радіо (де немає фіксованої тривалості)
            audioContainer.setVisibility(View.GONE);
            if (rewindForwardContainer != null) {
                rewindForwardContainer.setVisibility(View.GONE);
            }
        }

        if (isVideo || isGoogleDrive) {
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(currentUri);

            MediaController mc = new MediaController(this);
            mc.setAnchorView(videoView);
            videoView.setMediaController(mc);

            videoView.setOnPreparedListener(mp -> {
                if (mp.getDuration() > 0) {
                    seekBar.setMax(mp.getDuration());
                }
                updateSeekBar();
                videoView.start();
            });
            videoView.setOnCompletionListener(mp -> videoView.start());
        } else {
            videoView.setVisibility(View.GONE);
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, currentUri);
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    if (mp.getDuration() > 0) seekBar.setMax(mp.getDuration());
                    updateSeekBar();
                });
                mediaPlayer.setLooping(true);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Toast.makeText(this, "Помилка відтворення", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null || (isVideo && videoView.isPlaying())) {
                    if (audioContainer.getVisibility() == View.GONE) {
                        tvMetadata.setText("Радіо: " + currentStationName);
                        tvTime.setText("Прямий ефір");
                    } else {
                        int curr = isVideo ? videoView.getCurrentPosition() : mediaPlayer.getCurrentPosition();
                        int dur = isVideo ? videoView.getDuration() : mediaPlayer.getDuration();
                        seekBar.setProgress(curr);
                        tvTime.setText(formatTime(curr) + " / " + formatTime(dur));
                    }
                    handler.postDelayed(this, 1000);
                }
            }
        }, 0);
    }

    private void seekRelative(int delta) {
        if (isVideo) videoView.seekTo(videoView.getCurrentPosition() + delta);
        else if (mediaPlayer != null) mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + delta);
    }

    private String formatTime(int ms) {
        int s = (ms / 1000) % 60;
        int m = (ms / 60000) % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void playMedia() { if(isVideo) videoView.start(); else if(mediaPlayer != null) mediaPlayer.start(); }
    private void pauseMedia() { if(isVideo) videoView.pause(); else if(mediaPlayer != null) mediaPlayer.pause(); }
    private void stopMedia() {
        handler.removeCallbacksAndMessages(null);
        if (isVideo) { videoView.stopPlayback(); videoView.setVisibility(View.GONE); }
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        seekBar.setProgress(0);
        tvTime.setText("00:00 / 00:00");
    }

    private void loadMetadata(Uri uri) {
        try {
            MediaMetadataRetriever r = new MediaMetadataRetriever();
            r.setDataSource(this, uri);
            String title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            tvMetadata.setText((title != null ? title : "Локальний файл") + " - " + (artist != null ? artist : "Невідомо"));
        } catch (Exception e) { tvMetadata.setText("Метадані недоступні"); }
    }
}