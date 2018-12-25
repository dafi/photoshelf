package com.ternaryop.photoshelf.domselector

import android.net.Uri
import com.ternaryop.photoshelf.api.Response
import com.ternaryop.photoshelf.api.extractor.ImageGallery
import com.ternaryop.photoshelf.api.extractor.ImageGalleryResult
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.api.parser.ParserService
import com.ternaryop.photoshelf.util.html.HtmlDocumentSupport
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern

fun String.normalizeWhitespaces() = replace("\\s{2,}", " ")

class GalleryExtractor(private val domSelectors: DomSelectors, private val parserService: ParserService) {
    fun getGallery(galleryUrl: String): Single<Response<ImageGalleryResult>> {
        return Single.fromCallable {
            val uri = Uri.parse(galleryUrl)
            val uriScheme = checkNotNull(uri.scheme) { "Invalid gallery url: $galleryUrl" }
            val html = HtmlDocumentSupport.download(galleryUrl)
            val htmlDocument = Jsoup.parse(html)
            val selectorFromURL = domSelectors.getSelectorFromUrl(galleryUrl)
            val gallerySelector = selectorFromURL.gallery
            val title = findTitle(gallerySelector, htmlDocument)
            val baseuri = uriScheme + "://" + uri.host
            val gallery = extractGallery(gallerySelector, htmlDocument, html, baseuri)
            val titleParsed = parserService.components(title).blockingGet().response

            Response(ImageGalleryResult(ImageGallery(uri.host, title, titleParsed, gallery)))
        }
    }

    private fun extractGallery(selector: Gallery, htmlDocument: Document, html: String, baseuri: String): List<ImageInfo> {
        if (selector.regExp != null) {
            return extractByRegExp(selector, html.replace("""([\n\r\t])""".toRegex(), ""))
        }
        val list = mutableListOf<ImageInfo>()
        list += extractImages(selector, htmlDocument, baseuri)
        list += extractImageFromMultiPage(selector, htmlDocument, baseuri)
        return list
    }

    private fun extractByRegExp(selector: Gallery, html: String): MutableList<ImageInfo> {
        val matches = Pattern.compile(selector.regExp!!).matcher(html)
        val thumbIndex = selector.regExpThumbUrlIndex
        val imageIndex = selector.regExpImageUrlIndex

        val galleryItemBuilder = GalleryItemBuilder(domSelectors)
        val list = mutableListOf<ImageInfo>()

        while (matches.find()) {
            val thumbnailURL = matches.group(thumbIndex)
            val destinationDocumentURL = matches.group(imageIndex)

            list.add(galleryItemBuilder.build(selector, thumbnailURL, destinationDocumentURL))
        }

        return list
    }

    private fun extractImageFromMultiPage(selector: Gallery, startPageDocument: Document, baseuri: String): List<ImageInfo> {
        val list = mutableListOf<ImageInfo>()

        if (selector.multiPage === null) {
            return list
        }
        var element: Element? = startPageDocument.select(selector.multiPage).first()
        while (element != null) {
            val pageUrl = HtmlDocumentSupport.absUrl(baseuri, element.attr("href"))
            val pageDocument = Jsoup.parse(HtmlDocumentSupport.download(pageUrl))
            val pageUrlSel = domSelectors.getSelectorFromUrl(pageUrl)
            list += extractImages(pageUrlSel.gallery, pageDocument, pageUrl)
            element = pageDocument.select(selector.multiPage).first()
        }
        return list
    }

    private fun extractImages(selector: Gallery, htmlDocument: Document, baseuri: String): List<ImageInfo> {
        val list = mutableListOf<ImageInfo>()
        for (t in htmlDocument.select(selector.container)) {
            buildGalleryItem(selector, t, baseuri)?. also { list.add(it) }
        }
        return list
    }

    private fun buildGalleryItem(selector: Gallery, thumbnailImage: Element, baseuri: String): ImageInfo? {
        val galleryItemBuilder = GalleryItemBuilder(domSelectors)
        var galleryItem = galleryItemBuilder.fromSrcSet(selector, thumbnailImage, 400)

        if (galleryItem == null) {
            galleryItem = galleryItemBuilder.fromThumbnailElement(selector, thumbnailImage, baseuri)
        }
        return galleryItem
    }

    private fun findTitle(gallery: Gallery, htmlDocument: Document): String {
        var title = ""
        if (gallery.title !== null) {
            title = htmlDocument.select(gallery.title).text().trim()
        }
        if (title.isEmpty()) {
            title = htmlDocument.title().trim()
        }

        return title.normalizeWhitespaces()
    }
}