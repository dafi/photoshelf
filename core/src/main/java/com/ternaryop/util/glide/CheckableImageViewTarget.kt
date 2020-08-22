package com.ternaryop.util.glide

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.ternaryop.widget.CheckableImageView
import java.lang.ref.WeakReference

/**
 * Ensure the checked status is updated only after the imageView contains a valid image (e.g. load is completed)
 */
class CheckableImageViewTarget(imageView: CheckableImageView, var checked: Boolean) : CustomTarget<Drawable>() {
    private val imageViewRef: WeakReference<CheckableImageView> = WeakReference(imageView)

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        imageViewRef.get()?.also { imageView ->
            imageView.setImageDrawable(resource)
            imageView.isChecked = checked
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        imageViewRef.clear()
    }
}
