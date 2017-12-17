package com.ternaryop.photoshelf.dialogs.mru;

/**
 * Created by dave on 16/12/17.
 * MRU actions
 */

public interface OnMRUListener {
    void onItemsSelected(MRUDialog dialog, int[] position);
    void onItemDelete(MRUDialog dialog, int position);
}
