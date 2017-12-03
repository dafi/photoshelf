package com.ternaryop.photoshelf.adapter;

import java.util.List;

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

/**
 * Created by dave on 13/04/16.
 * The ViewHolder used by Photo objects
 */
class PhotoViewHolder extends RecyclerView.ViewHolder {
    final TextView title;
    final TextView timeDesc;
    final TextView caption;
    final ImageView thumbImage;
    final ImageView menu;
    final TextView noteCountText;
    final ViewGroup otherTagsContainer;

    public PhotoViewHolder(View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title_textview);
        timeDesc = itemView.findViewById(R.id.time_desc);
        caption = itemView.findViewById(R.id.caption);
        menu = itemView.findViewById(R.id.menu);
        noteCountText = itemView.findViewById(R.id.note_count);

        thumbImage = itemView.findViewById(R.id.thumbnail_image);
        otherTagsContainer = itemView.findViewById(R.id.other_tags_container);
    }

    public void bindModel(PhotoShelfPost post, ImageLoader imageLoader, int thumbnailWidth, boolean showUploadTime) {
        updateTitles(post, showUploadTime);
        displayImage(post, imageLoader, thumbnailWidth);
        setupOtherTags(post);
        updateItemColors(post);
    }

    private void setupOtherTags(PhotoShelfPost post) {
        final List<String> tags = post.getTags();
        int tagsCount = tags.isEmpty() ? 0 : tags.size() - 1;
        int viewCount = otherTagsContainer.getChildCount();
        int delta = tagsCount - viewCount;

        if (delta < 0) {
            for (int i = tagsCount; i < viewCount; i++) {
                otherTagsContainer.getChildAt(i).setVisibility(View.GONE);
            }
        } else if (delta > 0) {
            for (int i = 0; i < delta; i++) {
                otherTagsContainer.addView(LayoutInflater.from(otherTagsContainer.getContext()).inflate(R.layout.other_tag, null));
            }
        }
        for (int i = 1; i < tags.size(); i++) {
            String tag = tags.get(i);
            final TextView view = (TextView) otherTagsContainer.getChildAt(i - 1);
            view.setId(R.id.other_tag_text_view);
            view.setText(String.format("#%s", tag));
            view.setTag(tag);
            view.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("ResourceType")
    private void setColors(int resArray) {
        TypedArray array = itemView.getContext().getResources().obtainTypedArray(resArray);
        itemView.setBackground(array.getDrawable(0));
        title.setTextColor(array.getColorStateList(1));
        timeDesc.setTextColor(array.getColorStateList(2));
        caption.setTextColor(array.getColorStateList(3));
        menu.setImageDrawable(array.getDrawable(4));
        noteCountText.setTextColor(array.getColorStateList(3));

        for (int i = 0; i < otherTagsContainer.getChildCount(); i++) {
            final TextView view = (TextView) otherTagsContainer.getChildAt(i);
            view.setTextColor(array.getColorStateList(3));
        }

        array.recycle();
    }

    private void displayImage(PhotoShelfPost post, ImageLoader imageLoader, int thumbnailWidth) {
        TumblrAltSize altSize = post.getClosestPhotoByWidth(thumbnailWidth);
        setImageDimension(altSize, thumbnailWidth);

        imageLoader.displayImage(altSize.getUrl(), thumbImage);
    }

    private void setImageDimension(TumblrAltSize altSize, int thumbnailWidth) {
        ViewGroup.LayoutParams imageLayoutParams = thumbImage.getLayoutParams();
        int minThumbnainWidth = Math.max(thumbnailWidth, altSize.getWidth());
        // convert from pixel to DIP
        imageLayoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minThumbnainWidth, itemView.getContext().getResources().getDisplayMetrics());
        imageLayoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, altSize.getHeight(), itemView.getContext().getResources().getDisplayMetrics());
    }

    private void updateTitles(PhotoShelfPost post, boolean showUploadTime) {
        title.setText(post.getFirstTag());
        caption.setText(Html.fromHtml(StringUtils.stripHtmlTags("a|img|p|br", post.getCaption())));
        timeDesc.setText(post.getLastPublishedTimestampAsString());
        // use noteCountText for both uploadTime and notes
        if (showUploadTime) {
            showUploadTime(post);
        } else {
            updateNote(post);
        }
    }

    private void showUploadTime(PhotoShelfPost post) {
        noteCountText.setVisibility(View.VISIBLE);
        noteCountText.setText(itemView.getResources().getString(R.string.uploaded_at_time, DateTimeFormat.forPattern("dd/MM/yyyy HH:mm").print(post.getTimestamp() * 1000)));
    }

    private void updateNote(PhotoShelfPost post) {
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

    public void setOnClickListeners(PhotoShelfPost post, View.OnClickListener listener) {
        if (listener != null) {
            final int position = getAdapterPosition();
            if (post.getScheduleTimeType() == PhotoShelfPost.ScheduleTime.POST_PUBLISH_NEVER) {
                title.setOnClickListener(null);
            } else {
                title.setOnClickListener(listener);
                title.setTag(position);
            }

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

    private void updateItemColors(PhotoShelfPost post) {
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

    public void setOtherTagsClickListener(View.OnClickListener listener) {
        if (listener != null) {
            otherTagsContainer.setTag(getAdapterPosition());
            for (int i = 0; i < otherTagsContainer.getChildCount(); i++) {
                otherTagsContainer.getChildAt(i).setOnClickListener(listener);
            }
        }
    }
}
