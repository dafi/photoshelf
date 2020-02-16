package com.ternaryop.photoshelf.home.fragment

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
            val command = Command.execute {
                ApiManager.postService().getStats(blogName).response
            }
            postResult(HomeModelResult.Stats(command))
        }
    }
}

sealed class HomeModelResult {
    data class Stats(val command: Command<StatsResult>) : HomeModelResult()
}
