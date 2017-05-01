package com.ternaryop.photoshelf.customsearch;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dave on 01/05/17.
 * Hold the custom seacrh result
 */

public class CustomSearchResult {
    private String correctedQuery;

    public CustomSearchResult() {
    }

    public CustomSearchResult(JSONObject json) throws JSONException {
        correctedQuery = getCorrectedQuery(json);
    }

    public static String getCorrectedQuery(JSONObject json) throws JSONException {
        if (!json.has("spelling")) {
            return null;
        }
        return json.getJSONObject("spelling").getString("correctedQuery");
    }

    public String getCorrectedQuery() {
        return correctedQuery;
    }

    public void setCorrectedQuery(String correctedQuery) {
        this.correctedQuery = correctedQuery;
    }
}
