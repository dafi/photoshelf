package com.ternaryop.phototumblrshare.activity;

import java.util.Date;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ternaryop.phototumblrshare.AppSupport;
import com.ternaryop.phototumblrshare.AppSupport.AppSupportCallback;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.DBHelper;
import com.ternaryop.tumblr.AuthenticationCallback;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DialogUtils;

public class MainActivity extends PhotoTumblrActivity implements OnClickListener, AuthenticationCallback {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		enableUI(Tumblr.isLogged(this));

		((TextView)findViewById(R.id.version_number)).setText(getVersion());
	}
	
	public String getVersion() {
	    try {
	        String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
	        int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			return String.valueOf("v" + versionName + " build " + versionCode);
	    } catch (Exception e) {
	    }
	    return "N/A";
	}
	
	@Override
	protected void onResume() {
		super.onResume();

        // if we are returning from authentication then enable the UI
	    boolean handled = Tumblr.handleOpenURI(this, getIntent().getData(), this);

	    // show the preference only if we aren't in the middle of URI handling and not already logged in
		if (!Tumblr.isLogged(this) && !handled) {
	    	PhotoPreferencesActivity.startPreferencesActivityForResult(this);
		}
	}

	private void enableUI(boolean enabled) {
		for (int buttonId : new int[] {
				R.id.draft_button,
				R.id.scheduled_button,
				R.id.test_page_button,
				R.id.browse_images_by_tags_button,
				R.id.browse_tags_button,
				R.id.birthdays}) {
			Button button = (Button)findViewById(buttonId);
			button.setOnClickListener(this);
			button.setEnabled(enabled);
		}
		if (DBHelper.getInstance(this).getBirthdayDAO().hasBirthdaysInDate(new Date(), appSupport.getSelectedBlogName())) {
			findViewById(R.id.birthdays).setVisibility(View.VISIBLE);
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
	        case R.id.action_settings:
	        	PhotoPreferencesActivity.startPreferencesActivityForResult(this);
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
			ScheduledListActivity.startScheduledListActivity(this, getBlogName());
			break;
		case R.id.test_page_button:
	    	ImagePickerActivity.startImagePicker(this, getString(R.string.test_page_url));
			break;
		case R.id.browse_images_by_tags_button:
			TagPhotoBrowserActivity.startPhotoBrowserActivity(this, getBlogName(), null);
			break;
		case R.id.browse_tags_button:
			TagListActivity.startTagListActivity(this, getBlogName(), null);
			break;
		case R.id.birthdays:
			BirthdaysActivity.startBirthdaysActivity(this, getBlogName());
			break;
		}
	}

	@Override
	public void tumblrAuthenticated(final String token, final String tokenSecret, final Exception error) {
		if (error == null) {
			Toast.makeText(this,
					getString(R.string.authentication_success_title),
					Toast.LENGTH_LONG)
					.show();
			// after authentication cache blog names
			appSupport.fetchBlogNames(this, new AppSupportCallback() {
				@Override
				public void onComplete(AppSupport appSupport, Exception error) {
					enableUI(token != null && tokenSecret != null);
				}
			});
		} else {
			DialogUtils.showErrorDialog(this, error);
		}
	}
}
