package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.widget.CheckableImageView;

public class GridViewPhotoAdapter extends RecyclerView.Adapter<GridViewPhotoAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {
    private final ImageLoader imageLoader;
    private final Context context;
    private boolean showButtons;

    private OnPhotoBrowseClickMultiChoice onPhotoBrowseClick;
    private final ArrayList<Pair<Birthday, TumblrPhotoPost>> items;

    final SelectionArrayViewHolder<ViewHolder> selection = new SelectionArrayViewHolder<>(this);

    public GridViewPhotoAdapter(Context context, String prefix) {
        this.context = context;
        imageLoader = new ImageLoader(context.getApplicationContext(), prefix, R.drawable.stub);
        items = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.gridview_photo_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Pair<Birthday, TumblrPhotoPost> item = getItem(position);

        holder.bindModel(item, imageLoader, showButtons);

        View.OnClickListener listener = onPhotoBrowseClick == null ? null : this;
        if (showButtons && onPhotoBrowseClick != null) {
            holder.setOnClickListeners(listener);
        }
        holder.setVisibility(showButtons);
        holder.thumbImage.setChecked(selection.isSelected(position));

        holder.setOnClickMultiChoiceListeners(listener, this);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Pair<Birthday, TumblrPhotoPost> getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void clear() {
        items.clear();
    }

    public void addAll(List<Pair<Birthday, TumblrPhotoPost>> posts) {
        items.addAll(posts);
    }

    public void updatePostByTag(TumblrPhotoPost newPost, boolean notifyChange) {
        String name = newPost.getTags().get(0);

        for (int i = 0; i < getItemCount(); i++) {
            Pair<Birthday, TumblrPhotoPost> item = getItem(i);
            TumblrPhotoPost post = item.second;
            if (post.getTags().get(0).equalsIgnoreCase(name)) {
                items.set(i, Pair.create(item.first, newPost));

                if (notifyChange) {
                    notifyDataSetChanged();
                }
                break;
            }
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.ic_show_image_action:
                onPhotoBrowseClick.onThumbnailImageClick((Integer) v.getTag());
                break;
            case R.id.list_row:
                onPhotoBrowseClick.onItemClick((Integer) v.getTag());
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        onPhotoBrowseClick.onItemLongClick((Integer) v.getTag());
        return true;
    }

    public void setOnPhotoBrowseClick(OnPhotoBrowseClickMultiChoice onPhotoBrowseClick) {
        this.onPhotoBrowseClick = onPhotoBrowseClick;
    }

    public boolean isShowButtons() {
        return showButtons;
    }

    public void setShowButtons(boolean showButtons) {
        this.showButtons = showButtons;
    }

    public List<Pair<Birthday, TumblrPhotoPost>> getSelectedItems() {
        ArrayList<Pair<Birthday, TumblrPhotoPost>> list = new ArrayList<>(getSelection().getItemCount());
        for (int pos : getSelection().getSelectedPositions()) {
            list.add(getItem(pos));
        }
        return list;
    }

    public Selection getSelection() {
        return selection;
    }

    public void selectAll() {
        getSelection().setSelectedRange(0, getItemCount(), true);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView caption;
        final CheckableImageView thumbImage;
        final ImageView bgAction;
        final ImageView showImageAction;

        public ViewHolder(View vi) {
            super(vi);
            caption = (TextView)vi.findViewById(R.id.caption);
            thumbImage = (CheckableImageView)vi.findViewById(R.id.thumbnail_image);
            bgAction = (ImageView)vi.findViewById(R.id.bg_actions);
            showImageAction = (ImageView)vi.findViewById(R.id.ic_show_image_action);
        }

        public void bindModel(Pair<Birthday, TumblrPhotoPost> item, ImageLoader imageLoader, boolean showButtons) {
            setVisibility(showButtons);
            updateTitles(item);
            displayImage(item.second, imageLoader);
        }

        private void updateTitles(Pair<Birthday, TumblrPhotoPost> item) {
            caption.setText(String.format(Locale.US, "%s, %d", item.second.getTags().get(0), getAge(item.first)));
        }

        public int getAge(Birthday birthday) {
            return Calendar.getInstance().get(Calendar.YEAR) - birthday.getBirthDateCalendar().get(Calendar.YEAR);
        }

        private void setVisibility(boolean showButtons) {
            showImageAction.setVisibility(showButtons ? View.VISIBLE : View.INVISIBLE);
            bgAction.setVisibility(showButtons ? View.VISIBLE : View.INVISIBLE);
        }

        private void displayImage(TumblrPhotoPost post, ImageLoader imageLoader) {
            imageLoader.displayImage(post.getClosestPhotoByWidth(250).getUrl(), thumbImage);
        }

        public void setOnClickListeners(View.OnClickListener listener) {
            showImageAction.setOnClickListener(listener);
            showImageAction.setTag(getAdapterPosition());
        }

        public void setOnClickMultiChoiceListeners(View.OnClickListener listener, View.OnLongClickListener longClickListener) {
            if (listener != null) {
                final int position = getAdapterPosition();
                itemView.setOnClickListener(listener);
                itemView.setOnLongClickListener(longClickListener);
                itemView.setLongClickable(true);
                itemView.setTag(position);
            }
        }
    }
}