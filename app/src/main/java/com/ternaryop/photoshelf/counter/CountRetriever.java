package com.ternaryop.photoshelf.counter;

import android.widget.TextView;

public interface CountRetriever {
    public void updateCount(TextView textView);
    public String getBlogName();
    public void setBlogName(String blogName);
}
