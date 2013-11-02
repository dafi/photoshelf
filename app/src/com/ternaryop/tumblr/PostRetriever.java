package com.ternaryop.tumblr;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.utils.AbsProgressBarAsyncTask;
import com.ternaryop.utils.JSONUtils;

public class PostRetriever extends AbsProgressBarAsyncTask<Void, List<TumblrPost>, List<TumblrPost> > {
	private String apiUrl;
	private final Callback<List<TumblrPost>> callback;
	private long lastPublishTimestamp;

	public PostRetriever(Context context, long publishTimestamp, Callback<List<TumblrPost>> callback) {
		super(context, context.getString(R.string.start_import_title));
		this.callback = callback;
		this.lastPublishTimestamp = publishTimestamp;
	}

	protected JSONArray readPostsFromOffset(int offset) throws Exception {
    	HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(apiUrl + "&offset=" + offset);
    	JSONObject json = JSONUtils.jsonFromInputStream(client.execute(request).getEntity().getContent());
		JSONObject response = json.getJSONObject("response");

		return response.getJSONArray("posts");
    }
	
	@SuppressWarnings("unchecked")
	@Override
	protected List<TumblrPost> doInBackground(Void... params) {
        ArrayList<TumblrPost> allPostList = new ArrayList<TumblrPost>();
		try {
	    	int offset = 0;
			boolean loadNext;
			
			do {
				JSONArray jsonArray = readPostsFromOffset(offset);
				loadNext = jsonArray.length() > 0;
                offset += jsonArray.length();
                ArrayList<TumblrPost> list = new ArrayList<TumblrPost>();
                Tumblr.addPostsToList(list, jsonArray);
				
				for (TumblrPost tumblrPost : list) {
					if (lastPublishTimestamp < tumblrPost.getTimestamp()) {
						allPostList.add(tumblrPost);
					} else {
						loadNext = false;
						break;
					}
				}
				// refresh UI
				publishProgress(allPostList);
			} while (loadNext);
		} catch (Exception e) {
			e.printStackTrace();
			setError(e);
		}
		return allPostList;
	}

	@Override
	protected void onPostExecute(List<TumblrPost> allPosts) {
		// do not call super.onPostExecute() because it shows the alert message
		getProgressDialog().dismiss();
		if (getError() == null) {
			callback.complete(allPosts);
		} else {
			callback.failure(getError());
		}
	}

	protected void onProgressUpdate(List<TumblrPost>... values) {
		getProgressDialog().setMessage(getContext().getString(R.string.posts_read_count_title, values[0].size()));
	}

	public String getApiUrl() {
		return apiUrl;
	}

	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}
}