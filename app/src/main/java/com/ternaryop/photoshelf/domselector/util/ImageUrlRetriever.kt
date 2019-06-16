package com.ternaryop.photoshelf.domselector.util

import android.net.Uri
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.Response
import com.ternaryop.photoshelf.api.extractor.ImageGalleryResult
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.domselector.DomSelectors
import com.ternaryop.photoshelf.domselector.GalleryExtractor
import com.ternaryop.photoshelf.domselector.ImageExtractor
import com.ternaryop.utils.network.UriUtils
import com.ternaryop.utils.network.resolveShorten
import com.ternaryop.utils.network.saveURL
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.net.URL

fun DomSelectors.readImageGallery(url: String): Single<Response<ImageGalleryResult>> {
    return GalleryExtractor(this, ApiManager.parserService())
        .getGallery(URL(url).resolveShorten().toString())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
}

fun DomSelectors.retrieveImageUrl(list: List<ImageInfo>, destDirectory: File? = null): Observable<Uri> {
    return Observable
        .fromIterable(list)
        .flatMapMaybe { imageInfo -> retrieveImageUrl(imageInfo) }
        .map { url -> makeUri(url, destDirectory) }
}

private fun DomSelectors.retrieveImageUrl(imageInfo: ImageInfo): Maybe<String> {
    return getImageURL(imageInfo)
        .flatMap { link ->
            if (link.isEmpty()) {
                Maybe.empty()
            } else Maybe.just(UriUtils.resolveRelativeURL(imageInfo.documentUrl, link))
        }
}

private fun DomSelectors.getImageURL(imageInfo: ImageInfo): Maybe<String> {
    // parse document only if the imageURL is not set (ie isn't cached)
    return imageInfo.imageUrl?.let { Maybe.just(it) }
        ?: imageInfo.documentUrl?.let { ImageExtractor(this).getImageURL(it).toMaybe() }
        ?: Maybe.empty()
}

private fun makeUri(url: String, destDirectory: File?): Uri {
    return if (destDirectory != null) {
        val file = File(destDirectory, url.hashCode().toString())
        FileOutputStream(file).use { fos ->
            URL(url).saveURL(fos)
            Uri.fromFile(file)
        }
    } else {
        Uri.parse(url)
    }
}
