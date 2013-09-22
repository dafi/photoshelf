package com.ternaryop.phototumblrshare;

import android.app.Activity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class ImagePickerWebViewClient extends WebViewClient {
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		Activity activity = (Activity) view.getContext();

		String domSelector = new ImageDOMSelectorFinder(activity).getSelectorFromUrl(url);
		if (domSelector != null) {
			ImageViewerActivity.startImageViewer(activity, url);
			return true;
		}
		String message = activity.getResources().getString(R.string.unable_to_find_domain_mapper_for_url);
		Toast.makeText(activity.getApplicationContext(),
				String.format(message, url),
				Toast.LENGTH_LONG).show();
		return false;
	}
}