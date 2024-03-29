package com.ternaryop.photoshelf.birthday.browser.adapter

import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.Response
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.BirthdayResult
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.api.birthday.ListResult

class BirthdayShowFlags {
    private var flags = SHOW_ALL
    private val birthdayService = ApiManager.birthdayService()

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

    suspend fun find(pattern: String, month: Int, offset: Int, limit: Int): Response<BirthdayResult> {
        val params = FindParams(name = pattern, month = month + 1, offset = offset, limit = limit).toQueryMap()
        return when {
            isShowIgnored -> wrapBirthdayResult(birthdayService.findIgnored(params))
            isShowInSameDay -> birthdayService.findSameDay(params)
            isShowMissing -> wrapBirthdayResult((birthdayService.findMissingNames(params)))
            isWithoutPost -> birthdayService.findOrphans(params)
            else -> birthdayService.findByDate(params)
        }
    }

    private fun wrapBirthdayResult(response: Response<ListResult>): Response<BirthdayResult> {
        val listResult = response.response.names
        return Response(BirthdayResult(0, listResult.map { Birthday(it) }))
    }

    companion object {
        const val SHOW_ALL = 1
        const val SHOW_IGNORED = 1 shl 1
        const val SHOW_IN_SAME_DAY = 1 shl 2
        const val SHOW_MISSING = 1 shl 3
        const val SHOW_WITHOUT_POSTS = 1 shl 4
    }
}
