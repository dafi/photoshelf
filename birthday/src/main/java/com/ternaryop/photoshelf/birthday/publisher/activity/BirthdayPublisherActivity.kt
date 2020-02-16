package com.ternaryop.photoshelf.birthday.publisher.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.ternaryop.photoshelf.activity.AbsPhotoShelfActivity
import com.ternaryop.photoshelf.birthday.R
import org.koin.android.ext.android.inject

class BirthdayPublisherActivity : AbsPhotoShelfActivity() {
    private val fragmentFactory: FragmentFactory by inject()

    override val contentViewLayoutId: Int = R.layout.activity_birthday_publisher

    // no frame inside the layout so no need for contentFrameId
    override val contentFrameId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = fragmentFactory
        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): Fragment? = null
}
