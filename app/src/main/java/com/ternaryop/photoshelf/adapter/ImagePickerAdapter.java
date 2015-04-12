package com.ternaryop.photoshelf.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.ImageInfo;
import com.ternaryop.photoshelf.R;

public class ImagePickerAdapter extends BaseAdapter implements View.OnClickListener {
    private final ImageLoader imageLoader;
	private final LayoutInflater inflater;
	private OnPhotoPickerClick onPhotoPickerClick;
	private ArrayList<ImageInfo> items;

	public ImagePickerAdapter(Context context) {
        imageLoader = new ImageLoader(context.getApplicationContext(), "picker", R.drawable.stub);
		inflater = LayoutInflater.from(context);
		items = new ArrayList<>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.gridview_photo_picker_item, parent, false);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		if (onPhotoPickerClick != null) {
			holder.caption.setOnClickListener(this);
			holder.caption.setTag(position);
		}

		ImageInfo imageInfo = getItem(position);
		imageLoader.displayImage(imageInfo.getThumbnailURL(), holder.thumbImage);

		return convertView;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public ImageInfo getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItem(position).getSelector() != null;
	}

	public void addAll(List<ImageInfo> list) {
		items.addAll(list);
	}

	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.caption:
				onPhotoPickerClick.viewClick((Integer) v.getTag());
				break;
		}
	}

	private class ViewHolder {
		final TextView caption;
		final ImageView thumbImage;

		public ViewHolder(View vi) {
			caption = (TextView)vi.findViewById(R.id.caption);
			thumbImage = (ImageView)vi.findViewById(R.id.thumbnail_image);
		}
	}

	public void setOnPhotoPickerClick(OnPhotoPickerClick onPhotoPickerClick) {
		this.onPhotoPickerClick = onPhotoPickerClick;
	}

	public interface OnPhotoPickerClick {
		public void viewClick(int position);
	}
}