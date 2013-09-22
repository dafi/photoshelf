package com.ternaryop.phototumblrshare;

import java.util.List;

import android.app.Activity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class ImagePickerWebViewClient extends WebViewClient {
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		final Activity activity = (Activity) view.getContext();

		String domSelector = new ImageDOMSelectorFinder(activity).getSelectorFromUrl(url);
		if (domSelector != null) {

			ImageUrlRetriever imageUrlRetriever = new ImageUrlRetriever(activity, new ImageUrlRetriever.OnImagesRetrieved() {
				@Override
				public void onImagesRetrieved(String title, List<String> imageUrls) {
					ImageViewerActivity.startImageViewer(activity, imageUrls.get(0));
				}
			});
			imageUrlRetriever.setUseActionMode(false);
			imageUrlRetriever.addOrRemoveUrl(domSelector, url);
			imageUrlRetriever.retrieve();
			return true;
		}
		String message = activity.getResources().getString(R.string.unable_to_find_domain_mapper_for_url);
		Toast.makeText(activity.getApplicationContext(),
				String.format(message, url),
				Toast.LENGTH_LONG).show();
		return false;
	}
}