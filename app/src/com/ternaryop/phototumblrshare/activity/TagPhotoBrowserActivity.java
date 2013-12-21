package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.TagCursorAdapter;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class TagPhotoBrowserActivity extends PostsListActivity implements OnQueryTextListener, OnSuggestionListener {
	private static final String POST_TAG = "postTag";
	private String postTag;
	private SearchView searchView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
	    Bundle bundle = getIntent().getExtras();
		postTag = bundle.getString(POST_TAG);
		if (getBlogName() != null && postTag != null && postTag.trim().length() > 0) {
			onQueryTextSubmit(postTag.trim());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.tag_browser, menu);
		
		searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		setupSearchView();		
		
		return true;
	}

	private void setupSearchView() {
		searchView.setQueryHint(getString(R.string.enter_tag_hint));
		searchView.setOnQueryTextListener(this);
		
	    searchView.setOnSuggestionListener(this);
		TagCursorAdapter adapter = new TagCursorAdapter(
				getActionBar().getThemedContext(),
				android.R.layout.simple_dropdown_item_1line,
				getBlogName());
		searchView.setSuggestionsAdapter(adapter);
	}
	
	protected void refreshUI() {
		setTitle(getString(R.string.browser_image_title, postTag, photoAdapter.getCount(), totalPosts));
		photoAdapter.notifyDataSetChanged();
	}
	
	protected void readPhotoPosts() {
		if (isScrolling) {
			return;
		}
		refreshUI();
		isScrolling = true;

		new AbsProgressBarAsyncTask<Void, String, List<PhotoSharePost> >(this, getString(R.string.reading_tags_title, postTag)) {
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
					params.put("tag", postTag);
					params.put("offset", String.valueOf(offset));
					List<TumblrPhotoPost> photoPosts = Tumblr.getSharedTumblr(getContext())
							.getPhotoPosts(getBlogName(), params);

					List<PhotoSharePost> photoShareList = new ArrayList<PhotoSharePost>(); 
			    	for (TumblrPost post : photoPosts) {
			    		photoShareList.add(new PhotoSharePost((TumblrPhotoPost)post,
			    				post.getTimestamp() * 1000));
					}
			    	if (photoPosts.size() > 0) {
			    		totalPosts = photoShareList.get(0).getTotalPosts();
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

	public static void startPhotoBrowserActivity(Context context, String blogName, String postTag) {
		Intent intent = new Intent(context, TagPhotoBrowserActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(BLOG_NAME, blogName);
		bundle.putString(POST_TAG, postTag);
		intent.putExtras(bundle);

		context.startActivity(intent);
	}

	public static void startPhotoBrowserActivityForResult(Activity activity, String blogName, String postTag, int requestCode) {
		Intent intent = new Intent(activity, TagPhotoBrowserActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(BLOG_NAME, blogName);
		bundle.putString(POST_TAG, postTag);
		intent.putExtras(bundle);

		activity.startActivityForResult(intent, requestCode);
	}

	@Override
	protected int getActionModeMenuId() {
		return R.menu.tag_browser_context;
	}
	
	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		postTag = query;
		offset = 0;
		hasMorePosts = true;
		photoAdapter.clear();
		photoAdapter.notifyDataSetChanged();
		readPhotoPosts();
		return false;
	}

	@Override
	public boolean onSuggestionClick(int position) {
		String query = ((Cursor)searchView.getSuggestionsAdapter().getItem(position)).getString(1);
		searchView.setQuery(query, true);
		return true;
	}

	@Override
	public boolean onSuggestionSelect(int position) {
		return true;
	}
}
