package com.ternaryop.photoshelf.util.sort;

/**
 * Created by dave on 12/06/16.
 * Sort items and toggle the direction
 */
public interface Sortable {
    boolean isDefaultAscending();
    void setAscending(boolean direction);
    boolean isAscending();
    void sort();
}
