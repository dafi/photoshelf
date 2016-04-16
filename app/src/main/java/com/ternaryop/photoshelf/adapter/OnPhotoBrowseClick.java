package com.ternaryop.photoshelf.adapter;

import android.view.View;

public interface OnPhotoBrowseClick {
    void onTagClick(int position);
    void onThumbnailImageClick(int position);
    void onOverflowClick(View view, int position);
}
