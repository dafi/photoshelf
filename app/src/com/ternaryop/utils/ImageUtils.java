package com.ternaryop.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class ImageUtils {

	public static Bitmap readImage(String imageUrl) throws IOException {
		URL url = new URL(imageUrl);
		HttpURLConnection connection  = (HttpURLConnection) url.openConnection();
	
		InputStream is = connection.getInputStream();
		Bitmap bitmap = BitmapFactory.decodeStream(is);
		is.close();
		connection.disconnect();
		
		return bitmap;
	}

	public static Bitmap getResizedBitmap(Bitmap bitmap, float newWidth, float newHeight) {
	    int width = bitmap.getWidth();
	    int height = bitmap.getHeight();

	    float scaleWidth = newWidth / width;
	    float scaleHeight = newHeight / height;

	    // create a matrix for the manipulation
	    Matrix matrix = new Matrix();
	    // resize the bit map
	    matrix.postScale(scaleWidth, scaleHeight);

	    // recreate the new Bitmap
	    return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
	}
}
