package com.ternaryop.tumblr

class TumblrException : RuntimeException {

    constructor(detailMessage: String) : super(detailMessage)

    constructor(throwable: Throwable) : super(throwable)

    companion object {

        /**
         *
         */
        private const val serialVersionUID = -3382759637444771469L
    }
}
