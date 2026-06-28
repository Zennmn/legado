package io.legado.app.utils

import java.io.InputStream
import java.security.MessageDigest

/**
 * 将字符串转化为MD5
 */
@Suppress("unused")
object MD5Utils {

    fun md5Encode(str: String?): String {
        return digest(str?.toByteArray() ?: byteArrayOf())
    }

    fun md5Encode(inputStream: InputStream): String {
        return digest(inputStream.readBytes())
    }

    fun md5Encode16(str: String): String {
        var reStr = md5Encode(str)
        reStr = reStr.substring(8, 24)
        return reStr
    }

    private fun digest(bytes: ByteArray): String {
        return MessageDigest.getInstance("MD5")
            .digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
