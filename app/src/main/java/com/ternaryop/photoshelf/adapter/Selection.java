package com.ternaryop.photoshelf.adapter;

/**
 * Created by dave on 13/04/16.
 * The selection list
 */
public interface Selection {
    boolean isSelected(int position);
    int getItemCount();
    void toggle(int position);
    void setSelected(int position, boolean selected);
    void clear();

    int[] getSelectedPositions();
}
