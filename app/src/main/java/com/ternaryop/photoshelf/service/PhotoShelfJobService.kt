package com.ternaryop.photoshelf.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.TimeUnit

class PhotoShelfJobService : AbsJobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        return when (params?.jobId) {
            BIRTHDAY_JOB_ID -> BirthdayJob.runJob(this, params)
            EXPORT_JOB_ID -> ExportJob.runJob(this, params)
            else -> false
        }
    }

    companion object {
        const val BIRTHDAY_JOB_ID = 1
        const val EXPORT_JOB_ID = 2

        private val PERIODIC_BIRTHDAY_MILLIS = TimeUnit.DAYS.toMillis(1)
        private val PERIODIC_EXPORT_MILLIS = TimeUnit.HOURS.toMillis(3)

        fun scheduleBirthday(context: Context) {
            val jobInfo = JobInfo.Builder(BIRTHDAY_JOB_ID, ComponentName(context, PhotoShelfJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPeriodic(PERIODIC_BIRTHDAY_MILLIS)
                .build()
            (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(jobInfo)
        }

        fun scheduleExport(context: Context) {
            val jobInfo = JobInfo.Builder(EXPORT_JOB_ID, ComponentName(context, PhotoShelfJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                .setPeriodic(PERIODIC_EXPORT_MILLIS)
                .build()
            (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(jobInfo)
        }
    }
}
