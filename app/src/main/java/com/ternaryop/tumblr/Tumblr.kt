package com.ternaryop.tumblr

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
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

    fun getApiUrl(tumblrName: String, suffix: String): String {
        return "$API_PREFIX/blog/$tumblrName.tumblr.com$suffix"
    }

    fun draftPhotoPost(tumblrName: String, uri: Uri, caption: String, tags: String) {
        try {
            createPhotoPost(tumblrName, uri, caption, tags, "draft")
        } catch (e: JSONException) {
            throw TumblrException(e)
        }
    }

    fun publishPhotoPost(tumblrName: String, uri: Uri, caption: String, tags: String) {
        try {
            createPhotoPost(tumblrName, uri, caption, tags, "published")
        } catch (e: JSONException) {
            throw TumblrException(e)
        }
    }

    @Throws(JSONException::class)
    fun createPhotoPost(tumblrName: String, uri: Uri, caption: String, tags: String, state: String): Long {
        val apiUrl = getApiUrl(tumblrName, "/post")
        val params = HashMap<String, Any>()

        if (uri.scheme == "file") {
            params["data"] = File(uri.path)
        } else {
            params["source"] = uri.toString()
        }
        params["state"] = state
        params["type"] = "photo"
        params["caption"] = caption
        params["tags"] = tags

        val json = consumer.jsonFromPost(apiUrl, params)
        return json.getJSONObject("response").getLong("id")
    }

    fun draftTextPost(tumblrName: String, title: String, body: String, tags: String) {
        try {
            createTextPost(tumblrName, title, body, tags, "draft")
        } catch (e: JSONException) {
            throw TumblrException(e)
        }
    }

    fun publishTextPost(tumblrName: String, title: String, body: String, tags: String) {
        try {
            createTextPost(tumblrName, title, body, tags, "published")
        } catch (e: JSONException) {
            throw TumblrException(e)
        }
    }

    @Throws(JSONException::class)
    private fun createTextPost(tumblrName: String, title: String, body: String, tags: String, state: String): Long {
        val apiUrl = getApiUrl(tumblrName, "/post")
        val params = HashMap<String, Any>()

        params["state"] = state
        params["type"] = "text"
        params["title"] = title
        params["body"] = body
        params["tags"] = tags

        val json = consumer.jsonFromPost(apiUrl, params)
        return json.getJSONObject("response").getLong("id")
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

    fun getPhotoPosts(tumblrName: String, params: Map<String, String>): List<TumblrPhotoPost> {
        val apiUrl = getApiUrl(tumblrName, "/posts/photo")
        val list = mutableListOf<TumblrPhotoPost>()

        try {
            val paramsWithKey = HashMap(params)
            paramsWithKey["api_key"] = consumer.consumerKey

            val json = consumer.jsonFromGet(apiUrl, paramsWithKey)
            val arr = json.getJSONObject("response").getJSONArray("posts")
            val totalPosts = json.getJSONObject("response").optLong("total_posts", -1)
            for (i in 0 until arr.length()) {
                val post = build(arr.getJSONObject(i)) as TumblrPhotoPost
                if (totalPosts != -1L) {
                    post.totalPosts = totalPosts
                }
                list.add(post)
            }
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

    fun getFollowers(tumblrName: String, params: Map<String, String>?, followers: TumblrFollowers?): TumblrFollowers {
        val apiUrl = getApiUrl(tumblrName, "/followers")

        val modifiedParams = HashMap<String, String>()
        if (params != null) {
            modifiedParams.putAll(params)
        }
        modifiedParams["base-hostname"] = "$tumblrName.tumblr.com"

        try {
            val json = consumer.jsonFromGet(apiUrl, modifiedParams)
            val resultFollowers = followers ?: TumblrFollowers()
            resultFollowers.add(json.getJSONObject("response"))

            return resultFollowers
        } catch (e: Exception) {
            throw TumblrException(e)
        }
    }

    fun getFollowers(tumblrName: String, offset: Int, limit: Int, followers: TumblrFollowers): TumblrFollowers {
        val params = HashMap<String, String>()
        params["offset"] = Integer.toString(offset)
        params["limit"] = Integer.toString(limit)

        return getFollowers(tumblrName, params, followers)
    }

    companion object {
        const val MAX_POST_PER_REQUEST = 20
        private const val API_PREFIX = "http://api.tumblr.com/v2"

        private var instance: Tumblr? = null

        fun getSharedTumblr(context: Context): Tumblr {
            if (instance == null) {
                instance = Tumblr(TumblrHttpOAuthConsumer(context))
            }
            return instance!!
        }

        fun isLogged(context: Context): Boolean {
            return TumblrHttpOAuthConsumer.isLogged(context)
        }

        fun login(context: Context) {
            TumblrHttpOAuthConsumer.loginWithActivity(context)
        }

        fun logout(context: Context) {
            TumblrHttpOAuthConsumer.logout(context)
        }

        fun handleOpenURI(context: Context, uri: Uri?, callback: AuthenticationCallback): Boolean {
            return TumblrHttpOAuthConsumer.handleOpenURI(context, uri, callback)
        }

        @Throws(JSONException::class)
        fun addPostsToList(list: MutableList<TumblrPost>, arr: JSONArray) {
            (0 until arr.length()).mapTo(list) { build(arr.getJSONObject(it)) }
        }

        @Throws(JSONException::class)
        fun build(json: JSONObject): TumblrPost {
            val type = json.getString("type")

            require(type == "photo") { "Unable to build post for type $type" }

            return TumblrPhotoPost(json)
        }
    }
}
