package com.ternaryop.utils.image;

import android.content.Context;
import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;
import com.ternaryop.utils.ImageUtils;

/**
 * Picasso transformation to scale bitmap for default display
 */
public class ScaleForDefaultDisplayTransformer implements Transformation {
    private Context context;

    public ScaleForDefaultDisplayTransformer(Context context) {
        this.context = context;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap scaled = ImageUtils.scaleBitmapForDefaultDisplay(context, source);
        if (scaled != source) {
            source.recycle();
        }
        return scaled;
    }

    @Override
    public String key() {
        return "ScaleForDefaultDisplayTransformer";
    }
}
