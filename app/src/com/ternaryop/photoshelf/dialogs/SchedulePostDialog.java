package com.ternaryop.photoshelf.dialogs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.tumblr.AbsCallback;
import com.ternaryop.tumblr.Tumblr;
import org.json.JSONObject;

public class SchedulePostDialog extends Dialog implements View.OnClickListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    private final Calendar scheduleDateTime;
    private final Button chooseDateButton;
    private final Button chooseTimeButton;
    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat dateFormat;
    private final PhotoShelfPost item;
    private final String blogName;
    private final onPostScheduleListener onPostSchedule;

    public SchedulePostDialog(Context context, String blogName, PhotoShelfPost item, Calendar scheduleDateTime, onPostScheduleListener onPostSchedule) {
        super(context);
        this.blogName = blogName;
        this.item = item;
        this.scheduleDateTime = scheduleDateTime;
        this.onPostSchedule = onPostSchedule;
        setContentView(R.layout.dialog_schedule_post);
        
        setTitle(context.getString(R.string.schedule_dialog_title, item.getFirstTag()));
        findViewById(R.id.cancelButton).setOnClickListener(this);
        findViewById(R.id.schedule_button).setOnClickListener(this);

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
                checkAndSchedule();
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
        }
    }

    private void checkAndSchedule() {
        if (scheduleDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
            new AlertDialog.Builder(getContext())
            .setMessage(R.string.scheduled_time_is_in_the_past_continue_title)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    schedulePost();
                }
            })
            .setNegativeButton(android.R.string.no, null)
            .show();
        } else {
            schedulePost();
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
