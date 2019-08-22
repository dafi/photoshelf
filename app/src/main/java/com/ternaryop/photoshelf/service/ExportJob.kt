package com.ternaryop.photoshelf.service

import android.app.job.JobParameters
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.importer.BatchExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        jobService.launch(Dispatchers.IO) {
            try {
                BatchExporter(appSupport).export()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                jobService.jobFinished(params, false)
            }

        }
    }
}
