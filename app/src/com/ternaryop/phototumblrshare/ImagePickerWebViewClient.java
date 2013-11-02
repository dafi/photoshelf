package com.ternaryop.phototumblrshare;

import java.util.List;

import com.ternaryop.phototumblrshare.activity.ImageViewerActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ImagePickerWebViewClient extends WebViewClient {
	private final ProgressBar progressBar;

	public ImagePickerWebViewClient(ProgressBar progressBar) {
		this.progressBar = progressBar;
		progressBar.setVisibility(View.GONE);
	}
	
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		final Context context = view.getContext();

		String domSelector = new ImageDOMSelectorFinder(context).getSelectorFromUrl(url);
		if (domSelector != null) {

			ImageUrlRetriever imageUrlRetriever = new ImageUrlRetriever(context, new ImageUrlRetriever.OnImagesRetrieved() {
				@Override
				public void onImagesRetrieved(String title, List<String> imageUrls) {
					ImageViewerActivity.startImageViewer(context, imageUrls.get(0));
				}
			});
			imageUrlRetriever.setUseActionMode(false);
			imageUrlRetriever.addOrRemoveUrl(domSelector, url);
			imageUrlRetriever.retrieve();
			return true;
		}
		String message = context.getString(R.string.unable_to_find_domain_mapper_for_url);
		Toast.makeText(context.getApplicationContext(),
				String.format(message, url),
				Toast.LENGTH_LONG).show();
		return false;
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		progressBar.setProgress(0);
		progressBar.setVisibility(View.VISIBLE);
	}
	@Override
	public void onPageFinished(WebView view, String url) {
		progressBar.setVisibility(View.GONE);
	}
}