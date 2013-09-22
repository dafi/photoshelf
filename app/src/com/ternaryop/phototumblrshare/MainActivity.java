package com.ternaryop.phototumblrshare;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.webkit.WebView;

import com.ternaryop.utils.URLUtils;

public class MainActivity extends Activity implements OnLongClickListener {
	private WebView webView;
	private ImageUrlRetriever imageUrlRetriever;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		imageUrlRetriever = new ImageUrlRetriever(this);
		webView = new WebView(this);
		setContentView(webView);
		webView.setWebViewClient(new ImagePickerWebViewClient());
		webView.setOnLongClickListener(this);

		// Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	    	// Allow to click the actionbar to return to caller
		    getActionBar().setDisplayHomeAsUpEnabled(true);
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
			// resolveShortenURL can't be called on main thread so we
			// resolve into a separated thread
			new AsyncTask<String, Void, String>() {
				@Override
				protected String doInBackground(String... params) {
					return URLUtils.resolveShortenURL(params[0]);
				}
				
				@Override
				protected void onPostExecute(String url) {
					webView.loadUrl(url);
				}
			}.execute(url);
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

	@Override
	public boolean onLongClick(View v) {
		imageUrlRetriever.setTitle(webView.getTitle());
		Message msg = new Message();
		msg.setTarget(new LongClickHandler(this));
		webView.requestFocusNodeHref(msg);
		return false;
	}
	
	private final static class LongClickHandler extends Handler {
		private final MainActivity activity;

		public LongClickHandler(MainActivity activity) {
			this.activity = activity;
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String url = msg.getData().getString("url");
			String domSelector = new ImageDOMSelectorFinder(activity).getSelectorFromUrl(url);
			if (domSelector != null) {
				activity.imageUrlRetriever.addOrRemoveUrl(domSelector, url);
			}
		}
	}
}
