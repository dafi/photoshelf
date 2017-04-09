package com.ternaryop.photoshelf.extractor;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dave on 01/04/17.
 * The mapping object used to hold the Gallery result
 */

public class ImageGallery {
    private String domain;
    private String title;
    private List<ImageInfo> imageInfoList;

    public ImageGallery() {
    }

    public ImageGallery(JSONObject json) throws JSONException {
        domain= json.getString("domain");
        title = json.getString("title");
        final JSONArray array = json.getJSONArray("gallery");
        imageInfoList = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            imageInfoList.add(new ImageInfo(array.getJSONObject(i)));
        }
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<ImageInfo> getImageInfoList() {
        return imageInfoList;
    }

    public void setImageInfoList(List<ImageInfo> imageInfoList) {
        this.imageInfoList = imageInfoList;
    }
}
