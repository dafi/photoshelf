package com.ternaryop.phototumblrshare.dialogs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.json.JSONObject;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.list.PhotoSharePost;
import com.ternaryop.tumblr.AbsCallback;
import com.ternaryop.tumblr.Tumblr;

public class SchedulePostDialog extends Dialog implements View.OnClickListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

	private final Calendar scheduleDateTime;
	private Button chooseDateButton;
	private Button chooseTimeButton;
	private SimpleDateFormat timeFormat;
	private SimpleDateFormat dateFormat;
	private final PhotoSharePost item;
	private final String blogName;
	private final onPostScheduleListener onPostSchedule;

	public SchedulePostDialog(Context context, String blogName, PhotoSharePost item, Calendar scheduleDateTime, onPostScheduleListener onPostSchedule) {
		super(context);
		this.blogName = blogName;
		this.item = item;
		this.scheduleDateTime = scheduleDateTime;
		this.onPostSchedule = onPostSchedule;
		setContentView(R.layout.dialog_schedule_post);
		
		setTitle(R.string.schedule_post);
		((TextView)findViewById(R.id.post_title_textview)).setText(item.getFirstTag());
		((Button)findViewById(R.id.cancelButton)).setOnClickListener(this);
		((Button)findViewById(R.id.schedule_button)).setOnClickListener(this);

		// init date
		chooseDateButton = (Button)findViewById(R.id.choose_date_button);
		dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
		chooseDateButton.setText(dateFormat.format(scheduleDateTime.getTime()));
		chooseDateButton.setOnClickListener(this);

		// init time
		chooseTimeButton = (Button)findViewById(R.id.choose_time_button);
		timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
		chooseTimeButton.setText(timeFormat.format(scheduleDateTime.getTime()));
		chooseTimeButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.cancelButton:
				dismiss();
				return;
			case R.id.schedule_button:
				schedulePost();
				return;
			case R.id.choose_time_button:
				new TimePickerDialog(getContext(), this,
						scheduleDateTime.get(Calendar.HOUR_OF_DAY),
						scheduleDateTime.get(Calendar.MINUTE), true)
				.show();
				return;
			case R.id.choose_date_button:
				new DatePickerDialog(getContext(), this,
						scheduleDateTime.get(Calendar.YEAR),
						scheduleDateTime.get(Calendar.MONTH),
						scheduleDateTime.get(Calendar.DAY_OF_MONTH))
				.show();
				return;
		}
	}

	private void schedulePost() {
		Tumblr.getSharedTumblr(getContext())
		.schedulePost(blogName, item.getPostId(), scheduleDateTime.getTimeInMillis(), new AbsCallback(this, R.string.parsing_error) {

			@Override
			public void complete(JSONObject result) {
				if (onPostSchedule != null) {
					onPostSchedule.onPostScheduled(item.getPostId(), scheduleDateTime);
				}
				dismiss();
			}
		});
	}

	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		scheduleDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
		scheduleDateTime.set(Calendar.MINUTE, minute);
		chooseTimeButton.setText(timeFormat.format(scheduleDateTime.getTime()));
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		scheduleDateTime.set(Calendar.YEAR, year);
		scheduleDateTime.set(Calendar.MONTH, monthOfYear);
		scheduleDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		chooseDateButton.setText(dateFormat.format(scheduleDateTime.getTime()));
	}
	
	public interface onPostScheduleListener {
		void onPostScheduled(long id, Calendar scheduledDateTime);
	}
}
