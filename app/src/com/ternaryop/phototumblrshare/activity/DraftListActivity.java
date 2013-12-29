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

import com.ternaryop.phototumblrshare.DraftPostHelper;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.DBHelper;
import com.ternaryop.phototumblrshare.db.Importer;
import com.ternaryop.phototumblrshare.db.Importer.ImportCompleteCallback;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog.onPostScheduleListener;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;

public class DraftListActivity extends PostsListActivity {
	private HashMap<String, TumblrPost> queuedPosts;
	private Calendar lastScheduledDate;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        photoAdapter.setOnPhotoBrowseClick(this);
        photoAdapter.setRecomputeGroupIds(true);

        refreshCache();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.draft, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_draft_refresh:
		    refreshCache();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void refreshCache() {
		Importer.importFromTumblr(this, getBlogName(), new ImportCompleteCallback() {
			@Override
			public void complete() {
				readPhotoPosts();
			}
		});
	}

	protected void refreshUI() {
		setTitle(getResources().getQuantityText(R.plurals.posts_in_draft, photoAdapter.getCount()));
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
							DBHelper.getInstance(getContext()).getPostTagDAO());

					ArrayList<String> tags = new ArrayList<String>(tagsForDraftPosts.keySet());
					
					// get last published
					this.publishProgress(getContext().getString(R.string.finding_last_published_posts));
					Map<String, Long> lastPublishedPhotoByTags = publisher.getLastPublishedPhotoByTags(
							Tumblr.getSharedTumblr(getContext()),
							getBlogName(),
							tags,
							DBHelper.getInstance(getContext()).getPostTagDAO());
					
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
