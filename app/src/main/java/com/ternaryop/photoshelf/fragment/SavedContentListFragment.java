package com.ternaryop.photoshelf.fragment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.ternaryop.feedly.FeedlyContent;
import com.ternaryop.feedly.FeedlyManager;
import com.ternaryop.feedly.FeedlyRateLimit;
import com.ternaryop.feedly.TokenExpiredException;
import com.ternaryop.photoshelf.BuildConfig;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.ImagePickerActivity;
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentAdapter;
import com.ternaryop.photoshelf.adapter.feedly.OnFeedlyContentClick;
import com.ternaryop.photoshelf.view.PhotoShelfSwipe;
import com.ternaryop.utils.JSONUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import org.json.JSONArray;

import static com.ternaryop.photoshelf.adapter.feedly.FeedlyContentAdapter.SORT_TITLE_NAME;

public class SavedContentListFragment extends AbsPhotoShelfFragment implements OnFeedlyContentClick {
    public static final String PREF_MAX_FETCH_ITEMS_COUNT = "savedContent.MaxFetchItemCount";
    public static final String PREF_NEWER_THAN_HOURS = "savedContent.NewerThanHours";
    public static final String PREF_DELETE_ON_REFRESH = "savedContent.DeleteOnRefresh";
    public static final String PREF_SORT_TYPE = "savedContent.SortType";
    public static final String PREF_SORT_ASCENDING = "savedContent.SortAscending";

    public static final int IDEFAULT_MAX_FETCH_ITEMS_COUNT = 300;
    public static final int DEFAULT_NEWER_THAN_HOURS = 24;
    public static final int ONE_HOUR_MILLIS = 60 * 60 * 1000;
    public static final String PREF_FEEDLY_ACCESS_TOKEN = "feedlyAccessToken";

