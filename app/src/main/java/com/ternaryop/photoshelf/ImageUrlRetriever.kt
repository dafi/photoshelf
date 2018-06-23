package com.ternaryop.photoshelf

import android.content.Context
import android.net.Uri
import android.widget.ProgressBar
import com.ternaryop.photoshelf.api.extractor.ImageGallery
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.util.network.ApiManager
import com.ternaryop.utils.network.UriUtils
import com.ternaryop.utils.network.resolveShorten
import com.ternaryop.utils.network.saveURL
import com.ternaryop.utils.reactivex.ProgressIndicatorObservable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

class ImageUrlRetriever(private val context: Context, private val progressBar: ProgressBar) {
    fun readImageGallery(url: String): Observable<ImageGallery> {
        return readImageGalleryObservable(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun retrieve(list: List<ImageInfo>, useFile: Boolean): Observable<Uri> {
        return Observable
                .fromIterable(list)
                .flatMap { imageInfo -> makeUriObservable(imageInfo, useFile) }
                .compose(ProgressIndicatorObservable.apply(
                        progressBar,
                        list.size))
    }

    private fun readImageGalleryObservable(url: String): Observable<ImageGallery> {
        return Observable.fromCallable { ApiManager.imageExtractorManager(context).getGallery(URL(url).resolveShorten().toString()) }
    }

    @Throws(Exception::class)
    private fun makeUriObservable(imageInfo: ImageInfo, useFile: Boolean): ObservableSource<Uri> {
        val uri = makeUri(retrieveImageUrl(imageInfo), useFile)
        return if (uri == null) Observable.empty() else Observable.just(uri)
    }

    @Throws(Exception::class)
    private fun retrieveImageUrl(imageInfo: ImageInfo): String? {
        val link = getImageURL(imageInfo)

        return if (link.isEmpty()) {
            null
        } else resolveRelativeURL(imageInfo.documentUrl, link)
    }

    @Throws(Exception::class)
    private fun resolveRelativeURL(baseURL: String?, link: String): String {
        val uri = UriUtils.encodeIllegalChar(link, "UTF-8")
        return when {
            uri.isAbsolute -> uri.toString()
            baseURL != null -> UriUtils.encodeIllegalChar(baseURL, "UTF-8").resolve(uri).toString()
            else -> throw IllegalArgumentException("baseUrl is null")
        }
    }

    @Throws(Exception::class)
    private fun getImageURL(imageInfo: ImageInfo): String {
        val link = imageInfo.imageUrl
        // parse document only if the imageURL is not set (ie isn't cached)
        if (link != null) {
            return link
        }
        val url = imageInfo.documentUrl
        return ApiManager.imageExtractorManager(context).getImageUrl(url!!)
    }

    @Throws(IOException::class)
    private fun makeUri(url: String?, useFile: Boolean): Uri? {
        if (url == null) {
            return null
        }
        if (useFile) {
            val file = File(context.cacheDir, url.hashCode().toString())
            FileOutputStream(file).use { fos ->
                URL(url).saveURL(fos)
                return Uri.fromFile(file)
            }
        } else {
            return Uri.parse(url)
        }
    }
}
