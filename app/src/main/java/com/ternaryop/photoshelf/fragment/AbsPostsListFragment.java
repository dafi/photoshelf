package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.PluralsRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.ImageViewerActivity;
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity;
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice;
import com.ternaryop.photoshelf.adapter.PhotoAdapter;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.photoshelf.adapter.Selection;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.dialogs.TumblrPostDialog;
import com.ternaryop.photoshelf.view.ColorItemDecoration;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrAltSize;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.DialogUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.ternaryop.photoshelf.db.TumblrPostCache.CACHE_TYPE_DRAFT;

public abstract class AbsPostsListFragment extends AbsPhotoShelfFragment implements OnPhotoBrowseClickMultiChoice, SearchView.OnQueryTextListener, ActionMode.Callback {
    protected static final int POST_ACTION_PUBLISH = 1;
    protected static final int POST_ACTION_DELETE = 2;
    protected static final int POST_ACTION_EDIT = 3;
    protected static final int POST_ACTION_SAVE_AS_DRAFT = 4;

    public static final int POST_ACTION_OK = -1;

    private static final String LOADER_PREFIX_POSTS_THUMB = "postsThumb";

    protected PhotoAdapter photoAdapter;
    protected int offset;
    protected boolean hasMorePosts;
    protected boolean isScrolling;
    protected long totalPosts;
    protected RecyclerView recyclerView;
    protected SearchView searchView;

    private int[] singleSelectionMenuIds;

    ActionMode actionMode;
    protected ColorItemDecoration colorItemDecoration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(getPostListViewResource(), container, false);

        photoAdapter = new PhotoAdapter(getActivity(), LOADER_PREFIX_POSTS_THUMB);

        recyclerView = rootView.findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(photoAdapter);
        colorItemDecoration = new ColorItemDecoration();
        recyclerView.addItemDecoration(colorItemDecoration);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItem = ((LinearLayoutManager)layoutManager).findFirstVisibleItemPosition();

                boolean loadMore = totalItemCount > 0 &&
                        (firstVisibleItem + visibleItemCount >= totalItemCount);

