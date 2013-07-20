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
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("text/plain".equals(type)) {
	            handleSendText(intent);
	        }
	    } else {
			beginPostToTumblr("aa http://kosty555.info/topic/214066-scarlett-johansson-films-a-new-commercial-for-dolce-gabbana-in-manhattan-new-york-city-jul-13-2013-x11/");
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
			WebView view = (WebView) findViewById(R.id.webView);
			view.setWebViewClient(new ImagePickerWebViewClient(this));
			view.loadUrl(url);
		} else {
			String message = getResources().getString(R.string.url_not_found_description);
			new AlertDialog.Builder(this)
				.setTitle(R.string.url_not_found)
				.setMessage(String.format(message, textWithUrl))
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
