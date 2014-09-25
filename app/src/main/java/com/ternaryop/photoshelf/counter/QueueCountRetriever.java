package com.ternaryop.photoshelf.counter;

import android.content.Context;
import android.widget.BaseAdapter;

import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.TumblrUtils;

public class QueueCountRetriever extends AbsCountRetriever {
    public QueueCountRetriever(Context context, String blogName, BaseAdapter adapter) {
        super(context, blogName, adapter);
    }

    @Override
    protected Long getCount() {
        return TumblrUtils.getQueueCount(Tumblr.getSharedTumblr(getContext()), getBlogName());
    }    
}