package com.ternaryop.photoshelf.birthday;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dave on 16/04/17.
 * Hold the info returned by {@link BirthdayManager}
 */

public class BirthdayInfo {
    private String name;
    private String birthdate;
    private String source;

    public BirthdayInfo() {
    }

    public BirthdayInfo(JSONObject json) throws JSONException {
        name = json.getString("name");
        birthdate = json.getString("birthdate");
        source = json.getString("source");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
