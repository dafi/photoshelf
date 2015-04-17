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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

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
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.utils.TaskWithUI;
import com.ternaryop.widget.ProgressHighlightViewLayout;
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout;

public class DraftListFragment extends AbsPostsListFragment implements WaitingResultSwipeRefreshLayout.OnRefreshListener {
    private HashMap<String, TumblrPost> queuedPosts;
    private Calendar lastScheduledDate;
    private WaitingResultSwipeRefreshLayout swipeLayout;

    private ProgressHighlightViewLayout progressHighlightViewLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        View view = View.inflate(getActivity(), R.layout.draft_empty_list, (ViewGroup) rootView);
        progressHighlightViewLayout = (ProgressHighlightViewLayout) view.findViewById(android.R.id.empty);
        progressHighlightViewLayout.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_loop));
        photoListView.setEmptyView(progressHighlightViewLayout);

        swipeLayout = (WaitingResultSwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setColorScheme(R.array.progress_swipe_colors);
        swipeLayout.setOnRefreshListener(this);

        return rootView;
    }

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
        // do not start another refresh if the current one is running
        if (swipeLayout.isWaitingResult()) {
            return;
        }
        onRefreshStarted();
        task = new Importer(getActivity(), null).importFromTumblr(getBlogName(), getCurrentTextView(), new ImportCompleteCallback() {
            @Override
            public void complete() {
                progressHighlightViewLayout.incrementProgress();
                readPhotoPosts();
            }
        });
    }

    public TextView getCurrentTextView() {
        return (TextView) progressHighlightViewLayout.getCurrentView();
    }

    private void onRefreshStarted() {
        photoAdapter.clear();
        photoAdapter.notifyDataSetInvalidated();
        progressHighlightViewLayout.startProgress();
        swipeLayout.setRefreshingAndWaintingResult(true);
    }

    private void onRefreshCompleted() {
        swipeLayout.setRefreshingAndWaintingResult(false);
        progressHighlightViewLayout.stopProgress();
    }

    @Override
    protected void readPhotoPosts() {
        task = (TaskWithUI) new AbsProgressIndicatorAsyncTask<Void, String, List<PhotoShelfPost> >(getActivity(), getString(R.string.reading_draft_posts), getCurrentTextView()) {
            @Override
            protected void onProgressUpdate(String... values) {
                progressHighlightViewLayout.incrementProgress();
            }

            @Override
            protected void onPostExecute(List<PhotoShelfPost> posts) {
                super.onPostExecute(posts);

                if (!hasError()) {
                    photoAdapter.addAll(posts);
                }
                onRefreshCompleted();
                refreshUI();
            }

            @Override
            public void recreateUI() {
                super.recreateUI();
                onRefreshStarted();
            }

            @Override
            protected List<PhotoShelfPost> doInBackground(Void... params) {
                try {
                    // reading drafts
                    HashMap<String, List<TumblrPost> > tagsForDraftPosts = new HashMap<String, List<TumblrPost>>();
                    queuedPosts = new HashMap<String, TumblrPost>();
                    DraftPostHelper publisher = new DraftPostHelper();
                    publisher.getDraftAndQueueTags(Tumblr.getSharedTumblr(getContext()), getBlogName(), tagsForDraftPosts, queuedPosts);

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
                photoAdapter.removeAndRecalcGroups(item, lastScheduledDate);
                refreshUI();
                if (mode != null) {
                    mode.finish();
                }
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
            } catch (Exception ignored) {
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

    @Override
    protected boolean handleMenuItem(MenuItem item, List<PhotoShelfPost> postList, ActionMode mode) {
        switch (item.getItemId()) {
            case R.id.post_schedule:
                showScheduleDialog(postList.get(0), mode);
                return true;
        }
        return super.handleMenuItem(item, postList, mode);
    }

    @Override
    public void onRefresh() {
        refreshCache();
    }
}
