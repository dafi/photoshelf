package com.ternaryop.photoshelf.adapter;

import android.support.v7.widget.RecyclerView;

/**
 * Created by dave on 13/04/16.
 *
 * Notify changes to the adapter delegate
 */
public class SelectionArrayViewHolder<T extends RecyclerView.ViewHolder> extends SelectionArray {
    private RecyclerView.Adapter<T> adapter;

    public SelectionArrayViewHolder(RecyclerView.Adapter<T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void toggle(int position) {
        super.toggle(position);
        adapter.notifyItemChanged(position);
    }

    @Override
    public void setSelected(int position, boolean selected) {
        super.setSelected(position, selected);
        adapter.notifyItemChanged(position);
    }

    @Override
    public void clear() {
        super.clear();
        adapter.notifyDataSetChanged();
    }
}
