package com.ternaryop.phototumblrshare.activity;

import java.util.Date;
import java.util.List;

import android.os.Bundle;
import android.widget.TextView;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.Birthday;
import com.ternaryop.phototumblrshare.db.BirthdayDAO;
import com.ternaryop.phototumblrshare.db.DBHelper;

public class BirthdaysActivity extends PhotoTumblrActivity {
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_birthdays_main);

		BirthdayDAO birthdayDatabaseHelper = DBHelper
				.getInstance(this)
				.getBirthdayDAO();
		List<Birthday> list = birthdayDatabaseHelper.getBirthdayByDate(new Date(), null);
		StringBuilder sb = new StringBuilder();
		for (Birthday birthday : list) {
			sb.append(birthday + "\n");
		}
		((TextView)findViewById(R.id.text)).setText(sb);
	}
}
