package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.phototumblrshare.DraftPostHelper;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.DBHelper;
import com.ternaryop.phototumblrshare.db.LastPublishedPostCache;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog.onPostScheduleListener;
import com.ternaryop.phototumblrshare.list.OnPhotoBrowseClick;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class DraftListActivity extends PostsListActivity implements OnPhotoBrowseClick {
	private static final String LOADER_PREFIX_AVATAR = "avatar";
	
	private HashMap<String, TumblrPost> queuedPosts;
	private Calendar lastScheduledDate;
	
	private ImageLoader blogAvatarImageLoader;
	private MenuItem blogNameMenuItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
		blogAvatarImageLoader = new ImageLoader(this, LOADER_PREFIX_AVATAR);
        photoAdapter.setOnPhotoBrowseClick(this);
        photoAdapter.setRecomputeGroupIds(true);

        readPhotoPosts();
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getGroupId()) {
		case R.id.group_menu_actionbar_blog:
			String blogName = item.getTitle().toString();
			appSupport.setSelectedBlogName(blogName);
			setActionBarIcon();
			blogAvatarImageLoader.displayIcon(Blog.getAvatarUrlBySize(blogName, 32), blogNameMenuItem);
			readPhotoPosts();
			return true;
		}
		
		switch (item.getItemId()) {
		case R.id.action_draft_refresh:
			readPhotoPosts();
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
		readPhotoPosts();
	}

	protected void refreshUI() {
		int resId = photoAdapter.getCount() == 1 ? R.string.posts_in_draft_singular : R.string.posts_in_draft_plural;
		setTitle(getString(resId, photoAdapter.getCount()));
		photoAdapter.notifyDataSetChanged();
	}

	@Override
	protected void readPhotoPosts() {
		photoAdapter.clear();
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
					photoAdapter.addAll(posts);
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
    protected int getActionModeMenuId() {
    	return R.menu.draft_context;
    }
	
	private void showScheduleDialog(final int position, final ActionMode mode) {
		final PhotoSharePost item = (PhotoSharePost)photoAdapter.getItem(position);
		SchedulePostDialog dialog = new SchedulePostDialog(this,
				getBlogName(),
				item,
				findScheduleTime(),
				new onPostScheduleListener() {
			@Override
			public void onPostScheduled(long id, Calendar scheduledDateTime) {
				lastScheduledDate = (Calendar) scheduledDateTime.clone();
				photoAdapter.remove(item);
				refreshUI();
				mode.finish();
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

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.post_schedule:
			SparseBooleanArray checkedItemPositions = photoListView.getCheckedItemPositions();
			int firstPosition = checkedItemPositions.keyAt(0);
			
			showScheduleDialog(firstPosition, mode);
			return true;
		}
		return super.onActionItemClicked(mode, item);
    }
}
