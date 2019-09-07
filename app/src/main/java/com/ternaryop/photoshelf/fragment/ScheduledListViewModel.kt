package com.ternaryop.photoshelf.fragment

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduledListViewModel(application: Application) : PhotoShelfViewModel<ScheduledListResult>(application) {
    private val tumblr = TumblrManager.getInstance(application)
    private var scheduledList: MutableList<PhotoShelfPost>? = null

    fun scheduled(blogName: String, params: Map<String, String>, refresh: Boolean) {
        if (refresh) {
            scheduledList = null
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (scheduledList == null) {
                    scheduledList = mutableListOf()
                }
                val list = tumblr.getQueue(blogName, params).map {
                    PhotoShelfPost(it as TumblrPhotoPost, it.scheduledPublishTime * DateUtils.SECOND_IN_MILLIS)
                }
                scheduledList?.apply {
                    addAll(list)
                    postResult(ScheduledListResult.Scheduled(Command.success(FetchedPosts(this, list.size))))
                }
            } catch (t: Throwable) {
                postResult(ScheduledListResult.Scheduled(Command.error(t)))
            }
        }
    }
}

sealed class ScheduledListResult {
    data class Scheduled(val command: Command<FetchedPosts>) : ScheduledListResult()
}
