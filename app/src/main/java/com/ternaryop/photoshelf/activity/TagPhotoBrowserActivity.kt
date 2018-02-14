package com.ternaryop.photoshelf.activity

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ternaryop.photoshelf.EXTRA_ALLOW_SEARCH
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_BROWSE_TAG
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.fragment.TagPhotoBrowserFragment

class TagPhotoBrowserActivity : AbsPhotoShelfActivity() {
    override val contentViewLayoutId: Int
        get() = R.layout.activity_tag_photo_browser

    override fun createFragment(): Fragment? = TagPhotoBrowserFragment()

    companion object {

        fun startPhotoBrowserActivity(context: Context, blogName: String, postTag: String, allowSearch: Boolean) {
            context.startActivity(createIntent(context, blogName, postTag, allowSearch))
        }

        fun startPhotoBrowserActivityForResult(fragment: Fragment, blogName: String, postTag: String, requestCode: Int, allowSearch: Boolean) {
            fragment.startActivityForResult(createIntent(fragment.activity, blogName, postTag, allowSearch), requestCode)
        }

        private fun createIntent(context: Context, blogName: String, postTag: String, allowSearch: Boolean): Intent {
            val intent = Intent(context, TagPhotoBrowserActivity::class.java)
            val bundle = Bundle()

            bundle.putString(EXTRA_BLOG_NAME, blogName)
            bundle.putString(EXTRA_BROWSE_TAG, postTag)
            bundle.putBoolean(EXTRA_ALLOW_SEARCH, allowSearch)
            intent.putExtras(bundle)
            return intent
        }
    }
}
