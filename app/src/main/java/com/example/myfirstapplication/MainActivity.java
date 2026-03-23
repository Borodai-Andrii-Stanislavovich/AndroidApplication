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
    private View rewindForwardContainer;
    private VideoView videoView;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private TextView tvTime, tvMetadata;
    private Uri currentUri;
    private boolean isVideo = false;
    private Handler handler = new Handler();
    private DatabaseHelper dbHelper;
    private View audioContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        seekBar = findViewById(R.id.seekBar);
        tvTime = findViewById(R.id.tvTime);
        tvMetadata = findViewById(R.id.tvMetadata);
        dbHelper = new DatabaseHelper(this);
        audioContainer = findViewById(R.id.audioContainer);

        rewindForwardContainer = findViewById(R.id.rewindForwardContainer);


        findViewById(R.id.btnRadioList).setOnClickListener(v -> showRadioListDialog());
        findViewById(R.id.btnDirectUrl).setOnClickListener(v -> showUrlDialog());

        // Вибір локального файлу
        findViewById(R.id.btnPickLocal).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimeTypes = {"audio/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(intent, 101);
        });


        // Керування
        findViewById(R.id.btnPlay).setOnClickListener(v -> playMedia());
        findViewById(R.id.btnPause).setOnClickListener(v -> pauseMedia());
        findViewById(R.id.btnStop).setOnClickListener(v -> stopMedia());

        // Перемотування (Зауваження 2)
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
        String[] names = new String[cursor.getCount() + 1];
        String[] urls = new String[cursor.getCount() + 1];

        int i = 0;
        while (cursor.moveToNext()) {
            names[i] = cursor.getString(1); // NAME
            urls[i] = cursor.getString(2); // URL
            i++;
        }
        names[i] = "[ + Додати нову станцію ]";

        new AlertDialog.Builder(this)
                .setTitle("Оберіть станцію")
                .setItems(names, (dialog, clicked) -> {
                    if (clicked == names.length - 1) {
                        showAddStationDialog();
                    } else {
                        currentUri = Uri.parse(urls[clicked]);
                        isVideo = false;
                        prepareMedia();
                        audioContainer.setVisibility(View.GONE); // Приховуємо часову лінію (Пункт 1)
                    }
                }).show();
    }

    private void showAddStationDialog() {
        LinearLayout layout = new LinearLayout(this);
        // Виправлено: VERTICAL замість vertical
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Назва");

        final EditText urlInput = new EditText(this);
        urlInput.setHint("URL (https://...)");

        layout.addView(nameInput);
        layout.addView(urlInput);

        new AlertDialog.Builder(this)
                .setTitle("Нова станція")
                .setView(layout)
                .setPositiveButton("Зберегти", (d, w) -> {
                    String name = nameInput.getText().toString();
                    String url = urlInput.getText().toString();
                    if (!name.isEmpty() && !url.isEmpty()) {
                        dbHelper.addStation(name, url);
                        Toast.makeText(this, "Додано!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Скасувати", null)
                .show();
    }

    private void showUrlDialog() {
        EditText input = new EditText(this);
        input.setHint("Вставте URL потоку (mp3/mp4/aac)");
        new AlertDialog.Builder(this)
                .setTitle("Відтворити з мережі")
                .setView(input)
                .setPositiveButton("Грати", (d, w) -> {
                    String url = input.getText().toString();
                    if (!url.isEmpty()) {
                        currentUri = Uri.parse(url);
                        // Якщо в URL є розширення відео, ставимо прапорець
                        isVideo = url.toLowerCase().contains(".mp4") || url.toLowerCase().contains(".mkv");
                        prepareMedia();
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            String mime = getContentResolver().getType(selectedUri);

            if (mime != null && (mime.startsWith("audio/") || mime.startsWith("video/"))) {
                currentUri = selectedUri;
                isVideo = mime.startsWith("video/");
                prepareMedia();
                loadMetadata(currentUri);
            } else {
                Toast.makeText(this, "Тільки аудіо або відео!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void prepareMedia() {
        stopMedia();

        // Перевіряємо, чи це локальний файл з пам'яті пристрою
        boolean isLocalFile = (currentUri != null && "content".equals(currentUri.getScheme()));

        if (isVideo || isLocalFile) {
            // Показуємо все для відео або локальних аудіо-файлів
            audioContainer.setVisibility(View.VISIBLE);
            if (rewindForwardContainer != null) {
                rewindForwardContainer.setVisibility(View.VISIBLE);
            }
        } else {
            // Ховаємо лінійку та кнопки перемоту для РАДІО (URL-потоків)
            audioContainer.setVisibility(View.GONE);
            if (rewindForwardContainer != null) {
                rewindForwardContainer.setVisibility(View.GONE);
            }
        }

        if (isVideo) {
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(currentUri);
            videoView.setMediaController(new MediaController(this));
            videoView.setOnPreparedListener(mp -> {
                seekBar.setMax(videoView.getDuration());
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
                    if (mp.getDuration() > 0) {
                        seekBar.setMax(mp.getDuration());
                    }
                    updateSeekBar();
                });
                mediaPlayer.setLooping(true);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                Toast.makeText(this, "Помилка відтворення", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void seekRelative(int delta) {
        if (isVideo) videoView.seekTo(videoView.getCurrentPosition() + delta);
        else if (mediaPlayer != null) mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + delta);
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int curr = isVideo ? videoView.getCurrentPosition() : (mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0);
                int dur = isVideo ? videoView.getDuration() : (mediaPlayer != null ? mediaPlayer.getDuration() : 0);
                seekBar.setProgress(curr);
                tvTime.setText(formatTime(curr) + " / " + formatTime(dur));
                handler.postDelayed(this, 1000);
            }
        }, 0);
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
            tvMetadata.setText("Зараз грає: " + (title != null ? title : "Unknown") + " - " + artist);
        } catch (Exception e) { tvMetadata.setText("Метадані недоступні"); }
    }
}

