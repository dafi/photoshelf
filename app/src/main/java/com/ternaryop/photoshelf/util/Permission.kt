package com.ternaryop.photoshelf.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import androidx.annotation.StringRes
import com.ternaryop.utils.security.PermissionUtil

fun askNotificationsPermission(
    activity: Activity,
    @StringRes messageId: Int,
    requestCode: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionUtil.askPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
            requestCode,
            AlertDialog.Builder(activity).setMessage(messageId)
        )
    }
}
