package com.ternaryop.photoshelf.dialogs

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import com.ternaryop.photoshelf.R
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.hourOfDay
import com.ternaryop.utils.date.minute
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SchedulePostDialog(context: Context,
    val item: TumblrPost,
    val scheduleDateTime: Calendar,
    private val onCloseListener: OnCloseDialogListener<SchedulePostDialog>) : Dialog(context), View.OnClickListener,
    TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
    private val chooseDateButton: Button
    private val chooseTimeButton: Button
    private val timeFormat: SimpleDateFormat

    init {
        setContentView(R.layout.dialog_schedule_post)

        setTitle(context.getString(R.string.schedule_dialog_title, item.firstTag))
        findViewById<View>(R.id.cancelButton).setOnClickListener(this)
        findViewById<View>(R.id.schedule_button).setOnClickListener(this)

        // init date
        chooseDateButton = findViewById<View>(R.id.choose_date_button) as Button
        chooseDateButton.setOnClickListener(this)
        updateDateButton()

        // init time
        chooseTimeButton = findViewById<View>(R.id.choose_time_button) as Button
        timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        updateTimeButton(scheduleDateTime)
        chooseTimeButton.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.cancelButton -> dismiss()
            R.id.schedule_button -> checkAndSchedule()
            R.id.choose_time_button -> TimePickerDialog(context, this,
                        scheduleDateTime.hourOfDay,
                        scheduleDateTime.minute, true)
                        .show()
            R.id.choose_date_button -> {
                val dialog = DatePickerDialog(context, this,
                        scheduleDateTime.year,
                        scheduleDateTime.month,
                        scheduleDateTime.dayOfMonth)
                dialog.datePicker.minDate = System.currentTimeMillis()
                dialog.show()
            }
        }
    }

    private fun checkAndSchedule() {
        if (scheduleDateTime.timeInMillis <= System.currentTimeMillis()) {
            AlertDialog.Builder(context)
                    .setMessage(R.string.scheduled_time_is_in_the_past_continue_title)
                    .setPositiveButton(android.R.string.yes) { _, _ -> schedulePost() }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
        } else {
            schedulePost()
        }
    }

    private fun schedulePost() {
        findViewById<View>(R.id.schedule_button).isEnabled = false
        if (onCloseListener.onClose(this, DialogInterface.BUTTON_POSITIVE)) {
            dismiss()
        }
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        scheduleDateTime.hourOfDay = hourOfDay
        scheduleDateTime.minute = minute
        updateTimeButton(scheduleDateTime)
    }

    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        scheduleDateTime.year = year
        scheduleDateTime.month = monthOfYear
        scheduleDateTime.dayOfMonth = dayOfMonth
        updateDateButton()
    }

    private fun updateDateButton() {
        val timeString = DateUtils.getRelativeTimeSpanString(
                scheduleDateTime.timeInMillis,
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_SHOW_DATE)

        chooseDateButton.text = timeString
    }

    private fun updateTimeButton(scheduleDateTime: Calendar) {
        chooseTimeButton.text = timeFormat.format(scheduleDateTime.time)
    }
}
