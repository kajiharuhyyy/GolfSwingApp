package com.example.golfswingapp;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

public class VideoUriUtil {

    public static Uri createVideoUri(Context context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "golf_swing_" + System.currentTimeMillis() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GolfSwingApp");

        Uri uri = context.getContentResolver().insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (uri == null) {
            throw new IllegalStateException("Failed to create MediaStore video Uri");
        }
        return uri;
    }
}
