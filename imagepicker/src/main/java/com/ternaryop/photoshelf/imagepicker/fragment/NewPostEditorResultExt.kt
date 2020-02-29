package com.ternaryop.photoshelf.imagepicker.fragment

import com.ternaryop.photoshelf.imagepicker.service.PostPublisherAction
import com.ternaryop.photoshelf.imagepicker.service.PostPublisherData
import com.ternaryop.photoshelf.tumblr.dialog.NewPostEditorResult

fun NewPostEditorResult.toPostPublisherData(url: String, publishClassName: String?) = PostPublisherData(
    url,
    blogName,
    htmlTitle,
    tags,
    if (isPublish) {
        PostPublisherAction.PUBLISH
    } else {
        PostPublisherAction.DRAFT
    },
    publishClassName
)
