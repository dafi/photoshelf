package com.ternaryop.phototumblrshare;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.webkit.WebView;

public class MainActivity extends Activity {
	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		webView = new WebView(this);
		setContentView(webView);
		webView.setWebViewClient(new ImagePickerWebViewClient());

		// Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("text/plain".equals(type)) {
	            handleSendText(intent);
	        }
	    } else {
			webView.loadUrl("file:///android_asset/index.html");
	    }
	}

	void handleSendText(Intent intent) {
		String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

	    if (sharedText != null) {
	    	beginPostToTumblr(sharedText);
	    }
	}

	private void beginPostToTumblr(String textWithUrl) {
		final Matcher m = Pattern.compile("(http:.*)").matcher(textWithUrl);

		if (m.find()) {
			String url = m.group(1);
			webView.loadUrl(url);
		} else {
			new AlertDialog.Builder(this)
				.setTitle(R.string.url_not_found)
				.setMessage(getResources().getString(R.string.url_not_found_description, textWithUrl))
				.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
