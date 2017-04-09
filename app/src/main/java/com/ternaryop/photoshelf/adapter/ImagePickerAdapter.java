package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.extractor.ImageInfo;
import com.ternaryop.widget.CheckableImageView;

public class ImagePickerAdapter extends RecyclerView.Adapter<ImagePickerAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener  {
    private final ImageLoader imageLoader;
    private final Context context;
    private OnPhotoBrowseClickMultiChoice onPhotoBrowseClick;
	private final ArrayList<ImageInfo> items;
    private boolean showButtons;

    final SelectionArrayViewHolder<ViewHolder> selection = new SelectionArrayViewHolder<>(this);

	public ImagePickerAdapter(Context context) {
        this.context = context;
        imageLoader = new ImageLoader(context.getApplicationContext(), "picker", R.drawable.stub);
		items = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.gridview_photo_picker_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        View.OnClickListener listener = onPhotoBrowseClick == null ? null : this;
        holder.bindModel(items.get(position), imageLoader, showButtons);
        if (showButtons && onPhotoBrowseClick != null) {
            holder.setOnClickListeners(listener);
        }
        holder.setOnClickMultiChoiceListeners(listener, this);
        holder.thumbImage.setChecked(selection.isSelected(position));
    }

	@Override
	public int getItemCount() {
		return items.size();
	}

	public ImageInfo getItem(int position) {
		return items.get(position);
	}

	public void addAll(List<ImageInfo> list) {
		items.addAll(list);
        notifyDataSetChanged();
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

    public void setShowButtons(boolean showButtons) {
        this.showButtons = showButtons;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView showImageAction;
		final CheckableImageView thumbImage;
		final ImageView bgAction;

		public ViewHolder(View itemView) {
			super(itemView);
			bgAction = (ImageView)itemView.findViewById(R.id.bg_actions);
			showImageAction = (ImageView)itemView.findViewById(R.id.ic_show_image_action);
			thumbImage = (CheckableImageView)itemView.findViewById(R.id.thumbnail_image);
		}

        public void bindModel(ImageInfo imageInfo, ImageLoader imageLoader, boolean showButtons) {
            setVisibility(showButtons);
            displayImage(imageInfo, imageLoader);
        }

        private void setVisibility(boolean showButtons) {
            showImageAction.setVisibility(showButtons ? View.VISIBLE : View.INVISIBLE);
            bgAction.setVisibility(showButtons ? View.VISIBLE : View.INVISIBLE);
        }

        private void displayImage(ImageInfo imageInfo, ImageLoader imageLoader) {
            imageLoader.displayImage(imageInfo.getThumbnailUrl(), thumbImage);
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

	public void setOnPhotoBrowseClick(OnPhotoBrowseClickMultiChoice onPhotoBrowseClick) {
		this.onPhotoBrowseClick = onPhotoBrowseClick;
	}

    public List<ImageInfo> getSelectedItems() {
        ArrayList<ImageInfo> list = new ArrayList<>(getSelection().getItemCount());
        for (int pos : getSelection().getSelectedPositions()) {
            list.add(getItem(pos));
        }
        return list;
    }

    public Selection getSelection() {
        return selection;
    }

    public void setEmptyView(final View view) {
        if (view != null) {
            registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    view.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
}