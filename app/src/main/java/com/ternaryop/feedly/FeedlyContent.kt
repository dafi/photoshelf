package com.ternaryop.feedly

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by dave on 24/02/17.
 * Contains the item content
 */

interface FeedlyContent {
    var id: String
    var title: String
    var originId: String
    var actionTimestamp: Long
    var origin: FeedlyOrigin
}

interface FeedlyOrigin {
    var title: String
}

class SimpleFeedlyOrigin @Throws(JSONException::class) constructor(json: JSONObject) : FeedlyOrigin {
    override var title = json.getString("title")!!
}

class SimpleFeedlyContent @Throws(JSONException::class) constructor(json: JSONObject) : FeedlyContent {
    override var id = json.getString("id")!!
    override var title = json.getString("title")!!
    override var originId = json.getString("originId")!!
    override var actionTimestamp = json.getLong("actionTimestamp")
    override var origin: FeedlyOrigin = SimpleFeedlyOrigin(json.getJSONObject("origin"))
}
