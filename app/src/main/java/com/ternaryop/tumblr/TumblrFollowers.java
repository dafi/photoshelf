package com.ternaryop.tumblr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dave on 18/01/15.
 * Info about followers
 */
public class TumblrFollowers implements Serializable {
    private long totalUsers;
    private ArrayList<TumblrUser> usersList;

    public TumblrFollowers() {
        usersList = new ArrayList<>();
    }

    public void add(JSONObject json) throws JSONException {
        totalUsers = json.getLong("total_users");
        JSONArray users = json.getJSONArray("users");
        for (int i = 0; i < users.length(); i++) {
            usersList.add(new TumblrUser(users.getJSONObject(i)));
        }
    }

    public List<TumblrUser> getUsersList() {
        return usersList;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

}
