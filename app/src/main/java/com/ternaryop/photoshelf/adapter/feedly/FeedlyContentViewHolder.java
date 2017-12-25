package com.ternaryop.photoshelf.adapter.feedly;

import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.ternaryop.feedly.FeedlyContent;
import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.R;

import static com.ternaryop.photoshelf.adapter.PostStyle.INDEX_TITLE_STYLE;
import static com.ternaryop.photoshelf.adapter.PostStyle.INDEX_TITLE_TEXT_COLOR;
import static com.ternaryop.photoshelf.adapter.PostStyle.INDEX_VIEW_BACKGROUND;

/**
 * Created by dave on 24/02/17.
 * The ViewHolder used by the Feedly list
 */
class FeedlyContentViewHolder extends RecyclerView.ViewHolder {
    public static final int FAVICON_SIZE = 16;
    final TextView title;
    final TextView subtitle;
    final CheckBox checkbox;
    final ImageView faviconImage;
    final View sidebar;

    public FeedlyContentViewHolder(View itemView) {
        super(itemView);
        sidebar = itemView.findViewById(R.id.sidebar);
        title = itemView.findViewById(android.R.id.text1);
        subtitle = itemView.findViewById(android.R.id.text2);
        checkbox = itemView.findViewById(android.R.id.checkbox);
        faviconImage = itemView.findViewById(R.id.thumbnail_image);
    }

    public void bindModel(FeedlyContentDelegate content, ImageLoader imageLoader) {
        // setting listener to null resolved the lost of unchecked state
        // http://stackoverflow.com/a/32428115/195893
        checkbox.setOnCheckedChangeListener(null);
        updateCheckbox(content);
        updateTitles(content);
        updateItemColors(content);
        displayImage(content, imageLoader, FAVICON_SIZE);
    }

    private void displayImage(FeedlyContentDelegate content, ImageLoader imageLoader, int size) {
        setImageDimension(size);

        if (content.getDomain() != null) {
            String faviconUrl = String.format("https://www.google.com/s2/favicons?domain_url=%s", content.getDomain());
            imageLoader.displayImage(faviconUrl, faviconImage);
        }
    }

    private void setImageDimension(int size) {
        ViewGroup.LayoutParams imageLayoutParams = faviconImage.getLayoutParams();
        // convert from pixel to DIP
        imageLayoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, itemView.getContext().getResources().getDisplayMetrics());
        imageLayoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, itemView.getContext().getResources().getDisplayMetrics());
    }

    private void updateCheckbox(FeedlyContentDelegate content) {
        sidebar.setVisibility(View.VISIBLE);
        checkbox.setButtonDrawable(R.drawable.checkbox_bookmark);
        checkbox.setChecked(content.checked);
    }

    private void updateTitles(FeedlyContentDelegate content) {
        title.setText(content.getTitle());

        subtitle.setText(String.format("%s / %s / %s", content.getOrigin().getTitle(),
                content.getActionTimestampAsString(itemView.getContext()),
                content.getLastPublishTimestampAsString(itemView.getContext())));
    }

    private void updateItemColors(FeedlyContentDelegate content) {
        if (content.lastPublishTimestamp < 0) {
            setColors(R.array.post_never);
        } else {
            setColors(R.array.post_normal);
        }
    }
    @SuppressWarnings("ResourceType")
    private void setColors(int resArray) {
        TypedArray array = itemView.getContext().getResources().obtainTypedArray(resArray);
        itemView.setBackground(array.getDrawable(INDEX_VIEW_BACKGROUND));

        final int titleStyle = array.getResourceId(INDEX_TITLE_STYLE, 0);
        if (Build.VERSION.SDK_INT < 23) {
            title.setTextAppearance(itemView.getContext(), titleStyle);
        } else {
            title.setTextAppearance(titleStyle);
        }
        subtitle.setTextColor(array.getColorStateList(INDEX_TITLE_TEXT_COLOR));
        array.recycle();
    }

    @SuppressWarnings("unused")
    public void setOnClickListeners(FeedlyContent content, View.OnClickListener listener) {
        if (listener == null) {
            return;
        }
        final int position = getAdapterPosition();
        itemView.setOnClickListener(listener);
        itemView.setTag(position);
    }

    @SuppressWarnings("unused")
    public void setOnCheckedChangeListener(FeedlyContentDelegate content, CompoundButton.OnCheckedChangeListener listener) {
        if (listener == null) {
            return;
        }
        final int position = getAdapterPosition();
        checkbox.setOnCheckedChangeListener(listener);
        checkbox.setTag(position);
    }
}
