package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class ScheduledListActivity extends PostsListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        photoAdapter.setOnPhotoBrowseClick(this);
		if (getBlogName() != null) {
			offset = 0;
			hasMorePosts = true;
			readPhotoPosts();
		}
	}

	@Override
    protected int getActionModeMenuId() {
    	return R.menu.scheduled_context;
    }
	
	protected void refreshUI() {
		setTitle(getString(R.string.browser_sheduled_images_title, photoAdapter.getCount(), totalPosts));
		photoAdapter.notifyDataSetChanged();
	}
	
	@Override
	protected void readPhotoPosts() {
		if (isScrolling) {
			return;
		}
		refreshUI();
		isScrolling = true;

		new AbsProgressBarAsyncTask<Void, String, List<PhotoSharePost> >(this, getString(R.string.reading_scheduled_posts)) {
			@Override
			protected void onProgressUpdate(String... values) {
				getProgressDialog().setMessage(values[0]);
			}
			
			@Override
			protected void onPostExecute(List<PhotoSharePost> posts) {
				super.onPostExecute(posts);
				
				if (getError() == null) {
			    	photoAdapter.addAll(posts);
					refreshUI();
				}
				isScrolling = false;
			}

			@Override
			protected List<PhotoSharePost> doInBackground(Void... voidParams) {
				try {
					HashMap<String, String> params = new HashMap<String, String>();
					params.put("offset", String.valueOf(offset));
					List<TumblrPost> photoPosts = Tumblr.getSharedTumblr(getContext())
							.getQueue(getBlogName(), params);

					List<PhotoSharePost> photoShareList = new ArrayList<PhotoSharePost>(); 
			    	for (TumblrPost post : photoPosts) {
				    	if (post.getType().equals("photo")) {
				    		photoShareList.add(new PhotoSharePost((TumblrPhotoPost)post,
				    				post.getScheduledPublishTime() * 1000));
				    	}
					}
			    	if (photoPosts.size() > 0) {
			    		totalPosts =  photoShareList.size();
			    		hasMorePosts = true;
			    	} else {
			    		totalPosts = photoAdapter.getCount() + photoShareList.size();
			    		hasMorePosts = false;
			    	}
			    	return photoShareList;
				} catch (Exception e) {
					e.printStackTrace();
					setError(e);
				}
				return Collections.emptyList();
			}
		}.execute();
	}

	public static void startScheduledListActivity(Context context, String blogName) {
		Intent intent = new Intent(context, ScheduledListActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(BLOG_NAME, blogName);
		intent.putExtras(bundle);

		context.startActivity(intent);
	}
}
