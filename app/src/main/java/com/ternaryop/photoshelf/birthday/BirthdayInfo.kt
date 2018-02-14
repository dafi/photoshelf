package com.ternaryop.photoshelf.birthday

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by dave on 16/04/17.
 * Hold the info returned by [BirthdayManager]
 */

class BirthdayInfo @Throws(JSONException::class) constructor(json: JSONObject) {
    var name = json.getString("name")!!
    var birthdate = json.getString("birthdate")!!
    var source = json.getString("source")!!
}
