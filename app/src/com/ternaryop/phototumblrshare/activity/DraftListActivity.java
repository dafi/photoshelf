package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.phototumblrshare.DraftPostHelper;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.DBHelper;
import com.ternaryop.phototumblrshare.db.LastPublishedPostCache;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog.onPostScheduleListener;
import com.ternaryop.phototumblrshare.list.OnPhotoBrowseClick;
import com.ternaryop.phototumblrshare.list.PhotoAdapter;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.AbsCallback;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrAltSize;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class DraftListActivity extends PhotoTumblrActivity implements OnPhotoBrowseClick, OnItemClickListener {
	private static final String LOADER_PREFIX_AVATAR = "avatar";
	private static final String LOADER_PREFIX_POSTS_THUMB = "postsThumb";
	
	private enum POST_ACTION {
		PUBLISH,
		DELETE
	};

	private PhotoAdapter adapter;
	
	private HashMap<String, TumblrPost> queuedPosts;
	private Calendar lastScheduledDate;
	// The menuInfo is null for submenus so store parent one
	// http://code.google.com/p/android/issues/detail?id=7139
	private AdapterView.AdapterContextMenuInfo subMenuContextMenuInfo;
	
	private ImageLoader blogAvatarImageLoader;
	private MenuItem blogNameMenuItem;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photo_list);
	    setActionBarIcon();
        
		blogAvatarImageLoader = new ImageLoader(this, LOADER_PREFIX_AVATAR);
        adapter = new PhotoAdapter(this, LOADER_PREFIX_POSTS_THUMB);
        adapter.setOnPhotoBrowseClick(this);
        adapter.setRecomputeGroupIds(true);

        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        registerForContextMenu(list);
        list.setOnItemClickListener(this);
        readDraftPosts();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.draft, menu);
		// strangely isn't accessing from onOptionsItemSelected
		// so we store here
		blogNameMenuItem = menu.findItem(R.id.action_blogname);
		// set icon to currect avatar blog 
		blogAvatarImageLoader.displayIcon(Blog.getAvatarUrlBySize(getBlogName(), 32), blogNameMenuItem);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getGroupId()) {
		case R.id.group_menu_actionbar_blog:
			String blogName = item.getTitle().toString();
			appSupport.setSelectedBlogName(blogName);
			setActionBarIcon();
			blogAvatarImageLoader.displayIcon(Blog.getAvatarUrlBySize(blogName, 32), blogNameMenuItem);
			readDraftPosts();
			return true;
		}
		
		switch (item.getItemId()) {
		case R.id.action_draft_refresh:
			readDraftPosts();
			return true;
		case R.id.action_draft_clear_cache:
			clearCache();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void clearCache() {
		DBHelper.getInstance(this).getLastPublishedPostCacheDAO().removeAll();
		readDraftPosts();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.list) {
			getMenuInflater().inflate(R.menu.draft_context, menu);
			menu.setHeaderTitle(R.string.post_actions_menu_header);
			AdapterView.AdapterContextMenuInfo contextMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
			PhotoSharePost post = (PhotoSharePost)adapter.getItem(contextMenuInfo.position);

			// fill the image size submenu
			SubMenu subMenu = menu.findItem(R.id.group_menu_image_dimension).getSubMenu();
			subMenu.setHeaderTitle(getString(R.string.menu_header_show_image, post.getFirstTag()));
			int index = 0;
			for(TumblrAltSize altSize : post.getFirstPhotoAltSize()) {
				// the item id is set to the image index into array
				subMenu.add(R.id.group_menu_item_image_dimension, index++, Menu.NONE, 
						getString(R.string.menu_image_dimension, altSize.getWidth(), altSize.getHeight()));
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

		switch (item.getGroupId()) {
		case R.id.group_menu_item_image_dimension:
			info = subMenuContextMenuInfo;
			PhotoSharePost post = (PhotoSharePost)adapter.getItem(info.position);
			String url = post.getFirstPhotoAltSize().get(item.getItemId()).getUrl();
    		ImageViewerActivity.startImageViewer(DraftListActivity.this, url);

    		// no longer needed, free up
			subMenuContextMenuInfo = null;
			return true;
		}

		switch (item.getItemId()) {
		case R.id.post_publish:
			showConfirmDialog(info.position, POST_ACTION.PUBLISH);
			break;
		case R.id.post_schedule:
			showScheduleDialog(info.position);
			break;
		case R.id.group_menu_image_dimension:
			// allow submenus to receive the menuInfo
			subMenuContextMenuInfo = info;
			return true;
		case R.id.post_delete:
			showConfirmDialog(info.position, POST_ACTION.DELETE);
			return true;
		default:
			return false;
		}
		return true;
	}

	private void deletePost(int position) {
		final PhotoSharePost item = (PhotoSharePost)adapter.getItem(position);
		Tumblr.getSharedTumblr(this).deletePost(getBlogName(), item.getPostId(), new AbsCallback(this, R.string.parsing_error) {

			@Override
			public void complete(Tumblr tumblr, JSONObject result) {
				adapter.remove(item);
				refreshUI();
			}
		});
	}

	private void showScheduleDialog(final int position) {
		final PhotoSharePost item = (PhotoSharePost)adapter.getItem(position);
		SchedulePostDialog dialog = new SchedulePostDialog(this,
				getBlogName(),
				item,
				findScheduleTime(),
				new onPostScheduleListener() {
			@Override
			public void onPostScheduled(long id, Calendar scheduledDateTime) {
				lastScheduledDate = (Calendar) scheduledDateTime.clone();
				adapter.remove(item);
				refreshUI();
			}
		});
		dialog.show();
	}

	private Calendar findScheduleTime() {
		Calendar cal;
		
		if (lastScheduledDate == null) {
			cal = Calendar.getInstance();
			long maxScheduledTime = System.currentTimeMillis();

			try {
				for (TumblrPost post : queuedPosts.values()) {
					long scheduledTime = post.getScheduledPublishTime() * 1000;
					if (scheduledTime > maxScheduledTime) {
						maxScheduledTime = scheduledTime;
					}
				}
			} catch (Exception e) {
			}
	    	cal.setTime(new Date(maxScheduledTime));
			cal.set(Calendar.MINUTE, 0);
	    	cal.set(Calendar.SECOND, 0);
	    	cal.set(Calendar.MILLISECOND, 0);
		} else {
			cal = (Calendar) lastScheduledDate.clone();
		}
    	// set next queued post time
		cal.add(Calendar.HOUR, appSupport.getDefaultScheduleHoursSpan());
		
		return cal;
	}

	private void showConfirmDialog(final int position, final POST_ACTION postAction) {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which) {
		        case DialogInterface.BUTTON_POSITIVE:
		        	switch (postAction) {
					case PUBLISH:
			            publishPost(position);
						break;
					case DELETE:
						deletePost(position);
						break;
					}
		            break;
		        }
		    }
		};

		final PhotoSharePost item = (PhotoSharePost)adapter.getItem(position);
		String message = null;
    	switch (postAction) {
		case PUBLISH:
			message = getString(R.string.publish_post_confirm, item.getFirstTag());
			break;
		case DELETE:
			message = getString(R.string.delete_post_confirm, item.getFirstTag());
			break;
		}
		
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setPositiveButton(android.R.string.yes, dialogClickListener)
	    .setNegativeButton(android.R.string.no, dialogClickListener)
	    .show();		
	}
	
	private void publishPost(final int position) {
		final PhotoSharePost item = (PhotoSharePost)adapter.getItem(position);
		Tumblr.getSharedTumblr(this).publishPost(getBlogName(), item.getPostId(), new AbsCallback(this, R.string.parsing_error) {

			@Override
			public void complete(Tumblr tumblr, JSONObject result) {
				adapter.remove(item);
				refreshUI();
			}
		});
	}

	private void refreshUI() {
		int resId = adapter.getCount() == 1 ? R.string.posts_in_draft_singular : R.string.posts_in_draft_plural;
		setTitle(getString(resId, adapter.getCount()));
		adapter.notifyDataSetChanged();
	}

	private void readDraftPosts() {
		adapter.clear();
		refreshUI();
		
		new AbsProgressBarAsyncTask<Void, String, List<PhotoSharePost> >(this, getString(R.string.reading_draft_posts)) {
			@Override
			protected void onProgressUpdate(String... values) {
				getProgressDialog().setMessage(values[0]);
			}
			
			@Override
			protected void onPostExecute(List<PhotoSharePost> posts) {
				super.onPostExecute(posts);
				
				if (getError() == null) {
					adapter.addAll(posts);
					refreshUI();
				}
			}

			@Override
			protected List<PhotoSharePost> doInBackground(Void... params) {
				try {
					HashMap<String, List<TumblrPost> > tagsForDraftPosts = new HashMap<String, List<TumblrPost>>();
					queuedPosts = new HashMap<String, TumblrPost>();
					DraftPostHelper publisher = new DraftPostHelper();
					publisher.getDraftAndQueueTags(Tumblr.getSharedTumblr(getContext()), getBlogName(), tagsForDraftPosts, queuedPosts,
							DBHelper.getInstance(getContext()).getLastPublishedPostCacheDAO());

					ArrayList<String> tags = new ArrayList<String>(tagsForDraftPosts.keySet());
					
					// get last published
					this.publishProgress(getContext().getString(R.string.finding_last_published_posts));
					Map<String, LastPublishedPostCache> lastPublishedPhotoByTags = publisher.getLastPublishedPhotoByTags(
							Tumblr.getSharedTumblr(getContext()),
							getBlogName(),
							tags,
							DBHelper.getInstance(getContext()).getLastPublishedPostCacheDAO());
					
					return publisher.getDraftPostSortedByPublishDate(
							tagsForDraftPosts,
							queuedPosts,
							lastPublishedPhotoByTags);
				} catch (Exception e) {
					e.printStackTrace();
					setError(e);
				}
				return Collections.emptyList();
			}
		}.execute();
	}

	public static void startDraftListActivity(Context context) {
		Intent intent = new Intent(context, DraftListActivity.class);
		Bundle bundle = new Bundle();

		intent.putExtras(bundle);

		context.startActivity(intent);
	}

	@Override
	public void onPhotoBrowseClick(PhotoSharePost post) {
		TagPhotoBrowserActivity.startPhotoBrowserActivity(this, getBlogName(), post.getFirstTag());
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		PhotoSharePost item = (PhotoSharePost) parent.getItemAtPosition(position);
		ImageViewerActivity.startImageViewer(this, item.getFirstPhotoAltSize().get(0).getUrl());
	}
}
