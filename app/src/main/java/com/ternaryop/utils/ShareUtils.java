package com.ternaryop.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

public class ShareUtils {
    public static void shareImage(Context context, String fullPath, String mimeType, String subject, String chooserTitle) {
        ContentValues values = new ContentValues(4);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.DATA, fullPath);
        // sharing on google plus works only using MediaStore 
        Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType(mimeType);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
        sharingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        
        context.startActivity(Intent.createChooser(sharingIntent, chooserTitle));
    }
}
