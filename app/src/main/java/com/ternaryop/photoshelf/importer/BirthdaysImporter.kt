package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.api.birthday.BirthdayService.Companion.MAX_BIRTHDAY_COUNT
import com.ternaryop.photoshelf.api.birthday.FindParams
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
    val params = FindParams(offset = 0, limit = MAX_BIRTHDAY_COUNT).toQueryMap()

    return ApiManager.birthdayService(context).findMissingNames(params).toObservable()
        .flatMap {
            Observable.generate<StringProgressInfo, StringProgressInfo>(Callable {
                    Importer.SimpleImportProgressInfo(it.response.names.size, it.response.names)
            },
                BiConsumer { iterator: StringProgressInfo, emitter: Emitter<StringProgressInfo> ->
                    if (iterator.progress < iterator.max) {
                        val name = iterator.list[iterator.progress]

                        ApiManager.birthdayService(context)
                            .getByName(name, true)
                            .subscribe { _ -> iterator.items.add(name) }

                        ++iterator.progress
                        emitter.onNext(iterator)
                    } else {
                        emitter.onNext(iterator)
                        emitter.onComplete()
                    }
                })

        }
}
