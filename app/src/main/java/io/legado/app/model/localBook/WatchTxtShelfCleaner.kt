package io.legado.app.model.localBook

import io.legado.app.data.entities.Book
import java.io.File

object WatchTxtShelfCleaner {

    fun booksToRemove(downloadDir: File, books: Iterable<Book>): List<Book> {
        return books.filterNot { WatchTxtRetentionPolicy.shouldKeep(downloadDir, it) }
    }

    fun staleDownloadTxtBooks(downloadDir: File, books: Iterable<Book>): List<Book> {
        return booksToRemove(downloadDir, books)
    }

    fun isMissingDownloadTxtBook(downloadDir: File, book: Book): Boolean {
        val path = WatchTxtRetentionPolicy.retainedPath(downloadDir, book) ?: return false
        return !File(path).isFile
    }
}
