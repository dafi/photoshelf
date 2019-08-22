package com.ternaryop.photoshelf.service

import android.app.job.JobParameters
import android.content.Context
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.birthday.notifyBirthday
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import kotlinx.coroutines.launch
import java.util.Calendar

object BirthdayJob : Job {
    override fun runJob(jobService: AbsJobService, params: JobParameters?): Boolean {
        val blogName = AppSupport(jobService).selectedBlogName ?: return false
        if (hasAlreadyNotifiedToday(jobService)) {
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
        val appSupport = AppSupport(context)
        val lastBirthdayShowTime = Calendar.getInstance()
        lastBirthdayShowTime.timeInMillis = appSupport.lastBirthdayShowTime
        val now = Calendar.getInstance()
        if (now.dayOfMonth == lastBirthdayShowTime.dayOfMonth && now.month == lastBirthdayShowTime.month) {
            return true
        }
        appSupport.lastBirthdayShowTime = now.timeInMillis
        return false
    }
}
