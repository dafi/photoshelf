package com.ternaryop.photoshelf.tagphotobrowser.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ternaryop.compat.os.getSerializableCompat
import com.ternaryop.photoshelf.activity.AbsPhotoShelfActivity
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.photoshelf.fragment.appFragmentFactory
import com.ternaryop.photoshelf.tagphotobrowser.R
import com.ternaryop.photoshelf.tagphotobrowser.fragment.TagPhotoBrowserFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagPhotoBrowserActivity : AbsPhotoShelfActivity() {
    private lateinit var blogName: String

    override val contentViewLayoutId: Int = com.ternaryop.photoshelf.tagnavigator.R.layout.activity_tag_photo_browser
    override val contentFrameId: Int = com.ternaryop.photoshelf.tumblr.dialog.R.id.content_frame

    override fun onCreate(savedInstanceState: Bundle?) {
        val data = tagPhotoBrowserData(intent.extras)
        blogName = data?.blogName ?: checkNotNull(PreferenceManager.getDefaultSharedPreferences(this).selectedBlogName)
        supportFragmentManager.fragmentFactory = appFragmentFactory

        super.onCreate(savedInstanceState)
    }

    override fun createFragment(): Fragment =
        supportFragmentManager.fragmentFactory.instantiate(
            classLoader, TagPhotoBrowserFragment::class.java.name
        )

    companion object {
        private const val EXTRA_TAG_PHOTO_BROWSER_DATA = "com.ternaryop.photoshelf.extra.TAG_PHOTO_BROWSER_DATA"
        private const val EXTRA_RETURN_SELECTED_POST = "com.ternaryop.photoshelf.extra.EXTRA_RETURN_SELECTED_POST"

        fun createIntent(
            context: Context,
            tagPhotoBrowserData: TagPhotoBrowserData,
            returnSelectedPost: Boolean = false
        ): Intent {
            val intent = Intent(context, TagPhotoBrowserActivity::class.java)
            val bundle = Bundle()

            bundle.putSerializable(EXTRA_TAG_PHOTO_BROWSER_DATA, tagPhotoBrowserData)
            bundle.putBoolean(EXTRA_RETURN_SELECTED_POST, returnSelectedPost)
            intent.putExtras(bundle)
            return intent
        }

        fun tagPhotoBrowserData(bundle: Bundle?) =
            bundle?.getSerializableCompat(EXTRA_TAG_PHOTO_BROWSER_DATA, TagPhotoBrowserData::class.java)

        fun returnSelectedPost(bundle: Bundle?, defaultValue: Boolean = false) =
            bundle?.getBoolean(EXTRA_RETURN_SELECTED_POST, defaultValue) ?: defaultValue
    }
}
