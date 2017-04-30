package com.ternaryop.photoshelf.fragment;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity;
import com.ternaryop.photoshelf.adapter.GridViewPhotoAdapter;
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice;
import com.ternaryop.photoshelf.adapter.Selection;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.photoshelf.service.PublishIntentService;
import com.ternaryop.photoshelf.view.AutofitGridLayoutManager;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout;

public class BirthdaysPublisherFragment extends AbsPhotoShelfFragment implements SwipeRefreshLayout.OnRefreshListener, OnPhotoBrowseClickMultiChoice, ActionMode.Callback  {
    private static final int PICK_IMAGE_REQUEST_CODE = 100;
    private static final String LOADER_PREFIX = "mediumThumb";

    private GridViewPhotoAdapter gridViewPhotoAdapter;
    private WaitingResultSwipeRefreshLayout swipeLayout;
    private ActionMode actionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthdays_publisher, container, false);

        gridViewPhotoAdapter = new GridViewPhotoAdapter(getActivity(), LOADER_PREFIX);
        gridViewPhotoAdapter.setOnPhotoBrowseClick(this);

        RecyclerView.LayoutManager layout = new AutofitGridLayoutManager(getActivity(), (int) getResources().getDimension(R.dimen.grid_layout_thumb_width));
        RecyclerView gridView = (RecyclerView) rootView.findViewById(R.id.gridview);
        gridView.setAdapter(gridViewPhotoAdapter);
        gridView.setHasFixedSize(true);
        gridView.setLayoutManager(layout);

        swipeLayout = (WaitingResultSwipeRefreshLayout) rootView.findViewById(R.id.swipe_container);
        swipeLayout.setColorScheme(R.array.progress_swipe_colors);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setRefreshing(true);
        refresh();

        setHasOptionsMenu(true);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, new IntentFilter(PublishIntentService.BIRTHDAY_INTENT));
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void refresh() {
        // do not start another refresh if the current one is running
        if (swipeLayout.isWaitingResult()) {
            return;
        }
        Calendar now = Calendar.getInstance(Locale.US);
        PublishIntentService.startBirthdayListIntent(getActivity(), now);
        swipeLayout.setRefreshingAndWaintingResult(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.birtdays_publisher, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_refresh:
            refresh();
            return true;
        case R.id.action_selectall:
            selectAll();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void selectAll() {
        if (actionMode == null) {
            actionMode = getActivity().startActionMode(this);
        }
        gridViewPhotoAdapter.selectAll();
        updateSubTitle();
    }

    private void publish(final ActionMode mode, final boolean saveAsDraft) {
        new AbsProgressIndicatorAsyncTask<Void, String, List<TumblrPhotoPost>>(getActivity(), "") {
            @Override
            protected void onProgressUpdate(String... values) {
                setProgressMessage(values[0]);
            }

            @Override
            protected List<TumblrPhotoPost> doInBackground(Void... voidParams) {
                try {
                    InputStream is = getContext().getAssets().open("cake.png");
                    Bitmap cakeImage = BitmapFactory.decodeStream(is);
                    is.close();

                    for (Pair<Birthday, TumblrPhotoPost> pair : gridViewPhotoAdapter.getSelectedItems()) {
                        final TumblrPhotoPost post = pair.second;
                        String name = post.getTags().get(0);
                        publishProgress(getContext().getString(R.string.sending_cake_title, name));
                        BirthdayUtils.createBirthdayPost(getContext(), cakeImage, post, getBlogName(), saveAsDraft);
                    }
                } catch (Exception e) {
                    setError(e);
                }
                return null;
            }
            
            protected void onPostExecute(List<TumblrPhotoPost> result) {
                super.onPostExecute(null);
                if (!hasError()) {
                    mode.finish();
                }
            }
        }.execute();
    }

    @Override
    public void onItemClick(int position) {
        if (actionMode == null) {
            onThumbnailImageClick(position);
        } else {
            updateSelection(position);
        }
    }

    @Override
    public void onTagClick(int position) {
    }

    @Override
    public void onItemLongClick(int position) {
        if (actionMode == null) {
            actionMode = getActivity().startActionMode(this);
        }
        gridViewPhotoAdapter.getSelection().toggle(position);
    }

    private void updateSelection(int position) {
        Selection selection = gridViewPhotoAdapter.getSelection();
        selection.toggle(position);
        if (selection.getItemCount() == 0) {
            actionMode.finish();
        } else {
            updateSubTitle();
        }
    }

    private void updateSubTitle() {
        Selection selection = gridViewPhotoAdapter.getSelection();
        int selectionCount = selection.getItemCount();
        actionMode.setSubtitle(getResources().getQuantityString(
                R.plurals.selected_items_total,
                selectionCount,
                selectionCount,
                gridViewPhotoAdapter.getItemCount()));
    }

    @Override
    public void onThumbnailImageClick(int position) {
        TumblrPhotoPost post = gridViewPhotoAdapter.getItem(position).second;
        TagPhotoBrowserActivity.startPhotoBrowserActivityForResult(this, getBlogName(),
                post.getTags().get(0),
                PICK_IMAGE_REQUEST_CODE,
                false);
    }

    @Override
    public void onOverflowClick(View view, int position) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE) {
            TumblrPhotoPost post = (TumblrPhotoPost) data.getSerializableExtra(Constants.EXTRA_POST);
            gridViewPhotoAdapter.updatePostByTag(post, true);
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(getString(R.string.select_images));
        mode.setSubtitle(getResources().getQuantityString(
                R.plurals.selected_items_total,
                1,
                1,
                gridViewPhotoAdapter.getItemCount()));
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.birtdays_publisher_context, menu);
        gridViewPhotoAdapter.setShowButtons(true);
        gridViewPhotoAdapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.actionMode = null;
        gridViewPhotoAdapter.setShowButtons(false);
        gridViewPhotoAdapter.getSelection().clear();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_publish:
            publish(mode, false);
            return true;
        case R.id.action_draft:
            publish(mode, true);
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onRefresh() {
        refresh();
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (PublishIntentService.BIRTHDAY_INTENT.equals(action)) {
                @SuppressWarnings("unchecked") List<Pair<Birthday, TumblrPhotoPost>> posts = (List<Pair<Birthday, TumblrPhotoPost>>) intent
                        .getSerializableExtra(PublishIntentService.RESULT_LIST1);

                swipeLayout.setRefreshingAndWaintingResult(false);
                gridViewPhotoAdapter.clear();
                gridViewPhotoAdapter.addAll(posts);
                gridViewPhotoAdapter.notifyDataSetChanged();
            }
        }
    };
}
