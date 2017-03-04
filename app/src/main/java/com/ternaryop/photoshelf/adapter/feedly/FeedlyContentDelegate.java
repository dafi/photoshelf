package com.ternaryop.photoshelf.adapter.feedly;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.ternaryop.feedly.FeedlyContent;
import com.ternaryop.photoshelf.R;
import org.joda.time.format.DateTimeFormat;

/**
 * Contains fields related to UI state
 */
public class FeedlyContentDelegate extends FeedlyContent {
    private final FeedlyContent delegated;
    public boolean checked = true;
    public long lastPublishTimestamp;

    public FeedlyContentDelegate(FeedlyContent delegated) {
        this.delegated = delegated;
    }

    @Override
    public String getId() {
        return delegated.getId();
    }

    @Override
    public String getTitle() {
        return delegated.getTitle();
    }

    @Override
    public String getOriginId() {
        return delegated.getOriginId();
    }

    @Override
    public long getActionTimestamp() {
        return delegated.getActionTimestamp();
    }

    @Override
    public void setId(String id) {
        delegated.setId(id);
    }

    @Override
    public void setTitle(String title) {
        delegated.setTitle(title);
    }

    @Override
    public void setOriginId(String originId) {
        delegated.setOriginId(originId);
    }

    @Override
    public void setActionTimestamp(long actionTimestamp) {
        delegated.setActionTimestamp(actionTimestamp);
    }

    @Override
    public Origin getOrigin() {
        return delegated.getOrigin();
    }

    @Override
    public void setOrigin(Origin origin) {
        delegated.setOrigin(origin);
    }

    @NonNull
    public String getLastPublishTimestampAsString(Context context) {
        if (lastPublishTimestamp <= 0) {
            return context.getString(R.string.never_published);
        }
        return DateTimeFormat.forPattern("dd/MM/yyyy").print(lastPublishTimestamp * 1000);
    }

    @NonNull
    public String getActionTimestampAsString(Context context) {
        return DateUtils.getRelativeTimeSpanString(context, getActionTimestamp()).toString();
    }
}
