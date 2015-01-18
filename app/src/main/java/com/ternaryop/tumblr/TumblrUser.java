package com.ternaryop.tumblr;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dave on 18/01/15.
 * Contain tumblr user details
 */
public class TumblrUser implements Serializable {
    private String name;
    private boolean following;
    private String url;
    private long updated;

    public TumblrUser() {
    }

    public TumblrUser(JSONObject json) throws JSONException {
        name = json.getString("name");
        following = json.getBoolean("following");
        url = json.getString("url");
        updated = json.getLong("updated");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFollowing() {
        return following;
    }

    public void setFollowing(boolean following) {
        this.following = following;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        return name + " is following? " + following + " last update " + updated + " url " + url;
    }
}
