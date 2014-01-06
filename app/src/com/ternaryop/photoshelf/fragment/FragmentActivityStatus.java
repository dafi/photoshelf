package com.ternaryop.photoshelf.fragment;

import com.ternaryop.photoshelf.AppSupport;


/**
 * Communicate activity status to fragment
 * @author dave
 *
 */
public interface FragmentActivityStatus {
    public boolean isDrawerOpen();
    public AppSupport getAppSupport();
}
