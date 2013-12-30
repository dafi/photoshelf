package com.ternaryop.photoshelf;

public class ImageInfo {
	private String thumbnailURL;
	private String destinationDocumentURL;
	private String imageURL;
	private String selector;

	public ImageInfo(String thumbnailURL, String destinationDocumentURL, String selector) {
		this.thumbnailURL = thumbnailURL;
		this.destinationDocumentURL = destinationDocumentURL;
		this.selector = selector;
	}

	/**
	 * The thumbnail image url.
	 * This is present on the HTML document from which pick images  
	 * @return
	 */
	public String getThumbnailURL() {
		return thumbnailURL;
	}

	public void setThumbnailURL(String thumbnailURL) {
		this.thumbnailURL = thumbnailURL;
	}

	/**
	 * The destination document containing the image url
	 * @return
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
	 * @return
	 */
	public String getSelector() {
		return selector;
	}

	public void setSelector(String selector) {
		this.selector = selector;
	}

	/**
	 * The image url present inside the destination document url.
	 * If null must be retrieved from destination document
	 * @return
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

}