package io.legado.app.model.localBook

import io.legado.app.data.entities.Book
import io.legado.app.help.book.isLocal
import java.io.File
import java.net.URI

object WatchTxtShelfCleaner {

    fun staleDownloadTxtBooks(downloadDir: File, books: Iterable<Book>): List<Book> {
        return books.filter { isMissingDownloadTxtBook(downloadDir, it) }
    }

    fun isMissingDownloadTxtBook(downloadDir: File, book: Book): Boolean {
        val bookPath = pathInDownload(downloadDir, book) ?: return false
        return !File(bookPath).isFile
    }

    private fun pathInDownload(downloadDir: File, book: Book): String? {
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
