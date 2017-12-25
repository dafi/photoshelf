package com.ternaryop.photoshelf.adapter;

import java.util.List;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.tumblr.TumblrAltSize;
import com.ternaryop.utils.StringUtils;
import org.joda.time.format.DateTimeFormat;

import static com.ternaryop.photoshelf.adapter.PostStyle.*;

/**
 * Created by dave on 13/04/16.
 * The ViewHolder used by Photo objects
 */
class PhotoViewHolder extends RecyclerView.ViewHolder {
    final TextView timeDesc;
    final TextView caption;
    final ImageView thumbImage;
    final ImageView menu;
    final TextView noteCountText;
    final ViewGroup tagsContainer;
    private PhotoShelfPost post;

    public PhotoViewHolder(View itemView) {
        super(itemView);
        timeDesc = itemView.findViewById(R.id.time_desc);
        caption = itemView.findViewById(R.id.caption);
        menu = itemView.findViewById(R.id.menu);
        noteCountText = itemView.findViewById(R.id.note_count);

        thumbImage = itemView.findViewById(R.id.thumbnail_image);
        tagsContainer = itemView.findViewById(R.id.tags_container);
    }

    public void bindModel(PhotoShelfPost post, ImageLoader imageLoader, int thumbnailWidth, boolean showUploadTime) {
        this.post = post;
        updateTitles(showUploadTime);
        displayImage(imageLoader, thumbnailWidth);
        setupTags();
        updateItemColors();
    }

    private void setupTags() {
        final List<String> tags = post.getTags();
        int tagsCount = tags.size();
        int viewCount = tagsContainer.getChildCount();
        int delta = tagsCount - viewCount;

        if (delta < 0) {
            for (int i = tagsCount; i < viewCount; i++) {
                tagsContainer.getChildAt(i).setVisibility(View.GONE);
            }
        } else if (delta > 0) {
            for (int i = 0; i < delta; i++) {
                tagsContainer.addView(LayoutInflater.from(tagsContainer.getContext()).inflate(R.layout.other_tag, null));
            }
        }
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            final TextView view = (TextView) tagsContainer.getChildAt(i);
            view.setId(R.id.tag_text_view);
            view.setText(String.format("#%s", tag));
            view.setTag(tag);
            view.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("ResourceType")
    private void setColors(int resArray) {
        TypedArray array = itemView.getContext().getResources().obtainTypedArray(resArray);
        itemView.setBackground(array.getDrawable(INDEX_VIEW_BACKGROUND));
        timeDesc.setTextColor(array.getColorStateList(INDEX_TIME_DESC_TEXT_COLOR));
        caption.setTextColor(array.getColorStateList(INDEX_CAPTION_TEXT_COLOR));
        menu.setImageTintList(array.getColorStateList(INDEX_MENU_OVERFLOW_COLOR));
        noteCountText.setTextColor(array.getColorStateList(INDEX_CAPTION_TEXT_COLOR));

        final ColorStateList titleTextColor = array.getColorStateList(INDEX_TITLE_TEXT_COLOR);
        for (int i = 0; i < tagsContainer.getChildCount(); i++) {
            final TextView view = (TextView) tagsContainer.getChildAt(i);
            view.setTextColor(titleTextColor);
        }

        array.recycle();
    }

    private void displayImage(ImageLoader imageLoader, int thumbnailWidth) {
        TumblrAltSize altSize = post.getClosestPhotoByWidth(thumbnailWidth);
        setImageDimension(altSize, thumbnailWidth);

        imageLoader.displayImage(altSize.getUrl(), thumbImage);
    }

    private void setImageDimension(TumblrAltSize altSize, int thumbnailWidth) {
        ViewGroup.LayoutParams imageLayoutParams = thumbImage.getLayoutParams();
        int minThumbnailWidth = Math.max(thumbnailWidth, altSize.getWidth());
        // convert from pixel to DIP
        imageLayoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minThumbnailWidth, itemView.getContext().getResources().getDisplayMetrics());
        imageLayoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, altSize.getHeight(), itemView.getContext().getResources().getDisplayMetrics());
    }

    private void updateTitles(boolean showUploadTime) {
        caption.setText(Html.fromHtml(StringUtils.stripHtmlTags("a|img|p|br", post.getCaption())));
        timeDesc.setText(post.getLastPublishedTimestampAsString());
        // use noteCountText for both uploadTime and notes
        if (showUploadTime) {
            showUploadTime();
        } else {
            updateNote();
        }
    }

    private void showUploadTime() {
        noteCountText.setVisibility(View.VISIBLE);
        noteCountText.setText(itemView.getResources().getString(R.string.uploaded_at_time, DateTimeFormat.forPattern("dd/MM/yyyy HH:mm").print(post.getTimestamp() * 1000)));
    }

    private void updateNote() {
        int noteCount = (int) post.getNoteCount();
        if (noteCount > 0) {
            noteCountText.setVisibility(View.VISIBLE);
            noteCountText.setText(itemView.getContext().getResources().getQuantityString(
                    R.plurals.note_title,
                    noteCount,
                    noteCount));
        } else {
            noteCountText.setVisibility(View.GONE);
        }
    }

    public void setOnClickListeners(View.OnClickListener listener) {
        if (listener != null) {
            setTagsClickListener(listener);
            final int position = getAdapterPosition();
            thumbImage.setOnClickListener(listener);
            thumbImage.setTag(position);

            menu.setOnClickListener(listener);
            menu.setTag(position);
        }
    }

    public void setOnClickMultiChoiceListeners(View.OnClickListener listener, View.OnLongClickListener longClickListener) {
        if (listener != null) {
            itemView.setOnClickListener(listener);
            itemView.setOnLongClickListener(longClickListener);
            itemView.setLongClickable(true);
            itemView.setTag(getAdapterPosition());
        }
    }

    private void updateItemColors() {
        switch (post.getScheduleTimeType()) {
            case POST_PUBLISH_NEVER:
                setColors(R.array.post_never);
                break;
            case POST_PUBLISH_FUTURE:
                setColors(R.array.post_future);
                break;
            default:
                setColors(post.getGroupId() % 2 == 0 ? R.array.post_even : R.array.post_odd);
                break;
        }
    }

    private void setTagsClickListener(View.OnClickListener listener) {
        tagsContainer.setTag(getAdapterPosition());
        for (int i = 0; i < tagsContainer.getChildCount(); i++) {
            tagsContainer.getChildAt(i).setOnClickListener(listener);
        }
    }
}
