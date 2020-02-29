package com.ternaryop.photoshelf.imagepicker.service

enum class PostPublisherAction {
    DRAFT,
    PUBLISH;

    val isDraft: Boolean
        get() = this == DRAFT

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.ordinal == value }
    }
}

class PostPublisherData(
    val url: String,
    val blogName: String,
    val postTitle: String,
    val postTags: String,
    val action: PostPublisherAction,
    val publishClassName: String? = null
) {
    fun newPublishClassInstance(): OnPublish? {
        return try {
            publishClassName?.let { Class.forName(it).newInstance() as OnPublish }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}
