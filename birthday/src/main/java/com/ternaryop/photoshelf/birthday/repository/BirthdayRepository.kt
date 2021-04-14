package com.ternaryop.photoshelf.birthday.repository

import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.lifecycle.CommandMutableLiveData
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BirthdayRepository @Inject constructor() {
    private val _count = CommandMutableLiveData<Int>()
    val count = _count.asLiveData()

    suspend fun count() = _count.post(true) { birthdayCount() }

    private suspend fun birthdayCount(): Int {
        val now = Calendar.getInstance()
        return ApiManager.birthdayService().findByDate(
            FindParams(onlyTotal = true, month = now.month + 1, dayOfMonth = now.dayOfMonth).toQueryMap())
            .response.total.toInt()
    }

    fun clearCount() {
        _count.setLastValue(null, false)
    }
}
