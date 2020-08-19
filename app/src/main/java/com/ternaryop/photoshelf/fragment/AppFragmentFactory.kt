package com.ternaryop.photoshelf.fragment

import androidx.fragment.app.FragmentFactory
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.birthday.browser.fragment.BirthdayBrowserFragment
import com.ternaryop.photoshelf.birthday.publisher.fragment.BirthdayPublisherFragment
import com.ternaryop.photoshelf.feedly.fragment.FeedlyListFragment
import com.ternaryop.photoshelf.imagepicker.OnPublishAddBirthdate
import com.ternaryop.photoshelf.imagepicker.fragment.ImagePickerFragment
import com.ternaryop.photoshelf.tagnavigator.fragment.TagListFragment
import com.ternaryop.photoshelf.tagphotobrowser.fragment.TagPhotoBrowserFragment
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.ui.draft.fragment.DraftListFragment
import com.ternaryop.photoshelf.tumblr.ui.publish.fragment.PublishedPostsListFragment
import com.ternaryop.photoshelf.tumblr.ui.schedule.fragment.ScheduledListFragment
import javax.inject.Inject

class AppFragmentFactory @Inject constructor(
    private val ivas: ImageViewerActivityStarter,
    private val pd: TumblrPostDialog
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String) =
        when (loadFragmentClass(classLoader, className)) {
            DraftListFragment::class.java -> DraftListFragment(ivas, pd)
            ScheduledListFragment::class.java -> ScheduledListFragment(ivas, pd)
            PublishedPostsListFragment::class.java -> PublishedPostsListFragment(ivas, pd)
            TagPhotoBrowserFragment::class.java -> TagPhotoBrowserFragment(ivas, pd)
            TagListFragment::class.java -> TagListFragment(ivas)
            ImagePickerFragment::class.java -> ImagePickerFragment(ivas, pd, OnPublishAddBirthdate::class.java)
            BirthdayBrowserFragment::class.java -> BirthdayBrowserFragment(ivas)
            BirthdayPublisherFragment::class.java -> BirthdayPublisherFragment(ivas)
            FeedlyListFragment::class.java -> FeedlyListFragment(ivas)
            else -> super.instantiate(classLoader, className)
        }
}
