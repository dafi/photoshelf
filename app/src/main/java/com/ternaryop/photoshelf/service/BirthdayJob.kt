package com.ternaryop.photoshelf.service

import android.app.job.JobParameters
import android.content.Context
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import com.ternaryop.utils.log.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.Calendar
import java.util.Locale

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
        val now = Calendar.getInstance(Locale.US)
        val d = ApiManager.birthdayService(jobService).findByDate(
            FindParams(month = now.month + 1, dayOfMonth = now.dayOfMonth, blogName = blogName).toQueryMap())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { jobService.jobFinished(params, false) }
            .map { it.response }
            .subscribe({ birthdayResult ->
                birthdayResult.birthdays?.let { list ->
                    if (list.isNotEmpty()) {
                        NotificationUtil(jobService).notifyTodayBirthdays(list, now.year)
                    }
                }
            }, { t ->
                Log.error(t, jobService.logFile)
                NotificationUtil(jobService).notifyError(t, "Error") })
        jobService.compositeDisposable.add(d)
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
