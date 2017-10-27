package com.ternaryop.photoshelf.fragment;

import android.support.v7.widget.Toolbar;

import com.ternaryop.photoshelf.AppSupport;


/**
 * Communicate activity status to fragment
 * @author dave
 *
 */
public interface FragmentActivityStatus {
    boolean isDrawerOpen();
    AppSupport getAppSupport();
    Toolbar getToolbar();
}
