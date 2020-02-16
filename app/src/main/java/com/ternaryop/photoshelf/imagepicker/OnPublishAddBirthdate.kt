package com.ternaryop.photoshelf.imagepicker

import android.content.Context
import com.ternaryop.photoshelf.birthday.util.addBirthdate
import com.ternaryop.photoshelf.imagepicker.service.OnPublish

class OnPublishAddBirthdate : OnPublish {
    override suspend fun publish(context: Context, tags: List<String>) {
        tags.firstOrNull()?.also { name -> addBirthdate(context, name) }
    }
}
