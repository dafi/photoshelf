package com.ternaryop.photoshelf.imagepicker.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.ternaryop.photoshelf.activity.AbsPhotoShelfActivity
import com.ternaryop.photoshelf.imagepicker.R
import com.ternaryop.photoshelf.imagepicker.fragment.ImagePickerFragment
import org.koin.android.ext.android.inject

class ImagePickerActivity : AbsPhotoShelfActivity() {
    private val fragmentFactory: FragmentFactory by inject()

    override val contentViewLayoutId: Int = R.layout.activity_image_picker
    override val contentFrameId: Int = R.id.content_frame

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = fragmentFactory
        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): Fragment? =
        supportFragmentManager.fragmentFactory.instantiate(
            classLoader, ImagePickerFragment::class.java.name)

    companion object {

        fun startImagePicker(context: Context, url: String) {
            val intent = Intent(context, ImagePickerActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.data = Uri.parse(url)
            context.startActivity(intent, null)
        }
    }
}
