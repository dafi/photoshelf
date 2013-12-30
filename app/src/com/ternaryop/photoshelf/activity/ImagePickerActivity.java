package com.ternaryop.photoshelf.activity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.ternaryop.photoshelf.ImageDOMSelectorFinder;
import com.ternaryop.photoshelf.ImagePickerWebViewClient;
import com.ternaryop.photoshelf.ImageUrlRetriever;
import com.ternaryop.photoshelf.ImageUrlRetriever.OnImagesRetrieved;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import com.ternaryop.utils.URLUtils;

@SuppressLint("SetJavaScriptEnabled")
public class ImagePickerActivity extends PhotoTumblrActivity implements OnLongClickListener, OnImagesRetrieved {
	private static final String TEXT_WITH_URL = "textWithUrl";

	private WebView webView;
	private ImageUrlRetriever imageUrlRetriever;
	private ProgressBar progressBar;

    private ImageDOMSelectorFinder domSelectorFinder;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);
		setTitle(R.string.image_picker_activity_title);
	    setActionBarIcon();

		imageUrlRetriever = new ImageUrlRetriever(this, this);
		progressBar = (ProgressBar) findViewById(R.id.webview_progressbar);
		prepareWebView();
		domSelectorFinder = new ImageDOMSelectorFinder(this);

	    String textWithUrl = getTextWithUrl();

		if (textWithUrl != null) {
			beginPostToTumblr(textWithUrl);				
		}
	}
    
    /**
     * Get url from
     * - action send (ie. share feature)
     * - action view (eg. links inside emails) 
     * - internal activities (using bundle TEXT_WITH_URL)
     * Url may contain extra text if it comes from share, url will be extracted later 
     * @return
     */
    private String getTextWithUrl() {
		// Get intent, action and MIME type
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();
	    Uri uri = intent.getData();
	    
	    String textWithUrl = null;

	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("text/plain".equals(type)) {
	    		textWithUrl = intent.getStringExtra(Intent.EXTRA_TEXT);
	        }
	    } else if (Intent.ACTION_VIEW.equals(action) && uri != null) {
	        textWithUrl = uri.toString();
	    } else {
			textWithUrl = intent.getStringExtra(TEXT_WITH_URL);
	    }
	    return textWithUrl;
    }

	private void prepareWebView() {
		webView = (WebView) findViewById(R.id.webview_view);
		webView.setWebViewClient(new ImagePickerWebViewClient(progressBar));
		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				progressBar.setProgress(newProgress);
			}
		});
		webView.setOnLongClickListener(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "targetUrlRetriever");
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
				.setMessage(getString(R.string.url_not_found_description, textWithUrl))
				.show();
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
	
	// This class is static to be sure the handler doesn't leak
	private final static class LongClickHandler extends Handler {
		private final ImagePickerActivity activity;

		public LongClickHandler(ImagePickerActivity activity) {
			this.activity = activity;
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String url = msg.getData().getString("src");
			String js = String.format("document.querySelector(\"img[src='%1$s']\").parentNode.href", url);
            activity.webView.loadUrl("javascript:var url = " + js + "; targetUrlRetriever.setTargetHrefURL(url);");
		}
	}

	@Override
    public void onImagesRetrieved(ImageUrlRetriever imageUrlRetriever) {
        TitleData titleData = TitleParser.instance().parseTitle(imageUrlRetriever.getTitle());
        TumblrPostDialog dialog = new TumblrPostDialog(this);
        if (imageUrlRetriever.getImageUrls() != null) {
            dialog.setImageUrls(imageUrlRetriever.getImageUrls());
        } else {
            dialog.setImageFiles(imageUrlRetriever.getImageFiles());
        }
		dialog.setPostTitle(titleData.toHtml());
		dialog.setPostTags(titleData.getTags());
		
		dialog.show();
	}

	public static void startImagePicker(Context context, String textWithUrl) {
		Intent intent = new Intent(context, ImagePickerActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(TEXT_WITH_URL, textWithUrl);
		intent.putExtras(bundle);

		context.startActivity(intent);
	}

    /**
     * Workaround for 4.4 bug 
     * http://code.google.com/p/android/issues/detail?id=62928
     * @param url
     */
    @JavascriptInterface
    public void setTargetHrefURL(final String url) {
        runOnUiThread(new Runnable() {
            public void run() {
                String domSelector = domSelectorFinder.getSelectorFromUrl(url);
                if (domSelector != null) {
                    imageUrlRetriever.addOrRemoveUrl(domSelector, url);
                }
            }
        });
    }
}
