package com.ternaryop.photoshelf.lifecycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class PhotoShelfViewModel<VM> : ViewModel() {
    protected val mResult = MutableLiveData<Event<VM>>()

    val result: LiveData<Event<VM>>
        get() = mResult

    fun postResult(result: VM) = mResult.postValue(Event(result))
}
