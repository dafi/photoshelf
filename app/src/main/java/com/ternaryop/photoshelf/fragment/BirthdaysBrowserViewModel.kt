package com.ternaryop.photoshelf.fragment

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.adapter.birthday.BirthdayShowFlags
import com.ternaryop.photoshelf.adapter.birthday.nullDate
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.BirthdayResult
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.utils.date.toIsoFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BirthdaysBrowserViewModel(application: Application) : PhotoShelfViewModel<BirthdaysBrowserModelResult>(application) {
    val showFlags = BirthdayShowFlags()
    var month: Int = 0

    fun find(actionId: BirthdaysBrowserModelResult.ActionId, pattern: String, offset: Int, limit: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                postResult(BirthdaysBrowserModelResult.Find(actionId,
                    Command.success(showFlags.find(pattern, month - 1, offset, limit).response)))
            } catch (t: Throwable) {
                postResult(BirthdaysBrowserModelResult.Find(actionId, Command.error(t)))
            }
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
            { command -> postResult(BirthdaysBrowserModelResult.DeleteBirthdays(command)) })
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
    data class Find(val actionId: ActionId, val command: Command<BirthdayResult>) : BirthdaysBrowserModelResult()
    data class MarkAsIgnored(val command: Command<List<Birthday>>) : BirthdaysBrowserModelResult()
    data class UpdateByName(val command: Command<Birthday>) : BirthdaysBrowserModelResult()
    data class DeleteBirthdays(val command: Command<List<Birthday>>) : BirthdaysBrowserModelResult()

    enum class ActionId {
        QUERY_BY_TYPING,
        RESUBMIT_QUERY
    }
}