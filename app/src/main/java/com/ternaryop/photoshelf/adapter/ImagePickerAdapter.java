package com.ternaryop.photoshelf.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ternaryop.lazyimageloader.ImageLoader;
import com.ternaryop.photoshelf.ImageInfo;
import com.ternaryop.photoshelf.R;

public class ImagePickerAdapter extends ArrayAdapter<ImageInfo> implements View.OnClickListener {
    private ImageLoader imageLoader;
	private final LayoutInflater inflater;
	private OnPhotoPickerClick onPhotoPickerClick;

	public ImagePickerAdapter(Context context) {
		super(context, 0);
        imageLoader = new ImageLoader(context.getApplicationContext(), "picker", R.drawable.stub);
		inflater = LayoutInflater.from(context);
	}

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
		ViewGroup.LayoutParams imageLayoutParams = holder.thumbImage.getLayoutParams();
//		int minThumbnainWidth = 140;
//		int minThumbnainHeight = 140;
//		// convert from pixel to DIP
//		imageLayoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minThumbnainWidth, getContext().getResources().getDisplayMetrics());
//		imageLayoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minThumbnainHeight, getContext().getResources().getDisplayMetrics());
		imageLoader.displayImage(imageInfo.getThumbnailURL(), holder.thumbImage);

		return convertView;
	}

	public void onClick(final View v) {
		switch (v.getId()) {
			case R.id.caption:
				onPhotoPickerClick.viewClick((Integer) v.getTag());
				break;
		}
	}

	@Override
	public boolean isEnabled(int position) {
		return getItem(position).getSelector() != null;
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