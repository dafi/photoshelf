package com.ternaryop.photoshelf.activity

import android.support.v4.app.Fragment

import com.ternaryop.photoshelf.R

class BirthdaysPublisherActivity : AbsPhotoShelfActivity() {
    override val contentViewLayoutId: Int
        get() = R.layout.activity_birthdays_publisher

    override fun createFragment(): Fragment? = null
}
