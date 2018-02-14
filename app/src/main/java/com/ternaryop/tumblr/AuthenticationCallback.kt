package com.ternaryop.tumblr

interface AuthenticationCallback {
    fun tumblrAuthenticated(token: String?, tokenSecret: String?, error: Exception?)
}
