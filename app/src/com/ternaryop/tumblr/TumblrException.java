package com.ternaryop.tumblr;

public class TumblrException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3382759637444771469L;


	public TumblrException() {
		super();
	}

	public TumblrException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public TumblrException(String detailMessage) {
		super(detailMessage);
	}

	public TumblrException(Throwable throwable) {
		super(throwable);
	}
}
