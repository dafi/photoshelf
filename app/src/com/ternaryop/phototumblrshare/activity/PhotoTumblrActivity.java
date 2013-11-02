package com.ternaryop.phototumblrshare.activity;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;

import com.fedorvlasov.lazylist.ImageLoader;
import com.fedorvlasov.lazylist.ImageLoader.ImageLoaderCallback;
import com.ternaryop.phototumblrshare.AppSupport;
import com.ternaryop.tumblr.Blog;

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
}
