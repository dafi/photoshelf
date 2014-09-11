package com.ternaryop.photoshelf.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;

import com.ternaryop.photoshelf.R;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class PostRetriever extends AbsProgressBarAsyncTask<Void, Integer, List<TumblrPost> > {
    private final Callback<List<TumblrPost>> callback;
    private final long lastPublishTimestamp;
    private HashMap<String, String> params;
    private String tumblrName;

    public PostRetriever(Context context, long publishTimestamp, Callback<List<TumblrPost>> callback) {
        super(context, context.getString(R.string.start_import_title));
        this.callback = callback;
        this.lastPublishTimestamp = publishTimestamp;
    }

    public void readPhotoPosts(String tumblrName, String tag) {
        this.tumblrName = tumblrName;

        params = new HashMap<String, String>();
        params.put("type", "photo");
        if (tag != null && tag.trim().length() > 0) {
            params.put("tag", tag);
        }
        execute();
    }

    @Override
    protected List<TumblrPost> doInBackground(Void... voidParams) {
        ArrayList<TumblrPost> allPostList = new ArrayList<TumblrPost>();
        try {
            int offset = 0;
            boolean loadNext;
            
            do {
                params.put("offset", String.valueOf(offset));
                List<TumblrPost> postsList = Tumblr
                        .getSharedTumblr(getContext())
                        .getPublicPosts(tumblrName, params);
                loadNext = postsList.size() > 0;
                offset += postsList.size();
                
                for (TumblrPost tumblrPost : postsList) {
                    if (lastPublishTimestamp < tumblrPost.getTimestamp()) {
                        allPostList.add(tumblrPost);
                    } else {
                        loadNext = false;
                        break;
                    }
                }
                // refresh UI
                publishProgress(allPostList.size());
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
        if (!hasError()) {
            callback.complete(allPosts);
        } else {
            callback.failure(getError());
        }
    }

    protected void onProgressUpdate(Integer... values) {
        getProgressDialog().setMessage(getContext().getString(R.string.posts_read_count_title, values[0]));
    }
}