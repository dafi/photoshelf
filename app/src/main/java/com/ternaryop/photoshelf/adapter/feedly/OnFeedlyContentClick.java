package com.ternaryop.photoshelf.adapter.feedly;

public interface OnFeedlyContentClick {
    void onTitleClick(int position);
    void onToggleClick(int position, boolean checked);
}
