package com.ternaryop.photoshelf.birthday.browser.fragment

import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.BirthdayService.Companion.MAX_BIRTHDAY_COUNT
import com.ternaryop.photoshelf.birthday.browser.adapter.BirthdayShowFlags
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.util.post.FetchedData
import com.ternaryop.photoshelf.util.post.PageFetcher
import com.ternaryop.photoshelf.util.post.removeItems
import com.ternaryop.utils.date.toIsoFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BirthdayBrowserViewModel @Inject constructor() : PhotoShelfViewModel<BirthdayBrowserModelResult>() {
    val showFlags = BirthdayShowFlags()
    var month: Int = 0
    val pageFetcher = PageFetcher<Birthday>(MAX_BIRTHDAY_COUNT)

    fun find(actionId: BirthdayBrowserModelResult.ActionId, pattern: String, fetchCache: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = pageFetcher.fetch(fetchCache) { pi ->
                showFlags.find(pattern, month - 1, pi.offset, pageFetcher.limitCount).response.birthdays
            }
            postResult(BirthdayBrowserModelResult.Find(actionId, command))
        }
    }

    fun markAsIgnored(list: List<Birthday>) {
        processList(list,
            { birthday ->
                ApiManager.birthdayService().markAsIgnored(birthday.name)
                birthday.birthdate = null
            },
            { command -> postResult(BirthdayBrowserModelResult.MarkAsIgnored(command)) })
    }

    fun updateByName(birthday: Birthday) {
        val birthdate = birthday.birthdate ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute {
                ApiManager.birthdayService().updateByName(birthday.name, birthdate.toIsoFormat())
                birthday
            }
            postResult(BirthdayBrowserModelResult.UpdateByName(command))
        }
    }

    fun deleteBirthdays(list: List<Birthday>) {
        processList(list,
            { birthday -> ApiManager.birthdayService().deleteByName(birthday.name) },
            { command ->
                // data contains all successful processed items
                // no matter if the command is Error or Success
                command.data?.also { pageFetcher.removeItems(it) }
                postResult(BirthdayBrowserModelResult.DeleteBirthday(command))
            })
    }

    private fun processList(
        list: List<Birthday>,
        action: suspend (Birthday) -> Unit,
        actionResult: (Command<List<Birthday>>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val processedItems = mutableListOf<Birthday>()
            try {
                list.forEach { birthday ->
                    action(birthday)
                    processedItems += birthday
                }
                actionResult(Command.success(processedItems))
            } catch (expected: Throwable) {
                actionResult(Command.error(expected, processedItems))
            }
        }
    }
}

sealed class BirthdayBrowserModelResult {
    data class Find(val actionId: ActionId, val command: Command<FetchedData<Birthday>>) : BirthdayBrowserModelResult()
    data class MarkAsIgnored(val command: Command<List<Birthday>>) : BirthdayBrowserModelResult()
    data class UpdateByName(val command: Command<Birthday>) : BirthdayBrowserModelResult()
    data class DeleteBirthday(val command: Command<List<Birthday>>) : BirthdayBrowserModelResult()

    enum class ActionId {
        QUERY_BY_TYPING,
        RESUBMIT_QUERY
    }
}
