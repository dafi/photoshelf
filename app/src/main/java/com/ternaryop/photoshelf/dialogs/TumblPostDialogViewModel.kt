package com.ternaryop.photoshelf.dialogs

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.parser.TitleComponentsResult
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : PhotoShelfViewModel<TumblrPostModelResult>(application) {
    private val misspelledName: MisspelledName by lazy {
        MisspelledName(application)
    }

    fun parse(title: String, swapDayMonth: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                postResult(TumblrPostModelResult.TitleParsed(Command.success(ApiManager.parserService().components(title, swapDayMonth).response)))
            } catch (t: Throwable) {
                postResult(TumblrPostModelResult.TitleParsed(Command.error(t)))
            }
        }
    }

    fun searchMisspelledName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                postResult(TumblrPostModelResult.MisspelledInfo(Command.success(misspelledName.getMisspelledInfo(name))))
            } catch (t: Throwable) {
                postResult(TumblrPostModelResult.MisspelledInfo(Command.error(t)))
            }
        }
    }
}

sealed class TumblrPostModelResult {
    data class TitleParsed(val command: Command<TitleComponentsResult>) : TumblrPostModelResult()
    data class MisspelledInfo(val command: Command<Pair<Int, String>>) : TumblrPostModelResult()
}