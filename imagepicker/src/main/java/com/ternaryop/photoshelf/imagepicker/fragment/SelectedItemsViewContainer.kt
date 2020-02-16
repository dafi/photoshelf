package com.ternaryop.photoshelf.imagepicker.fragment

import android.content.Context
import android.view.animation.AnticipateOvershootInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.imagepicker.R
import com.ternaryop.photoshelf.imagepicker.adapter.ImagePickerAdapter

internal class SelectedItemsViewContainer(
    val context: Context,
    private val constraintLayout: ConstraintLayout,
    selectionListView: RecyclerView
) {
    val adapter = ImagePickerAdapter(context)
    private var isVisible = false
    private val constraintSet = ConstraintSet()

    init {
        selectionListView.adapter = adapter
        selectionListView.setHasFixedSize(true)
        selectionListView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        constraintSet.clone(context, R.layout.fragment_image_picker)
    }

    fun toggleVisibility() = show(!isVisible)

    fun show(show: Boolean) {
        isVisible = show
        if (isVisible) {
            constraintSet.clear(R.id.selectedItems, ConstraintSet.TOP)
        } else {
            constraintSet.connect(R.id.selectedItems, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        }

        val transition = ChangeBounds()
        transition.interpolator = AnticipateOvershootInterpolator(1.0f)
        transition.duration = ANIMATION_DURATION_MILLIS

        TransitionManager.beginDelayedTransition(constraintLayout, transition)

        constraintSet.applyTo(constraintLayout)
    }

    fun updateList(items: List<ImageInfo>) {
        adapter.clear()
        if (items.isEmpty()) {
            show(false)
        } else {
            adapter.addAll(items)
        }
    }

    companion object {
        private const val ANIMATION_DURATION_MILLIS = 1000L
    }
}
