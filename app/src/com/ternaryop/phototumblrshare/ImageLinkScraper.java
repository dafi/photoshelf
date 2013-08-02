package com.ternaryop.phototumblrshare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.ternaryop.phototumblrshare.parsers.TitleData;
import com.ternaryop.phototumblrshare.parsers.TitleParser;

public class ImageLinkScraper extends AsyncTask<Object, Integer, List<String>> {
	private final Context context;
	private String title;
	private ProgressDialog progressDialog;
	private ImageAdapter imageAdapter;

	public ImageLinkScraper(Context context, String title, ImageAdapter imageAdapter) {
		this.context = context;
		this.title = title;
		this.imageAdapter = imageAdapter;
		this.progressDialog = new ProgressDialog(context);
	}
	
	@Override
	protected void onPreExecute() {
		progressDialog.setMessage(context.getString(R.string.getting_image_urls));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setMax(imageAdapter.getSelectedItems().size());
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
			// TODO unselect also if cancel is pressed on dialog, it's wrong
			imageAdapter.unselectAll();
		} catch (Exception e) {
			new AlertDialog.Builder(context)
			.setTitle(R.string.parsing_error)
			.setMessage(e.getLocalizedMessage())
			.show();
		}
	}
}
