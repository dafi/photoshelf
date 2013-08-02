package com.ternaryop.phototumblrshare;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.ternaryop.utils.ImageUtils;

public class ImageAdapter extends ArrayAdapter<ImageInfo> {
	private LayoutInflater mInflater;
	private List<ImageInfo> selectedList = new ArrayList<ImageInfo>();

	public ImageAdapter(Context c) {
		super(c, 0);
        mInflater = LayoutInflater.from(c);
	}

	static class ViewHolder {
        ImageView image;
    }

	Set<String> loadingUrl = new HashSet<String>();
	
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.list_item_icon_text, null);
			holder = new ViewHolder();
			holder.image = (ImageView) convertView.findViewById(R.id.text);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		ImageInfo imageInfo = getItem(position);

		System.out.println("called view for " + imageInfo.thumbnailURL);
		if (imageInfo.bitmap == null) {
			new LoadImage(holder, position, imageInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void)null);
		} else {
			holder.image.setImageBitmap(imageInfo.bitmap);
		}
        holder.image.setAlpha(imageInfo.selected ? 100 : 255);

        return convertView;
	}

	private class LoadImage extends AsyncTask<Void, Void, Bitmap> {
		private ImageInfo imageInfo;
		private ViewHolder holder;
		private int position;

		public LoadImage(ViewHolder holder, int position, ImageInfo imageInfo) {
			this.holder = holder;
			this.position = position;
			this.imageInfo = imageInfo;
		}

		@Override
		protected Bitmap doInBackground(Void... b) {
			try {
				// http://stackoverflow.com/a/11323297/195893
				return ImageUtils.readImage(imageInfo.thumbnailURL);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				imageInfo.bitmap = bitmap;
				holder.image.setImageBitmap(bitmap);
			}
		}
	}
	
	public void toogleItem(int position) {
		ImageInfo item = getItem(position);

		if (item.selected) {
			item.selected = false;
			selectedList.remove(item);
		} else {
			item.selected = true;
			selectedList.add(item);
		}
	}

	public List<ImageInfo> getSelectedItems() {
		return selectedList;
	}
	
	public void unselectAll() {
		for (ImageInfo imageInfo : selectedList) {
			imageInfo.selected = false;
		}
		selectedList.clear();
	}
}