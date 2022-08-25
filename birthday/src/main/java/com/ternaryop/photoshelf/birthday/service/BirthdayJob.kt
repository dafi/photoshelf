package com.ternaryop.photoshelf.birthday.service

import android.app.job.JobParameters
import android.content.Context
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.birthday.util.lastBirthdayShowTime
import com.ternaryop.photoshelf.birthday.util.notifyBirthday
import com.ternaryop.photoshelf.birthday.util.showBirthdaysNotification
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.photoshelf.service.AbsJobService
import com.ternaryop.photoshelf.service.Job
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import kotlinx.coroutines.launch
import java.util.Calendar

object BirthdayJob : Job {
    override fun runJob(jobService: AbsJobService, params: JobParameters?): Boolean {
        val blogName = PreferenceManager.getDefaultSharedPreferences(jobService).selectedBlogName ?: return false

        if (!PreferenceManager.getDefaultSharedPreferences(jobService).showBirthdaysNotification ||
            hasAlreadyNotifiedToday(jobService)
        ) {
            return false
        }
        notifyBirthday(jobService, blogName, params)
        return true
    }

    private fun notifyBirthday(jobService: AbsJobService, blogName: String, params: JobParameters?) {
        jobService.launch {
            notifyBirthday(jobService, blogName)
            jobService.jobFinished(params, false)
        }
    }

    private fun hasAlreadyNotifiedToday(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lastBirthdayShowTime = Calendar.getInstance()
        lastBirthdayShowTime.timeInMillis = prefs.lastBirthdayShowTime
        val now = Calendar.getInstance()
        if (now.dayOfMonth == lastBirthdayShowTime.dayOfMonth && now.month == lastBirthdayShowTime.month) {
            return true
        }
        prefs.lastBirthdayShowTime = now.timeInMillis
        return false
    }
}
