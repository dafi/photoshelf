package com.ternaryop.tumblr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TumblrPhoto implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -5458693563884708164L;
    private String caption;
    private List<TumblrAltSize> altSizes;
    
    public TumblrPhoto() {
    }

    public TumblrPhoto(JSONObject json) throws JSONException {
        caption = json.getString("caption");
        JSONArray jsonSizes = json.getJSONArray("alt_sizes");
        altSizes = new ArrayList<TumblrAltSize>(jsonSizes.length());
        for (int i = 0; i < jsonSizes.length(); i++) {
            altSizes.add(new TumblrAltSize(jsonSizes.getJSONObject(i)));
        }
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public List<TumblrAltSize> getAltSizes() {
        return altSizes;
    }

    public void setAltSizes(List<TumblrAltSize> altSizes) {
        this.altSizes = altSizes;
    }
}
