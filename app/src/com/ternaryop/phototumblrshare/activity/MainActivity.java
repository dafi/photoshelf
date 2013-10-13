package com.ternaryop.phototumblrshare.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.ternaryop.phototumblrshare.AppSupport;
import com.ternaryop.phototumblrshare.R;

public class MainActivity extends PhotoTumblrActivity implements OnClickListener {

	private AppSupport appSupport;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		for (int buttonId : new int[] {
				R.id.draft_button,
				R.id.scheduled_button,
				R.id.test_page_button,
				R.id.tumblr_login_button}) {
			((Button)findViewById(buttonId)).setOnClickListener(this);
		}
        appSupport = new AppSupport(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.action_draft_posts:
	        	DraftListActivity.startDraftListActivity(this);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.draft_button:
			DraftListActivity.startDraftListActivity(this);
			break;
		case R.id.scheduled_button:
			ScheduledListActivity.startScheduledListActivity(this, appSupport.getSelectedBlogName());
			break;
		case R.id.test_page_button:
	    	ImagePickerActivity.startImagePicker(this,
	    			getResources().getString(R.string.test_page_url));
			break;
		case R.id.tumblr_login_button:
			new AppSupport(this).fetchBlogNames(this, null);
			break;
		}
	}
}
