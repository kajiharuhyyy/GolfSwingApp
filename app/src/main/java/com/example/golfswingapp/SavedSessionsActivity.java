package com.example.golfswingapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SavedSessionsActivity extends AppCompatActivity {

    static class SessionItem {
        final Uri uri;
        final String name;
        final long dateAddedSec;
        SessionItem(Uri uri, String name, long dateAddedSec) {
            this.uri = uri;
            this.name = name;
            this.dateAddedSec = dateAddedSec;
        }
    }

    private final List<SessionItem> items = new ArrayList<>();
    private SessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionAdapter(items, item -> {
            Intent intent = new Intent(this, AnalyzerActivity.class);
            intent.putExtra("sessionUri", item.uri);
            startActivity(intent);
        });
        recycler.setAdapter(adapter);

        loadSessions();
    }

    private void loadSessions() {
        items.clear();

        Uri collection = MediaStore.Files.getContentUri("external");
        String[] projection = new String[] {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.RELATIVE_PATH,
                MediaStore.Files.FileColumns.MIME_TYPE
        };

        // Documents/GolfSwing に入れた JSON を拾う
        String selection =
                MediaStore.Files.FileColumns.MIME_TYPE + "=? AND " +
                        MediaStore.Files.FileColumns.RELATIVE_PATH + "=?";

        String[] selectionArgs = new String[] {
                "application/json",
                "Documents/GolfSwing/"
        };

        String sortOrder = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, sortOrder)) {
            if (cursor == null) return;

            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String name = cursor.getString(nameCol);
                long dateAdded = cursor.getLong(dateCol);

                Uri uri = Uri.withAppendedPath(collection, String.valueOf(id));
                items.add(new SessionItem(uri, name, dateAdded));
            }
        }

        adapter.notifyDataSetChanged();
    }
}
