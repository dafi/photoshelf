package com.ternaryop.photoshelf.fragment

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.StatsResult
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : PhotoShelfViewModel<HomeModelResult>(application) {

    fun loadStats(blogName: String) {
        viewModelScope.launch {
            try {
                postResult(HomeModelResult.Stats(Command.success(ApiManager.postService().getStats(blogName).response)))
            } catch (t: Throwable) {
                postResult(HomeModelResult.Stats(Command.error(t)))
            }
        }
    }
}

sealed class HomeModelResult {
    data class Stats(val command: Command<StatsResult>) : HomeModelResult()
}