package com.ternaryop.photoshelf.adapter;

/**
 * Allow adapter to listen for clicks used by multi choice views
 */
public interface OnPhotoBrowseClickMultiChoice extends OnPhotoBrowseClick {
    /**
     * The click occurs on entire row
     * @param position the clicked item position
     */
    void onItemClick(int position);

    /**
     * The long click occurs on entire row
     * @param position the clicked item position
     */
    void onItemLongClick(int position);
}
