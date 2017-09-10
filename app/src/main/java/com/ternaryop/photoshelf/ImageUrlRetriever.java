package com.ternaryop.photoshelf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.ternaryop.photoshelf.extractor.ImageExtractorManager;
import com.ternaryop.photoshelf.extractor.ImageGallery;
import com.ternaryop.photoshelf.extractor.ImageInfo;
import com.ternaryop.utils.URLUtils;
import com.ternaryop.utils.UriUtils;
import com.ternaryop.utils.reactivex.ProgressIndicatorObservable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class ImageUrlRetriever {
    private final Context context;
    private final ImageExtractorManager imageExtractorManager;

    public ImageUrlRetriever(@NonNull Context context) {
        this.context = context;
        this.imageExtractorManager = new ImageExtractorManager(context.getString(R.string.PHOTOSHELF_EXTRACTOR_ACCESS_TOKEN));
    }

    public Observable<ImageGallery> readImageGallery(@NonNull final String url) {
        return readImageGalleryObservable(url)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Uri> retrieve(@NonNull final List<ImageInfo> list, final boolean useFile) {
        return retriveUrlsObservable(list, useFile)
                .compose(ProgressIndicatorObservable.<Uri>apply( // cast needed, see http://blog.danlew.net/2015/03/02/dont-break-the-chain/ "Update 3/11/2015"
                        context,
                        R.string.image_retriever_title,
                        list.size()));
    }

    private Observable<ImageGallery> readImageGalleryObservable(final String url) {
        return Observable
                .just(url)
                .flatMap(new Function<String, ObservableSource<ImageGallery>>() {
                    @Override
                    public ObservableSource<ImageGallery> apply(String url) throws Exception {
                        final String galleryUrl = URLUtils.resolveShortenURL(url);
                        return Observable.just(imageExtractorManager.getGallery(galleryUrl));
                    }
                });
    }

    private Observable<Uri> retriveUrlsObservable(final List<ImageInfo> list, final boolean useFile) {
        return Observable
                .fromIterable(list)
                .flatMap(new Function<ImageInfo, ObservableSource<Uri>>() {
                    @Override
                    public ObservableSource<Uri> apply(ImageInfo imageInfo) throws Exception {
                        return makeUriObservable(imageInfo, useFile);
                    }
                })
                .filter(new Predicate<Uri>() {
                    @Override
                    public boolean test(Uri uri) throws Exception {
                        return uri != null;
                    }
                });
    }

    private ObservableSource<Uri> makeUriObservable(ImageInfo imageInfo, boolean useFile) throws Exception {
        try {
            final Uri uri = makeUri(retrieveImageUrl(imageInfo), useFile);
            return uri == null ? Observable.<Uri>empty() : Observable.just(uri);
        } catch (InterruptedIOException ex) {
            // this occurs when user dismisses the progress dialog while retrieval isn't completed
            return Observable.empty();
        }
    }

    private String retrieveImageUrl(ImageInfo imageInfo) throws Exception {
        String link = getImageURL(imageInfo);

        if (link.isEmpty()) {
            return null;
        }
        return resolveRelativeURL(imageInfo.getDocumentUrl(), link);
    }

    private String resolveRelativeURL(final String baseURL, final String link) throws Exception {
        URI uri = UriUtils.encodeIllegalChar(link, "UTF-8", 20);
        if (uri.isAbsolute()) {
            return uri.toString();
        }
        return UriUtils.encodeIllegalChar(baseURL, "UTF-8", 20).resolve(uri).toString();
    }

    private String getImageURL(ImageInfo imageInfo) throws Exception {
        final String link = imageInfo.getImageUrl();
        // parse document only if the imageURL is not set (ie isn't cached)
        if (link != null) {
            return link;
        }
        final String url = imageInfo.getDocumentUrl();
        return imageExtractorManager.getImageUrl(url);
    }

    private Uri makeUri(final String url, final boolean useFile) throws IOException {
        if (url == null) {
            return null;
        }
        if (useFile) {
            File file = new File(context.getCacheDir(), String.valueOf(url.hashCode()));
            try (FileOutputStream fos = new FileOutputStream(file)) {
                URLUtils.saveURL(url, fos);
                return Uri.fromFile(file);
            }
        } else {
            return Uri.parse(url);
        }
    }
}
