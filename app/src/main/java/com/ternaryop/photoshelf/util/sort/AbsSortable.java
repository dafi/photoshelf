package com.ternaryop.photoshelf.util.sort;

/**
 * Created by dave on 12/06/16.
 * Common implementation
 */
public abstract class AbsSortable implements Sortable {
    private final boolean isDefaultAscending;
    private final int sortId;
    private boolean ascending;

    public AbsSortable(boolean isDefaultAscending, int sortId) {
        this.isDefaultAscending = isDefaultAscending;
        this.ascending = isDefaultAscending;
        this.sortId = sortId;
    }

    @Override
    public void setAscending(boolean direction) {
        this.ascending = direction;
    }

    @Override
    public boolean isAscending() {
        return this.ascending;
    }

    @Override
    public boolean isDefaultAscending() {
        return isDefaultAscending;
    }

    public int getSortId() {
        return sortId;
    }
}
