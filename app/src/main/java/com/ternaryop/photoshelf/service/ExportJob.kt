package com.ternaryop.photoshelf.service

import android.app.job.JobParameters
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.importer.BatchExporter
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object ExportJob : Job {
    override fun runJob(jobService: AbsJobService, params: JobParameters?): Boolean {
        val appSupport = AppSupport(jobService)
        if (!appSupport.isAutomaticExportEnabled) {
            return false
        }
        export(jobService, params)
        return true
    }

    private fun export(jobService: AbsJobService, params: JobParameters?) {
        val appSupport = AppSupport(jobService)
        val d = Completable
            .fromCallable { BatchExporter(appSupport).export() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally { jobService.jobFinished(params, false) }
            .subscribe({}, { it.printStackTrace() })
        jobService.compositeDisposable.add(d)
    }
}
