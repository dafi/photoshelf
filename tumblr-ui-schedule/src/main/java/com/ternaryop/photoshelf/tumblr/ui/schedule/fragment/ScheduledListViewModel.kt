package com.ternaryop.photoshelf.tumblr.ui.schedule.fragment

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.repository.tumblr.TumblrRepository
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.util.post.FetchedData
import com.ternaryop.photoshelf.util.post.PageFetcher
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduledListViewModel(
    application: Application,
    private val tumblrRepository: TumblrRepository
) : PhotoShelfViewModel<ScheduledListResult>(application) {
    private val tumblr = TumblrManager.getInstance(application)
    val pageFetcher = PageFetcher<PhotoShelfPost>(Tumblr.MAX_POST_PER_REQUEST)

    fun scheduled(blogName: String, params: Map<String, String>, fetchCache: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = pageFetcher.fetch(fetchCache) {
                tumblr.getQueue(blogName, params).map {
                    PhotoShelfPost(it as TumblrPhotoPost, it.scheduledPublishTime * DateUtils.SECOND_IN_MILLIS)
                }
            }
            postResult(ScheduledListResult.Scheduled(command))
        }
    }

    fun updateCount(count: Int) {
        tumblrRepository.updateScheduledCount(count)
    }
}

sealed class ScheduledListResult {
    data class Scheduled(val command: Command<FetchedData<PhotoShelfPost>>) : ScheduledListResult()
}
