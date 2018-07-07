package com.ternaryop.photoshelf.adapter.birthday

import android.content.Context
import com.ternaryop.photoshelf.api.birthday.BirthdayManager
import com.ternaryop.photoshelf.util.network.ApiManager
import io.reactivex.Observable

class BirthdayShowFlags(context: Context) {
    private var flags = SHOW_ALL
    private val birthdayManager = ApiManager.birthdayManager(context)

    val isShowIgnored: Boolean
        get() = flags and SHOW_IGNORED != 0

    val isShowInSameDay: Boolean
        get() = flags and SHOW_IN_SAME_DAY != 0

    val isShowMissing: Boolean
        get() = flags and SHOW_MISSING != 0

    val isWithoutPost: Boolean
        get() = flags and SHOW_WITHOUT_POSTS != 0

    fun isOn(value: Int): Boolean = flags and value != 0

    @Suppress("ComplexMethod")
    fun setFlag(value: Int, show: Boolean) {
        flags = when {
            // SHOW_ALL is the default value and it can't be hidden
            value and SHOW_ALL != 0 -> SHOW_ALL
            value and SHOW_IGNORED != 0 -> if (show) SHOW_IGNORED else SHOW_ALL
            value and SHOW_IN_SAME_DAY != 0 -> if (show) SHOW_IN_SAME_DAY else SHOW_ALL
            value and SHOW_MISSING != 0 -> if (show) SHOW_MISSING else SHOW_ALL
            value and SHOW_WITHOUT_POSTS != 0 -> if (show) SHOW_WITHOUT_POSTS else SHOW_ALL
            else -> throw AssertionError("value $value not supported")
        }
    }

    fun find(pattern: String, month: Int, offset: Int, limit: Int): Observable<BirthdayManager.BirthdayResult> {
        val params = BirthdayManager.FindParams(name = pattern, month = month + 1, offset = offset, limit = limit)
        return Observable.fromCallable {
            when {
                isShowIgnored -> wrapBirthdayResult(birthdayManager.findIgnored(params))
                isShowInSameDay -> birthdayManager.findSameDay(params)
                isShowMissing -> wrapBirthdayResult(birthdayManager.findMissingNames(params))
                isWithoutPost -> birthdayManager.findOrphans(params)
                else -> birthdayManager.findByDate(params)
            }
        }
    }

    private fun wrapBirthdayResult(list: List<String>): BirthdayManager.BirthdayResult {
        return BirthdayManager.BirthdayResult(0, list.map { BirthdayManager.Birthday(it, nullDate) })
    }

    companion object {
        const val SHOW_ALL = 1
        const val SHOW_IGNORED = 1 shl 1
        const val SHOW_IN_SAME_DAY = 1 shl 2
        const val SHOW_MISSING = 1 shl 3
        const val SHOW_WITHOUT_POSTS = 1 shl 4
    }
}