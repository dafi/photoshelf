package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
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

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.dialogs.TumblrPostDialog;
import com.ternaryop.phototumblrshare.list.PhotoAdapter;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.DialogUtils;

public class TagPhotoBrowserActivity extends PhotoTumblrActivity implements OnScrollListener, OnQueryTextListener {
 	private static final String LOADER_PREFIX_POSTS_THUMB = "postsThumb";
	private static final String POST_TAG = "postTag";
	private static final String BLOG_NAME = "blogName";
	private PhotoAdapter adapter;
	private String blogName;
	private String postTag;
	private int offset;
	private boolean hasMorePosts;
	private boolean isScrolling;
	private long totalPosts;
	private SearchView searchView;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);
        
        adapter = new PhotoAdapter(this, LOADER_PREFIX_POSTS_THUMB);

        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        		PhotoSharePost item = (PhotoSharePost) parent.getItemAtPosition(position);
        		ImageViewerActivity.startImageViewer(TagPhotoBrowserActivity.this, item.getFirstPhotoAltSize().get(0).getUrl());
        	}
		});
        list.setOnScrollListener(this);
        registerForContextMenu(list);
        
	    Bundle bundle = getIntent().getExtras();
		blogName = bundle.getString(BLOG_NAME);
		postTag = bundle.getString(POST_TAG);
		if (blogName != null && postTag != null && postTag.trim().length() > 0) {
			postTag = postTag.trim();
			offset = 0;
			hasMorePosts = true;
			readPhotoPosts();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.tag_browser, menu);
		
		searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		searchView.setOnQueryTextListener(this);
		return true;
	}
	
	private void refreshUI() {
		setTitle(getResources().getString(R.string.browser_image_title, postTag, adapter.getCount(), totalPosts));
		adapter.notifyDataSetChanged();
	}
	
	private void readPhotoPosts() {
		if (isScrolling) {
			return;
		}
		refreshUI();

		final Context activityContext = this;
		new AsyncTask<Void, String, Void>() {
			ProgressDialog progressDialog;
			Exception error;

			protected void onPreExecute() {
				progressDialog = new ProgressDialog(activityContext);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setMessage(getString(R.string.reading_scheduled_posts));
				progressDialog.show();
				isScrolling = true;
			}
			
			@Override
			protected void onProgressUpdate(String... values) {
				progressDialog.setMessage(values[0]);
			}
			
			@Override
			protected void onPostExecute(Void result) {
				progressDialog.dismiss();
				
				if (error == null) {
					refreshUI();
				} else {
					DialogUtils.showErrorDialog(activityContext, error);
				}
				isScrolling = false;
			}

			@Override
			protected Void doInBackground(Void... voidParams) {
				try {
					HashMap<String, String> params = new HashMap<String, String>();
					params.put("tag", postTag);
					params.put("offset", String.valueOf(offset));
					List<TumblrPhotoPost> photoPosts = Tumblr.getSharedTumblr(activityContext)
							.getPhotoPosts(blogName, params);

					List<PhotoSharePost> photoShareList = new ArrayList<PhotoSharePost>(); 
			    	for (TumblrPost post : photoPosts) {
			    		photoShareList.add(new PhotoSharePost((TumblrPhotoPost)post,
			    				post.getTimestamp() * 1000));
					}
			        // we must reset the flag every time before an add operation
			        adapter.setNotifyOnChange(false);
		    		adapter.addAll(photoShareList);
			    	if (photoPosts.size() > 0) {
			    		totalPosts = photoPosts.get(0).getTotalPosts();
			    		hasMorePosts = true;
			    	} else {
			    		totalPosts = adapter.getCount();
			    		hasMorePosts = false;
			    	}
				} catch (Exception e) {
					e.printStackTrace();
					error = e;
				}
				return null;
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
		final PhotoSharePost item = (PhotoSharePost)adapter.getItem(position);
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
		adapter.clear();
		adapter.notifyDataSetChanged();
		readPhotoPosts();
		return false;
	}
}
