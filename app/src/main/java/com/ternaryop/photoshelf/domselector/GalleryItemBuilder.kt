package com.ternaryop.photoshelf.domselector

import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.util.html.HtmlDocumentSupport
import org.jsoup.nodes.Element

class GalleryItemBuilder(private val domSelectors: DomSelectors) {
    fun fromSrcSet(selector: Gallery, thumbnailImage: Element, minThumbWidth: Int): ImageInfo? {
        val srcSet = HtmlDocumentSupport.parseSrcSet(thumbnailImage.attr("srcset")) ?: return null
        var largeImage = thumbnailImage.parent().attr("href")
        // Prefer always the parent url if present
        // this could resolve from small images on srcSet
        if (largeImage.isBlank()) {
            largeImage = srcSet.last().url
        }
        val srcSetItem = srcSet.firstOrNull { it.width >= minThumbWidth } ?: return null
        // INSANE HACK: this should skip small images
        // It isn't sure it works always
        if (srcSetItem.url != largeImage) {
            return build(selector, srcSetItem.url, largeImage)
        }

        return null
    }

    fun fromThumbnailElement(selector: Gallery, thumbnailImage: Element, baseuri: String): ImageInfo? {
        val href = thumbnailImage.parent().attr("href")
        if (href.isBlank()) {
            return null
        }

        val destinationDocumentURL = HtmlDocumentSupport.absUrl(baseuri, href)
        val destinationSelector = domSelectors.getSelectorFromUrl(destinationDocumentURL)
        if (destinationSelector.hasImage) {
            val thumbImageSelAttr = destinationSelector.gallery.thumbImageSelAttr
            val thumbnailURL = HtmlDocumentSupport.absUrl(baseuri, thumbnailImage.attr(thumbImageSelAttr))
            return build(selector, thumbnailURL, destinationDocumentURL)
        }
        return null
    }

    fun build(selector: Gallery, thumbnailURL: String, destinationDocumentURL: String): ImageInfo {
        return ImageInfo(
            thumbnailURL,
            if (selector.isImageDirectUrl) null else destinationDocumentURL,
            if (selector.isImageDirectUrl) destinationDocumentURL else null)
    }
}