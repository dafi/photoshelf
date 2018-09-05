package com.ternaryop.tumblr

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class Tumblr constructor(val consumer: TumblrHttpOAuthConsumer) {

    val blogList: Array<Blog>
        get() {
            val apiUrl = "$API_PREFIX/user/info"

            try {
                val json = consumer.jsonFromGet(apiUrl)
                val jsonBlogs = json.getJSONObject("response").getJSONObject("user").getJSONArray("blogs")
                return (0 until jsonBlogs.length()).map { Blog(jsonBlogs.getJSONObject(it)) }.toTypedArray()
            } catch (e: Exception) {
                throw TumblrException(e)
            }
        }

    fun publishPost(tumblrName: String, id: Long): Long {
        val apiUrl = getApiUrl(tumblrName, "/post/edit")

        val params = HashMap<String, String>()
        params["id"] = id.toString()
        params["state"] = "published"

        try {
            val jsonObject = consumer.jsonFromPost(apiUrl, params)
            val response = jsonObject.getJSONObject("response")
            return response.getLong("id")
        } catch (e: Exception) {
            throw TumblrException(e)
        }
    }

    fun deletePost(tumblrName: String, id: Long) {
        val apiUrl = getApiUrl(tumblrName, "/post/delete")

        val params = HashMap<String, String>()
        params["id"] = id.toString()

        try {
            consumer.jsonFromPost(apiUrl, params)
        } catch (e: Exception) {
            throw TumblrException(e)
        }
    }

    fun editPost(tumblrName: String, params: Map<String, String>) {
        if (!params.containsKey("id")) {
            throw TumblrException("The id is mandatory to edit post")
        }
        val apiUrl = getApiUrl(tumblrName, "/post/edit")

        consumer.jsonFromPost(apiUrl, params)
    }

    fun getPublicPosts(tumblrName: String, params: Map<String, String>): List<TumblrPost> {
        val apiUrl = getApiUrl(tumblrName, "/posts" + getPostTypeAsUrlPath(params))

        val modifiedParams = HashMap(params)
        modifiedParams.remove("type")

        try {
            val json = consumer.jsonFromGet(apiUrl, modifiedParams)
            val jsonArray = json.getJSONObject("response").getJSONArray("posts")
            return (0 until jsonArray.length()).map { build(jsonArray.getJSONObject(it)) }
        } catch (e: JSONException) {
            throw TumblrException(e)
        }
    }

    /**
     * Return the post type contained into params (if any) prepended by "/" url path separator
     * @param params API params
     * @return the "/" + type or empty string if not present
     */
    private fun getPostTypeAsUrlPath(params: Map<String, String>): String {
        val type = params["type"]
        return if (type == null || type.trim { it <= ' ' }.isEmpty()) {
            ""
        } else "/$type"
    }

    companion object {
        const val MAX_POST_PER_REQUEST = 20
        private const val API_PREFIX = "https://api.tumblr.com/v2"

        fun getApiUrl(tumblrName: String, suffix: String): String = "$API_PREFIX/blog/$tumblrName.tumblr.com$suffix"

        fun addPostsToList(list: MutableList<TumblrPost>, arr: JSONArray) {
            (0 until arr.length()).mapTo(list) { build(arr.getJSONObject(it)) }
        }

        fun build(json: JSONObject): TumblrPost {
            val type = json.getString("type")

            require(type == "photo") { "Unable to build post for type $type" }

            return TumblrPhotoPost(json)
        }
    }
}
