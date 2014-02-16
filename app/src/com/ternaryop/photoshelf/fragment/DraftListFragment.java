package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.ternaryop.photoshelf.DraftPostHelper;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.db.Importer;
import com.ternaryop.photoshelf.db.Importer.ImportCompleteCallback;
import com.ternaryop.photoshelf.dialogs.SchedulePostDialog;
import com.ternaryop.photoshelf.dialogs.SchedulePostDialog.onPostScheduleListener;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;
import com.ternaryop.utils.TaskWithUI;

public class DraftListFragment extends AbsPostsListFragment {
    private HashMap<String, TumblrPost> queuedPosts;
    private Calendar lastScheduledDate;

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        photoAdapter.setOnPhotoBrowseClick(this);
        photoAdapter.setRecomputeGroupIds(true);

        if (taskUIRecreated()) {
        	return;
        }
    	refreshCache();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.draft, menu);
        super.onCreateOptionsMenu(menu, inflater);
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
        task = Importer.importFromTumblr(getActivity(), getBlogName(), new ImportCompleteCallback() {
            @Override
            public void complete() {
                readPhotoPosts();
            }
        });
    }

    @Override
    protected void readPhotoPosts() {
        photoAdapter.clear();
        
        task = (TaskWithUI) new AbsProgressBarAsyncTask<Void, String, List<PhotoShelfPost> >(getActivity(), getString(R.string.reading_draft_posts)) {
            @Override
            protected void onProgressUpdate(String... values) {
                getProgressDialog().setMessage(values[0]);
            }
            
            @Override
            protected void onPreExecute() {
            	super.onPreExecute();
            }
            
            @Override
            protected void onPostExecute(List<PhotoShelfPost> posts) {
                super.onPostExecute(posts);
                
                if (getError() == null) {
                    photoAdapter.addAll(posts);
                }
                refreshUI();
            }

            @Override
            protected List<PhotoShelfPost> doInBackground(Void... params) {
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

    @Override
    protected int getActionModeMenuId() {
        return R.menu.draft_context;
    }
    
    private void showScheduleDialog(final PhotoShelfPost item, final ActionMode mode) {
        SchedulePostDialog dialog = new SchedulePostDialog(getActivity(),
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
        cal.add(Calendar.HOUR, fragmentActivityStatus.getAppSupport().getDefaultScheduleHoursSpan());
        
        return cal;
    }

    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.post_schedule:
            showScheduleDialog(getSelectedPosts().get(0), mode);
            return true;
        }
        return super.onActionItemClicked(mode, item);
    }
}
