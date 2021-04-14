package com.ternaryop.photoshelf.tumblr.ui.schedule.fragment

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
import com.ternaryop.tumblr.getQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduledListViewModel @Inject constructor(
    private val tumblrRepository: TumblrRepository
) : PhotoShelfViewModel<ScheduledListResult>() {
    val pageFetcher = PageFetcher<PhotoShelfPost>(Tumblr.MAX_POST_PER_REQUEST)

    fun scheduled(blogName: String, params: Map<String, String>, fetchCache: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = pageFetcher.fetch(fetchCache) {
                tumblrRepository.tumblr.getQueue(blogName, params).map {
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
