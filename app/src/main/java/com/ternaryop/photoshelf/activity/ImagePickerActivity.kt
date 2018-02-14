package com.ternaryop.photoshelf.activity

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.net.Uri

import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.fragment.ImagePickerFragment

class ImagePickerActivity : AbsPhotoShelfActivity() {
    override val contentViewLayoutId: Int
        get() = R.layout.activity_image_picker

    override fun createFragment(): Fragment? = ImagePickerFragment()

    companion object {

        fun startImagePicker(context: Context, url: String) {
            val intent = Intent(context, ImagePickerActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(url)
            context.startActivity(intent, null)
        }
    }
}
