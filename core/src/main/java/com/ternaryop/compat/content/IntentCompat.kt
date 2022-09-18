package com.ternaryop.compat.content

import android.content.Intent
import android.os.Build
import java.io.Serializable

fun <T : Serializable> Intent.getSerializableExtraCompat(key: String?, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, clazz)
    } else {
        // suppress only the current known deprecated call
        // if future calls will become deprecated the warning will appear
        @Suppress("DEPRECATION")
        clazz.cast(getSerializableExtra(key))
    }
