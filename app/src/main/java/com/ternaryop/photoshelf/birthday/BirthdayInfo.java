package com.ternaryop.photoshelf.birthday;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dave on 16/04/17.
 * Hold the info returned by {@link BirthdayManager}
 */

public class BirthdayInfo {
    private String name;
    private String birthday;
    private String source;

    public BirthdayInfo() {
    }

    public BirthdayInfo(JSONObject json) throws JSONException {
        name = json.getString("name");
        birthday = json.getString("birthday");
        source = json.getString("source");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
