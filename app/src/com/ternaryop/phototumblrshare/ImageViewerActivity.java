package com.ternaryop.phototumblrshare;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;

public class ImageViewerActivity extends Activity {
	private static final String IMAGE_URL = "imageUrl";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.image_viewer);

	    getActionBar().setDisplayHomeAsUpEnabled(true);

	    Bundle bundle = getIntent().getExtras();
		String imageUrl = bundle.getString(IMAGE_URL);
		WebView webView = (WebView) findViewById(R.id.imageViewer);
    	String data = "<body><img src=\"" + imageUrl +"\"/></body>";
    	webView.loadData(data, "text/html", "UTF-8");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        	// clicked the actionbar
	        	// close and return to caller
	        	finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	public static void startImageViewer(Activity activity, String url) {
		Intent intent = new Intent(activity, ImageViewerActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(IMAGE_URL, url);
		intent.putExtras(bundle);

		activity.startActivity(intent);
	}
}
