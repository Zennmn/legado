package io.legado.app.utils

import io.legado.app.constant.AppLog
import java.net.URL

object NetworkUtils {

    fun getAbsoluteURL(baseURL: String?, relativePath: String): String {
        if (baseURL.isNullOrEmpty()) return relativePath.trim()
        val absoluteUrl = try {
            URL(baseURL.substringBefore(","))
        } catch (e: Exception) {
            e.printOnDebug()
            null
        }
        return getAbsoluteURL(absoluteUrl, relativePath)
    }

    fun getAbsoluteURL(baseURL: URL?, relativePath: String): String {
        val relativePathTrim = relativePath.trim()
        if (baseURL == null || relativePathTrim.isAbsUrl() || relativePathTrim.isDataUrl()) {
            return relativePathTrim
        }
        if (relativePathTrim.startsWith("javascript")) return ""
        return try {
            URL(baseURL, relativePath).toString()
        } catch (e: Exception) {
            AppLog.put("网址拼接出错\n${e.localizedMessage}", e)
            relativePathTrim
        }
    }
}
