package com.example.golfswingapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class AnalyzerActivity extends AppCompatActivity {

    private ExoPlayer player;
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

        String uriStr = getIntent().getStringExtra("videoUri");
        String mode = getIntent().getStringExtra("mode");

        if (uriStr == null) {
            finish();
            return;
        }

        OverlayView overlayView = findViewById(R.id.overlayView);
        if (!"BACK".equals(mode)) {
            overlayView.setVisibility(View.GONE);
        }

        overlayView.setInputEnabled(false);

        overlayView.setOnPointAddedListener((x, y) -> {
            if (player == null) return;

            overlayView.markImpactAsLast();

            player.pause();
            long pos = player.getCurrentPosition();
            player.seekTo(pos + FRAME_MS * AUTO_STEP_FRAMES);
        });

        Uri videoUri = Uri.parse(uriStr);

        playerView = findViewById(R.id.playerView);
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);


        player.setMediaItem(MediaItem.fromUri(videoUri));
        player.prepare();

        setupControls();

        player.play();

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

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}