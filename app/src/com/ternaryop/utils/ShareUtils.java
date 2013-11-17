package com.ternaryop.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

public class ShareUtils {
    public static void shareFile(Context context, String fullPath, String mimeType, String subject, String chooserTitle) {
		ContentValues values = new ContentValues(2);
	    values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
	    values.put(MediaStore.Images.Media.DATA, fullPath);
	    // sharing on google plus works only using MediaStore 
	    Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType(mimeType);
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
		
		context.startActivity(Intent.createChooser(sharingIntent, chooserTitle));
	}

}
