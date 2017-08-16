package com.ternaryop.photoshelf.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Set the background color for the item while an animation is running
 * Created by dave on 13/08/17.
 */

public class ColorItemDecoration extends RecyclerView.ItemDecoration {
    private final ColorDrawable background;

    public ColorItemDecoration() {
        background = new ColorDrawable(Color.TRANSPARENT);
    }

    public void setColor(@ColorInt int color) {
        background.setColor(color);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);

        // change the color only while animation is running
        if (parent.getItemAnimator() == null || !parent.getItemAnimator().isRunning()) {
            return;
        }
        final int left = 0;
        final int right = parent.getWidth();
        final int childCount = parent.getLayoutManager().getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getLayoutManager().getChildAt(i);
            final int top = child.getTop();
            final int bottom = child.getBottom();

            background.setBounds(left, top, right, bottom);
            background.draw(c);
        }
    }
}
