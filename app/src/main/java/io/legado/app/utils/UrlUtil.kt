package io.legado.app.utils

object UrlUtil {

    private val unExpectFileSuffixs = arrayOf("php", "html")
    private val fileSuffixRegex = Regex("^[a-z\\d]+$", RegexOption.IGNORE_CASE)

    fun replaceReservedChar(text: String): String {
        return text.replace("%", "%25")
            .replace(" ", "%20")
            .replace("\"", "%22")
            .replace("#", "%23")
            .replace("&", "%26")
            .replace("(", "%28")
            .replace(")", "%29")
            .replace("+", "%2B")
            .replace(",", "%2C")
            .replace("/", "%2F")
            .replace(":", "%3A")
            .replace(";", "%3B")
            .replace("<", "%3C")
            .replace("=", "%3D")
            .replace(">", "%3E")
            .replace("?", "%3F")
            .replace("@", "%40")
            .replace("\\", "%5C")
            .replace("|", "%7C")
    }

    fun getFileName(fileUrl: String, headerMap: Map<String, String>? = null): String? {
        return kotlin.runCatching {
            val url = java.net.URL(fileUrl)
            var fileName: String? = getFileNameFromPath(url)
            fileName
        }.getOrNull()
    }

    private fun getFileNameFromPath(fileUrl: java.net.URL): String? {
        val path = fileUrl.path ?: return null
        val suffix = getSuffix(path, "")
        return if (suffix != "" && !unExpectFileSuffixs.contains(suffix)) {
            path.substringAfterLast("/")
        } else {
            null
        }
    }

    fun getSuffix(str: String, default: String? = null): String {
        val suffix = str.substringAfterLast("/")
            .substringBefore("?")
            .substringBefore("#")
            .substringAfterLast(".", "")
        return if (suffix.length > 5 || !suffix.matches(fileSuffixRegex)) {
            default ?: "ext"
        } else {
            suffix
        }
    }
}
