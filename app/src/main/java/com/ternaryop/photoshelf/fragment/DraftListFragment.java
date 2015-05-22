package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
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
import com.ternaryop.photoshelf.adapter.PhotoAdapter;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.photoshelf.db.Importer;
import com.ternaryop.photoshelf.db.Importer.ImportCompleteCallback;
import com.ternaryop.photoshelf.dialogs.SchedulePostDialog;
import com.ternaryop.photoshelf.dialogs.SchedulePostDialog.onPostScheduleListener;
import com.ternaryop.photoshelf.dialogs.TagNavigatorDialog;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.utils.TaskWithUI;
import com.ternaryop.widget.ProgressHighlightViewLayout;
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout;

public class DraftListFragment extends AbsPostsListFragment implements WaitingResultSwipeRefreshLayout.OnRefreshListener {
    private static final int TAG_NAVIGATOR_DIALOG = 1;

    private HashMap<String, TumblrPost> queuedPosts;
    private Calendar lastScheduledDate;
    private WaitingResultSwipeRefreshLayout swipeLayout;

    private ProgressHighlightViewLayout progressHighlightViewLayout;
    private DraftPostHelper draftPostHelper;

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

        if (taskUIRecreated()) {
            return;
        }
        draftPostHelper = new DraftPostHelper(getActivity(), getBlogName());
        refreshCache();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.draft, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        switch (photoAdapter.getCurrentSort()) {
            case PhotoAdapter.SORT_TAG_NAME:
                menu.findItem(R.id.sort_tag_name).setChecked(true);
                break;
            case PhotoAdapter.SORT_LAST_PUBLISHED_TAG:
                menu.findItem(R.id.sort_published_tag).setChecked(true);
                break;
            case PhotoAdapter.SORT_UPLOAD_TIME:
                menu.findItem(R.id.sort_upload_time).setChecked(true);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isChecked = !item.isChecked();

        switch (item.getItemId()) {
            case R.id.clear_draft_cache:
                draftPostHelper.getDraftCache().clearCache();
                refreshCache();
                return true;
            case R.id.reload_draft:
                refreshCache();
                return true;
            case R.id.sort_tag_name:
                item.setChecked(isChecked);
                photoAdapter.sortByTagName();
                photoAdapter.notifyDataSetChanged();
                photoListView.setSelection(0);
                return true;
            case R.id.sort_published_tag:
                item.setChecked(isChecked);
                photoAdapter.sortByLastPublishedTag();
                photoAdapter.notifyDataSetChanged();
                photoListView.setSelection(0);
                return true;
            case R.id.sort_upload_time:
                item.setChecked(isChecked);
                photoAdapter.sortByUploadTime();
                photoAdapter.notifyDataSetChanged();
                photoListView.setSelection(0);
                return true;
            case R.id.action_tag_navigator:
                TagNavigatorDialog.newInstance(photoAdapter.getPhotoList(), this, TAG_NAVIGATOR_DIALOG).show(getFragmentManager(), "dialog");
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
        task = (TaskWithUI) new AbsProgressIndicatorAsyncTask<Void, String, List<PhotoShelfPost>>(getActivity(), getString(R.string.reading_draft_posts), getCurrentTextView()) {
            @Override
            protected void onProgressUpdate(String... values) {
                progressHighlightViewLayout.incrementProgress();
            }

            @Override
            protected void onPostExecute(List<PhotoShelfPost> posts) {
                super.onPostExecute(posts);

                if (!hasError()) {
                    photoAdapter.addAll(posts);
                    photoAdapter.sort();
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
                    HashMap<String, List<TumblrPost>> tagsForDraftPosts = new HashMap<String, List<TumblrPost>>();
                    queuedPosts = new HashMap<String, TumblrPost>();
                    draftPostHelper.getDraftAndQueueTags(tagsForDraftPosts, queuedPosts);

                    ArrayList<String> tags = new ArrayList<String>(tagsForDraftPosts.keySet());

                    // get last published
                    this.publishProgress(getContext().getString(R.string.finding_last_published_posts));
                    Map<String, Long> lastPublishedPhotoByTags = draftPostHelper.getLastPublishedPhotoByTags(tags);

                    return draftPostHelper.getDraftPosts(
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
                        draftPostHelper.getDraftCache().deleteItem(item);
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

    @Override
    public void onPostAction(TumblrPhotoPost post, int postAction, int resultCode) {
        if (resultCode == POST_ACTION_OK) {
            switch (postAction) {
                case POST_ACTION_EDIT:
                    draftPostHelper.getDraftCache().updateItem(post);
                    break;
                case POST_ACTION_PUBLISH:
                case POST_ACTION_DELETE:
                    draftPostHelper.getDraftCache().deleteItem(post);
                    break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAG_NAVIGATOR_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    photoListView.setSelection(TagNavigatorDialog.findTagIndex(photoAdapter.getPhotoList(), data));
                }
                break;
        }
    }

}
