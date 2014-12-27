package com.ternaryop.photoshelf.fragment;

import android.support.v7.widget.Toolbar;

import com.ternaryop.photoshelf.AppSupport;


/**
 * Communicate activity status to fragment
 * @author dave
 *
 */
public interface FragmentActivityStatus {
    public boolean isDrawerOpen();
    public AppSupport getAppSupport();
    public Toolbar getToolbar();
}
