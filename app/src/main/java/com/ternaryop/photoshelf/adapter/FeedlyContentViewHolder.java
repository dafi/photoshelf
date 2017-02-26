package com.ternaryop.photoshelf.adapter;

import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.ternaryop.feedly.FeedlyContent;
import com.ternaryop.photoshelf.R;

/**
 * Created by dave on 24/02/17.
 * The ViewHolder used by the Feedly list
 */
class FeedlyContentViewHolder extends RecyclerView.ViewHolder {
    final TextView title;
    final TextView subtitle;
    final CheckBox checkbox;

    public FeedlyContentViewHolder(View itemView) {
        super(itemView);
        title = (TextView) itemView.findViewById(android.R.id.text1);
        subtitle = (TextView) itemView.findViewById(android.R.id.text2);
        checkbox = (CheckBox) itemView.findViewById(android.R.id.checkbox);
    }

    public void bindModel(FeedlyContentAdapter.FeedlyContentDelegate content) {
        updateCheckbox(content);
        updateTitles(content);
    }

    private void updateCheckbox(FeedlyContentAdapter.FeedlyContentDelegate content) {
        checkbox.setButtonDrawable(R.drawable.checkbox_bookmark);
        checkbox.setVisibility(View.VISIBLE);
        checkbox.setChecked(content.checked);
    }

    private void updateTitles(FeedlyContent content) {
        title.setText(content.getTitle());
        if (Build.VERSION.SDK_INT < 23) {
            title.setTextAppearance(itemView.getContext(), R.style.FeedlyContentTitle);
        } else {
            title.setTextAppearance(R.style.FeedlyContentTitle);
        }
        subtitle.setText(content.getOrigin().getTitle() + " / " + android.text.format.DateUtils.getRelativeTimeSpanString(itemView.getContext(), content.getActionTimestamp()));
    }

    public void setOnClickListeners(FeedlyContent content, View.OnClickListener listener) {
        if (listener == null) {
            return;
        }
        final int position = getAdapterPosition();
        itemView.setOnClickListener(listener);
        itemView.setTag(position);
    }

    public void setOnCheckedChangeListener(FeedlyContent content, CompoundButton.OnCheckedChangeListener listener) {
        if (listener == null) {
            return;
        }
        final int position = getAdapterPosition();
        checkbox.setOnCheckedChangeListener(listener);
        checkbox.setTag(position);

    }
}
