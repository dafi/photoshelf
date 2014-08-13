package com.ternaryop.photoshelf;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.ternaryop.utils.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ImageUrlRetriever {
	private final Context context;
	private String title;
	private Exception error = null;
	private Map<String, String> urlSelectorMap = new HashMap<String, String>();
	private ActionMode actionMode;
	private final OnImagesRetrieved callback;
	private boolean useActionMode;
	private boolean useFile;
	private ArrayList<String> imageUrls;
	private ArrayList<File> imageFiles;

	public ImageUrlRetriever(Context context, OnImagesRetrieved callback) {
		this.context = context;
		this.callback = callback;
		useActionMode = true;
	}

	public void addOrRemoveUrl(String domSelector, String url) {
		if (urlSelectorMap.get(url) == null) {
			urlSelectorMap.put(url, domSelector);
		} else {
			urlSelectorMap.remove(url);
		}
		if (useActionMode) {
			if (urlSelectorMap.size() == 0) {
				getActionMode((Activity) context).finish();
			} else {
				getActionMode((Activity) context).invalidate();
			}
		}
	}

	protected ActionMode getActionMode(Activity activity) {
		if (actionMode == null) {
			actionMode = activity.startActionMode(mActionModeCallback);
		}
		return actionMode;
	}

	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.action_context, menu);
			return true;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			mode.setTitle(context.getString(R.string.select_images));
	        mode.setSubtitle(context.getResources().getQuantityString(
	                R.plurals.selected_items,
	                urlSelectorMap.size(),
	                urlSelectorMap.size()));
			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		    useFile = item.getItemId() == R.id.create_from_file;

		    switch (item.getItemId()) {
			case R.id.showDialog:
				retrieve();
				mode.finish();
				return true;
            case R.id.create_from_file:
                retrieve();
                mode.finish();
                return true;
			default:
				return false;
			}
		}

		// Called when the user exits the action mode
		public void onDestroyActionMode(ActionMode mode) {
			actionMode = null;
            // remove selection to ensures it's empty if the user clicks on 'done' actionbar's button
            urlSelectorMap.clear();
		}
	};

	public void retrieve() {
		new UrlRetrieverAsyncTask().execute(urlSelectorMap);
	}

	public boolean isUseActionMode() {
		return useActionMode;
	}

	public void setUseActionMode(boolean useActionMode) {
		this.useActionMode = useActionMode;
	}

	public interface OnImagesRetrieved {
		public void onImagesRetrieved(ImageUrlRetriever imageUrlRetriever);
	}

	class UrlRetrieverAsyncTask extends AsyncTask<Object, Integer, Void> {
		private ProgressDialog progressDialog;

		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage(context.getString(R.string.image_retriever_title));
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMax(urlSelectorMap.size());
			progressDialog.show();
		}

		@Override
		protected Void doInBackground(Object... params) {
		    if (useFile) {
		        imageFiles = new ArrayList<File>();
		        imageUrls = null;
		    } else {
	            imageUrls = new ArrayList<String>();
	            imageFiles = null;
		    }
			try {
				@SuppressWarnings("unchecked")
				Map<String, String> urls = (Map<String, String>) params[0];
				int i = 1;
				for (String url : urls.keySet()) {
					String selector = urls.get(url);
					String link;
					// if the selector is empty then 'url' is an image
					// and doesn't need to be parsed
					if (selector.trim().length() == 0) {
					    link = url;
					} else {
	                    Document htmlDocument = Jsoup.connect(url).get();
	                    if (title == null) {
	                        title = htmlDocument.title();
	                    }
	                    link = htmlDocument.select(selector).attr("src");
					}
					if (!link.isEmpty()) {
						// if necessary resolve relative urls
						try {
							URI uri = new URI(link);
							if (!uri.isAbsolute()) {
								uri = new URI(url).resolve(uri);
								link = uri.toString();
							}
							if (imageFiles != null) {
							    File file = new File(context.getCacheDir(), String.valueOf(link.hashCode()));
                                FileOutputStream fos = new FileOutputStream(file);
							    try {
							        IOUtils.saveURL(link, fos);
							        imageFiles.add(file);
							    } finally {
							        fos.close();
							    }
							} else {
							    imageUrls.add(link);
							}
						} catch (URISyntaxException e) {
						}
					}
					publishProgress(i++);
				}
			} catch (Exception e) {
				error = e;
				return null;
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Void voidParam) {
			try {
				progressDialog.dismiss();
				if (error == null) {
                    callback.onImagesRetrieved(ImageUrlRetriever.this);
					urlSelectorMap.clear();
				} else {
					new AlertDialog.Builder(context)
					.setTitle(R.string.url_not_found)
					.setMessage(error.getLocalizedMessage())
					.show();
				}
			} catch (Exception e) {
				new AlertDialog.Builder(context)
				.setTitle(R.string.parsing_error)
				.setMessage(title + "\n" + e.getLocalizedMessage())
				.show();
			}
		}
	}
	
	public List<File> getImageFiles() {
	    return imageFiles;
	}
	
	public List<String> getImageUrls() {
	    return imageUrls;
	}
}
