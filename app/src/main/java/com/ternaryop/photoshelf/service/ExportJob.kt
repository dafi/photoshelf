package com.ternaryop.photoshelf.service

import android.app.job.JobParameters
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.core.prefs.isAutomaticExportEnabled
import com.ternaryop.photoshelf.importer.BatchExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ExportJob : Job {
    override fun runJob(jobService: AbsJobService, params: JobParameters?): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(jobService)
        if (!prefs.isAutomaticExportEnabled) {
            return false
        }
        export(jobService, params)
        return true
    }

    private fun export(jobService: AbsJobService, params: JobParameters?) {
        jobService.launch(Dispatchers.IO) {
            try {
                BatchExporter(jobService).export()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                jobService.jobFinished(params, false)
            }
        }
    }
}
