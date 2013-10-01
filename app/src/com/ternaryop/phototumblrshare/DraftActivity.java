package com.ternaryop.phototumblrshare;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.ternaryop.phototumblrshare.db.PostTag;
import com.ternaryop.phototumblrshare.db.PostTagDatabaseHelper;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog;
import com.ternaryop.phototumblrshare.dialogs.SchedulePostDialog.onPostScheduleListener;
import com.ternaryop.phototumblrshare.list.LazyAdapter;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;

public class DraftActivity extends PhotoTumblrActivity {
    private LazyAdapter adapter;
	private AppSupport appSupport;
	private Map<String, JSONObject> queuedPosts;
	private Calendar lastScheduledDate;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft);
        
        appSupport = new AppSupport(this);

        adapter = new LazyAdapter(this);
        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        registerForContextMenu(list);

		if (appSupport.getSelectedBlogName() == null) {
			Toast.makeText(this.getApplicationContext(),
					getResources().getString(R.string.no_selected_blog),
					Toast.LENGTH_LONG)
					.show();
		} else {
			readDraftPosts();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.draft, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
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
		new PostTagDatabaseHelper(this).removeAll();
		readDraftPosts();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (v.getId() == R.id.list) {
			getMenuInflater().inflate(R.menu.draft_context, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.post_publish:
			showPublishDialog(info.position);
			break;
		case R.id.post_schedule:
			showScheduleDialog(info.position);
			break;
		default:
			break;
		}
		return true;
	}

	private void showScheduleDialog(final int position) {
		@SuppressWarnings("unchecked")
		SchedulePostDialog dialog = new SchedulePostDialog(this,
				appSupport.getSelectedBlogName(),
				(Map<String, String>)adapter.getItem(position),
				findScheduleTime(),
				new onPostScheduleListener() {
			@Override
			public void onPostScheduled(long id, Calendar scheduledDateTime) {
				lastScheduledDate = (Calendar) scheduledDateTime.clone();
				adapter.remove(position);
				adapter.notifyDataSetChanged();
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
				for (JSONObject post : queuedPosts.values()) {
					long scheduledTime = post.getLong("scheduled_publish_time") * 1000;
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
		
		new AlertDialog.Builder(DraftActivity.this)
		.setMessage(R.string.are_you_sure)
		.setPositiveButton(android.R.string.yes, dialogClickListener)
	    .setNegativeButton(android.R.string.no, dialogClickListener)
	    .show();		
	}
	
	private void publishPost(final int position) {
		Tumblr.getTumblr(this, new Callback<Void>() {

			@Override
			public void complete(Tumblr tumblr, Void result) {
				@SuppressWarnings("unchecked")
				Map<String, String> item = (Map<String, String>)adapter.getItem(position);
				final long id = Long.valueOf(item.get(LazyAdapter.KEY_ID));
				tumblr.publishPost(appSupport.getSelectedBlogName(), id, new Callback<JSONObject>() {

					@Override
					public void complete(Tumblr tumblr, JSONObject result) {
						adapter.remove(position);
						adapter.notifyDataSetChanged();
					}

					@Override
					public void failure(Tumblr tumblr, Exception e) {
						new AlertDialog.Builder(DraftActivity.this)
						.setTitle(R.string.parsing_error)
						.setMessage(e.getLocalizedMessage())
						.show();
					}
				});
			}

			@Override
			public void failure(Tumblr tumblr, Exception e) {
			}
		});
	}

	private void readDraftPosts() {
		final String tumblrName = appSupport.getSelectedBlogName();
		adapter.clear();
		Tumblr.getTumblr(this, new Callback<Void>() {
			ProgressDialog progressDialog;

			@Override
			public void complete(final Tumblr t, Void result) {
				new AsyncTask<Void, String, Void>() {

					protected void onPreExecute() {
						progressDialog = new ProgressDialog(DraftActivity.this);
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
						setTitle(getResources().getString(R.string.posts_in_draft, adapter.getCount()));
						progressDialog.dismiss();
						adapter.notifyDataSetChanged();
					}

					@Override
					protected Void doInBackground(Void... params) {
						try {
							Context context = DraftActivity.this;
							TumblrPublisher publisher = new TumblrPublisher();
							// get daft posts
							Map<String, List<JSONObject> > tagsForDraftPosts = publisher.getTagsForDraftPosts(t.getDraftPosts(tumblrName));
							ArrayList<String> tags = new ArrayList<String>(tagsForDraftPosts.keySet());
							
							// get queued posts
							this.publishProgress(context.getResources().getString(R.string.reading_queued_posts));
							queuedPosts = publisher.getTagsForQueuedPosts(t.getQueue(tumblrName));

							// get last published
							this.publishProgress(context.getResources().getString(R.string.finding_last_published_posts));
							Map<String, PostTag> lastPublishedPhotoByTags = publisher.getLastPublishedPhotoByTags(tags, t, tumblrName, new PostTagDatabaseHelper(context));
							
							List<JSONObject> posts = publisher.getDraftPostSortedByPublishDate(tagsForDraftPosts, queuedPosts, lastPublishedPhotoByTags);

							for (JSONObject post : posts) {
					            HashMap<String, String> map = new HashMap<String, String>();
					            
					            JSONObject photos = post.getJSONArray("photos").getJSONObject(0);
					            JSONArray altSizes = photos.getJSONArray("alt_sizes");
					            // TODO find 75x75 image url
					            JSONObject smallestImage = altSizes.getJSONObject(altSizes.length() - 1);
					            map.put(LazyAdapter.KEY_ID, "" + post.getLong("id"));
					            map.put(LazyAdapter.KEY_TITLE, post.getJSONArray("tags").getString(0));
					            long photoTimestamp = post.getLong("photo-tumblr-share-timestamp");
								map.put(LazyAdapter.KEY_TIME, TumblrPublisher.formatPublishDaysAgo(photoTimestamp));
					            map.put(LazyAdapter.KEY_THUMB_URL, smallestImage.getString("url"));
					            if (photoTimestamp == Long.MAX_VALUE) {
					            	map.put(LazyAdapter.KEY_LAST_PUBLISH, LazyAdapter.KEY_PUBLISH_NEVER);
					            } else if (photoTimestamp > System.currentTimeMillis()) {
					            	map.put(LazyAdapter.KEY_LAST_PUBLISH, LazyAdapter.KEY_PUBLISH_FUTURE);
					            } else {
					            	map.put(LazyAdapter.KEY_LAST_PUBLISH, LazyAdapter.KEY_PUBLISH_PAST);
					            }
					 
					            adapter.addItem(map);
					        }
						} catch (Exception e) {
							// TODO: handle exception
							e.printStackTrace();
						}
						return null;
					}
					
				}.execute();
			}

			@Override
			public void failure(Tumblr tumblr, Exception ex) {
				DialogUtils.showErrorDialog(DraftActivity.this, ex);
			}
		});
	}
}
