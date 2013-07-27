package com.ternaryop.phototumblrshare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.ternaryop.phototumblrshare.parsers.TitleData;
import com.ternaryop.phototumblrshare.parsers.TitleParser;

public class ImagePickerWebViewClient extends WebViewClient {
	static HashMap<String, String> domainMap = new HashMap<String, String>();
	
	private Map<String, String> urlSelectorMap = new HashMap<String, String>();
	private ActionMode actionMode;
	private String title;
	private Context context;
	
	static {
		domainMap.put("http://images.bangtidy.net", "#theimage");
		domainMap.put("http://x05.org", "#img_obj");
		domainMap.put("http://www.imagebam.com", "[onclick*=scale]");
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		context = view.getContext();
		
		for (String domain : domainMap.keySet()) {
			if (url.startsWith(domain)) {
				if (urlSelectorMap.get(url) == null) {
					urlSelectorMap.put(url, domainMap.get(domain));
				} else {
					urlSelectorMap.remove(url);
				}
				title = view.getTitle();
				if (urlSelectorMap.size() == 0) {
					getActionMode((Activity) context).finish();
				} else {
					getActionMode((Activity) context).invalidate();
				}
				return true;
			}
		}
		String message = context.getResources().getString(R.string.unable_to_find_domain_mapper_for_url);
		Toast.makeText(context.getApplicationContext(),
				String.format(message, url),
				Toast.LENGTH_LONG).show();
		return false;
	}

	protected ActionMode getActionMode(Activity activity) {
		if (actionMode == null) {
			actionMode = activity.startActionMode(mActionModeCallback);
		}
		return actionMode;
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.action_context, menu);
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			menu.findItem(R.id.counter).setTitle(urlSelectorMap.size() + " urls");
			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.showDialog:
				new ImageLinkRetrieverAsyncTask(context, title).execute(urlSelectorMap);
				getActionMode((Activity) context).finish();
				return true;
			default:
				return false;
			}
		}

		// Called when the user exits the action mode
		public void onDestroyActionMode(ActionMode mode) {
			actionMode = null;
		}
	};

	class ImageLinkRetrieverAsyncTask extends AsyncTask<Object, Integer, List<String>> {
		private final Context context;
		private String title;
		ProgressDialog progressDialog;

		public ImageLinkRetrieverAsyncTask(Context context, String title) {
			this.context = context;
			this.title = title;
			progressDialog = new ProgressDialog(context);
		}
		
		@Override
		protected void onPreExecute() {
			progressDialog.setMessage("Getting image urls");//context.getResources().getString(R.string.preparing));
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMax(urlSelectorMap.size());
			progressDialog.show();
		}

		@Override
		protected List<String> doInBackground(Object... params) {
			ArrayList<String> imageUrls = new ArrayList<String>();
			try {
				@SuppressWarnings("unchecked")
				Map<String, String> urls = (Map<String, String>) params[0];
				int i = 1;
				for (String url : urls.keySet()) {
					String selector = urls.get(url);
					Document htmlDocument = Jsoup.connect(url).get();
					if (title == null) {
						title = htmlDocument.title();
					}
					String link = htmlDocument.select(selector).attr("src");
					if (!link.isEmpty()) {
						imageUrls.add(link);
					}
					publishProgress(i++);
				}
			} catch (Exception e) {
				new AlertDialog.Builder(context)
				.setTitle(R.string.url_not_found)
				.setMessage(e.getLocalizedMessage())
				.show();
			}
			return imageUrls;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(List<String> imageUrls) {
			try {
				progressDialog.dismiss();
				TitleData titleData = TitleParser.instance().parseTitle(title);
				TumblrPostDialog dialog = new TumblrPostDialog(context);
				dialog.setImageUrls(imageUrls);
				dialog.setTitle(titleData.toString());
				dialog.setTags(titleData.tags);
				
				dialog.show();
				urlSelectorMap.clear();
			} catch (Exception e) {
				new AlertDialog.Builder(context)
				.setTitle(R.string.parsing_error)
				.setMessage(e.getLocalizedMessage())
				.show();
			}
		}
	}
}