package com.ternaryop.photoshelf.feedly.fragment

import java.io.Serializable

internal interface Content {
    fun read(blogName: String, idListToDelete: List<String>?)
    fun remove(data: MarkSavedData)
}

internal class ReadLaterContent(private val viewModel: FeedlyViewModel) : Content {
    override fun read(blogName: String, idListToDelete: List<String>?) {
        viewModel.readLater(blogName, idListToDelete)
    }

    override fun remove(data: MarkSavedData) {
        viewModel.markSaved(data)
    }
}

internal class UnreadContent(private val viewModel: FeedlyViewModel) : Content {
    override fun read(blogName: String, idListToDelete: List<String>?) {
        viewModel.unread(blogName, idListToDelete)
    }

    override fun remove(data: MarkSavedData) {
    }
}

internal fun newContentFrom(
    contentType: Serializable?,
    viewModel: FeedlyViewModel
) = when (contentType ?: FeedlyContentType.ReadLater) {
    FeedlyContentType.ReadLater -> ReadLaterContent(viewModel)
    FeedlyContentType.Unread -> UnreadContent(viewModel)
    else -> ReadLaterContent(viewModel)
}
