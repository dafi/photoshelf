package com.ternaryop.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * Used to select items in listView
 * @author dave
 *
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {
	private boolean isChecked;

	public CheckableRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public CheckableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckableRelativeLayout(Context context, int checkableId) {
		super(context);
	}

	public boolean isChecked() {
		return isChecked;
	}

	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
		setSelected(isChecked);
	}

	public void toggle() {
		setChecked(!isChecked);
	}
}