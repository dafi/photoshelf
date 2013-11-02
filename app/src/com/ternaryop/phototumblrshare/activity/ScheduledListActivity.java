package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.list.PhotoAdapter;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.AbsCallback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class ScheduledListActivity extends PhotoTumblrActivity implements OnScrollListener, OnItemClickListener {
 	private static final String LOADER_PREFIX_POSTS_THUMB = "postsThumb";
	private static final String BLOG_NAME = "blogName";
	private PhotoAdapter photoAdapter;
	private int offset;
	private boolean hasMorePosts;
	private boolean isScrolling;
	private long totalPosts;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);
	    setActionBarIcon();
        
        photoAdapter = new PhotoAdapter(this, LOADER_PREFIX_POSTS_THUMB);

        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(photoAdapter);
        list.setOnItemClickListener(this);
        list.setOnScrollListener(this);
        registerForContextMenu(list);

		if (getBlogName() != null) {
			offset = 0;
			hasMorePosts = true;
			readPhotoPosts();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.list) {
			getMenuInflater().inflate(R.menu.scheduled_context, menu);
			menu.setHeaderTitle(R.string.post_actions_menu_header);
		}
	}
	
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

		switch (item.getItemId()) {
		case R.id.post_publish:
			//showPublishDialog(info.position);
			return true;
		case R.id.post_save_draft:
			saveDraft(info.position);
			return true;
		default:
			return false;
		}
	}
	
	private void saveDraft(int position) {
		final PhotoSharePost item = (PhotoSharePost)photoAdapter.getItem(position);
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which) {
		        case DialogInterface.BUTTON_POSITIVE:
		    		Tumblr.getSharedTumblr(ScheduledListActivity.this).saveDraft(
		    				getBlogName(),
		    				item.getPostId(),
		    				new AbsCallback(ScheduledListActivity.this, R.string.parsing_error) {
		
				    			@Override
				    			public void complete(JSONObject result) {
				    				photoAdapter.remove(item);
				    				refreshUI();
				    			}
		    				});
		            break;
		        }
		    }
		};
		
		new AlertDialog.Builder(this)
		.setMessage(getString(R.string.save_to_draft_confirm, item.getFirstTag()))
		.setPositiveButton(android.R.string.yes, dialogClickListener)
	    .setNegativeButton(android.R.string.no, dialogClickListener)
	    .show();		
	}

	private void refreshUI() {
		setTitle(getString(R.string.browser_sheduled_images_title, photoAdapter.getCount(), totalPosts));
		photoAdapter.notifyDataSetChanged();
	}
	
	private void readPhotoPosts() {
		if (isScrolling) {
			return;
		}
		refreshUI();
		isScrolling = true;

		new AbsProgressBarAsyncTask<Void, String, List<PhotoSharePost> >(this, getString(R.string.reading_draft_posts)) {
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

	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		PhotoSharePost item = (PhotoSharePost) parent.getItemAtPosition(position);
		ImageViewerActivity.startImageViewer(this, item.getFirstPhotoAltSize().get(0).getUrl());
	}

}