                if (loadMore && hasMorePosts && !isScrolling) {
                    offset += Tumblr.MAX_POST_PER_REQUEST;
                    readPhotoPosts();
                }
            }
        });

        setHasOptionsMenu(true);
        return rootView;
    }

    protected int getPostListViewResource() {
        return R.layout.fragment_photo_list;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean isMenuVisible = !fragmentActivityStatus.isDrawerOpen();
        menu.setGroupVisible(R.id.menu_photo_action_bar, isMenuVisible);
        setupSearchView(menu);
        super.onPrepareOptionsMenu(menu);
    }

    protected abstract void readPhotoPosts();

    protected abstract int getActionModeMenuId();

    public void finish(TumblrPhotoPost post) {
        Intent data = new Intent();
        data.putExtra(Constants.EXTRA_POST, post);
        // Activity finished ok, return the data
        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.select_posts);
        mode.setSubtitle(getResources().getQuantityString(R.plurals.selected_items, 1, 1));
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(getActionModeMenuId(), menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return handleMenuItem(item, photoAdapter.getSelectedPosts(), mode);
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.actionMode = null;
        photoAdapter.getSelection().clear();
    }

    public void updateMenuItems() {
        int selectCount = photoAdapter.getSelection().getItemCount();
        boolean singleSelection = selectCount == 1;

        for (int itemId : getSingleSelectionMenuIds()) {
            MenuItem item = actionMode.getMenu().findItem(itemId);
            if (item != null) {
                item.setVisible(singleSelection);
            }
        }
    }

    protected int[] getSingleSelectionMenuIds() {
        if (singleSelectionMenuIds == null) {
            singleSelectionMenuIds = new int[] {R.id.post_schedule, R.id.post_edit, R.id.group_menu_image_dimension, R.id.show_post};
        }
        return singleSelectionMenuIds;
    }

    public void browseImageBySize(final PhotoShelfPost post) {
        final ArrayAdapter<TumblrAltSize> arrayAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.select_dialog_item,
                post.getFirstPhotoAltSize());

        // Show the cancel button without setting a listener
        // because it isn't necessary
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.menu_header_show_image, post.getFirstTag()))
                .setNegativeButton(android.R.string.cancel, null);

        builder.setAdapter(arrayAdapter,
                (dialog, which) -> {
                    final TumblrAltSize item = arrayAdapter.getItem(which);
                    if (item != null) {
                        ImageViewerActivity.startImageViewer(getActivity(), item.getUrl(), post);
                    }
                });
        builder.show();
    }

    public void showPost(final PhotoShelfPost post) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(post.getPostUrl()));
        startActivity(i);
    }

    private void showConfirmDialog(final int postAction, final ActionMode mode, final List<PhotoShelfPost> postsList) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (postAction) {
                case POST_ACTION_PUBLISH:
                    publishPost(mode, postsList);
                    break;
                case POST_ACTION_DELETE:
                    deletePost(mode, postsList);
                    break;
                case POST_ACTION_SAVE_AS_DRAFT:
                    saveAsDraft(mode, postsList);
                    break;
            }
        };

        String message = getResources().getQuantityString(getActionConfirmStringId(postAction),
                postsList.size(),
                postsList.size(),
                postsList.get(0).getFirstTag());
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private @PluralsRes int getActionConfirmStringId(int postAction) {
        switch (postAction) {
            case POST_ACTION_PUBLISH:
                return R.plurals.publish_post_confirm;
            case POST_ACTION_DELETE:
                return R.plurals.delete_post_confirm;
            case POST_ACTION_SAVE_AS_DRAFT:
                return R.plurals.save_to_draft_confirm;
            default:
                throw new AssertionError("Invalid post action");
        }
    }

    private void saveAsDraft(final ActionMode mode, final List<PhotoShelfPost> postList) {
        colorItemDecoration.setColor(ContextCompat.getColor(getActivity(), R.color.photo_item_animation_save_as_draft_bg));
        executePostAction(mode, postList, post -> {
            Tumblr.getSharedTumblr(getActivity()).saveDraft(
                    getBlogName(),
                    post.getPostId());
            DBHelper.getInstance(getActivity()).getTumblrPostCacheDAO().updateItem(post, CACHE_TYPE_DRAFT);
            onPostAction(post, POST_ACTION_SAVE_AS_DRAFT, POST_ACTION_OK);
        });
    }

    private void deletePost(final ActionMode mode, final List<PhotoShelfPost> postList) {
        colorItemDecoration.setColor(ContextCompat.getColor(getActivity(), R.color.photo_item_animation_delete_bg));
        executePostAction(mode, postList, post -> {
            Tumblr.getSharedTumblr(getActivity()).deletePost(getBlogName(), post.getPostId());
            DBHelper.getInstance(getActivity()).getPostDAO().deleteById(post.getPostId());
            onPostAction(post, POST_ACTION_DELETE, POST_ACTION_OK);
        });
    }

    private void publishPost(ActionMode mode, final List<PhotoShelfPost> postList) {
        colorItemDecoration.setColor(ContextCompat.getColor(getActivity(), R.color.photo_item_animation_publish_bg));
        executePostAction(mode, postList, post -> {
            Tumblr.getSharedTumblr(getActivity()).publishPost(getBlogName(), post.getPostId());
            onPostAction(post, POST_ACTION_PUBLISH, POST_ACTION_OK);
        });
    }

    protected void refreshUI() {
        if (searchView != null && searchView.isIconified()) {
            if (hasMorePosts) {
                getSupportActionBar().setSubtitle(getString(R.string.post_count_1_of_x,
                        photoAdapter.getItemCount(),
                        totalPosts));
            } else {
                getSupportActionBar().setSubtitle(getResources().getQuantityString(
                        R.plurals.posts_count,
                        photoAdapter.getItemCount(),
                        photoAdapter.getItemCount()));
                photoAdapter.notifyCountChanged();
            }
        }

        // use post() to resolve the following error:
        // Cannot call this method in a scroll callback. Scroll callbacks might be run during a measure & layout pass where you cannot change theRecyclerView data.
        // Any method call that might change the structure of the RecyclerView or the adapter contents should be postponed to the next frame.
        recyclerView.post(() -> {
            // notifyDataSetChanged() can 'hide' the remove item animation started by notifyItemRemoved()
            // so we wait for finished animations before call it
            recyclerView.getItemAnimator().isRunning(() -> photoAdapter.notifyDataSetChanged());
        });
    }

    private void updateUIAfterPostAction(final ActionMode mode, final List<Pair<PhotoShelfPost, Throwable>> postsWithError) {
        refreshUI();
        // all posts have been deleted so call actionMode.finish()
        if (postsWithError.isEmpty()) {
            if (mode != null) {
                // when action mode is on the finish() method could be called while the item animation is running stopping it
                // so we wait the animation is completed and then call finish()
                recyclerView.post(() -> recyclerView.getItemAnimator().isRunning(mode::finish));
            }
            return;
        }
        // leave posts not processed checked
        photoAdapter.getSelection().clear();
        for (Pair<PhotoShelfPost, Throwable> pair : postsWithError) {
            int position = photoAdapter.getPosition(pair.first);
            photoAdapter.getSelection().setSelected(position, true);
        }
        DialogUtils.showSimpleMessageDialog(getActivity(),
                R.string.generic_error,
                getActivity().getResources().getQuantityString(
                        R.plurals.general_posts_error,
                        postsWithError.size(),
                        postsWithError.get(postsWithError.size() - 1).second.getMessage(),
                        postsWithError.size()));

    }

    private void executePostAction(final ActionMode mode, final List<PhotoShelfPost> postList, final Consumer<PhotoShelfPost> consumer) {
        compositeDisposable.add(Observable
                .fromIterable(postList)
                .flatMap(post -> {
                    try {
                        consumer.accept(post);
                        return Observable.just(Pair.create(post, (Throwable)null));
                    } catch (Throwable e) {
                        return Observable.just(Pair.create(post, e));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(pair -> {
                    if (pair.second == null) {
                        photoAdapter.remove(pair.first);
                    }
                })
                .filter(pair -> pair.second != null)
                .toList()
                .subscribe(postsWithError -> updateUIAfterPostAction(mode, postsWithError)));
    }

    @Override
    public void onTagClick(int position, String tag) {
        TagPhotoBrowserActivity.startPhotoBrowserActivity(getActivity(), getBlogName(), tag, false);
    }

    @Override
    public void onThumbnailImageClick(int position) {
        final PhotoShelfPost post = photoAdapter.getItem(position);
        ImageViewerActivity.startImageViewer(getActivity(), post.getFirstPhotoAltSize().get(0).getUrl(), post);
    }

    @Override
    public void onOverflowClick(int position, View view) {
        final PhotoShelfPost post = photoAdapter.getItem(position);
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(getActionModeMenuId(), popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            ArrayList<PhotoShelfPost> postList = new ArrayList<>();
            postList.add(post);
            return handleMenuItem(item, postList, null);
        });
        popupMenu.show();
    }

    @Override
    public void onItemClick(int position) {
        if (actionMode == null) {
            handleClickedThumbnail(position);
        } else {
            updateSelection(position);
        }
    }

    @Override
    public void onItemLongClick(int position) {
        if (actionMode == null) {
            actionMode = getActivity().startActionMode(this);
        }
        updateSelection(position);
    }

    private void handleClickedThumbnail(int position) {
        final PhotoShelfPost post = photoAdapter.getItem(position);
        if (getActivity().getCallingActivity() == null) {
            onThumbnailImageClick(position);
        } else {
            finish(post);
        }
    }

    private void updateSelection(int position) {
        Selection selection = photoAdapter.getSelection();
        selection.toggle(position);
        if (selection.getItemCount() == 0) {
            actionMode.finish();
        } else {
            updateMenuItems();
            int selectionCount = selection.getItemCount();
            actionMode.setSubtitle(getResources().getQuantityString(
                    R.plurals.selected_items,
                    selectionCount,
                    selectionCount));
        }
    }

    protected boolean handleMenuItem(MenuItem item, List<PhotoShelfPost> postList, ActionMode mode) {
        switch (item.getItemId()) {
            case R.id.post_publish:
                showConfirmDialog(POST_ACTION_PUBLISH, mode, postList);
                return true;
            case R.id.group_menu_image_dimension:
                browseImageBySize(postList.get(0));
                return true;
            case R.id.post_delete:
                showConfirmDialog(POST_ACTION_DELETE, mode, postList);
                return true;
            case R.id.post_edit:
                showEditDialog(postList.get(0), mode);
                return true;
            case R.id.post_save_draft:
                showConfirmDialog(POST_ACTION_SAVE_AS_DRAFT, mode, postList);
                return true;
            case R.id.show_post:
                showPost(postList.get(0));
                return true;
            default:
                return false;
        }
    }

    protected SearchView setupSearchView(Menu menu) {
        MenuItem searchMenu = menu.findItem(R.id.action_search);
        if (searchMenu != null) {
            searchView = (SearchView) searchMenu.getActionView();
            searchView.setQueryHint(getString(R.string.enter_tag_hint));
            searchView.setOnQueryTextListener(this);
        }
        return searchView;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        photoAdapter.getFilter().filter(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    protected void resetAndReloadPhotoPosts() {
        offset = 0;
        totalPosts = 0;
        hasMorePosts = true;
        photoAdapter.clear();
        readPhotoPosts();
    }

    /**
     * Overridden (if necessary) by subclasses to be informed about post action result,
     * the default implementation does nothing
     * @param post the post processed by action
     * @param postAction the action executed
     * @param resultCode on success POST_ACTION_OK
     */
    public void onPostAction(@SuppressWarnings("UnusedParameters") TumblrPhotoPost post, @SuppressWarnings("UnusedParameters") int postAction, @SuppressWarnings({"SameParameterValue", "UnusedParameters"}) int resultCode) {
    }

    @Override
    public void onEditDone(final TumblrPostDialog dialog, final TumblrPhotoPost photoPost, final Completable completable) {
        completable
                .doOnSubscribe(d -> compositeDisposable.add(d))
                .subscribe(() -> {
                    AbsPostsListFragment.super.onEditDone(dialog, photoPost, completable);
                    onPostAction(photoPost, POST_ACTION_EDIT, POST_ACTION_OK);
                }, t -> DialogUtils.showErrorDialog(getActivity(), t));
    }
}
