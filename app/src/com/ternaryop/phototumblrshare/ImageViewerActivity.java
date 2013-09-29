package com.ternaryop.phototumblrshare;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class ImageViewerActivity extends Activity {
	private static final String IMAGE_URL = "imageUrl";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);

	    getActionBar().setDisplayHomeAsUpEnabled(true);

	    ProgressBar progressBar = (ProgressBar) findViewById(R.id.webview_progressbar);
	    
	    Bundle bundle = getIntent().getExtras();
		String imageUrl = bundle.getString(IMAGE_URL);
    	String data = "<body><img src=\"" + imageUrl +"\"/></body>";
    	prepareWebView(progressBar).loadData(data, "text/html", "UTF-8");
	}

	private WebView prepareWebView(final ProgressBar progressBar) {
		WebView webView = (WebView) findViewById(R.id.webview_view);
		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				progressBar.setProgress(newProgress);
			}
		});
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				progressBar.setProgress(0);
				progressBar.setVisibility(View.VISIBLE);
			}
			@Override
			public void onPageFinished(WebView view, String url) {
				progressBar.setVisibility(View.GONE);
			}
		});
		return webView;
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
