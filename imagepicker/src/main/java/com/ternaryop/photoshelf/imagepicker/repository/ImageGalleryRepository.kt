package com.ternaryop.photoshelf.imagepicker.repository

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.ternaryop.photoshelf.api.extractor.ImageGalleryResult
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.domselector.DomSelectorManager
import com.ternaryop.photoshelf.domselector.util.readImageGallery
import com.ternaryop.photoshelf.domselector.util.retrieveImageUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader

private const val GALLERY_EXPIRE_TIMESPAN_MS = 10 * 60 * 1_000L

class ImageGalleryRepository(private val context: Context) {
    private val domSelectors = DomSelectorManager.selectors(context)
    private val cacheDir: File
        get() = File(context.cacheDir, "imageGallery").apply { mkdirs() }
    private val gson = GsonBuilder().create()

    suspend fun readImageGallery(url: String, expireTimespan: Long = GALLERY_EXPIRE_TIMESPAN_MS): ImageGalleryResult {
        return loadCache(url, expireTimespan) ?: writeCache(url, domSelectors.readImageGallery(url).response)
    }

    suspend fun retrieveImageUri(imageInfo: ImageInfo, destDirectory: File? = null): Uri? {
        return domSelectors.retrieveImageUri(imageInfo, destDirectory)
    }

    suspend fun image(imageInfo: ImageInfo): Pair<ImageInfo, Uri> {
        return domSelectors.retrieveImageUri(imageInfo)?.let { Pair(imageInfo, it) }
            ?: throw IOException("Unable to find Url for ${imageInfo.documentUrl}")
    }

    private fun loadCache(url: String, expireTimespan: Long): ImageGalleryResult? {
        val file = cacheFile(url)
        val currentTimeMillis = System.currentTimeMillis()

        return if (!hasExpired(file, currentTimeMillis, expireTimespan)) {
            FileInputStream(file).use { gson.fromJson(InputStreamReader(it), ImageGalleryResult::class.java) }
        } else {
            null
        }
    }

    private fun writeCache(url: String, imageGalleryResult: ImageGalleryResult): ImageGalleryResult {
        val file = cacheFile(url)

        FileWriter(file).use { it.write(gson.toJson(imageGalleryResult)) }
        return imageGalleryResult
    }

    suspend fun prefetch(
        urls: List<String>,
        expireTimespan: Long = GALLERY_EXPIRE_TIMESPAN_MS
    ) = withContext(Dispatchers.IO) {
        val currentTimeMillis = System.currentTimeMillis()

        urls.forEach { url ->
            launch {
                fetch(url, currentTimeMillis, expireTimespan)
            }
        }
    }

    private suspend fun fetch(url: String, currentTimeMillis: Long, expireTimespan: Long) {
        val file = cacheFile(url)

        if (hasExpired(file, currentTimeMillis, expireTimespan)) {
            val imageGalleryResult = domSelectors.readImageGallery(url).response
            writeCache(url, imageGalleryResult)
        }
    }

    private fun hasExpired(file: File, currentTime: Long, expireTimespan: Long): Boolean {
        return !file.exists() || (currentTime - file.lastModified()) > expireTimespan
    }

    private fun cacheFile(url: String) = File(cacheDir, url.hashCode().toString())
}
