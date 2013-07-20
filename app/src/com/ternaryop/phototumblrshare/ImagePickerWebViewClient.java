package com.ternaryop.phototumblrshare;

import java.util.HashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.ternaryop.phototumblrshare.parsers.TitleData;
import com.ternaryop.phototumblrshare.parsers.TitleParser;

public class ImagePickerWebViewClient extends WebViewClient {
	/**
	 * 
	 */
	private final Context context;

	static HashMap<String, String> domainMap = new HashMap<String, String>();

	static {
		domainMap.put("http://images.bangtidy.net", "#theimage");
		domainMap.put("http://x05.org", "#img_obj");
		domainMap.put("http://www.imagebam.com", "[onclick*=scale]");
	}

	public ImagePickerWebViewClient(Context context) {
		this.context = context;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		for (String domain : domainMap.keySet()) {
			if (url.startsWith(domain)) {
				new ImageLinkRetrieverAsyncTask(context, view.getTitle()).execute(new String[]{url, domainMap.get(domain)});
				return true;
			}
		}
		String message = context.getResources().getString(R.string.unable_to_find_domain_mapper_for_url);
		Toast.makeText(context.getApplicationContext(),
				String.format(message, url),
				Toast.LENGTH_LONG).show();
		return false;
	}

	class ImageLinkRetrieverAsyncTask extends AsyncTask<String, Object, String> {
		private final Context context;
		private String title;


		public ImageLinkRetrieverAsyncTask(Context context, String title) {
			this.context = context;
			this.title = title;
		}
		
		@Override
		protected String doInBackground(String... params) {
			String link = null;
			try {
				String url = params[0];
				String selector = params[1];
				Document htmlDocument = Jsoup.connect(url).get();
				if (title == null) {
					title = htmlDocument.title();
				}
				link = htmlDocument.select(selector).attr("src");
			} catch (Exception e) {
				new AlertDialog.Builder(context)
				.setTitle(R.string.url_not_found)
				.setMessage(e.getLocalizedMessage())
				.show();
			}
			return link;
		}

		@Override
		protected void onPostExecute(String url) {
			try {
				TitleData titleData = TitleParser.instance().parseTitle(title);
				TumblrPostDialog dialog = new TumblrPostDialog(context);
				dialog.setImageUrl(url);
				dialog.setTitle(titleData.toString());
				dialog.setTags(titleData.tags);
				
				dialog.show();		
			} catch (Exception e) {
				new AlertDialog.Builder(context)
				.setTitle(R.string.parsing_error)
				.setMessage(e.getLocalizedMessage())
				.show();
			}
		}
	}
}