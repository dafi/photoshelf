package com.ternaryop.util.crypto

import java.security.MessageDigest

object CryptoUtil {
    fun md5(str: String) = MessageDigest.getInstance("MD5")
        .apply { update(str.toByteArray()) }
        .digest()
        .joinToString("") { "%02x".format(it) }
}
