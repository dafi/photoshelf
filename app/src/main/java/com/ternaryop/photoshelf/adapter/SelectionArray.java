package com.ternaryop.photoshelf.adapter;

import android.util.SparseBooleanArray;

/**
 * Created by dave on 13/04/16.
 * Hold selection index state
 */
public class SelectionArray implements Selection {
    private SparseBooleanArray items = new SparseBooleanArray();

    public void toggle(int position) {
        if (items.get(position, false)) {
            items.delete(position);
        } else {
            items.put(position, true);
        }
    }

    public boolean isSelected(int position) {
        return items.get(position, false);
    }

    public int getItemCount() {
        return items.size();
    }

    public void setSelected(int position, boolean selected) {
        if (selected) {
            items.put(position, true);
        } else {
            items.delete(position);
        }
    }

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
}
