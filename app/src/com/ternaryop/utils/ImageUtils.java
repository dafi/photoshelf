package com.ternaryop.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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

}
