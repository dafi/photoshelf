package com.ternaryop.photoshelf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.net.Uri;

import com.ternaryop.utils.URLUtils;

/**
 * Created by dave on 12/09/16.
 * Contains urls or paths to images
 */
public class ImageCollector {
    private final ArrayList<Uri> imageUrls = new ArrayList<>();
    private final Context context;
    private boolean useFile;

    public ImageCollector(Context context) {
        this.context = context;
    }

    public void addUrl(String url) throws IOException {
        if (useFile) {
            File file = new File(context.getCacheDir(), String.valueOf(url.hashCode()));
            try (FileOutputStream fos = new FileOutputStream(file)) {
                URLUtils.saveURL(url, fos);
                imageUrls.add(Uri.fromFile(file));
            }
        } else {
            imageUrls.add(Uri.parse(url));
        }
    }

    public ArrayList<Uri> getImageUrls() {
        return imageUrls;
    }

    public void setCollectFiles(boolean useFile) {
        this.useFile = useFile;
    }
}
