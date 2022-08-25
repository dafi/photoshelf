package com.ternaryop.photoshelf

import android.content.Context
import com.ternaryop.tumblr.android.TumblrManager

interface PhotoShelfApplicationEnv {
    fun setup(context: Context)

    companion object {
        fun debugSetup(context: Context) {
            if (TumblrManager.isLogged(context)) {
                return
            }
            val appEnv = Class
                .forName("com.ternaryop.photoshelf.debug.DebugApplicationEnv")
                .newInstance() as PhotoShelfApplicationEnv
            appEnv.setup(context)
        }
    }
}
