package com.ternaryop.tumblr

import org.scribe.model.OAuthRequest
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URLConnection

/**
 * Convert a OAuthRequest POST into a multi-part OAuthRequest
 * @author jc
 */
class MultipartConverter @Throws(IOException::class)
constructor(private val originalRequest: OAuthRequest, bodyMap: Map<String, *>) {

    private val boundary = java.lang.Long.toHexString(System.nanoTime())

    private var bodyLength: Int = 0
    private lateinit var responsePieces: MutableList<Any>

    val request: OAuthRequest
        get() {
            val request = OAuthRequest(originalRequest.verb, originalRequest.url)
            request.addHeader("Authorization", originalRequest.headers["Authorization"])
            request.addHeader("Content-Type", "multipart/form-data, boundary=$boundary")
            request.addHeader("Content-length", bodyLength.toString())
            request.addPayload(complexPayload())
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

    @Throws(IOException::class)
    private fun computeBody(bodyMap: Map<String, *>) {
        responsePieces = ArrayList()

        var message = StringBuilder()
        message.append("Content-Type: multipart/form-data; boundary=").append(boundary).append("\r\n\r\n")
        for (key in bodyMap.keys) {
            val o = bodyMap[key] ?: continue
            if (o is File) {
                val mime = URLConnection.guessContentTypeFromName(o.name)

                var dis: DataInputStream? = null
                val result = ByteArray(o.length().toInt())

                try {
                    dis = DataInputStream(BufferedInputStream(FileInputStream(o)))
                    dis.readFully(result)
                } finally {
                    if (dis != null) try {
                        dis.close()
                    } catch (ignored: Exception) {
                    }
                }

                message.append("--").append(boundary).append("\r\n")
                message.append("Content-Disposition: form-data; name=\"").append(key).append("\"; filename=\"").append(o.name).append("\"\r\n")
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
}
