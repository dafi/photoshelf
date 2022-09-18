package com.ternaryop.compat.content

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.getPackageInfoCompat(packageName: String, flags: Long): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
    } else {
        // suppress only the current known deprecated call
        // if future calls will become deprecated the warning will appear
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags.toInt())
    }

fun PackageManager.getActivityInfoCompat(component: ComponentName, flags: Long): ActivityInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getActivityInfo(component, PackageManager.ComponentInfoFlags.of(flags))
    } else {
        // suppress only the current known deprecated call
        // if future calls will become deprecated the warning will appear
        @Suppress("DEPRECATION")
        getActivityInfo(component, flags.toInt())
    }
