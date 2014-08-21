package com.ternaryop.photoshelf.adapter;

import android.view.View;

public interface OnPhotoBrowseClick {
	public void onPhotoBrowseClick(PhotoShelfPost post);
	public void onThumbnailImageClick(PhotoShelfPost post);
    public void onOverflowClick(View view, PhotoShelfPost post);
}
