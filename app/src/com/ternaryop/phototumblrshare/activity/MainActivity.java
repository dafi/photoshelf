package com.ternaryop.phototumblrshare.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.ternaryop.phototumblrshare.AppSupport;
import com.ternaryop.phototumblrshare.AppSupport.AppSupportCallback;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.tumblr.AuthenticationCallback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;

public class MainActivity extends PhotoTumblrActivity implements OnClickListener {

	private AppSupport appSupport;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		boolean enabled = Tumblr.isLogged(this);
		enableUI(enabled);
		
        appSupport = new AppSupport(this);

        // if we are returning from authentication then enable the UI
	    Tumblr.handleOpenURI(this, getIntent().getData(), new AuthenticationCallback() {
			@Override
			public void authenticated(final String token, final String tokenSecret, final Exception error) {
				if (error == null) {
					Toast.makeText(getApplicationContext(),
							getResources().getString(R.string.authentication_success_title),
							Toast.LENGTH_LONG)
							.show();
					// after authentication cache blog names
					appSupport.fetchBlogNames(MainActivity.this, new AppSupportCallback() {
						@Override
						public void onComplete(AppSupport appSupport, Exception error) {
							enableUI(token != null && tokenSecret != null);
						}
					});
				} else {
					DialogUtils.showErrorDialog(MainActivity.this, error);
				}
			}
		});
	}

	private void enableUI(boolean enabled) {
		for (int buttonId : new int[] {
				R.id.draft_button,
				R.id.scheduled_button,
				R.id.test_page_button,
				R.id.tumblr_login_button}) {
			Button button = (Button)findViewById(buttonId);
			button.setOnClickListener(this);
			// tumblr login button is always enabled
			if (buttonId != R.id.tumblr_login_button) {
				button.setEnabled(enabled);
			}
		}
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
			Tumblr.login(this);
			break;
		}
	}
}
