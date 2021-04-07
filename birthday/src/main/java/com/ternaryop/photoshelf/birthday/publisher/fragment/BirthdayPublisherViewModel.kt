package com.ternaryop.photoshelf.birthday.publisher.fragment

import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.api.birthday.ImageSize
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BirthdayPublisherViewModel @Inject constructor() : PhotoShelfViewModel<BirthdayPublisherModelResult>() {
    private var birthdays: MutableList<Birthday>? = null

    fun listByDate(birthday: Calendar, blogName: String) {
        if (birthdays != null) {
            postResult(BirthdayPublisherModelResult.ListByDate(Command.success(birthdays)))
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute {
                birthdays = ApiManager.birthdayService().findByDate(
                    FindParams(
                        month = birthday.month + 1,
                        dayOfMonth = birthday.dayOfMonth,
                        pickImages = true,
                        blogName = blogName).toQueryMap())
                    .response
                    .birthdays?.toMutableList()
                birthdays
            }
            postResult(BirthdayPublisherModelResult.ListByDate(command))
        }
    }

    fun updatePostByTag(newPost: TumblrPhotoPost): Birthday? {
        val items = birthdays ?: return null

        val name = newPost.tags[0]
        val index = items.indexOfFirst { it.name.equals(name, ignoreCase = true) }

        if (index == -1) {
            return null
        }
        val birthdayInfo = items[index]
        items[index] = Birthday(
            birthdayInfo.name,
            birthdayInfo.birthdate,
            newPost.firstPhotoAltSize?.map { ImageSize(it.width, it.height, it.url) })

        return items[index]
    }

    fun clearBirthdays() {
        birthdays = null
    }
}

sealed class BirthdayPublisherModelResult {
    data class ListByDate(val command: Command<List<Birthday>?>) : BirthdayPublisherModelResult()
}