    private FeedlyContentAdapter adapter;
    protected RecyclerView recyclerView;
    private FeedlyManager feedlyManager;
    private SharedPreferences preferences;
    private PhotoShelfSwipe photoShelfSwipe;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.saved_content_list, container, false);

        initRecyclerView(rootView);

        setHasOptionsMenu(true);

        photoShelfSwipe = new PhotoShelfSwipe(rootView, R.id.swipe_container, new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh(true);
            }
        });
        return rootView;
    }

    private void initRecyclerView(View rootView) {
        adapter = new FeedlyContentAdapter(getActivity(), fragmentActivityStatus.getAppSupport().getSelectedBlogName());

        recyclerView = (RecyclerView) rootView.findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        adapter.setSortType(preferences.getInt(PREF_SORT_TYPE, SORT_TITLE_NAME));
        adapter.setClickListener(this);

        feedlyManager = new FeedlyManager(preferences.getString(PREF_FEEDLY_ACCESS_TOKEN, getString(R.string.FEEDLY_ACCESS_TOKEN)),
                getString(R.string.FEEDLY_USER_ID),
                getString(R.string.FEEDLY_REFRESH_TOKEN));

        refresh(false);
    }

    private void refresh(final boolean deleteItemsIfAllowed) {
        // do not start another refresh if the current one is running
        if (photoShelfSwipe.getSwipe().isWaitingResult()) {
            return;
        }
        Single
                .fromCallable(callableFeedlyReader(deleteItemsIfAllowed))
                .compose(photoShelfSwipe.<List<FeedlyContent>>applySwipe())
                .subscribe(new FeedlyObserver<List<FeedlyContent>>() {
                    @Override
                    public void onSuccess(List<FeedlyContent> posts) {
                        setItems(posts);
                    }
                });
    }

    @NonNull
    private Callable<List<FeedlyContent>> callableFeedlyReader(final boolean deleteItemsIfAllowed) {
        return new Callable<List<FeedlyContent>>() {
            @Override
            public List<FeedlyContent> call() throws Exception {
                if (BuildConfig.DEBUG) {
                    return fakeCall();
                }
                deleteItems(deleteItemsIfAllowed);
                return readSavedContents();
            }
        };
    }

    private void setItems(final List<FeedlyContent> items) {
        adapter.clear();
        adapter.addAll(items);
        adapter.sort();
        adapter.notifyDataSetChanged();
        scrollToPosition(0);

        refreshUI();
    }

    private void deleteItems(final boolean deleteItemsIfAllowed) throws Exception {
        if (deleteItemsIfAllowed && deleteOnRefresh()) {
            List<String> idList = new ArrayList<>();
            for (FeedlyContent fc : adapter.getUncheckedItems()) {
                idList.add(fc.getId());
            }
            feedlyManager.markSaved(idList, false);
        }
    }

    @Nullable
    private List<FeedlyContent> readSavedContents() throws Exception {
        long ms = System.currentTimeMillis() - getNewerThanHours() * ONE_HOUR_MILLIS;
        return feedlyManager.getStreamContents(feedlyManager.getGlobalSavedTag(), getMaxFetchitemCount(), ms, null);
    }

    @Nullable
    private List<FeedlyContent> fakeCall() throws Exception {
        try (InputStream is = getActivity().getAssets().open("sample/feedly.json")) {
            final JSONArray items = JSONUtils.jsonFromInputStream(is).getJSONArray("items");
            final ArrayList<FeedlyContent> list = new ArrayList<>(items.length());
            for (int i = 0; i < items.length(); i++) {
                list.add(new FeedlyContent(items.getJSONObject(i)));
            }
            return list;
        }
    }


    @Override
    protected void refreshUI() {
        getSupportActionBar().setSubtitle(getResources().getQuantityString(
                R.plurals.posts_count,
                adapter.getItemCount(),
                adapter.getItemCount()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.saved_content, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        switch (adapter.getCurrentSort()) {
            case SORT_TITLE_NAME:
                menu.findItem(R.id.sort_title_name).setChecked(true);
                break;
            case FeedlyContentAdapter.SORT_SAVED_TIMESTAMP:
                menu.findItem(R.id.sort_saved_time).setChecked(true);
                break;
            case FeedlyContentAdapter.SORT_LAST_PUBLISH_TIMESTAMP:
                menu.findItem(R.id.sort_published_tag).setChecked(true);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isChecked = !item.isChecked();

        switch (item.getItemId()) {
            case R.id.action_refresh:
                refresh(true);
                return true;
            case R.id.action_api_usage:
                showAPIUsage();
                return true;
            case R.id.action_refresh_token:
                refreshToken();
                return true;
            case R.id.action_settings:
                settings();
                return true;
            case R.id.sort_title_name:
                item.setChecked(isChecked);
                adapter.sortByTitleName();
                adapter.notifyDataSetChanged();
                scrollToPosition(0);
                saveSortSettings();
                return true;
            case R.id.sort_saved_time:
                item.setChecked(isChecked);
                adapter.sortBySavedTimestamp();
                adapter.notifyDataSetChanged();
                scrollToPosition(0);
                saveSortSettings();
                return true;
            case R.id.sort_published_tag:
                item.setChecked(isChecked);
                adapter.sortByLastPublishTimestamp();
                adapter.notifyDataSetChanged();
                scrollToPosition(0);
                saveSortSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshToken() {
        Single
                .fromCallable(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return feedlyManager.refreshAccessToken(
                                getString(R.string.FEEDLY_CLIENT_ID),
                                getString(R.string.FEEDLY_CLIENT_SECRET));
                    }
                })
                .compose(photoShelfSwipe.<String>applySwipe())
                .subscribe(new FeedlyObserver<String>() {
                    @Override
                    public void onSuccess(final String accessToken) {
                        preferences.edit().putString(PREF_FEEDLY_ACCESS_TOKEN, accessToken).apply();
                        feedlyManager.setAccessToken(preferences.getString(PREF_FEEDLY_ACCESS_TOKEN, accessToken));
                        // hide swipe otherwise refresh() exists immediatelly
                        photoShelfSwipe.getSwipe().setRefreshingAndWaintingResult(false);
                        refresh(true);
                    }
                });
    }

    private void saveSortSettings() {
        preferences
                .edit()
                .putInt(PREF_SORT_TYPE, adapter.getCurrentSort())
                .putBoolean(PREF_SORT_ASCENDING, adapter.getCurrentSortable().isAscending())
                .apply();
    }

    public void scrollToPosition(int position) {
        // offset set to 0 put the item to the top
        ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(position, 0);
    }

    private void showAPIUsage() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.api_usage)
                .setMessage(getString(R.string.feedly_api_calls_count, FeedlyRateLimit.instance.getApiCallsCount()) + "\n"
                        + getString(R.string.feedly_api_reset_limit, FeedlyRateLimit.instance.getApiResetLimitAsString()))
                .show();
    }

    private void settings() {
        final View settingsView = getActivity().getLayoutInflater().inflate(R.layout.saved_content_settings, null);
        fillSettingsView(settingsView);
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.settings)
                .setView(settingsView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateSettings(settingsView);
                    }
                })
                .setNegativeButton(R.string.cancel_title, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void updateSettings(View view) {
        EditText fetch = (EditText) view.findViewById(R.id.max_fetch_items_count);
        EditText newerThanHours = (EditText) view.findViewById(R.id.newer_than_hours);
        CheckBox deleteOnRefresh = (CheckBox) view.findViewById(R.id.delete_on_refresh);
        preferences.edit()
                .putInt(PREF_MAX_FETCH_ITEMS_COUNT, Integer.parseInt(fetch.getText().toString()))
                .putInt(PREF_NEWER_THAN_HOURS, Integer.parseInt(newerThanHours.getText().toString()))
                .putBoolean(PREF_DELETE_ON_REFRESH, deleteOnRefresh.isChecked())
                .apply();
    }

    private void fillSettingsView(View view) {
        EditText fetch = (EditText) view.findViewById(R.id.max_fetch_items_count);
        EditText newerThanHours = (EditText) view.findViewById(R.id.newer_than_hours);
        CheckBox deleteOnRefresh = (CheckBox) view.findViewById(R.id.delete_on_refresh);

        fetch.setText(String.valueOf(getMaxFetchitemCount()));
        newerThanHours.setText(String.valueOf(getNewerThanHours()));
        deleteOnRefresh.setChecked(deleteOnRefresh());
    }

    @Override
    public void onTitleClick(int position) {
        ImagePickerActivity.startImagePicker(getActivity(), adapter.getItem(position).getOriginId());
    }

    @Override
    public void onToggleClick(final int position, final boolean checked) {
        if (deleteOnRefresh()) {
            return;
        }
        Completable
                .fromAction(new Action() {
                    @Override
                    public void run() throws Exception {
                        ArrayList<String> list = new ArrayList<>();
                        list.add(adapter.getItem(position).getId());
                        feedlyManager.markSaved(list, checked);
                    }
                })
                .compose(photoShelfSwipe.<Void>applyCompletableSwipe())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable d) throws Exception {
                        compositeDisposable.add(d);
                    }
                })
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {}
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable t) throws Exception {
                        showSnackbar(makeSnake(recyclerView, t));
                    }
                });
    }

    private int getNewerThanHours() {
        return preferences.getInt(PREF_NEWER_THAN_HOURS, DEFAULT_NEWER_THAN_HOURS);
    }

    private boolean deleteOnRefresh() {
        return preferences.getBoolean(PREF_DELETE_ON_REFRESH, false);
    }

    private int getMaxFetchitemCount() {
        return preferences.getInt(PREF_MAX_FETCH_ITEMS_COUNT, IDEFAULT_MAX_FETCH_ITEMS_COUNT);
    }

    @NonNull
    @Override
    protected Snackbar makeSnake(@NonNull View view, @NonNull Throwable t) {
        if (t instanceof TokenExpiredException) {
            Snackbar snackbar = Snackbar.make(recyclerView, R.string.token_expired, Snackbar.LENGTH_INDEFINITE);
            snackbar
                    .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.snack_error_color))
                    .setAction(getResources().getString(R.string.refresh).toLowerCase(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refreshToken();
                        }
                    });
            return snackbar;
        }
        return super.makeSnake(view, t);
    }

    abstract class FeedlyObserver<T> implements SingleObserver<T> {
        @Override
        public void onSubscribe(Disposable d) {
            compositeDisposable.add(d);
        }

        @Override
        public void onError(Throwable t) {
            showSnackbar(makeSnake(recyclerView, t));
        }
    }
}
