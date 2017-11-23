package com.ternaryop.photoshelf.dialogs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.adapter.PhotoShelfPost;
import com.ternaryop.tumblr.Tumblr;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SchedulePostDialog extends Dialog implements View.OnClickListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    private final Calendar scheduleDateTime;
    private final Button chooseDateButton;
    private final Button chooseTimeButton;
    private final SimpleDateFormat timeFormat;
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
        chooseDateButton.setOnClickListener(this);
        updateDateButton();

        // init time
        chooseTimeButton = (Button)findViewById(R.id.choose_time_button);
        timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        updateTimeButton(scheduleDateTime);
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
                final DatePickerDialog dialog = new DatePickerDialog(getContext(), this,
                        scheduleDateTime.get(Calendar.YEAR),
                        scheduleDateTime.get(Calendar.MONTH),
                        scheduleDateTime.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(System.currentTimeMillis());
                dialog.show();
        }
    }

    private void checkAndSchedule() {
        if (scheduleDateTime.getTimeInMillis() <= System.currentTimeMillis()) {
            new AlertDialog.Builder(getContext())
            .setMessage(R.string.scheduled_time_is_in_the_past_continue_title)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> schedulePost())
            .setNegativeButton(android.R.string.no, null)
            .show();
        } else {
            schedulePost();
        }
    }

    private void schedulePost() {
        findViewById(R.id.schedule_button).setEnabled(false);
        final Completable completable = Completable
                .fromAction(() -> Tumblr.getSharedTumblr(getContext())
                        .schedulePost(blogName, item, scheduleDateTime.getTimeInMillis()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> dismiss());
        if (onPostSchedule != null) {
            onPostSchedule.onPostScheduled(item.getPostId(), scheduleDateTime, completable);
        }
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        scheduleDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        scheduleDateTime.set(Calendar.MINUTE, minute);
        updateTimeButton(scheduleDateTime);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        scheduleDateTime.set(Calendar.YEAR, year);
        scheduleDateTime.set(Calendar.MONTH, monthOfYear);
        scheduleDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateDateButton();
    }
    
    public interface onPostScheduleListener {
        @SuppressWarnings("UnusedParameters")
        void onPostScheduled(long id, Calendar scheduledDateTime, Completable completable);
    }

    private void updateDateButton() {
        CharSequence timeString = DateUtils.getRelativeTimeSpanString(
                scheduleDateTime.getTimeInMillis(),
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_DATE);

        chooseDateButton.setText(timeString);
    }

    private void updateTimeButton(Calendar scheduleDateTime) {
        chooseTimeButton.setText(timeFormat.format(scheduleDateTime.getTime()));
    }
}
