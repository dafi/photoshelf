package com.ternaryop.phototumblrshare.activity;

import com.ternaryop.phototumblrshare.AppSupport;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

@SuppressLint("Registered")
public abstract class PhotoTumblrActivity extends Activity {
	protected AppSupport appSupport;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    getActionBar().setDisplayHomeAsUpEnabled(true);
	    appSupport = new AppSupport(this);
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

}
