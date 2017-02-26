package com.ternaryop.feedly;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by dave on 24/02/17.
 * Contains the item content
 */

public class FeedlyContent {
    private String id;
    private String title;
    private String originId;
    private long actionTimestamp;
    private Origin origin;

    public FeedlyContent() {
    }

    public FeedlyContent(JSONObject json) throws JSONException {
        id = json.get("id").toString();
        title = json.get("title").toString();
        originId = json.get("originId").toString();
        actionTimestamp = Long.parseLong(json.get("actionTimestamp").toString());
        origin = new Origin(json.getJSONObject("origin"));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public long getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(long actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public static class Origin {
        private String title;

        public Origin(JSONObject json) throws JSONException {
            title = json.get("title").toString();
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
