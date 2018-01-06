package com.ternaryop.photoshelf.extractor;

import org.json.JSONException;
import org.json.JSONObject;

public class ImageInfo {
    private String thumbnailUrl;
    private String documentUrl;
    private String imageUrl;

    public ImageInfo() {
    }

    public ImageInfo(JSONObject json) throws JSONException {
        thumbnailUrl = json.getString("thumbnailUrl");
        documentUrl = json.optString("documentUrl", null);
        imageUrl = json.optString("imageUrl", null);
    }

    /**
     * The thumbnail image url.
     * This is present on the HTML document from which pick images  
     * @return thumbnail url
     */
    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    /**
     * The destination document containing the image url
     * @return destination document url
     */
    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String imageURL) {
        this.documentUrl = imageURL;
    }

    /**
     * The image url present inside the destination document url.
     * If null must be retrieved from destination document
     * @return image url
     */
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "thumb " + thumbnailUrl + " doc " + documentUrl;
    }
}
