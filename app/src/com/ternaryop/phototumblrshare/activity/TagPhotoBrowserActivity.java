package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.TagCursorAdapter;
import com.ternaryop.phototumblrshare.dialogs.TumblrPostDialog;
import com.ternaryop.phototumblrshare.list.PhotoAdapter;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class TagPhotoBrowserActivity extends PhotoTumblrActivity implements OnScrollListener, OnQueryTextListener, OnSuggestionListener {
 	private static final String LOADER_PREFIX_POSTS_THUMB = "postsThumb";
	private static final String POST_TAG = "postTag";
	private PhotoAdapter photoAdapter;
	private String postTag;
	private int offset;
	private boolean hasMorePosts;
	private boolean isScrolling;
	private long totalPosts;
	private SearchView searchView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);
	    setActionBarIcon();
        
        photoAdapter = new PhotoAdapter(this, LOADER_PREFIX_POSTS_THUMB);

        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(photoAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        		PhotoSharePost item = (PhotoSharePost) parent.getItemAtPosition(position);
        		ImageViewerActivity.startImageViewer(TagPhotoBrowserActivity.this, item.getFirstPhotoAltSize().get(0).getUrl());
        	}
		});
        list.setOnScrollListener(this);
        registerForContextMenu(list);
        
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
	
	private void refreshUI() {
		setTitle(getString(R.string.browser_image_title, postTag, photoAdapter.getCount(), totalPosts));
		photoAdapter.notifyDataSetChanged();
	}
	
	private void readPhotoPosts() {
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

	public static void startPhotoBrowserActivity(Context context, String blogName, String postTag) {
		Intent intent = new Intent(context, TagPhotoBrowserActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(BLOG_NAME, blogName);
		bundle.putString(POST_TAG, postTag);
		intent.putExtras(bundle);

		context.startActivity(intent);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		boolean loadMore = totalItemCount > 0 &&
	            (firstVisibleItem + visibleItemCount >= totalItemCount);

		if (loadMore && hasMorePosts && !isScrolling) {
			offset += Tumblr.MAX_POST_PER_REQUEST;
			readPhotoPosts();
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.list) {
			getMenuInflater().inflate(R.menu.tag_browser_context, menu);
			menu.setHeaderTitle(R.string.post_actions_menu_header);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.post_edit:
			showEditDialog(info.position);
			break;
		default:
			return false;
		}
		return true;
	}

	private void showEditDialog(final int position) {
		final PhotoSharePost item = (PhotoSharePost)photoAdapter.getItem(position);
		TumblrPostDialog editDialog = new TumblrPostDialog(this, item.getPostId());

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which) {
		        case DialogInterface.BUTTON_POSITIVE:
		        	item.setTags(((TumblrPostDialog)dialog).getPostTags());
		        	item.setCaption(((TumblrPostDialog)dialog).getPostTitle());
		        	refreshUI();
		            break;
		        }
		    }
		};
		editDialog.setPostTitle(item.getCaption());
		editDialog.setPostTags(item.getTags());
		editDialog.setEditButton(dialogClickListener);
		
		editDialog.show();
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
