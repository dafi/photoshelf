package com.ternaryop.tumblr

import com.github.scribejava.core.model.OAuthRequest
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

/**
 * Convert a OAuthRequest POST into a multi-part OAuthRequest
 * @author jc
 */
class MultipartConverter constructor(private val originalRequest: OAuthRequest, bodyMap: Map<String, *>) {

    private val boundary = java.lang.Long.toHexString(System.nanoTime())

    private var bodyLength: Int = 0
    private lateinit var responsePieces: MutableList<Any>

    val request: OAuthRequest
        get() {
            val request = OAuthRequest(originalRequest.verb, originalRequest.url)
            request.addHeader("Authorization", originalRequest.headers["Authorization"])
            request.addHeader("Content-Type", "multipart/form-data, boundary=$boundary")
            request.addHeader("Content-length", bodyLength.toString())
            request.setPayload(complexPayload())
            return request
        }

    init {
        this.computeBody(bodyMap)
    }

    private fun complexPayload(): ByteArray {
        var used = 0
        val payload = ByteArray(bodyLength)
        var local: ByteArray
        for (piece in responsePieces) {
            local = if (piece is StringBuilder) {
                piece.toString().toByteArray()
            } else {
                piece as ByteArray
            }
            System.arraycopy(local, 0, payload, used, local.size)
            used += local.size
        }
        return payload
    }

    private fun addResponsePiece(arr: ByteArray) {
        responsePieces.add(arr)
        bodyLength += arr.size
    }

    private fun addResponsePiece(builder: StringBuilder) {
        responsePieces.add(builder)
        bodyLength += builder.toString().toByteArray().size
    }

    private fun computeBody(bodyMap: Map<String, *>) {
        responsePieces = ArrayList()

        var message = StringBuilder()
        message.append("Content-Type: multipart/form-data; boundary=").append(boundary).append("\r\n\r\n")
        for (key in bodyMap.keys) {
            val o = bodyMap[key] ?: continue
            if (o is File) {
                val mime = URLConnection.guessContentTypeFromName(o.name)
                val result = readFile(o)

                message.append("--").append(boundary).append("\r\n")
                message.append("Content-Disposition: form-data; name=\"")
                    .append(key)
                    .append("\"; filename=\"")
                    .append(o.name).append("\"\r\n")
                message.append("Content-Type: ").append(mime).append("\r\n\r\n")
                this.addResponsePiece(message)
                this.addResponsePiece(result)
                message = StringBuilder("\r\n")
            } else {
                message.append("--").append(boundary).append("\r\n")
                message.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n\r\n")
                message.append(o.toString()).append("\r\n")
            }
        }

        message.append("--").append(boundary).append("--\r\n")
        this.addResponsePiece(message)
    }

    private fun readFile(o: File): ByteArray {
        val result = ByteArray(o.length().toInt())

        DataInputStream(BufferedInputStream(FileInputStream(o))).use { it.readFully(result) }
        return result
    }
}
