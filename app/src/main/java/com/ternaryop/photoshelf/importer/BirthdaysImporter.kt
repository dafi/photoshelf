package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.api.birthday.BirthdayService.Companion.MAX_BIRTHDAY_COUNT
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.api.ApiManager
import io.reactivex.Observable

/**
 * Created by dave on 24/03/18.
 * Import and export birthdays
 */
typealias StringProgressInfo = Importer.SimpleImportProgressInfo<String>

fun Importer.importMissingBirthdaysFromWeb(): Observable<StringProgressInfo> {
    val params = FindParams(offset = 0, limit = MAX_BIRTHDAY_COUNT).toQueryMap()
    val info = Importer.SimpleImportProgressInfo<String>()

    return ApiManager.birthdayService(context).findMissingNames(params)
        .flatMapObservable {
            info.max = it.response.names.size
            info.list.addAll(it.response.names)
            Observable.fromIterable(it.response.names)
        }
        .flatMapSingle { name -> ApiManager.birthdayService(context).getByName(name, true) }
        .doOnNext { response ->
            val nameResult  = response.response
            if (nameResult.isNew) {
                info.items.add(nameResult.birthday.name)
            }
            ++info.progress
        }
        .map { info }
}
