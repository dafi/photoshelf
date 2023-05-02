package com.ternaryop.photoshelf.util.app

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED

fun PackageManager.showImagePickerOnShareMenu(packageName: String, enable: Boolean) {
    val state = if (enable) COMPONENT_ENABLED_STATE_ENABLED else COMPONENT_ENABLED_STATE_DISABLED

    setComponentEnabledSetting(
        ComponentName(packageName, "$packageName.alias.share.image.picker"),
        state,
        PackageManager.DONT_KILL_APP
    )
}
