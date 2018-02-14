package com.ternaryop.tumblr

interface Callback<in T> {
    /**
     * Used to communicate with tumblr after APIs calls, is always called on main thread
     * @param result some API can return data
     */
    fun complete(result: T)

    /**
     * Called when some error stop execution, is always called on main thread
     * @param e the exception that caused the failure
     */
    fun failure(e: Exception)
}
