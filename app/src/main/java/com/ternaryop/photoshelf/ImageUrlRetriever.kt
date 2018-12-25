package com.ternaryop.photoshelf

import android.content.Context
import android.net.Uri
import android.widget.ProgressBar
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.Response
import com.ternaryop.photoshelf.api.extractor.ImageGalleryResult
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.domselector.DomSelectorManager
import com.ternaryop.photoshelf.domselector.GalleryExtractor
import com.ternaryop.photoshelf.domselector.ImageExtractor
import com.ternaryop.utils.network.UriUtils
import com.ternaryop.utils.network.resolveShorten
import com.ternaryop.utils.network.saveURL
import com.ternaryop.utils.reactivex.ProgressIndicatorObservable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ImageUrlRetriever(private val context: Context, private val progressBar: ProgressBar) {
    fun readImageGallery(url: String): Single<Response<ImageGalleryResult>> {
        return readImageGalleryObservable(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun retrieve(list: List<ImageInfo>, useFile: Boolean): Observable<Uri> {
        return Observable
                .fromIterable(list)
                .flatMapMaybe { imageInfo -> retrieveImageUrl(imageInfo) }
                .map { url -> makeUri(url, useFile) }
                .compose(ProgressIndicatorObservable.apply(
                        progressBar,
                        list.size))
    }

    private fun readImageGalleryObservable(url: String): Single<Response<ImageGalleryResult>> {
        return GalleryExtractor(DomSelectorManager.selectors(context), ApiManager.parserService(context))
            .getGallery(URL(url).resolveShorten().toString())
    }

    private fun retrieveImageUrl(imageInfo: ImageInfo): Maybe<String> {
        return getImageURL(imageInfo)
            .flatMapMaybe { link ->
                if (link.isEmpty()) {
                    Maybe.empty()
                } else Maybe.just(UriUtils.resolveRelativeURL(imageInfo.documentUrl, link))
            }
    }

    private fun getImageURL(imageInfo: ImageInfo): Single<String> {
        val link = imageInfo.imageUrl
        // parse document only if the imageURL is not set (ie isn't cached)
        if (link != null) {
            return Single.just(link)
        }
        val url = imageInfo.documentUrl
        return ImageExtractor(DomSelectorManager.selectors(context)).getImageURL(url!!)
    }

    private fun makeUri(url: String, useFile: Boolean): Uri {
        return if (useFile) {
            val file = File(context.cacheDir, url.hashCode().toString())
            FileOutputStream(file).use { fos ->
                URL(url).saveURL(fos)
                Uri.fromFile(file)
            }
        } else {
            Uri.parse(url)
        }
    }
}
