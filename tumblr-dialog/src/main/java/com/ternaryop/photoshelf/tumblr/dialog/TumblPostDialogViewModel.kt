package com.ternaryop.photoshelf.tumblr.dialog

import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.parser.TitleComponentsResult
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.misspelled.MisspelledName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val misspelledName: MisspelledName
) : PhotoShelfViewModel<TumblrPostModelResult>() {

    fun parse(title: String, swapDayMonth: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute {
                ApiManager.parserService().components(title, swapDayMonth).response
            }
            postResult(TumblrPostModelResult.TitleParsed(command))
        }
    }

    fun searchMisspelledName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = if (name.isBlank()) {
                Command.success(MisspelledName.Info.NotFound(name))
            } else {
                Command.execute { misspelledName.getMisspelledInfo(name) }
            }
            postResult(TumblrPostModelResult.MisspelledInfo(command))
        }
    }
}

sealed class TumblrPostModelResult {
    data class TitleParsed(val command: Command<TitleComponentsResult>) : TumblrPostModelResult()
    data class MisspelledInfo(val command: Command<MisspelledName.Info>) : TumblrPostModelResult()
}
