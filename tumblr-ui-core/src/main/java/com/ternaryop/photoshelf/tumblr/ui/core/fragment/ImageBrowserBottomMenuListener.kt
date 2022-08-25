package com.ternaryop.photoshelf.tumblr.ui.core.fragment

import android.view.Menu
import android.view.MenuItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.ImageViewerData
import com.ternaryop.photoshelf.fragment.BottomMenuListener
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.tumblr.TumblrPhotoPost

class ImageBrowserBottomMenuListener(
    private val imageViewerActivityStarter: ImageViewerActivityStarter,
) : BottomMenuListener {
    override val title: String?
        get() = null
    override val menuId: Int
        get() = R.menu.image_size_browser

    override fun setupMenu(menu: Menu, sheet: BottomSheetDialogFragment) {
        post(sheet)?.firstPhotoAltSize?.forEachIndexed { index, altSize ->
            menu.add(Menu.NONE, index, Menu.NONE, altSize.toString())
        }
    }

    override fun onItemSelected(item: MenuItem, sheet: BottomSheetDialogFragment) {
        val post = post(sheet) ?: return
        val list = post.firstPhotoAltSize ?: return
        val altSize = list[item.itemId]
        imageViewerActivityStarter.startImageViewer(
            sheet.requireContext(),
            ImageViewerData(altSize.url, post.caption, post.firstTag)
        )
    }

    private fun post(sheet: BottomSheetDialogFragment) =
        sheet.arguments?.getSerializable(ARG_PHOTO_POST) as? TumblrPhotoPost

    companion object {
        const val ARG_PHOTO_POST = "image.browser.photo.post"
    }
}
