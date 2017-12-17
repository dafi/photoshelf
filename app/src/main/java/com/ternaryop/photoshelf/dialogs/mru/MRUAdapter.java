package com.ternaryop.photoshelf.dialogs.mru;

import java.util.List;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by dave on 15/12/17.
 * The adapter used to selected and remove MRU items
 */

public class MRUAdapter extends RecyclerView.Adapter<MRUViewHolder> implements View.OnClickListener {
    private MRUDialog dialog;
    private final List<String> items;
    private OnMRUListener onMRUListener;

    public MRUAdapter(MRUDialog dialog, List<String> items) {
        this.dialog = dialog;
        this.items = items;
    }

    void setOnMRUListener(OnMRUListener onMRUListener) {
        this.onMRUListener = onMRUListener;
    }

    @Override
    public MRUViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MRUViewHolder(LayoutInflater.from(dialog.getActivity()).inflate(android.R.layout.simple_list_item_1, parent, false));
    }

    @Override
    public void onBindViewHolder(MRUViewHolder holder, int position) {
        final String item = items.get(position);
        holder.bindModel(item);

        holder.setOnClickListeners(onMRUListener == null ? null : this);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public String getItem(int position) {
        return items.get(position);
    }

    @Override
    public void onClick(View v) {
        final int[] positions = new int[]{(int) v.getTag()};
        onMRUListener.onItemsSelected(dialog, positions);
    }

    public void removeAt(int position) {
        onMRUListener.onItemDelete(dialog, position);
        items.remove(position);
        notifyItemRemoved(position);
    }
}
