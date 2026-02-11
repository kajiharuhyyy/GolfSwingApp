package com.example.golfswingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.AlternativeSpan;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    private String selectedMode = "BACK";
    private Uri pendingCameraUri;

    private ActivityResultLauncher<String> pickFromGallery =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) launchAnalyzer(uri);
            });

    private final ActivityResultLauncher<String[]> pickFromFiles =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    launchAnalyzer(uri);
                }
            });

    private final ActivityResultLauncher<Uri> takeVideo =
            registerForActivityResult(new ActivityResultContracts.CaptureVideo(), success -> {
                if (success && pendingCameraUri != null) {
                    launchAnalyzer(pendingCameraUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnBackMode = findViewById(R.id.btnBackMode);
        Button btnFrontMode = findViewById(R.id.btnFrontMode);


        btnBackMode.setOnClickListener(v -> {
            selectedMode = "BACK";
            showSourceChooser();
        });
        btnFrontMode.setOnClickListener(v -> {
            selectedMode = "FRONT";
            showSourceChooser();
        });
    }

    private void showSourceChooser() {
        String[] items = {"カメラで撮る", "写真フォルダから選ぶ", "ファイルから選ぶ"};

        new AlertDialog.Builder(this)
                .setTitle("動画の取り込み方法を選択")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        startCameraRecording();
                    } else if (which == 1) {
                        pickFromGallery.launch("video/*");
                    } else {
                        pickFromFiles.launch(new String[]{"video/*"});
                    }
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void startCameraRecording() {
        pendingCameraUri = VideoUriUtil.createVideoUri(this);
        takeVideo.launch(pendingCameraUri);
    }

    private void launchAnalyzer(Uri videoUri) {
        Intent i = new Intent(this, AnalyzerActivity.class);
        i.putExtra("videoUri", videoUri.toString());
        i.putExtra("mode", selectedMode);
        startActivity(i);
    }
}