package com.ternaryop.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * Used to select items in listView
 * 
 * @author dave
 * 
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {
    private boolean isChecked;
    private List<Checkable> checkableViews;

    public CheckableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public CheckableRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CheckableRelativeLayout(Context context, int checkableId) {
        super(context);
        init();
    }

    private void init() {
        checkableViews = new ArrayList<Checkable>();
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
        setSelected(isChecked);
        for (Checkable c : checkableViews) {
            c.setChecked(checked);
        }
    }

    public void toggle() {
        isChecked = !isChecked;
        setSelected(isChecked);
        for (Checkable c : checkableViews) {
            c.toggle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int childCount = this.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            findCheckableChildren(this.getChildAt(i));
        }
    }

    private void findCheckableChildren(View v) {
        if (v instanceof Checkable) {
            checkableViews.add((Checkable) v);
        }
        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            final int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                findCheckableChildren(vg.getChildAt(i));
            }
        }
    }
}