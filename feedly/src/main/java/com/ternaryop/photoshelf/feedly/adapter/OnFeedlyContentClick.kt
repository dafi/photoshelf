package com.ternaryop.photoshelf.feedly.adapter

interface OnFeedlyContentClick {
    fun onTitleClick(position: Int)
    fun onToggleClick(position: Int, checked: Boolean)
    fun onTagClick(position: Int)
}
