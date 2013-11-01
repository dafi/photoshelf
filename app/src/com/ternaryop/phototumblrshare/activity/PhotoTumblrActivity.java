package com.ternaryop.phototumblrshare.activity;

import com.ternaryop.phototumblrshare.AppSupport;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

@SuppressLint("Registered")
public abstract class PhotoTumblrActivity extends Activity {
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
