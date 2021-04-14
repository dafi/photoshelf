package com.ternaryop.photoshelf.tumblr.dialog

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.hourOfDay
import com.ternaryop.utils.date.minute
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SchedulePostDialog : DialogFragment(), View.OnClickListener,
    TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    private lateinit var post: TumblrPost
    private lateinit var scheduleDateTime: Calendar
    private lateinit var chooseDateButton: Button
    private lateinit var chooseTimeButton: Button
    private lateinit var timeFormat: SimpleDateFormat
    private lateinit var scheduleButton: Button

    @SuppressLint("InflateParams") // for dialogs passing null for root is valid, ignore the warning
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_schedule_post, null)

        post = checkNotNull(arguments?.getSerializable(ARG_POST) as? TumblrPost)
        scheduleDateTime = checkNotNull(arguments?.getSerializable(ARG_SCHEDULE_DATE) as? Calendar)

        setupUI(view)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .setPositiveButton(R.string.schedule_title) { _, _ -> }
            .create()

        dialog.setOnShowListener {
            scheduleButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            scheduleButton.setOnClickListener {
                checkAndSchedule()
            }
        }
        return dialog
    }

    private fun setupUI(view: View) {
        // init date
        chooseDateButton = view.findViewById(R.id.choose_date_button)
        chooseDateButton.setOnClickListener(this)
        updateDateButton()

        // init time
        chooseTimeButton = view.findViewById(R.id.choose_time_button)
        timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        updateTimeButton(scheduleDateTime)
        chooseTimeButton.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.choose_time_button -> TimePickerDialog(context, this,
                scheduleDateTime.hourOfDay,
                scheduleDateTime.minute, true)
                .show()
            R.id.choose_date_button -> {
                val dialog = DatePickerDialog(requireContext(), this,
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
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.scheduled_time_is_in_the_past_continue_title)
                .setPositiveButton(android.R.string.ok) { _, _ -> schedulePost() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            schedulePost()
        }
    }

    private fun schedulePost() {
        scheduleButton.isEnabled = false
        parentFragmentManager.setFragmentResult(
            checkNotNull(arguments?.getString(EXTRA_REQUEST_KEY)),
            Bundle().also {
                parentFragmentManager.putFragment(it, EXTRA_DIALOG, this)
                it.putSerializable(EXTRA_SCHEDULE_DATA, SchedulePostData(post, scheduleDateTime))
            }
        )
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

    companion object {
        private const val ARG_SCHEDULE_DATE = "scheduleDate"
        private const val ARG_POST = "post"
        private const val EXTRA_REQUEST_KEY = "requestKey"
        const val EXTRA_DIALOG = "dialog"
        const val EXTRA_SCHEDULE_DATA = "scheduleData"

        fun newInstance(
            post: TumblrPost,
            scheduleDateTime: Calendar,
            requestKey: String
        ) = SchedulePostDialog().apply {
            arguments = bundleOf(
                EXTRA_REQUEST_KEY to requestKey,
                ARG_SCHEDULE_DATE to scheduleDateTime,
                ARG_POST to post
            )
        }
    }
}
