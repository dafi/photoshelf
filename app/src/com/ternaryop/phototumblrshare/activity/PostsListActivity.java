package com.ternaryop.phototumblrshare.activity;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.list.OnPhotoBrowseClick;
import com.ternaryop.phototumblrshare.list.PhotoAdapter;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.AbsCallback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrAltSize;

public abstract class PostsListActivity extends PhotoTumblrActivity implements OnScrollListener, OnItemClickListener, MultiChoiceModeListener, OnPhotoBrowseClick {
	protected enum POST_ACTION {
		PUBLISH,
		DELETE
	};

	protected static final String LOADER_PREFIX_POSTS_THUMB = "postsThumb";

	protected PhotoAdapter photoAdapter;
	protected int offset;
	protected boolean hasMorePosts;
	protected boolean isScrolling;
	protected long totalPosts;
	protected ListView photoListView;

	private int[] singleSelectionMenuIds;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);
	    setActionBarIcon();
        
        photoAdapter = new PhotoAdapter(this, LOADER_PREFIX_POSTS_THUMB);

        photoListView = (ListView)findViewById(R.id.list);
        photoListView.setAdapter(photoAdapter);
        photoListView.setOnItemClickListener(this);
        photoListView.setOnScrollListener(this);
        photoListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        photoListView.setMultiChoiceModeListener(this);
	}

	protected abstract void readPhotoPosts();
	
    protected abstract int getActionModeMenuId();

	protected void saveAsDraft(final ActionMode mode) {
		final List<PhotoSharePost> selectedPosts = getSelectedPosts();
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which) {
		        case DialogInterface.BUTTON_POSITIVE:
		        	RefreshCallback refreshCallback = new RefreshCallback(selectedPosts);
		        	for (final PhotoSharePost post : selectedPosts) {
		        		refreshCallback.post = post;
			    		Tumblr.getSharedTumblr(PostsListActivity.this).saveDraft(
			    				getBlogName(),
			    				post.getPostId(),
			    				refreshCallback);
					}
		    		refreshCallback.finish(mode);
		            break;
		        }
		    }
		};
		
		String message;
		if (selectedPosts.size() == 1) {
			message = getString(R.string.save_to_draft_singular_confirm, selectedPosts.get(0).getFirstTag());
		} else {
			message = getString(R.string.save_to_draft_plural_confirm, selectedPosts.size());
		}
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setPositiveButton(android.R.string.yes, dialogClickListener)
	    .setNegativeButton(android.R.string.no, dialogClickListener)
	    .show();		
	}

	private void deletePost(ActionMode mode) {
		final List<PhotoSharePost> selectedPosts = getSelectedPosts();
    	RefreshCallback refreshCallback = new RefreshCallback(selectedPosts);

		for (final PhotoSharePost post : selectedPosts) {
			refreshCallback.post = post;
			Tumblr.getSharedTumblr(this).deletePost(getBlogName(),
					post.getPostId(),
					refreshCallback);
		}
		refreshCallback.finish(mode);
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
		PhotoSharePost item = (PhotoSharePost) parent.getItemAtPosition(position);
		ImageViewerActivity.startImageViewer(this, item.getFirstPhotoAltSize().get(0).getUrl(), item.getCaption());
	}

	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(R.string.select_posts);
        mode.setSubtitle(getString(R.string.selected_singular, 1));
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(getActionModeMenuId(), menu);
        return true;
    }

	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

	protected List<PhotoSharePost> getSelectedPosts() {
    	SparseBooleanArray checkedItemPositions = photoListView.getCheckedItemPositions();
    	ArrayList<PhotoSharePost> list = new ArrayList<PhotoSharePost>();
    	for (int i = 0; i < checkedItemPositions.size(); i++) {
    		list.add(photoAdapter.getItem(checkedItemPositions.keyAt(i)));
    	}
    	return list;
	}
    
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		SparseBooleanArray checkedItemPositions = photoListView.getCheckedItemPositions();
		int firstPosition = checkedItemPositions.keyAt(0);
		
		switch (item.getItemId()) {
		case R.id.post_publish:
			showConfirmDialog(POST_ACTION.PUBLISH, mode);
			return true;
		case R.id.group_menu_image_dimension:
			browseImageBySize(photoAdapter.getItem(firstPosition));
			return true;
		case R.id.post_delete:
			showConfirmDialog(POST_ACTION.DELETE, mode);
			return true;
		case R.id.post_edit:
			showEditDialog(photoAdapter.getItem(firstPosition), mode);
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
        
        if (singleSelection) {
            mode.setSubtitle(getString(R.string.selected_singular, 1));
        } else {
            mode.setSubtitle(getString(R.string.selected_plural, selectCount));
        }
    }

    protected int[] getSingleSelectionMenuIds() {
    	if (singleSelectionMenuIds == null) {
    		singleSelectionMenuIds = new int[] {R.id.post_schedule, R.id.post_edit, R.id.group_menu_image_dimension};
    	}
		return singleSelectionMenuIds;
	}

	public void browseImageBySize(final PhotoSharePost post) {
        final ArrayAdapter<TumblrAltSize> arrayAdapter = new ArrayAdapter<TumblrAltSize>(
                this,
                android.R.layout.select_dialog_item,
                post.getFirstPhotoAltSize());

    	AlertDialog.Builder builder = new AlertDialog.Builder(this)
		.setTitle(getString(R.string.menu_header_show_image, post.getFirstTag()))
		.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

        builder.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    	String url = arrayAdapter.getItem(which).getUrl();
                		ImageViewerActivity.startImageViewer(PostsListActivity.this, url, post.getCaption());
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

		List<PhotoSharePost> postsList = getSelectedPosts();
		String message = null;
    	switch (postAction) {
		case PUBLISH:
			if (postsList.size() == 1) {
				message = getString(R.string.publish_post_singular_confirm, postsList.get(0).getFirstTag());
			} else {
				message = getString(R.string.publish_post_plural_confirm, postsList.size());
			}
			break;
		case DELETE:
			if (postsList.size() == 1) {
				message = getString(R.string.delete_post_singular_confirm, postsList.get(0).getFirstTag());
			} else {
				message = getString(R.string.delete_post_plural_confirm, postsList.size());
			}
			break;
		}
		
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setPositiveButton(android.R.string.yes, dialogClickListener)
	    .setNegativeButton(android.R.string.no, dialogClickListener)
	    .show();		
	}
	
	private void publishPost(ActionMode mode) {
		final List<PhotoSharePost> selectedPosts = getSelectedPosts();
		RefreshCallback refreshCallback = new RefreshCallback(selectedPosts);
		for (final PhotoSharePost post : selectedPosts) {
			refreshCallback.post = post;
			Tumblr.getSharedTumblr(this).publishPost(getBlogName(),
					post.getPostId(),
					refreshCallback);
		}
		refreshCallback.finish(mode);
	}
	
	class RefreshCallback extends AbsCallback {
		PhotoSharePost post;
		private ArrayList<PhotoSharePost> remainingPosts;

		public RefreshCallback(List<PhotoSharePost> selectedPosts) {
			super(PostsListActivity.this, R.string.parsing_error);
		}

		@Override
		public void failure(Exception e) {
			super.failure(e);
			if (post != null) {
				if (remainingPosts == null) {
					remainingPosts = new ArrayList<PhotoSharePost>();
				}
				remainingPosts.add(post);
			}
		}
		
		@Override
		public void complete(JSONObject result) {
			if (post != null) {
				photoAdapter.remove(post);
			}
			post = null;
		}
		
		public void finish(ActionMode mode) {
			refreshUI();
			
			// all posts are processed so call actionMode.finish() 
			if (remainingPosts == null) {
				mode.finish();
				return;
			}
			// leave posts not processed checked
			for (int i = 0; i < photoAdapter.getCount(); i++) {
				photoListView.setItemChecked(i, false);
			}
			for (PhotoSharePost post : remainingPosts) {
				int position = photoAdapter.getPosition(post);
				photoListView.setItemChecked(position, true);
			}
		}
	}

	@Override
	public void onPhotoBrowseClick(PhotoSharePost post) {
		TagPhotoBrowserActivity.startPhotoBrowserActivity(this, getBlogName(), post.getFirstTag());
	}

	public void onThumbnailImageClick(PhotoSharePost post) {
		ImageViewerActivity.startImageViewer(this, post.getFirstPhotoAltSize().get(0).getUrl(), post.getCaption());
	}
}
