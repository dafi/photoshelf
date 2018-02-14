package com.ternaryop.photoshelf.adapter

/**
 * Allow adapter to listen for clicks used by multi choice views
 */
interface OnPhotoBrowseClickMultiChoice : OnPhotoBrowseClick {
    /**
     * The click occurs on entire row
     * @param position the clicked item position
     */
    fun onItemClick(position: Int)

    /**
     * The long click occurs on entire row
     * @param position the clicked item position
     */
    fun onItemLongClick(position: Int)
}
