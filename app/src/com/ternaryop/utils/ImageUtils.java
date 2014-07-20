package com.ternaryop.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.WindowManager;

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

    public static Bitmap scaleBitmapForDefaultDisplay(Context context, Bitmap bitmap) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE))
        .getDefaultDisplay()
        .getMetrics(metrics);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = metrics.scaledDensity;
        float scaleHeight = metrics.scaledDensity;

        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);

        // recreate the new Bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    public static void saveImageAsPNG(Bitmap image, File fullPath) throws IOException {
        OutputStream fout = null;

        try {
            fout = new FileOutputStream(fullPath);
            image.compress(Bitmap.CompressFormat.PNG, 100, fout);
            fout.flush();
        } finally {
            if (fout != null) try { fout.close(); } catch (Exception ex) {}
        }
    }
}
