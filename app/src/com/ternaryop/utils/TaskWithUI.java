package com.ternaryop.utils;

/**
 * Used with AsyncTask to handle device rotation and ui recreating
 * @author dave
 *
 */
public interface TaskWithUI {
	public void recreateUI();
	public void dismiss();
    public boolean isRunning();
}
