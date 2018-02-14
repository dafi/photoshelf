package com.ternaryop.photoshelf.db

import java.io.Serializable

open class Post(var id: Long, var blogId: Long, var tagId: Long, var publishTimestamp: Long, var showOrder: Int) : Serializable {
    override fun toString(): String = "$blogId[$id]: tag $tagId ts = $publishTimestamp order = $showOrder"
}