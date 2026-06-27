package io.legado.app.model.localBook

import android.net.Uri
import android.os.Environment
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import java.io.File

data class WatchTxtImportResult(
    val scannedCount: Int,
    val importedCount: Int,
    val failedFiles: List<String>,
    val removedCount: Int = 0
)

class WatchTxtImporter(
    private val downloadDirProvider: () -> File = {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    },
    private val importFile: (File) -> Unit = { file ->
        LocalBook.importFile(Uri.fromFile(file))
    },
    private val shelfBooksProvider: () -> List<Book> = {
        appDb.bookDao.all
    },
    private val removeBookFromShelf: (Book) -> Unit = { book ->
        LocalBook.deleteBook(book, false)
        book.delete()
    }
) {

    fun scanAndImport(): WatchTxtImportResult {
        val downloadDir = downloadDirProvider()
        val files = WatchTxtFileFilter.listTxtFiles(downloadDir)
        var removedCount = 0
        WatchTxtShelfCleaner.staleDownloadTxtBooks(downloadDir, shelfBooksProvider())
            .forEach { book ->
                runCatching {
                    removeBookFromShelf(book)
                }.onSuccess {
                    removedCount += 1
                }
            }
        var importedCount = 0
        val failedFiles = arrayListOf<String>()
        files.forEach { file ->
            runCatching {
                importFile(file)
            }.onSuccess {
                importedCount += 1
            }.onFailure {
                failedFiles.add(file.name)
            }
        }
        return WatchTxtImportResult(
            scannedCount = files.size,
            importedCount = importedCount,
            failedFiles = failedFiles,
            removedCount = removedCount
        )
    }
}
