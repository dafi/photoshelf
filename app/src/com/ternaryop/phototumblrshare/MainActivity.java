package com.ternaryop.phototumblrshare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class MainActivity extends Activity {
	private WebView webView;
	private GridView gridView;
	private ImageAdapter imageAdapter;
	boolean useWebView = true;
	String title;
	private TumblrActionModeCallback actionModeCallback;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (useWebView) {
			webView = new WebView(this);
			setContentView(webView);
			webView.setWebViewClient(new ImagePickerWebViewClient());
		} else {
			setContentView(R.layout.activity_main);
			gridView = (GridView) findViewById(R.id.gridview);
			imageAdapter = new ImageAdapter(this);
			gridView.setAdapter(imageAdapter);
			gridView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					// http://stackoverflow.com/questions/11326089/android-gridview-keep-item-selected
					imageAdapter.toogleItem(position);
					imageAdapter.notifyDataSetChanged();
					getActionModeCallback().invalidate();
				}
			});
		}

		
		
		
		
		
		

		// Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("text/plain".equals(type)) {
	            handleSendText(intent);
	        }
	    } else {
			if (useWebView) {
				webView.loadUrl("file:///android_asset/index.html");
			} else {
				beginPostToTumblr("http://www.celebfanforum.com/showthread.php?133173-Rachel-Bilson-quot-The-To-Do-List-quot-Hollywood-Premiere-in-Los-Angeles-July-23-2013&p=231095#post231095");
			}
	    }
	}

	public TumblrActionModeCallback getActionModeCallback() {
		if (actionModeCallback == null) {
			actionModeCallback = new TumblrActionModeCallback(MainActivity.this, imageAdapter);
		}
		actionModeCallback.startActionMode();
		return actionModeCallback;
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
			if (useWebView) {
				webView.loadUrl(url);
			} else {
				new ImageUrlExtractor().execute(new String[]{url});
			}
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

	class ImageUrlExtractor extends AsyncTask<String, Void, List<ImageInfo>> {

		@Override
		protected List<ImageInfo> doInBackground(String... urls) {
			List<ImageInfo> imageInfoList = new ArrayList<ImageInfo>();
			try {
				String url = urls[0];
				Document htmlDocument = Jsoup.connect(url).get();
				title = htmlDocument.title();
				Elements thumbnailImages = htmlDocument.select("a img[src*=jpg]");
				for (int i = 0; i < thumbnailImages.size(); i++) {
					Element thumbnailImage = thumbnailImages.get(i);
					String thumbnailURL = thumbnailImage.attr("src");
					String imageURL = thumbnailImage.parent().attr("href");
					imageInfoList.add(new ImageInfo(thumbnailURL, imageURL));
				}
			} catch (IOException e) {
				new AlertDialog.Builder(MainActivity.this)
				.setTitle(R.string.url_not_found)
				.setMessage(e.getLocalizedMessage())
				.show();
			}
			return imageInfoList;
		}

		@Override
		protected void onPostExecute(List<ImageInfo> result) {
			imageAdapter.addAll(result);
			imageAdapter.notifyDataSetChanged();
			gridView.invalidateViews();
		}
	}
}
