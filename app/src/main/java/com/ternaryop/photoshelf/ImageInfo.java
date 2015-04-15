package com.ternaryop.photoshelf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageInfo {
    private String thumbnailURL;
    private String destinationDocumentURL;
    private String imageURL;
    private String selector;
    private static final Pattern pageSelRE = Pattern.compile("pageSel:(.*)\\s+selAttr:(.*)");
    private String selAttr;

    public ImageInfo(String thumbnailURL, String destinationDocumentURL, String selector) {
        this.thumbnailURL = thumbnailURL;
        this.destinationDocumentURL = destinationDocumentURL;
        setSelector(selector);
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
     * The CSS selector to use to find the imageUrl contained into destination document
     * Should be null if the destination document is unknown by selector finder
     * @return css selector
     */
    public String getSelector() {
        return selector;
    }

    /**
     * The CSS selector to use to find the imageUrl.
     * @param selector if the image link is available inside a secondary url the selector can contain expressions in the form
     *                 of pageSel:**css selector** selAttr:**attribute** where "css selector" is used to select the element
     *                 and "attribute" is the element's attribute to use to get the image url.
     */
    public void setSelector(String selector) {
        Matcher matcher = pageSelRE.matcher(selector);
        if (matcher.find() && matcher.groupCount() == 2) {
            this.selector = matcher.group(1);
            this.selAttr = matcher.group(2);
        } else {
            this.selector = selector;
            this.selAttr = null;
        }
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

    public String getSelAttr() {
        return selAttr;
    }

    public boolean hasPageSel() {
        return selAttr != null;
    }
}