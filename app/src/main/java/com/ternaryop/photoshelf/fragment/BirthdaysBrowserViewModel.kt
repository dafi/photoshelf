package com.ternaryop.photoshelf.fragment

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.adapter.birthday.BirthdayShowFlags
import com.ternaryop.photoshelf.adapter.birthday.nullDate
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.BirthdayService.Companion.MAX_BIRTHDAY_COUNT
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.util.post.FetchedData
import com.ternaryop.photoshelf.util.post.PageFetcher
import com.ternaryop.photoshelf.util.post.removeItems
import com.ternaryop.utils.date.toIsoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BirthdaysBrowserViewModel(application: Application) : PhotoShelfViewModel<BirthdaysBrowserModelResult>(application) {
    val showFlags = BirthdayShowFlags()
    var month: Int = 0
    val pageFetcher = PageFetcher<Birthday>(MAX_BIRTHDAY_COUNT)

    fun find(actionId: BirthdaysBrowserModelResult.ActionId, pattern: String, fetchCache: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = pageFetcher.fetch(fetchCache) { pi ->
                showFlags.find(pattern, month - 1, pi.offset, pageFetcher.limitCount).response.birthdays
            }
            postResult(BirthdaysBrowserModelResult.Find(actionId, command))
        }
    }

    fun markAsIgnored(list: List<Birthday>) {
        processList(list,
            { birthday ->
                ApiManager.birthdayService().markAsIgnored(birthday.name)
                birthday.birthdate = nullDate
            },
            { command -> postResult(BirthdaysBrowserModelResult.MarkAsIgnored(command)) })
    }

    fun updateByName(birthday: Birthday) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ApiManager.birthdayService().updateByName(birthday.name, birthday.birthdate.toIsoFormat())
                postResult(BirthdaysBrowserModelResult.UpdateByName(Command.success(birthday)))
            } catch (t: Throwable) {
                postResult(BirthdaysBrowserModelResult.UpdateByName(Command.error(t)))
            }
        }
    }

    fun deleteBirthdays(list: List<Birthday>) {
        processList(list,
            { birthday -> ApiManager.birthdayService().deleteByName(birthday.name) },
            { command ->
                // data contains all successful processed items
                // no matter if the command is Error or Success
                command.data?.also { pageFetcher.removeItems(it) }
                postResult(BirthdaysBrowserModelResult.DeleteBirthdays(command))
            })
    }

    private fun processList(list: List<Birthday>,
        action: suspend (Birthday) -> Unit,
        actionResult: (Command<List<Birthday>>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val processedItems = mutableListOf<Birthday>()
            try {
                list.forEach { birthday ->
                    action(birthday)
                    processedItems += birthday
                }
                actionResult(Command.success(processedItems))
            } catch (t: Throwable) {
                actionResult(Command.error(t, processedItems))
            }
        }
    }
}

sealed class BirthdaysBrowserModelResult {
    data class Find(val actionId: ActionId, val command: Command<FetchedData<Birthday>>) : BirthdaysBrowserModelResult()
    data class MarkAsIgnored(val command: Command<List<Birthday>>) : BirthdaysBrowserModelResult()
    data class UpdateByName(val command: Command<Birthday>) : BirthdaysBrowserModelResult()
    data class DeleteBirthdays(val command: Command<List<Birthday>>) : BirthdaysBrowserModelResult()

    enum class ActionId {
        QUERY_BY_TYPING,
        RESUBMIT_QUERY
    }
}