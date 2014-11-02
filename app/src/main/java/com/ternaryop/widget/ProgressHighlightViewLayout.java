package com.ternaryop.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.widget.LinearLayout;

/**
 * Contains views which are highlighted (by default using fade animation) when the progress increments
 */
public class ProgressHighlightViewLayout extends LinearLayout {
    private int progress;
    private View currentView;
    private ProgressViewListener progressViewListener;
    private Animation animation;

    /**
     * Simple constructor to use when creating a ProgressHighlightViewLayout from code.
     * @param context
     */
    public ProgressHighlightViewLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating ProgressHighlightViewLayout from XML.
     * @param context
     * @param attrs
     */
    public ProgressHighlightViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public View getCurrentView() {
        return currentView;
    }

    public void startProgress() {
        progress = 0;
        updateProgress();
    }

    public void incrementProgress() {
        if (progress < getChildCount()) {
            ++progress;
            updateProgress();
        }
    }

    private void updateProgress() {
        unselectView();
        if (progress < getChildCount()) {
            currentView = getChildAt(progress);
            selectView();
        }
    }

    private void selectView() {
        if (animation != null) {
            currentView.startAnimation(animation);
        }
        if (progressViewListener != null) {
            progressViewListener.onSelectView(currentView);
        }
    }

    private void unselectView() {
        if (currentView != null) {
            if (animation != null) {
                currentView.clearAnimation();
            }
            if (progressViewListener != null) {
                progressViewListener.onUnselectView(currentView);
            }
        }
    }

    public void stopProgress() {
        unselectView();
    }

    public ProgressViewListener getProgressViewListener() {
        return progressViewListener;
    }

    public void setProgressViewListener(ProgressViewListener progressViewListener) {
        this.progressViewListener = progressViewListener;
    }

    public int getProgress() {
        return progress;
    }

    public int getMax() {
        return getChildCount();
    }

    @Override
    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public static interface ProgressViewListener {
        public void onSelectView(View view);
        public void onUnselectView(View view);
    }
}
