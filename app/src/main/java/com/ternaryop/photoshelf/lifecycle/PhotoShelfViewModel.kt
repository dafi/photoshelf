package com.ternaryop.photoshelf.lifecycle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

open class PhotoShelfViewModel<VM>(application: Application) : AndroidViewModel(application) {
    private val _result = MutableLiveData<VM>()

    val result: LiveData<VM>
        get() = _result

    fun postResult(result: VM) = _result.postValue(result)
}

enum class Status {
    SUCCESS,
    ERROR,
    PROGRESS
}

data class ProgressData(val step: Int, val itemCount: Int)

data class Command<out T>(val status: Status, val data: T? = null, val error: Throwable? = null, val progressData: ProgressData? = null) {
    companion object {
        fun <T> success(data: T?) = Command<T>(Status.SUCCESS, data)
        fun <T> error(error: Throwable, data: T? = null) = Command<T>(Status.ERROR, data, error = error)
        fun <T> progress(progressData: ProgressData) = Command<T>(Status.PROGRESS, progressData = progressData)
    }
}
