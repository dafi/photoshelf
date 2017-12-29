package com.ternaryop.photoshelf.fragment;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
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
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.db.Importer;
import com.ternaryop.photoshelf.db.TumblrPostCacheDAO;
import com.ternaryop.photoshelf.dialogs.SchedulePostDialog;
import com.ternaryop.photoshelf.dialogs.TagNavigatorDialog;
import com.ternaryop.photoshelf.event.CounterEvent;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.tumblr.TumblrPost;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.widget.ProgressHighlightViewLayout;
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.ternaryop.photoshelf.db.TumblrPostCache.CACHE_TYPE_DRAFT;

public class DraftListFragment extends AbsPostsListFragment implements WaitingResultSwipeRefreshLayout.OnRefreshListener {
    private static final int TAG_NAVIGATOR_DIALOG = 1;
    public static final String PREF_DRAFT_SORT_TYPE = "draft_sort_type";
    public static final String PREF_DRAFT_SORT_ASCENDING = "draft_sort_ascending";

    private List<TumblrPost> queuedPosts;
    private Calendar lastScheduledDate;
    private WaitingResultSwipeRefreshLayout swipeLayout;

    private ProgressHighlightViewLayout progressHighlightViewLayout;
    private DraftPostHelper draftPostHelper;
    private TumblrPostCacheDAO draftCache;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        View view = View.inflate(getActivity(), R.layout.draft_empty_list, (ViewGroup) rootView);
        progressHighlightViewLayout = view.findViewById(android.R.id.empty);
        progressHighlightViewLayout.setAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fade_loop));
        photoAdapter.setCounterType(CounterEvent.DRAFT);

        if (rootView != null) {
            swipeLayout = rootView.findViewById(R.id.swipe_container);
            swipeLayout.setColorScheme(R.array.progress_swipe_colors);
            swipeLayout.setOnRefreshListener(this);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        photoAdapter.setOnPhotoBrowseClick(this);
        loadSettings();

        draftPostHelper = new DraftPostHelper(getActivity(), getBlogName());
        draftCache = DBHelper.getInstance(getActivity()).getTumblrPostCacheDAO();

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
                draftCache.clearCache(CACHE_TYPE_DRAFT);
                refreshCache();
                return true;
            case R.id.reload_draft:
                refreshCache();
                return true;
            case R.id.sort_tag_name:
                item.setChecked(isChecked);
                photoAdapter.sortByTagName();
                photoAdapter.notifyDataSetChanged();
                scrollToPosition(0);
                saveSettings();
                return true;
            case R.id.sort_published_tag:
                item.setChecked(isChecked);
                photoAdapter.sortByLastPublishedTag();
                photoAdapter.notifyDataSetChanged();
                scrollToPosition(0);
                saveSettings();
                return true;
            case R.id.sort_upload_time:
                item.setChecked(isChecked);
                photoAdapter.sortByUploadTime();
                photoAdapter.notifyDataSetChanged();
                scrollToPosition(0);
                saveSettings();
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

        compositeDisposable.add(new Importer(getActivity()).importFromTumblr(getBlogName(), Importer.schedulers(), getCurrentTextView())
                .subscribe(posts -> {
                    progressHighlightViewLayout.incrementProgress();
                    // delete from cache the published posts
                    draftCache.delete(posts, CACHE_TYPE_DRAFT);

                    readPhotoPosts();
                }, t -> DialogUtils.showErrorDialog(getActivity(), t))
        );
    }

    public TextView getCurrentTextView() {
        return (TextView) progressHighlightViewLayout.getCurrentView();
    }

    private void onRefreshStarted() {
        photoAdapter.clear();
        progressHighlightViewLayout.setVisibility(View.VISIBLE);
        progressHighlightViewLayout.startProgress();
        swipeLayout.setRefreshingAndWaintingResult(true);
    }

    private void onRefreshCompleted() {
        swipeLayout.setRefreshingAndWaintingResult(false);
        progressHighlightViewLayout.stopProgress();
        progressHighlightViewLayout.setVisibility(View.GONE);
    }

    @Override
    protected void readPhotoPosts() {
        final long maxTimestamp = draftCache.findMostRecentTimestamp(getBlogName(), CACHE_TYPE_DRAFT);
        compositeDisposable.add(
                Single
                        .zip(
                                draftPostHelper.getNewerDraftPosts(maxTimestamp).subscribeOn(Schedulers.io()),
                                draftPostHelper.getQueuePosts().subscribeOn(Schedulers.io()),
                                this::getCachedPhotoPosts
                        )
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess(tagsForDraftPosts -> progressHighlightViewLayout.incrementProgress())
                        .observeOn(Schedulers.newThread())
                        .flatMap(draftPosts -> {
                            final Map<String, List<TumblrPost>> tagsForDraftPosts = draftPostHelper.groupPostByTag(draftPosts);
                            final Map<String, List<TumblrPost>> tagsForQueuePosts = draftPostHelper.groupPostByTag(queuedPosts);
                            return draftPostHelper
                                    .getTagLastPublishedMap(tagsForDraftPosts.keySet())
                                    .flatMap(lastPublished -> Single.just(draftPostHelper.getPhotoShelfPosts(
                                            tagsForDraftPosts,
                                            tagsForQueuePosts,
                                            lastPublished)));
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> compositeDisposable.add(disposable))
                        .doFinally(() -> {
                            onRefreshCompleted();
                            refreshUI();
                        })
                        .subscribe(posts -> {
                            photoAdapter.addAll(posts);
                            photoAdapter.sort();
                        }, t -> DialogUtils.showErrorDialog(getActivity(), t))
        );
    }

    private List<TumblrPost> getCachedPhotoPosts(List<TumblrPost> newerDraftPosts, List<TumblrPost> queuePosts) {
        // save queue for future use
        this.queuedPosts = queuePosts;

        // update the cache with new draft posts
        draftCache.write(newerDraftPosts, CACHE_TYPE_DRAFT);
        // delete from the cache any post moved from draft to scheduled
        draftCache.delete(queuePosts, CACHE_TYPE_DRAFT);
        // return the updated/cleaned up cache
        return draftCache.read(getBlogName(), CACHE_TYPE_DRAFT);
    }

    @Override
    protected int getActionModeMenuId() {
        return R.menu.draft_context;
    }

    private void showScheduleDialog(final PhotoShelfPost item, final ActionMode mode) {
        new SchedulePostDialog(getActivity(),
                getBlogName(),
                item,
                findScheduleTime(),
                (id, scheduledDateTime, completable) -> completable
                        .doOnSubscribe(d -> compositeDisposable.add(d))
                        .subscribe(() -> onScheduleRefreshUI(item, mode, scheduledDateTime), t -> DialogUtils.showErrorDialog(getActivity(), t)))
                .show();
    }

    private void onScheduleRefreshUI(final PhotoShelfPost item, final ActionMode mode, Calendar scheduledDateTime) {
        colorItemDecoration.setColor(ContextCompat.getColor(getActivity(), R.color.photo_item_animation_schedule_bg));
        lastScheduledDate = (Calendar) scheduledDateTime.clone();
        photoAdapter.removeAndRecalcGroups(item, lastScheduledDate);
        draftCache.deleteItem(item);
        refreshUI();
        if (mode != null) {
            mode.finish();
        }
    }

    private Calendar findScheduleTime() {
        Calendar cal;

        if (lastScheduledDate == null) {
            cal = Calendar.getInstance();
            long maxScheduledTime = System.currentTimeMillis();

            try {
                for (TumblrPost post : queuedPosts) {
                    long scheduledTime = post.getScheduledPublishTime() * 1000;
                    if (scheduledTime > maxScheduledTime) {
                        maxScheduledTime = scheduledTime;
                    }
                }
            } catch (Exception ignored) {
            }
            // Calendar.MINUTE isn't reset otherwise the calc may be inaccurate
            cal.setTime(new Date(maxScheduledTime));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else {
            cal = (Calendar) lastScheduledDate.clone();
        }
        // set next queued post time
        cal.add(Calendar.MINUTE, fragmentActivityStatus.getAppSupport().getDefaultScheduleMinutesTimeSpan());

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
                    draftCache.updateItem(post, CACHE_TYPE_DRAFT);
                    break;
                case POST_ACTION_PUBLISH:
                case POST_ACTION_DELETE:
                    draftCache.deleteItem(post);
                    break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAG_NAVIGATOR_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    scrollToPosition(TagNavigatorDialog.findTagIndex(photoAdapter.getPhotoList(), data));
                }
                break;
        }
    }

    public void scrollToPosition(int position) {
        // offset set to 0 put the item to the top
        ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(position, 0);
    }

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        photoAdapter.sort(
                preferences.getInt(PREF_DRAFT_SORT_TYPE, PhotoAdapter.SORT_LAST_PUBLISHED_TAG),
                preferences.getBoolean(PREF_DRAFT_SORT_ASCENDING, true));
    }

    private void saveSettings() {
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putInt(PREF_DRAFT_SORT_TYPE, photoAdapter.getCurrentSort())
                .putBoolean(PREF_DRAFT_SORT_ASCENDING, photoAdapter.getCurrentSortable().isAscending())
                .apply();
    }

}
