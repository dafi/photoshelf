package com.ternaryop.photoshelf.repository.tumblr

import android.app.Application
import android.net.Uri
import com.ternaryop.photoshelf.lifecycle.CommandMutableLiveData
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.draftCount
import com.ternaryop.tumblr.queueCount

class TumblrRepository(private val application: Application) {
    // user should be not logged when the manager is created so we create it lazily
    private val tumblr: Tumblr by lazy { TumblrManager.getInstance(application) }
    private val _draftCount = CommandMutableLiveData<Int>()
    private val _scheduledCount = CommandMutableLiveData<Int>()
    val draftCount = _draftCount.asLiveData()
    val scheduledCount = _scheduledCount.asLiveData()

    private val _authenticate = CommandMutableLiveData<Boolean>()
    val authenticate = _authenticate.asLiveData()

    suspend fun draftCount(blogName: String) =
        _draftCount.post(true) { tumblr.draftCount(blogName) }

    suspend fun scheduledCount(blogName: String) =
        _scheduledCount.post(true) { tumblr.queueCount(blogName) }

    fun updateDraftCount(count: Int) = _draftCount.setLastValue(count, true)

    fun updateScheduledCount(count: Int) = _scheduledCount.setLastValue(count, true)

    suspend fun handleCallbackUri(uri: Uri?) {
        if (TumblrManager.isLogged(application)) {
            return
        }
        _authenticate.post(false) { TumblrManager.handleOpenURI(application, uri) }
    }

    fun clearCount() {
        _draftCount.setLastValue(null, false)
        _scheduledCount.setLastValue(null, false)
    }
}
