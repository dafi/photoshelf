package com.ternaryop.phototumblrshare.activity;

import java.net.URI;
import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.ternaryop.phototumblrshare.R;

@SuppressLint("SetJavaScriptEnabled")
public class ImageViewerActivity extends PhotoTumblrActivity {
	private static final String IMAGE_URL = "imageUrl";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webview);
	    setActionBarIcon();

	    ProgressBar progressBar = (ProgressBar) findViewById(R.id.webview_progressbar);
	    
	    Bundle bundle = getIntent().getExtras();
		String imageUrl = bundle.getString(IMAGE_URL);
    	String data = "<body><img src=\"" + imageUrl +"\"/></body>";
    	prepareWebView(progressBar).loadData(data, "text/html", "UTF-8");
	    try {
			getActionBar().setSubtitle(new URI(imageUrl).getHost());
		} catch (URISyntaxException e) {
		}
	}

	private WebView prepareWebView(final ProgressBar progressBar) {
		WebView webView = (WebView) findViewById(R.id.webview_view);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(this, "dimRetriever");

//		webView.setInitialScale(1);
//		webView.getSettings().setLoadWithOverviewMode(true);
		webView.getSettings().setUseWideViewPort(true);		
		
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
				view.loadUrl("javascript:var img = document.querySelector('img');dimRetriever.setDimensions(img.width, img.height)");
			}
		});
		return webView;
	}

	public static void startImageViewer(Context context, String url) {
		Intent intent = new Intent(context, ImageViewerActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(IMAGE_URL, url);
		intent.putExtras(bundle);

		context.startActivity(intent);
	}

	@JavascriptInterface
	public void setDimensions(final int w, final int h) {
		runOnUiThread(new Runnable() {
		    public void run() {
				setTitle(getString(R.string.image_size, w, h));
				}
		  });
	}
}
