package com.ternaryop.photoshelf.tagphotobrowser.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.activity.AbsPhotoShelfActivity
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.photoshelf.tagphotobrowser.R
import com.ternaryop.photoshelf.tagphotobrowser.fragment.TagPhotoBrowserFragment
import org.koin.android.ext.android.inject

class TagPhotoBrowserActivity : AbsPhotoShelfActivity() {
    private lateinit var blogName: String

    override val contentViewLayoutId: Int = R.layout.activity_tag_photo_browser
    override val contentFrameId: Int = R.id.content_frame

    private val fragmentFactory: FragmentFactory by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val data = intent.extras?.getSerializable(EXTRA_TAG_PHOTO_BROWSER_DATA) as? TagPhotoBrowserData
        blogName = data?.blogName ?: checkNotNull(PreferenceManager.getDefaultSharedPreferences(this).selectedBlogName)
        supportFragmentManager.fragmentFactory = fragmentFactory

        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): Fragment? =
        supportFragmentManager.fragmentFactory.instantiate(
            classLoader, TagPhotoBrowserFragment::class.java.name)

    companion object {
        const val EXTRA_TAG_PHOTO_BROWSER_DATA = "com.ternaryop.photoshelf.extra.TAG_PHOTO_BROWSER_DATA"

        fun startPhotoBrowserActivity(context: Context, tagPhotoBrowserData: TagPhotoBrowserData) {
            context.startActivity(createIntent(context, tagPhotoBrowserData))
        }

        fun startPhotoBrowserActivityForResult(
            fragment: Fragment,
            requestCode: Int,
            tagPhotoBrowserData: TagPhotoBrowserData
        ) {
            fragment.startActivityForResult(createIntent(fragment.requireActivity(), tagPhotoBrowserData), requestCode)
        }

        private fun createIntent(context: Context, tagPhotoBrowserData: TagPhotoBrowserData): Intent {
            val intent = Intent(context, TagPhotoBrowserActivity::class.java)
            val bundle = Bundle()

            bundle.putSerializable(EXTRA_TAG_PHOTO_BROWSER_DATA, tagPhotoBrowserData)
            intent.putExtras(bundle)
            return intent
        }
    }
}
