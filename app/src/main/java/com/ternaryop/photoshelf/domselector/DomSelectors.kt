package com.ternaryop.photoshelf.domselector

import java.util.regex.Pattern

val emptySelector = Selector("", Image(), Gallery())

data class DomSelectors(val version: Int, val selectors: List<Selector>) {
    fun getSelectorFromUrl(url: String): Selector {
        return selectors.firstOrNull { Pattern.compile(it.urlPattern).matcher(url).find() } ?: emptySelector
    }
}

data class Gallery(
    val container: String = "a img",
    val isImageDirectUrl: Boolean = false,
    val regExp: String? = null,
    val regExpImageUrlIndex: Int = 0,
    val regExpThumbUrlIndex: Int = 0,
    val title: String? = null,
    val thumbImageSelAttr: String = "src",
    val multiPage: String? = null) {
    val hasImage: Boolean
        get() = isImageDirectUrl
}

class Image(
    val css: String? = null,
    val regExp: String? = null,
    val pageChain: List<PageChain>? = null,
    val postData: PostData? = null) {
    val hasImage: Boolean
        get() = css !== null || regExp !== null || pageChain !== null
}

data class PageChain(val pageSel: String, val selAttr: String)

data class PostData(val imgContinue: String)

data class Selector(
    val urlPattern: String = "",
    val image: Image = Image(),
    val gallery: Gallery = Gallery()) {
    val hasImage: Boolean
        get() = gallery.hasImage || image.hasImage
}
