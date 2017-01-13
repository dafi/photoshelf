package com.ternaryop.photoshelf;

import android.support.annotation.NonNull;

import com.ternaryop.photoshelf.selector.DOMSelector;

public class ImageInfo {
    private String thumbnailURL;
    private String destinationDocumentURL;
    private DOMSelector selector;
    private String imageURL;

    public ImageInfo(@NonNull String thumbnailURL, @NonNull String destinationDocumentURL, @NonNull DOMSelector selector) {
        this.thumbnailURL = thumbnailURL;
        this.destinationDocumentURL = destinationDocumentURL;
        this.selector = selector;
    }

    /**
     * The thumbnail image url.
     * This is present on the HTML document from which pick images  
     * @return thumbnail url
     */
    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    /**
     * The destination document containing the image url
     * @return destination document url
     */
    public String getDestinationDocumentURL() {
        return destinationDocumentURL;
    }

    public void setDestinationDocumentURL(String imageURL) {
        this.destinationDocumentURL = imageURL;
    }

    /**
     * The image url present inside the destination document url.
     * If null must be retrieved from destination document
     * @return image url
     */
    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    @Override
    public String toString() {
        return "thumb " + thumbnailURL + " doc " + destinationDocumentURL;
    }

    public DOMSelector getSelector() {
        return selector;
    }

    public boolean hasPageSel() {
        return selector.getImageChainList() != null;
    }
}
