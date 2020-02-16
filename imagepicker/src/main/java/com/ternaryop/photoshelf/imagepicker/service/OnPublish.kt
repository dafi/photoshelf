package com.ternaryop.photoshelf.imagepicker.service

import android.content.Context

interface OnPublish {
    suspend fun publish(context: Context, tags: List<String>)
}
