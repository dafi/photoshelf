package com.ternaryop.photoshelf.service

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

interface Job {
    fun runJob(jobService: AbsJobService, params: JobParameters?): Boolean
}

abstract class AbsJobService : JobService(), CoroutineScope {
    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate() {
        super.onCreate()
        job = Job()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        job.cancel()
        return false
    }
}
