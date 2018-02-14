package com.ternaryop.feedly

/**
 * Created by dave on 04/09/17.
 * The feedly token has expired
 */

class TokenExpiredException(message: String) : RuntimeException(message)
