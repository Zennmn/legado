package io.legado.app.model.localBook

import io.legado.app.data.entities.Book
import io.legado.app.help.book.isLocal
import java.io.File
import java.net.URI

object WatchTxtRetentionPolicy {

    fun shouldKeep(downloadDir: File, book: Book): Boolean {
        val path = retainedPath(downloadDir, book) ?: return false
        return File(path).isFile
    }

    fun retainedPath(downloadDir: File, book: Book): String? {
        if (!book.isLocal || !WatchTxtFileFilter.isTxtFileName(book.originName)) {
            return null
        }
        val bookPath = pathFromBookUrl(book.bookUrl)?.normalizedPath() ?: return null
        val downloadPath = downloadDir.normalizedPath()
        if (bookPath == downloadPath || !bookPath.startsWith(downloadPath + File.separator)) {
            return null
        }
        return bookPath
    }

    private fun pathFromBookUrl(bookUrl: String): String? {
        if (bookUrl.startsWith("content://", ignoreCase = true)) {
            return null
        }
        return if (bookUrl.startsWith("file://", ignoreCase = true)) {
            runCatching { File(URI(bookUrl)).path }.getOrNull()
        } else {
            bookUrl.takeIf { it.isNotBlank() }
        }
    }

    private fun String.normalizedPath(): String {
        return runCatching { File(this).canonicalPath }
            .getOrElse { File(this).absolutePath }
    }

    private fun File.normalizedPath(): String {
        return runCatching { canonicalPath }
            .getOrElse { absolutePath }
    }
}
