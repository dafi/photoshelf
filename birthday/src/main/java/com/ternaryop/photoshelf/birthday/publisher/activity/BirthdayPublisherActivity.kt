package com.ternaryop.photoshelf.birthday.publisher.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.activity.AbsPhotoShelfActivity
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.photoshelf.fragment.appFragmentFactory
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BirthdayPublisherActivity : AbsPhotoShelfActivity() {
    override val contentViewLayoutId: Int = R.layout.activity_birthday_publisher

    // no frame inside the layout so no need for contentFrameId
    override val contentFrameId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = appFragmentFactory
        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): Fragment? = null
}
