package com.ternaryop.phototumblrshare.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

import com.ternaryop.phototumblrshare.R;

public class ClickableTextView extends TextView implements OnTouchListener {
	public ClickableTextView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		setup();
	}

	public ClickableTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setup();
	}

	public ClickableTextView(Context context, int checkableId) {
		super(context);
		setup();
	}
	
	public ClickableTextView(Context context) {
		super(context);
		setup();
	}

	private void setup() {
		setOnTouchListener(this);
	}

	@Override
    public boolean onTouch(View v, MotionEvent event) {
        if (hasOnClickListeners()) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setTextColor(getContext().getResources().getColor(R.color.link_text_color_clicked));
                setBackgroundColor(getContext().getResources().getColor(R.color.link_text_color_clicked_bg));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setTextColor(getContext().getResources().getColor(R.color.link_text_color));
                setBackgroundColor(getContext().getResources().getColor(R.color.link_text_color_bg));
                break;
            }
        }

        // allow target view to handle click
        return false;
    }
}
