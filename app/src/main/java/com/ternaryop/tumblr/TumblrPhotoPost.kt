package com.ternaryop.tumblr

import org.json.JSONException
import org.json.JSONObject

open class TumblrPhotoPost : TumblrPost {

    private var photos: MutableList<TumblrPhoto> = mutableListOf()
    var caption = ""

    val firstPhotoAltSize: List<TumblrAltSize>?
        get() = if (photos.isEmpty()) null else photos[0].altSizes

    constructor()

    @Throws(JSONException::class)
    constructor(json: JSONObject) : super(json) {
        caption = json.getString("caption")

        val jsonPhotos = json.getJSONArray("photos")
        for (i in 0 until jsonPhotos.length()) {
            photos.add(TumblrPhoto(jsonPhotos.getJSONObject(i)))
        }
    }

    constructor(photoPost: TumblrPhotoPost) : super(photoPost) {
        photos = photoPost.photos
        caption = photoPost.caption
    }

    fun getClosestPhotoByWidth(width: Int): TumblrAltSize? {
        // some images don't have the exact (==) width so we get closest width (<=)
        return firstPhotoAltSize?.firstOrNull { it.width <= width }
    }

    override fun toString() = caption

    companion object {
        /**
         *
         */
        private const val serialVersionUID = 8910912231608271421L
    }
}
