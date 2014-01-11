package com.ternaryop.photoshelf.counter;

import android.content.Context;

import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.TumblrUtils;

public class DraftCountRetriever extends AbsCountRetriever {
    public DraftCountRetriever(Context context, String blogName) {
        super(context, blogName);
    }

    @Override
    protected Long getCount() {
        return TumblrUtils.getDraftCount(Tumblr.getSharedTumblr(getContext()), getBlogName());
    }    
}
