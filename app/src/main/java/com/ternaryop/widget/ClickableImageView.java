package com.ternaryop.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class ClickableImageView extends ImageView implements OnTouchListener {

    public ClickableImageView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        setOnTouchListener(this);
    }

    public ClickableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);
    }

    public ClickableImageView(Context context, int checkableId) {
        super(context);
        setOnTouchListener(this);
    }
    
    public ClickableImageView(Context context) {
        super(context);
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                //overlay is black with transparency of 0x77 (119)
                getDrawable().setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                invalidate();
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                //clear the overlay
                getDrawable().clearColorFilter();
                invalidate();
                break;
            }
        }

        // allow target view to handle click
        return false;
    }
}
