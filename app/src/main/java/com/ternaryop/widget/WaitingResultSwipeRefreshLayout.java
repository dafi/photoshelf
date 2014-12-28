package com.ternaryop.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;

/**
 * Created by dave on 13/09/14.
 * Add a flag to check if waiting result is in progress, the isRefreshing flag can't be used because it is set to true
 * before the onRefresh() is called so testing its value inside the onRefresh() is wrong
 */
public class WaitingResultSwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout {
    private boolean waitingResult;

    public WaitingResultSwipeRefreshLayout(Context context) {
        super(context);
        adjustPosition(context);
    }

    public WaitingResultSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        adjustPosition(context);
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

    /**
     * Starting since android.support.v4 v 21.0.0 the swipe refresh is shown under the toolbar (ie not visible!!)
     * so we adjust its position based on actionBar size
     * @param context
     */
    private void adjustPosition(Context context) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typedValue, true)) {
            setProgressViewOffset(false, 0, getResources().getDimensionPixelSize(typedValue.resourceId));
        }
    }
}
