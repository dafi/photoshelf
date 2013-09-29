package com.ternaryop.phototumblrshare;

import java.util.List;
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
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.ternaryop.phototumblrshare.ImageUrlRetriever.OnImagesRetrieved;
import com.ternaryop.phototumblrshare.parsers.TitleData;
import com.ternaryop.phototumblrshare.parsers.TitleParser;
import com.ternaryop.utils.URLUtils;

public class MainActivity extends PhotoTumblrActivity implements OnLongClickListener, OnImagesRetrieved {
	private WebView webView;
	private ImageUrlRetriever imageUrlRetriever;
	private ProgressBar progressBar;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_webview);
		imageUrlRetriever = new ImageUrlRetriever(this, this);
		webView = (WebView) findViewById(R.id.webview_view);
		progressBar = (ProgressBar) findViewById(R.id.webview_progressbar);
		webView.setWebViewClient(new ImagePickerWebViewClient(progressBar));
		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				progressBar.setProgress(newProgress);
			}
		});
		webView.setOnLongClickListener(this);

		// Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("text/plain".equals(type)) {
	            handleSendText(intent);
	        }
	    } else {
		    getActionBar().setDisplayHomeAsUpEnabled(false);
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
	        case R.id.action_draft_posts:
	        	startDraftActivity(this);
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

	@Override
	public void onImagesRetrieved(String title, List<String> imageUrls) {
		TitleData titleData = TitleParser.instance().parseTitle(title);
		TumblrPostDialog dialog = new TumblrPostDialog(this);
		dialog.setImageUrls(imageUrls);
		dialog.setPostTitle(titleData.toString());
		dialog.setPostTags(titleData.getTags());
		
		dialog.show();
	}

	public static void startDraftActivity(Activity activity) {
		Intent intent = new Intent(activity, DraftActivity.class);
		Bundle bundle = new Bundle();

		intent.putExtras(bundle);

		activity.startActivity(intent);
	}
}
