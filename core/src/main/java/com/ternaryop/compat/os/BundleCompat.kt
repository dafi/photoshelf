package com.ternaryop.compat.os

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

fun <T : Serializable> Bundle.getSerializableCompat(key: String?, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, clazz)
    } else {
        // suppress only the current known deprecated call
        // if future calls will become deprecated the warning will appear
        @Suppress("DEPRECATION")
        clazz.cast(getSerializable(key))
    }

fun <T : Parcelable> Bundle.getParcelableCompat(key: String?, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, clazz)
    } else {
        // suppress only the current known deprecated call
        // if future calls will become deprecated the warning will appear
        @Suppress("DEPRECATION")
        clazz.cast(getParcelable(key))
    }
