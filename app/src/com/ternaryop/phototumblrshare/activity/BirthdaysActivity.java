package com.ternaryop.phototumblrshare.activity;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.Birthday;
import com.ternaryop.phototumblrshare.db.BirthdayDAO;
import com.ternaryop.phototumblrshare.db.DBHelper;

public class BirthdaysActivity extends PhotoTumblrActivity implements ActionBar.OnNavigationListener {
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_birthdays_main);

		setupActionBar();
		
		BirthdayDAO birthdayDatabaseHelper = DBHelper
				.getInstance(this)
				.getBirthdayDAO();
		List<Birthday> list = birthdayDatabaseHelper.getBirthdayByDate(new Date());
		fillList(list);
	}

	private void fillList(List<Birthday> list) {
		StringBuilder sb = new StringBuilder();
		for (Birthday birthday : list) {
			sb.append(birthday + "\n");
		}
		TextView textView = (TextView)findViewById(R.id.text);
        textView.setText(sb);
        // allow textView to scroll
		textView.setMovementMethod(new ScrollingMovementMethod());
	}

	private void setupActionBar() {
		ActionBar actionBar = getActionBar();
		ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(
				actionBar.getThemedContext(),
				android.R.layout.simple_spinner_item,
				new DateFormatSymbols().getMonths());
		monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setListNavigationCallbacks(monthAdapter, this);
		actionBar.setSelectedNavigationItem(Calendar.getInstance().get(Calendar.MONTH));
	}

	public static void startBirthdaysActivity(Context context, String blogName) {
		Intent intent = new Intent(context, BirthdaysActivity.class);
		Bundle bundle = new Bundle();

		bundle.putString(BLOG_NAME, blogName);
		intent.putExtras(bundle);

		context.startActivity(intent);
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		List<Birthday> list = DBHelper.getInstance(this)
				.getBirthdayDAO()
				.getBirthdayByMonth(itemPosition + 1, getBlogName());
		fillList(list);

		return true;
	}
}
