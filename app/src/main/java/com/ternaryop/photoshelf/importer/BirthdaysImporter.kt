package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.api.birthday.BirthdayManager
import com.ternaryop.photoshelf.api.birthday.BirthdayManager.Companion.MAX_BIRTHDAY_COUNT
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.util.network.ApiManager
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.functions.BiConsumer
import java.util.concurrent.Callable

/**
 * Created by dave on 24/03/18.
 * Import and export birthdays
 */
typealias StringProgressInfo = Importer.SimpleImportProgressInfo<String>

fun Importer.importMissingBirthdaysFromWeb(blogName: String): Observable<StringProgressInfo> {
    return Observable.generate<StringProgressInfo, StringProgressInfo>(Callable {
        val params = BirthdayManager.FindParams(offset = 0, limit = MAX_BIRTHDAY_COUNT)
        val names = ApiManager.birthdayManager(context).findMissingNames(params)
        Importer.SimpleImportProgressInfo(names.size, names)
        },
        BiConsumer { iterator: StringProgressInfo, emitter: Emitter<StringProgressInfo> ->
            if (iterator.progress < iterator.max) {
                val name = iterator.list[iterator.progress]

                BirthdayUtils.searchBirthday(context, name)?.also { iterator.items.add(name) }

                ++iterator.progress
                emitter.onNext(iterator)
            } else {
                emitter.onNext(iterator)
                emitter.onComplete()
            }
    })
}
