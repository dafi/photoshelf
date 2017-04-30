package com.ternaryop.photoshelf.adapter;

import android.util.SparseBooleanArray;

/**
 * Created by dave on 13/04/16.
 * Hold selection index state
 */
public class SelectionArray implements Selection {
    private final SparseBooleanArray items = new SparseBooleanArray();

    @Override
    public void toggle(int position) {
        if (items.get(position, false)) {
            items.delete(position);
        } else {
            items.put(position, true);
        }
    }

    @Override
    public boolean isSelected(int position) {
        return items.get(position, false);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void setSelected(int position, boolean selected) {
        if (selected) {
            items.put(position, true);
        } else {
            items.delete(position);
        }
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public int[] getSelectedPositions() {
        int[] positions = new int[items.size()];

        for (int i = 0; i < items.size(); i++) {
            positions[i] = items.keyAt(i);
        }

        return positions;
    }

    @Override
    public void setSelectedRange(int start, int end, boolean selected) {
        for (int i = start; i < end; i++) {
            setSelected(i, selected);
        }
    }

}
