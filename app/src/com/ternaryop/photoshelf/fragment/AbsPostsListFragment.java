package com.ternaryop.photoshelf.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ternaryop.photoshelf.Constants;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.ImageViewerActivity;
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity;
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClick;
import com.ternaryop.photoshelf.adapter.PhotoAdapter;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrAltSize;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.AbsProgressBarAsyncTask;
import com.ternaryop.utils.DialogUtils;

public abstract class AbsPostsListFragment extends AbsPhotoShelfFragment implements OnScrollListener, OnItemClickListener, MultiChoiceModeListener, OnPhotoBrowseClick {
    protected enum POST_ACTION {
        PUBLISH,
        DELETE
    };

    private static final String LOADER_PREFIX_POSTS_THUMB = "postsThumb";

    protected PhotoAdapter photoAdapter;
    protected int offset;
    protected boolean hasMorePosts;
    protected boolean isScrolling;
    protected long totalPosts;
    protected ListView photoListView;

    private int[] singleSelectionMenuIds;

    private CharSequence subTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_photo_list, container, false);
        
        photoAdapter = new PhotoAdapter(getActivity(), LOADER_PREFIX_POSTS_THUMB);

        photoListView = (ListView)rootView.findViewById(R.id.list);
        photoListView.setOnItemClickListener(this);
        photoListView.setOnScrollListener(this);
        photoListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        photoListView.setMultiChoiceModeListener(this);
        photoListView.setAdapter(photoAdapter);

        setHasOptionsMenu(true);
        
        return rootView;
    }

    public void onPrepareOptionsMenu(Menu menu) {
        if (fragmentActivityStatus.isDrawerOpen()) {
            subTitle = getActivity().getActionBar().getSubtitle();
            getActivity().getActionBar().setSubtitle(null);
        } else {
            getActivity().getActionBar().setSubtitle(subTitle);
        }
        super.onPrepareOptionsMenu(menu);
    }
    
    protected abstract void readPhotoPosts();
    
    protected abstract int getActionModeMenuId();

    protected void saveAsDraft(final ActionMode mode) {
        final List<PhotoShelfPost> selectedPosts = getSelectedPosts();
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    new ActionExecutor(getActivity(), R.string.saving_posts_to_draft_title, mode) {
                        @Override
                        protected void executeAction(PhotoShelfPost post) {
                            Tumblr.getSharedTumblr(getContext()).saveDraft(
                                    getBlogName(),
                                    post.getPostId());
                        }
                    }.execute();
                    break;
                }
            }
        };
        
        String message = getResources().getQuantityString(R.plurals.save_to_draft_confirm,
                selectedPosts.size(),
                selectedPosts.size(),
                selectedPosts.get(0).getFirstTag());
        new AlertDialog.Builder(getActivity())
        .setMessage(message)
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, dialogClickListener)
        .show();        
    }

    private void deletePost(final ActionMode mode) {
        new ActionExecutor(getActivity(), R.string.deleting_posts_title, mode) {
            @Override
            protected void executeAction(PhotoShelfPost post) {
                Tumblr.getSharedTumblr(getContext()).deletePost(getBlogName(),
                        post.getPostId());
            }
        }.execute();
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        boolean loadMore = totalItemCount > 0 &&
                (firstVisibleItem + visibleItemCount >= totalItemCount);

        if (loadMore && hasMorePosts && !isScrolling) {
            offset += Tumblr.MAX_POST_PER_REQUEST;
            readPhotoPosts();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        PhotoShelfPost item = (PhotoShelfPost) parent.getItemAtPosition(position);
        if (getActivity().getCallingActivity() == null) {
            ImageViewerActivity.startImageViewer(getActivity(),
                    item.getFirstPhotoAltSize().get(0).getUrl(),
                    item);
        } else {
            finish(item);
        }
    }

    public void finish(TumblrPhotoPost post) {
        Intent data = new Intent();
        data.putExtra(Constants.EXTRA_POST, post);
        // Activity finished ok, return the data
        getActivity().setResult(Activity.RESULT_OK, data);
        getActivity().finish();
    }
    
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.select_posts);
        mode.setSubtitle(getResources().getQuantityString(R.plurals.selected_items, 1, 1));
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(getActionModeMenuId(), menu);
        return true;
    }

    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    protected List<PhotoShelfPost> getSelectedPosts() {
        SparseBooleanArray checkedItemPositions = photoListView.getCheckedItemPositions();
        ArrayList<PhotoShelfPost> list = new ArrayList<PhotoShelfPost>();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            int key = checkedItemPositions.keyAt(i);
            if (checkedItemPositions.get(key)) {
                list.add(photoAdapter.getItem(key));
            }
        }
        return list;
    }
    
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        PhotoShelfPost post = getSelectedPosts().get(0);
        
        switch (item.getItemId()) {
        case R.id.post_publish:
            showConfirmDialog(POST_ACTION.PUBLISH, mode);
            return true;
        case R.id.group_menu_image_dimension:
            browseImageBySize(post);
            return true;
        case R.id.post_delete:
            showConfirmDialog(POST_ACTION.DELETE, mode);
            return true;
        case R.id.post_edit:
            showEditDialog(post, mode);
            return true;
        case R.id.post_save_draft:
            saveAsDraft(mode);
            return true;
        default:
            return false;
        }
    }

    public void onDestroyActionMode(ActionMode mode) {
    }

    public void onItemCheckedStateChanged(ActionMode mode, int position,
            long id, boolean checked) {
        int selectCount = photoListView.getCheckedItemCount();
        boolean singleSelection = selectCount == 1;

        for (int itemId : getSingleSelectionMenuIds()) {
            MenuItem item = mode.getMenu().findItem(itemId);
            if (item != null) {
                item.setVisible(singleSelection);
            }
        }

        mode.setSubtitle(getResources().getQuantityString(
                R.plurals.selected_items,
                selectCount,
                selectCount));
    }

    protected int[] getSingleSelectionMenuIds() {
        if (singleSelectionMenuIds == null) {
            singleSelectionMenuIds = new int[] {R.id.post_schedule, R.id.post_edit, R.id.group_menu_image_dimension};
        }
        return singleSelectionMenuIds;
    }

    public void browseImageBySize(final PhotoShelfPost post) {
        final ArrayAdapter<TumblrAltSize> arrayAdapter = new ArrayAdapter<TumblrAltSize>(
                getActivity(),
                android.R.layout.select_dialog_item,
                post.getFirstPhotoAltSize());

        // Show the cancel button without setting a listener
        // because it isn't necessary
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
        .setTitle(getString(R.string.menu_header_show_image, post.getFirstTag()))
        .setNegativeButton(android.R.string.cancel, null);

        builder.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String url = arrayAdapter.getItem(which).getUrl();
                        ImageViewerActivity.startImageViewer(getActivity(), url, post);
                    }
                });
        builder.show();
    }

    private void showConfirmDialog(final POST_ACTION postAction, final ActionMode mode) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    switch (postAction) {
                    case PUBLISH:
                        publishPost(mode);
                        break;
                    case DELETE:
                        deletePost(mode);
                        break;
                    }
                    break;
                }
            }
        };

        List<PhotoShelfPost> postsList = getSelectedPosts();
        String message = null;
        switch (postAction) {
        case PUBLISH:
            message = getResources().getQuantityString(R.plurals.publish_post_confirm,
                    postsList.size(),
                    postsList.size(),
                    postsList.get(0).getFirstTag());
            break;
        case DELETE:
            message = getResources().getQuantityString(R.plurals.delete_post_confirm,
                    postsList.size(),
                    postsList.size(),
                    postsList.get(0).getFirstTag());
            break;
        }
        
        new AlertDialog.Builder(getActivity())
        .setMessage(message)
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, dialogClickListener)
        .show();        
    }
    
    private void publishPost(ActionMode mode) {
        new ActionExecutor(getActivity(), R.string.publishing_posts_title, mode) {
            @Override
            protected void executeAction(PhotoShelfPost post) {
                Tumblr.getSharedTumblr(getContext()).publishPost(getBlogName(),
                        post.getPostId());
            }
        }.execute();
    }

    abstract class ActionExecutor extends AbsProgressBarAsyncTask<Void, PhotoShelfPost, List<PhotoShelfPost>> {
        private ActionMode mode;

        public ActionExecutor(Context context, int resId, ActionMode mode) {
            super(context, context.getString(resId));
            this.mode = mode;
        }

        @Override
        protected void onProgressUpdate(PhotoShelfPost... values) {
            PhotoShelfPost post = values[0];
            photoAdapter.remove(post);
            getProgressDialog().setMessage(post.getTagsAsString());
        }
        
        @Override
        protected void onPostExecute(List<PhotoShelfPost> notDeletedPosts) {
            super.onPostExecute(null);
            
            refreshUI();
            // all posts have been deleted so call actionMode.finish() 
            if (notDeletedPosts.size() == 0) {
                mode.finish();
                return;
            }
            // leave posts not processed checked
            photoListView.clearChoices();
            for (PhotoShelfPost post : notDeletedPosts) {
                int position = photoAdapter.getPosition(post);
                photoListView.setItemChecked(position, true);
            }
            DialogUtils.showSimpleMessageDialog(getContext(),
                    R.string.generic_error,
                    getContext().getResources().getQuantityString(
                            R.plurals.general_posts_error,
                            notDeletedPosts.size(),
                            notDeletedPosts.size()));
        }

        @Override
        protected List<PhotoShelfPost> doInBackground(Void... voidParams) {
            List<PhotoShelfPost> notDeletedPosts = new ArrayList<PhotoShelfPost>();

            for (final PhotoShelfPost post : getSelectedPosts()) {
                try {
                    executeAction(post);
                    this.publishProgress(post);
                } catch (Exception e) {
                    notDeletedPosts.add(post);
                }
            }
            return notDeletedPosts;
        }
        
        protected abstract void executeAction(PhotoShelfPost post);
    };
    
    @Override
    public void onPhotoBrowseClick(PhotoShelfPost post) {
        TagPhotoBrowserActivity.startPhotoBrowserActivity(getActivity(), getBlogName(), post.getFirstTag(), false);
    }

    public void onThumbnailImageClick(PhotoShelfPost post) {
        ImageViewerActivity.startImageViewer(getActivity(), post.getFirstPhotoAltSize().get(0).getUrl(), post);
    }
}
