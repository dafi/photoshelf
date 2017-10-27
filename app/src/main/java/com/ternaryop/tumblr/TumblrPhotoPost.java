package com.ternaryop.tumblr;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TumblrPhotoPost extends TumblrPost {
    /**
     * 
     */
    private static final long serialVersionUID = 8910912231608271421L;
    
    private List<TumblrPhoto> photos;
    private String caption;

    public TumblrPhotoPost() {
        super();
    }

    public TumblrPhotoPost(JSONObject json) throws JSONException {
        super(json);

        caption = json.getString("caption");

        JSONArray jsonPhotos = json.getJSONArray("photos");
        photos = new ArrayList<>(jsonPhotos.length());
        for (int i = 0; i < jsonPhotos.length(); i++) {
            photos.add(new TumblrPhoto(jsonPhotos.getJSONObject(i)));
        }
    }

    public TumblrPhotoPost(TumblrPhotoPost photoPost) {
        super(photoPost);
        photos = photoPost.photos;
        caption = photoPost.caption;
    }

    public List<TumblrPhoto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<TumblrPhoto> photos) {
        this.photos = photos;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public List<TumblrAltSize> getFirstPhotoAltSize() {
        return photos.size() > 0 ? photos.get(0).getAltSizes() : null;
    }
    
    public TumblrAltSize getClosestPhotoByWidth(int width) {
        for (TumblrAltSize p : photos.get(0).getAltSizes()) {
            // some images don't have the exact (==) width so we get closest width (<=)
            if (p.getWidth() <= width) {
                return p;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return caption;
    }
}
