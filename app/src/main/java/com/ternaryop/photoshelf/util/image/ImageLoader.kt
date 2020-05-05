package com.ternaryop.photoshelf.util.image

import android.content.Context
import coil.Coil
import coil.ImageLoader
import com.ternaryop.photoshelf.R

object ImageLoader {
    fun setup(context: Context) {
        Coil.setDefaultImageLoader {
            ImageLoader(context) {
                error(R.drawable.ic_sync_problem_black_24dp)
            }
        }
    }
}
