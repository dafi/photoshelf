package com.ternaryop.photoshelf.lifecycle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

open class PhotoShelfViewModel<VM>(application: Application) : AndroidViewModel(application) {
    protected val mResult = MutableLiveData<Event<VM>>()

    val result: LiveData<Event<VM>>
        get() = mResult

    fun postResult(result: VM) = mResult.postValue(Event(result))
}
