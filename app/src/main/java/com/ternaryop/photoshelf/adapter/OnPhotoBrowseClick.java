package com.ternaryop.photoshelf.adapter;

import android.view.View;

public interface OnPhotoBrowseClick {
    public void onTagClick(int position);
    public void onThumbnailImageClick(int position);
    public void onOverflowClick(View view, int position);
}
