package com.ternaryop.tumblr

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

/**
 * Created by dave on 18/01/15.
 * Info about followers
 */
@Suppress("MemberVisibilityCanBePrivate")
class TumblrFollowers : Serializable {
    var totalUsers: Long = 0
        private set
    val usersList = mutableListOf<TumblrUser>()

    @Throws(JSONException::class)
    fun add(json: JSONObject) {
        totalUsers = json.getLong("total_users")
        val users = json.getJSONArray("users")
        for (i in 0 until users.length()) {
            usersList.add(TumblrUser(users.getJSONObject(i)))
        }
    }
}
