package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.app.AlertDialog;
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
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.phototumblrshare.DraftPostHelper;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.PostTag;
import com.ternaryop.phototumblrshare.db.PostTagDatabaseHelper;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog.onPostScheduleListener;
import com.ternaryop.phototumblrshare.list.OnPhotoBrowseClick;
import com.ternaryop.phototumblrshare.list.PhotoAdapter;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrAltSize;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.DialogUtils;

public class DraftListActivity extends PhotoTumblrActivity implements OnPhotoBrowseClick, OnItemClickListener {
	private static final String LOADER_PREFIX_AVATAR = "avatar";
	private static final String LOADER_PREFIX_POSTS_THUMB = "postsThumb";

	private PhotoAdapter adapter;
	
	private HashMap<String, TumblrPost> queuedPosts;
	private Calendar lastScheduledDate;
	// The menuInfo is null for submenus so store parent one
	// http://code.google.com/p/android/issues/detail?id=7139
	private AdapterView.AdapterContextMenuInfo subMenuContextMenuInfo;
	
	private String blogName;
	private ImageLoader blogAvatarImageLoader;
	private MenuItem blogNameMenuItem;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);
        
		blogAvatarImageLoader = new ImageLoader(this, LOADER_PREFIX_AVATAR);
        adapter = new PhotoAdapter(this, LOADER_PREFIX_POSTS_THUMB);
        adapter.setOnPhotoBrowseClick(this);

        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        registerForContextMenu(list);
        list.setOnItemClickListener(this);
        blogName = appSupport.getSelectedBlogName();
        readDraftPosts();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.draft, menu);
		// strangely isn't accessing from onOptionsItemSelected
		// so we store here
		blogNameMenuItem = menu.findItem(R.id.action_blogname);
		// set icon to currect avatar blog 
		blogAvatarImageLoader.displayIcon(blogNameMenuItem, Blog.getAvatarUrlBySize(blogName, 32));
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getGroupId()) {
		case R.id.group_menu_actionbar_blog:
			blogName = item.getTitle().toString();
			appSupport.setSelectedBlogName(blogName);
			blogAvatarImageLoader.displayIcon(blogNameMenuItem, Blog.getAvatarUrlBySize(blogName, 32));
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
		PostTagDatabaseHelper.getInstance(this).removeAll();
		readDraftPosts();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.list) {
			getMenuInflater().inflate(R.menu.draft_context, menu);
			AdapterView.AdapterContextMenuInfo contextMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
			PhotoSharePost post = (PhotoSharePost)adapter.getItem(contextMenuInfo.position);
			int index = 0;
			SubMenu subMenu = menu.addSubMenu(R.id.group_menu_image_dimension,
					Menu.NONE,
					Menu.NONE,
					getResources().getString(R.string.menu_show_image));
			subMenu.setHeaderTitle(getResources().getString(R.string.menu_header_show_image, post.getTags().get(0)));
			for(TumblrAltSize altSize : post.getFirstPhotoAltSize()) {
				// the item id is set to the image index into array
				subMenu.add(R.id.group_menu_item_image_dimension, index++, Menu.NONE, 
						getResources().getString(R.string.menu_image_dimension, altSize.getWidth(), altSize.getHeight()));
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

		switch (item.getGroupId()) {
		case R.id.group_menu_image_dimension:
			// allow submenus to receive the menuInfo
			subMenuContextMenuInfo = info;
			return true;
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
			showPublishDialog(info.position);
			break;
		case R.id.post_schedule:
			showScheduleDialog(info.position);
			break;
		default:
			return false;
		}
		return true;
	}

	private void showScheduleDialog(final int position) {
		final PhotoSharePost item = (PhotoSharePost)adapter.getItem(position);
		SchedulePostDialog dialog = new SchedulePostDialog(this,
				appSupport.getSelectedBlogName(),
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

	private void showPublishDialog(final int position) {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which) {
		        case DialogInterface.BUTTON_POSITIVE:
		            publishPost(position);
		            break;
		        }
		    }
		};
		
		new AlertDialog.Builder(DraftListActivity.this)
		.setMessage(R.string.are_you_sure)
		.setPositiveButton(android.R.string.yes, dialogClickListener)
	    .setNegativeButton(android.R.string.no, dialogClickListener)
	    .show();		
	}
	
	private void publishPost(final int position) {
		final PhotoSharePost item = (PhotoSharePost)adapter.getItem(position);
		Tumblr.getSharedTumblr(this).publishPost(appSupport.getSelectedBlogName(), item.getPostId(), new Callback<JSONObject>() {

			@Override
			public void complete(Tumblr tumblr, JSONObject result) {
				adapter.remove(item);
				refreshUI();
			}

			@Override
			public void failure(Tumblr tumblr, Exception e) {
				new AlertDialog.Builder(DraftListActivity.this)
				.setTitle(R.string.parsing_error)
				.setMessage(e.getLocalizedMessage())
				.show();
			}
		});
	}

	private void refreshUI() {
		setTitle(getResources().getString(R.string.posts_in_draft, adapter.getCount()));
		adapter.notifyDataSetChanged();
	}

	private void readDraftPosts() {
		adapter.clear();
		refreshUI();
		
		new AsyncTask<Void, String, Void>() {
			Exception error;
			ProgressDialog progressDialog;

			protected void onPreExecute() {
				progressDialog = new ProgressDialog(DraftListActivity.this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setMessage(getString(R.string.reading_draft_posts));
				progressDialog.show();
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
					DialogUtils.showErrorDialog(DraftListActivity.this, error);
				}
			}

			@Override
			protected Void doInBackground(Void... params) {
				try {
					Context context = DraftListActivity.this;

					HashMap<String, List<TumblrPost> > tagsForDraftPosts = new HashMap<String, List<TumblrPost>>();
					queuedPosts = new HashMap<String, TumblrPost>();
					DraftPostHelper publisher = new DraftPostHelper();
					publisher.getDraftAndQueueTags(Tumblr.getSharedTumblr(context), blogName, tagsForDraftPosts, queuedPosts,
							PostTagDatabaseHelper.getInstance(context));

					ArrayList<String> tags = new ArrayList<String>(tagsForDraftPosts.keySet());
					
					// get last published
					this.publishProgress(context.getResources().getString(R.string.finding_last_published_posts));
					Map<String, PostTag> lastPublishedPhotoByTags = publisher.getLastPublishedPhotoByTags(
							Tumblr.getSharedTumblr(context),
							blogName,
							tags,
							PostTagDatabaseHelper.getInstance(context));
					
					List<PhotoSharePost> posts = publisher.getDraftPostSortedByPublishDate(
							tagsForDraftPosts,
							queuedPosts,
							lastPublishedPhotoByTags);
			        // we must reset the flag every time before an add operation
			        adapter.setNotifyOnChange(false);
					adapter.addAll(posts);
				} catch (Exception e) {
					e.printStackTrace();
					error = e;
				}
				return null;
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
		TagPhotoBrowserActivity.startPhotoBrowserActivity(this, blogName, post.getTags().get(0));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		PhotoSharePost item = (PhotoSharePost) parent.getItemAtPosition(position);
		ImageViewerActivity.startImageViewer(this, item.getFirstPhotoAltSize().get(0).getUrl());
	}
}
