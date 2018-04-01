package com.ternaryop.photoshelf.adapter.feedly

interface OnFeedlyContentClick {
    fun onTitleClick(position: Int)
    fun onToggleClick(position: Int, checked: Boolean)
    fun onTagClick(position: Int)
}
