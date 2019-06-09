package com.ternaryop.photoshelf.service

import android.app.job.JobParameters
import android.app.job.JobService
import io.reactivex.disposables.CompositeDisposable

interface Job {
    fun runJob(jobService: AbsJobService, params: JobParameters?): Boolean
}

abstract class AbsJobService : JobService() {
    lateinit var compositeDisposable: CompositeDisposable

    override fun onCreate() {
        super.onCreate()
        compositeDisposable = CompositeDisposable()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        compositeDisposable.clear()
        return false
    }
}
