package com.ternaryop.photoshelf.feedly.prefs

import com.ternaryop.photoshelf.feedly.dialog.FeedlySettingsData

fun FeedlyPrefs.toSettings() = FeedlySettingsData(maxFetchItemCount, newerThanHours, deleteOnRefresh)
fun FeedlyPrefs.fromSettings(settingsData: FeedlySettingsData) {
    saveOtherSettings(
        settingsData.maxFetchItemCount,
        settingsData.newerThanHours,
        settingsData.deleteOnRefresh
    )
}
