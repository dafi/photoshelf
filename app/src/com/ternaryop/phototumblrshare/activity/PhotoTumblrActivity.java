package com.ternaryop.phototumblrshare.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;

import com.fedorvlasov.lazylist.ImageLoader;
import com.fedorvlasov.lazylist.ImageLoader.ImageLoaderCallback;
import com.ternaryop.phototumblrshare.AppSupport;
import com.ternaryop.phototumblrshare.dialogs.TumblrPostDialog;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.TumblrPhotoPost;

public abstract class PhotoTumblrActivity extends Activity {
	private static final String STATUS_BAR_LOGO = "statusBarLogo";
	protected static final String BLOG_NAME = "blogName";
	protected AppSupport appSupport;
	private String blogName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    getActionBar().setDisplayHomeAsUpEnabled(true);

	    appSupport = new AppSupport(this);

	    Bundle bundle = getIntent().getExtras();
	    if (bundle != null) {
			blogName = bundle.getString(BLOG_NAME);
	    }
	}

	protected void setActionBarIcon() {
		new ImageLoader(this, STATUS_BAR_LOGO).displayDrawable(Blog.getAvatarUrlBySize(getBlogName(), 96), new ImageLoaderCallback() {
			@Override
			public void display(Drawable drawable) {
			    getActionBar().setIcon(drawable);
			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        	// clicked the actionbar
	        	// close and return to caller
	        	finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	public String getBlogName() {
		return blogName == null ? appSupport.getSelectedBlogName() : blogName;
	}

	protected void refreshUI() {
		
	}
	
	protected void showEditDialog(final TumblrPhotoPost item) {
		TumblrPostDialog editDialog = new TumblrPostDialog(this, item.getPostId());

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which) {
		        case DialogInterface.BUTTON_POSITIVE:
		        	item.setTags(((TumblrPostDialog)dialog).getPostTags());
		        	item.setCaption(((TumblrPostDialog)dialog).getPostTitle());
		        	refreshUI();
		            break;
		        }
		    }
		};
		editDialog.setPostTitle(item.getCaption());
		editDialog.setPostTags(item.getTags());
		editDialog.setEditButton(dialogClickListener);
		
		editDialog.show();
	}
}
