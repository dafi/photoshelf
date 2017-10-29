package com.ternaryop.photoshelf.event;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import android.support.annotation.IntDef;

/**
 * Created by dave on 27/10/17.
 * Event posted when a count is available
 */

public class CounterEvent {
    @IntDef({NONE, BIRTHDAY, DRAFT, SCHEDULE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CounterType {}
    public static final int NONE = 0;
    public static final int BIRTHDAY = 1;
    public static final int DRAFT = 2;
    public static final int SCHEDULE = 3;

    private @CounterType int type;
    private long count;

    public CounterEvent() {
    }

    public CounterEvent(@CounterType int type, long count) {
        this.type = type;
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @CounterType
    public int getType() {
        return type;
    }

    public void setType(@CounterType int type) {
        this.type = type;
    }
}
