package com.ternaryop.photoshelf.dialogs.mru;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by dave on 16/12/17.
 * MRU Holder
 */
class MRUViewHolder extends RecyclerView.ViewHolder {
    private final TextView textView;

    public MRUViewHolder(View itemView) {
        super(itemView);
        textView = itemView.findViewById(android.R.id.text1);
    }

    public void bindModel(String item) {
        textView.setText(item);
    }

    public void setOnClickListeners(View.OnClickListener listener) {
        final int position = getAdapterPosition();
        textView.setOnClickListener(listener);
        textView.setTag(position);
    }
}
