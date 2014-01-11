package com.ternaryop.photoshelf.counter;

import android.content.Context;
import android.widget.BaseAdapter;

import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.TumblrUtils;

public class DraftCountRetriever extends AbsCountRetriever {
    public DraftCountRetriever(Context context, String blogName, BaseAdapter adapter) {
        super(context, blogName, adapter);
    }

    @Override
    protected Long getCount() {
        return TumblrUtils.getDraftCount(Tumblr.getSharedTumblr(getContext()), getBlogName());
    }    
}
