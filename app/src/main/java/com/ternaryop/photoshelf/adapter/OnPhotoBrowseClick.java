package com.ternaryop.photoshelf.adapter;

import android.view.View;

public interface OnPhotoBrowseClick {
    void onTagClick(int position, String clickedTag);
    void onThumbnailImageClick(int position);
    void onOverflowClick(int position, View view);
}
