package com.ternaryop.photoshelf.domselector

import android.net.Uri
import com.ternaryop.photoshelf.util.html.HtmlDocumentSupport
import io.reactivex.Single
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.ParseException
import java.util.regex.Pattern

class ImageExtractor(private val domSelectors: DomSelectors) {
    fun getImageURL(documentUrl: String): Single<String> {
        return Single.fromCallable {
            val selector = domSelectors.getSelectorFromUrl(documentUrl).image
            var url = ""

            if (selector.css != null) {
                url = urlFromCSS3Selector(selector, documentUrl)
            }
            if (url.isBlank() && selector.regExp != null) {
                url = urlFromRegExp(selector, documentUrl)
            }
            if (url.isBlank() && selector.pageChain != null) {
                url = urlFromChain(selector, documentUrl)
            }
            if (url.isBlank()) {
                url = documentUrl
            }
            val uri = Uri.parse(documentUrl)
            val uriScheme = checkNotNull(uri.scheme) { "Invalid image url: $documentUrl" }
            val baseuri = uriScheme + "://" + uri.host
            HtmlDocumentSupport.encodeUrlRfc3986(HtmlDocumentSupport.absUrl(baseuri, url))
        }
    }

    private fun urlFromCSS3Selector(selector: Image, documentUrl: String): String {
        return getDocumentFromUrl(documentUrl).select(selector.css).attr("src") ?: ""
    }

    private fun urlFromChain(selector: Image, documentUrl: String): String {
        selector.pageChain?.also { return getImageUrlFromPageSel(it, documentUrl) }
        return ""
    }

    private fun urlFromRegExp(selector: Image, documentUrl: String): String {
        val html = HtmlDocumentSupport.download(documentUrl).replace("""([\n\r\t])""".toRegex(), "")
        selector.regExp?.apply {
            val m = Pattern.compile(selector.regExp).matcher(html)
            if (m.find()) {
                val url = m.group(1)
                if (url != null) {
                    return url
                }
            }
        }
        throw ParseException("Unable to find image url for $documentUrl", 0)
    }

    /**
     * Iterate all PageSelector to find the destination image url.
     * Every PageSelector moves to an intermediate document page
     * @param pageChainList the list to traverse
     * @param url the starting document url
     * @return the imageUrl
     */
    private fun getImageUrlFromPageSel(pageChainList: List<PageChain>, url: String): String {
        var imageUrl = url

        for (pc in pageChainList) {
            imageUrl = getDocumentFromUrl(imageUrl).select(pc.pageSel).attr(pc.selAttr)
        }
        return imageUrl
    }

    private fun getDocumentFromUrl(url: String): Document {
        val domSelector = domSelectors.getSelectorFromUrl(url).image
        return Jsoup.parse(HtmlDocumentSupport.download(url, domSelector.postData?.imgContinue))
    }
}