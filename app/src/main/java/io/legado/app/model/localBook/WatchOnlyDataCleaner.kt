package io.legado.app.model.localBook

import android.os.Environment
import io.legado.app.data.appDb
import java.io.File

data class WatchOnlyDataCleanResult(
    val removedBooks: Int,
)

object WatchOnlyDataCleaner {

    fun clean(
        downloadDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    ): WatchOnlyDataCleanResult {
        val booksToRemove = WatchTxtShelfCleaner.booksToRemove(downloadDir, appDb.bookDao.all)
        booksToRemove.forEach { book ->
            LocalBook.deleteBook(book, false)
            appDb.bookChapterDao.delByBook(book.bookUrl)
            appDb.bookDao.delete(book)
        }

        return WatchOnlyDataCleanResult(
            removedBooks = booksToRemove.size
        )
    }
}
