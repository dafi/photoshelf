package com.ternaryop.tumblr

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Tumblr private constructor(val consumer: TumblrHttpOAuthConsumer) {

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

    fun getDraftPosts(tumblrName: String, maxTimestamp: Long): List<TumblrPost> {
        val apiUrl = getApiUrl(tumblrName, "/posts/draft")
        val list = mutableListOf<TumblrPost>()

        try {
            val json = consumer.jsonFromGet(apiUrl)
            var arr = json.getJSONObject("response").getJSONArray("posts")

            val params = HashMap<String, String>(1)
            while (arr.length() > 0) {
                for (i in 0 until arr.length()) {
                    val post = build(arr.getJSONObject(i))
                    if (post.timestamp <= maxTimestamp) {
                        return list
                    }
                    list.add(post)
                }
                val beforeId = arr.getJSONObject(arr.length() - 1).getLong("id")
                params["before_id"] = beforeId.toString() + ""

                arr = consumer.jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts")
            }
        } catch (e: Exception) {
            throw TumblrException(e)
        }

        return list
    }

    fun getQueue(tumblrName: String, params: Map<String, String>): List<TumblrPost> {
        val apiUrl = getApiUrl(tumblrName, "/posts/queue")
        val list = mutableListOf<TumblrPost>()

        try {
            val json = consumer.jsonFromGet(apiUrl, params)
            val arr = json.getJSONObject("response").getJSONArray("posts")
            addPostsToList(list, arr)
        } catch (e: Exception) {
            throw TumblrException(e)
        }

        return list
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

    fun saveDraft(tumblrName: String, id: Long) {
        val apiUrl = getApiUrl(tumblrName, "/post/edit")

        val params = HashMap<String, String>()
        params["id"] = id.toString()
        params["state"] = "draft"

        try {
            consumer.jsonFromPost(apiUrl, params)
        } catch (e: Exception) {
            throw TumblrException(e)
        }
    }

    fun schedulePost(tumblrName: String, post: TumblrPost, timestamp: Long): Long {
        try {
            val apiUrl = getApiUrl(tumblrName, "/post/edit")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
            val gmtDate = dateFormat.format(Date(timestamp))

            val params = HashMap<String, String>()
            params["id"] = post.postId.toString() + ""
            params["state"] = "queue"
            params["publish_on"] = gmtDate

            if (post is TumblrPhotoPost) {
                params["caption"] = post.caption
            }
            params["tags"] = post.tagsAsString

            return consumer.jsonFromPost(apiUrl, params).getJSONObject("response").getLong("id")
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
            val json = consumer.publicJsonFromGet(apiUrl, modifiedParams)
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
        private const val API_PREFIX = "http://api.tumblr.com/v2"

        private var instance: Tumblr? = null

        fun getApiUrl(tumblrName: String, suffix: String): String = "$API_PREFIX/blog/$tumblrName.tumblr.com$suffix"

        fun getSharedTumblr(context: Context): Tumblr {
            if (instance == null) {
                instance = Tumblr(TumblrHttpOAuthConsumer(context))
            }
            return instance!!
        }

        fun isLogged(context: Context): Boolean = TumblrHttpOAuthConsumer.isLogged(context)

        fun login(context: Context) = TumblrHttpOAuthConsumer.loginWithActivity(context)

        fun logout(context: Context) = TumblrHttpOAuthConsumer.logout(context)

        fun handleOpenURI(context: Context, uri: Uri?, callback: AuthenticationCallback): Boolean {
            return TumblrHttpOAuthConsumer.handleOpenURI(context, uri, callback)
        }

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
