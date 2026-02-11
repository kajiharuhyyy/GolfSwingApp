package com.example.golfswingapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class AnalyzerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private long startMs = 0L;
    private long endMs = Long.MAX_VALUE;
    private final float[] SPEEDS = new float[]{0.05f, 0.1f, 0.25f, 0.5f, 1.0f, 1.5f, 2.0f};
    private int speedIndex = 2; // 0.25x（0.05,0.1の次）
    private boolean editMode = false;
    private static final long FRAME_MS = 16;
    private static final int AUTO_STEP_FRAMES = 5;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyzer);

        startMs = getIntent().getLongExtra("startMs", 0L);
        endMs   = getIntent().getLongExtra("endMs", Long.MAX_VALUE);

        String uriStr = getIntent().getStringExtra("videoUri");
        String mode   = getIntent().getStringExtra("mode");
        if (uriStr == null) { finish(); return; }

        OverlayView overlayView = findViewById(R.id.overlayView);
        if (!"BACK".equals(mode)) overlayView.setVisibility(View.GONE);
        overlayView.setInputEnabled(false);

        Uri videoUri = Uri.parse(uriStr);

        playerView = findViewById(R.id.playerView);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.setMediaItem(MediaItem.fromUri(videoUri));
        player.prepare();

        // ★ここで範囲の頭へ
        player.seekTo(startMs);

        overlayView.setOnPointAddedListener((x, y) -> {
            if (player == null) return;

            // ※今は「点＝インパクト」になってる。必要ならボタンで分けよう。
            overlayView.markImpactAsLast();

            player.pause();
            long pos = player.getCurrentPosition();
            long next = Math.min(endMs, pos + FRAME_MS * AUTO_STEP_FRAMES);
            player.seekTo(next);
        });

        setupControls();

        player.play();

        // ★監視スタートはここが安全
        rangeHandler.post(rangeRunnable);
    }

    private final Handler rangeHandler = new Handler(Looper.getMainLooper());
    private final Runnable rangeRunnable = new Runnable() {
        @Override public void run() {
            if (player == null) return;
            long pos = player.getCurrentPosition();
            if (pos >= endMs) {
                player.pause();
                player.seekTo(endMs);
                saveSession();
            } else {
                rangeHandler.postDelayed(this, 33);
            }
        }
    };

    @Override protected void onStart() {
        super.onStart();
        rangeHandler.post(rangeRunnable);
    }

    private void setupControls() {
        Button btnSlower = findViewById(R.id.btnSlower);
        Button btnFaster = findViewById(R.id.btnFaster);
        Button btnUndo = findViewById(R.id.btnUndo);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnHome = findViewById(R.id.btnHome);
        Button btnTransition = findViewById(R.id.btnTransition);
        TextView txtSpeed = findViewById(R.id.txtSpeed);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnImpact = findViewById(R.id.btnImpact);

        OverlayView overlayView = findViewById(R.id.overlayView);

        applySpeed(txtSpeed);

        btnSlower.setOnClickListener(v -> {
            if (speedIndex > 0) speedIndex--;
            applySpeed(txtSpeed);
        });

        btnFaster.setOnClickListener(v -> {
            if (speedIndex < SPEEDS.length - 1) speedIndex++;
            applySpeed(txtSpeed);
        });

        btnUndo.setOnClickListener(v -> {
            overlayView.undo();
            if (player == null) return;

            long pos = player.getCurrentPosition();
            long next = pos - FRAME_MS * AUTO_STEP_FRAMES;
            if (next < 0) next = 0;
            player.seekTo(next);
        });

        btnEdit.setOnClickListener(v -> {
            editMode = !editMode;
            btnEdit.setText(editMode ? "編集ON" : "編集OFF");

            if (editMode) player.pause();
            overlayView.setInputEnabled(editMode);
        });

        btnBack.setOnClickListener(v -> finish()); // 1つ前に戻る（MainActivityへ）

        btnTransition.setOnClickListener(v -> overlayView.markTransitionAsLast());

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        btnSave.setOnClickListener(v -> saveSession());

        btnImpact.setOnClickListener(v -> overlayView.markImpactAsLast());


        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                overlayView.setInputEnabled(editMode && !isPlaying);
            }
        });
    }

    private void applySpeed(TextView txtSpeed) {
        if (player == null) return;
        float s = SPEEDS[speedIndex];
        player.setPlaybackSpeed(s);
        txtSpeed.setText(s + "x");
    }

    @Override protected void onStop() {
        rangeHandler.removeCallbacks(rangeRunnable);
        super.onStop();
        if (player != null) { player.release(); player = null; }
    }

    private Uri copyVideoToMovies(Uri srcUri, String displayName) throws Exception {
        android.content.ContentResolver cr = getContentResolver();

        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, displayName);
        values.put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/GolfSwing");

        Uri dstUri = cr.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (dstUri == null) throw new IllegalStateException("MediaStore insert failed");

        try (java.io.InputStream in = cr.openInputStream(srcUri);
             java.io.OutputStream out = cr.openOutputStream(dstUri)) {

            if (in == null || out == null) throw new IllegalStateException("Stream open failed");

            byte[] buf = new byte[1024 * 1024]; // 1MB
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        }

        return dstUri;
    }

    private Uri saveJsonToDocuments(String displayName, String json) throws Exception {
        android.content.ContentResolver cr = getContentResolver();

        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME, displayName);
        values.put(android.provider.MediaStore.Files.FileColumns.MIME_TYPE, "application/json");
        values.put(android.provider.MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents/GolfSwing");

        Uri dstUri = cr.insert(android.provider.MediaStore.Files.getContentUri("external"), values);
        if (dstUri == null) throw new IllegalStateException("MediaStore insert failed (json)");

        try (java.io.OutputStream out = cr.openOutputStream(dstUri)) {
            if (out == null) throw new IllegalStateException("OutputStream open failed (json)");
            out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
        }

        return dstUri;
    }

    private void saveSession() {
        try {
            OverlayView overlay = findViewById(R.id.overlayView);

            String originalUriStr = getIntent().getStringExtra("videoUri");
            if (originalUriStr == null) throw new IllegalStateException("videoUri is null");
            Uri originalUri = Uri.parse(originalUriStr);

            long now = System.currentTimeMillis();
            String videoName = "swing_" + now + ".mp4";
            String jsonName  = "swing_" + now + ".json";

            // 1) 動画をコピーして保存（Movies/GolfSwing）
            Uri savedVideoUri = copyVideoToMovies(originalUri, videoName);

            // 2) JSONを作る（videoUriは「コピーした先」を入れるのが重要！）
            org.json.JSONObject root = new org.json.JSONObject();
            root.put("videoUri", savedVideoUri.toString()); // ★コピー先に差し替え
            root.put("mode", getIntent().getStringExtra("mode"));
            root.put("startMs", startMs);
            root.put("endMs", endMs);
            root.put("impactIndex", overlay.getImpactIndex());
            root.put("transitionIndex", overlay.getTransitionIndex());

            org.json.JSONArray arr = new org.json.JSONArray();
            for (OverlayView.PointF p : overlay.getPoints()) {
                org.json.JSONObject o = new org.json.JSONObject();
                o.put("x", p.x);
                o.put("y", p.y);
                arr.put(o);
            }
            root.put("points", arr);

            String jsonText = root.toString();

            // 3) JSONも保存（Documents/GolfSwing）
            Uri savedJsonUri = saveJsonToDocuments(jsonName, jsonText);

            android.widget.Toast.makeText(
                    this,
                    "保存しました\n動画: Movies/GolfSwing/" + videoName + "\nJSON: Documents/GolfSwing/" + jsonName,
                    android.widget.Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            android.util.Log.e("SAVE", "saveSession failed", e);
            android.widget.Toast.makeText(this, "保存失敗: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }

}