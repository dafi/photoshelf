package com.ternaryop.photoshelf.imagepicker.service

import androidx.work.Data
import androidx.work.workDataOf

private const val PARAM_URL = "url"
private const val PARAM_BLOG_NAME = "blogName"
private const val PARAM_POST_TITLE = "postTitle"
private const val PARAM_POST_TAGS = "postTags"
private const val PARAM_PUBLISH_ACTION = "publishAction"
private const val PARAM_PUBLISH_CLASS_NAME = "onPublishClassName"

fun Data.toPostPublisherData() = PostPublisherData(
    checkNotNull(getString(PARAM_URL)),
    checkNotNull(getString(PARAM_BLOG_NAME)),
    checkNotNull(getString(PARAM_POST_TITLE)),
    checkNotNull(getString(PARAM_POST_TAGS)),
    checkNotNull(PostPublisherAction.fromInt(getInt(PARAM_PUBLISH_ACTION, Integer.MIN_VALUE))),
    getString(PARAM_PUBLISH_CLASS_NAME)
)

fun PostPublisherData.toWorkData() = workDataOf(
    PARAM_URL to url,
    PARAM_BLOG_NAME to blogName,
    PARAM_POST_TITLE to postTitle,
    PARAM_POST_TAGS to postTags,
    PARAM_PUBLISH_ACTION to action.ordinal,
    PARAM_PUBLISH_CLASS_NAME to publishClassName)
