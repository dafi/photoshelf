package com.ternaryop.photoshelf.fragment;

import java.io.InputStream;
import java.util.ArrayList;
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
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity;
import com.ternaryop.photoshelf.adapter.GridViewPhotoAdapter;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.photoshelf.service.PublishIntentService;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.AbsProgressIndicatorAsyncTask;
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout;

public class BirthdaysPublisherFragment extends AbsPhotoShelfFragment implements GridView.MultiChoiceModeListener, OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    private static final int PICK_IMAGE_REQUEST_CODE = 100;
    private static final String LOADER_PREFIX = "mediumThumb";

    private GridView gridView;
    private GridViewPhotoAdapter gridViewPhotoAdapter;
    private WaitingResultSwipeRefreshLayout swipeLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_birthdays_publisher, container, false);

        gridViewPhotoAdapter = new GridViewPhotoAdapter(getActivity(), LOADER_PREFIX);
        
        gridView = (GridView)rootView.findViewById(R.id.gridview);
        gridView.setAdapter(gridViewPhotoAdapter);
        gridView.setOnItemClickListener(this);
        gridView.setMultiChoiceModeListener(this);

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
        swipeLayout.setWaitingResult(true);
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
            selectAll(true);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void selectAll(boolean select) {
        if (select) {
            for (int i = 0; i < gridView.getCount(); i++) {
                gridView.setItemChecked(i, select);
            }
        } else {
            gridView.clearChoices();
        }
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

                    for (TumblrPhotoPost post : getCheckedPosts()) {
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

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        TumblrPhotoPost post = gridViewPhotoAdapter.getItem(position).second;
        TagPhotoBrowserActivity.startPhotoBrowserActivityForResult(this, getBlogName(),
                post.getTags().get(0),
                PICK_IMAGE_REQUEST_CODE,
                false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST_CODE) {
            TumblrPhotoPost post = (TumblrPhotoPost) data.getSerializableExtra(Constants.EXTRA_POST);
            gridViewPhotoAdapter.updatePostByTag(post, true);
        }
    }

    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.select_images);
        mode.setSubtitle(getResources().getQuantityString(R.plurals.selected_items, 0, 0));
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.birtdays_publisher_action, menu);
        return true;
    }
    
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

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

    private List<TumblrPhotoPost> getCheckedPosts() {
        SparseBooleanArray checkedItemPositions = gridView.getCheckedItemPositions();
        ArrayList<TumblrPhotoPost> list = new ArrayList<TumblrPhotoPost>();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            int key = checkedItemPositions.keyAt(i);
            if (checkedItemPositions.get(key)) {
                list.add(gridViewPhotoAdapter.getItem(key).second);
            }
        }
        return list;
    }
    
    public void onDestroyActionMode(ActionMode mode) {
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position,
            long id, boolean checked) {
        int selectCount = gridView.getCheckedItemCount();
        mode.setSubtitle(getResources().getQuantityString(R.plurals.selected_items, 1, selectCount));
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
