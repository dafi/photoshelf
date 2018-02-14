package com.ternaryop.photoshelf.adapter

import android.view.View

interface OnPhotoBrowseClick {
    fun onTagClick(position: Int, clickedTag: String)
    fun onThumbnailImageClick(position: Int)
    fun onOverflowClick(position: Int, view: View)
}
