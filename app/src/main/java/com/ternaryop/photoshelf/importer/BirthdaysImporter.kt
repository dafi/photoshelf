package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.BirthdayService.Companion.MAX_BIRTHDAY_COUNT
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.db.Importer

/**
 * Created by dave on 24/03/18.
 * Import and export birthdays
 */
typealias StringProgressInfo = Importer.SimpleImportProgressInfo<String>

suspend fun Importer.importMissingBirthdaysFromWeb(progress: (StringProgressInfo) -> Unit): StringProgressInfo {
    val params = FindParams(offset = 0, limit = MAX_BIRTHDAY_COUNT).toQueryMap()
    val info = Importer.SimpleImportProgressInfo<String>()

    val missing = ApiManager.birthdayService().findMissingNames(params).response
    info.max = missing.names.size
    info.list.addAll(missing.names)
    missing.names.forEach { name ->
        val nameResult = ApiManager.birthdayService().getByName(name, true).response
        if (nameResult.isNew) {
            info.items.add(nameResult.birthday.name)
        }
        ++info.progress
        progress(info)
    }
    return info
}
