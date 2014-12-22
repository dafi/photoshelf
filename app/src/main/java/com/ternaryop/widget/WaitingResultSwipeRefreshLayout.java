package com.ternaryop.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

/**
 * Created by dave on 13/09/14.
 * Add a flag to check if waiting result is in progress, the isRefreshing flag can't be used because it is set to true
 * before the onRefresh() is called so testing its value inside the onRefresh() is wrong
 */
public class WaitingResultSwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout {
    private boolean waitingResult;

    public WaitingResultSwipeRefreshLayout(Context context) {
        super(context);
    }

    public WaitingResultSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setColorScheme(int arrayResId) {
        final TypedArray colorScheme = getResources().obtainTypedArray(arrayResId);
        setColorSchemeResources(
                colorScheme.getResourceId(0, 0),
                colorScheme.getResourceId(1, 0),
                colorScheme.getResourceId(2, 0),
                colorScheme.getResourceId(3, 0)
        );
    }

    public boolean isWaitingResult() {
        return waitingResult;
    }

    public void setWaitingResult(boolean waitingResult) {
        this.waitingResult = waitingResult;
    }

    public void setRefreshingAndWaintingResult(boolean refreshing) {
        setRefreshing(refreshing);
        setWaitingResult(refreshing);
    }
}
