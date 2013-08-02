package com.ternaryop.phototumblrshare;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class ImageInfo {
	public String thumbnailURL;
	public String imageURL;
	public ImageView imageView;
	public int position;
	public boolean selected;
	public Bitmap bitmap;

	public ImageInfo(String thumbnailURL, String imageURL) {
		this.thumbnailURL = thumbnailURL;
		this.imageURL = imageURL;
	}

	@Override
	public String toString() {
		return thumbnailURL + " pos " + position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((imageURL == null) ? 0 : imageURL.hashCode());
		result = prime * result
				+ ((thumbnailURL == null) ? 0 : thumbnailURL.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageInfo other = (ImageInfo) obj;
		if (imageURL == null) {
			if (other.imageURL != null)
				return false;
		} else if (!imageURL.equals(other.imageURL))
			return false;
		if (thumbnailURL == null) {
			if (other.thumbnailURL != null)
				return false;
		} else if (!thumbnailURL.equals(other.thumbnailURL))
			return false;
		return true;
	}
}