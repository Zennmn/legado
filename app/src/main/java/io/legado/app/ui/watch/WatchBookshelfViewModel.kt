package io.legado.app.ui.watch

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.localBook.WatchOnlyDataCleaner
import io.legado.app.model.localBook.WatchTxtImportResult
import io.legado.app.model.localBook.WatchTxtImporter
import io.legado.app.model.localBook.WatchTxtShelfCleaner
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class WatchBookshelfViewModel(application: Application) : BaseViewModel(application) {

    val booksFlow: Flow<List<Book>> = appDb.bookDao.flowLocal()
        .map { books ->
            books
                .filter { it.originName.endsWith(".txt", ignoreCase = true) }
                .sortedWith(compareByDescending<Book> { it.durChapterTime }.thenBy { it.name })
        }

    val scanResultLiveData = MutableLiveData<WatchTxtImportResult>()
    val scanningLiveData = MutableLiveData<Boolean>()

    fun isMissingDownloadTxtBook(book: Book, downloadDir: File): Boolean {
        return WatchTxtShelfCleaner.isMissingDownloadTxtBook(downloadDir, book)
    }

    fun deleteFromShelf(book: Book, message: String = "已从书架移除") {
        execute(context = IO) {
            LocalBook.deleteBook(book, false)
            book.delete()
        }.onSuccess {
            context.toastOnUi(message)
        }.onError {
            AppLog.put("从手表书架移除失败\n${it.localizedMessage}", it)
            context.toastOnUi("移除失败\n${it.localizedMessage}")
        }
    }

    fun scanDownload(importer: WatchTxtImporter = WatchTxtImporter()) {
        execute(context = IO) {
            WatchOnlyDataCleaner.clean()
            importer.scanAndImport()
        }.onStart {
            scanningLiveData.postValue(true)
        }.onSuccess {
            scanResultLiveData.postValue(it)
            val removedText = if (it.removedCount > 0) {
                "，移除 ${it.removedCount} 个失效书架项"
            } else {
                ""
            }
            context.toastOnUi(
                if (it.scannedCount == 0) {
                    "Download 里没有 txt 文件$removedText"
                } else {
                    "已扫描 ${it.scannedCount} 个 txt 文件$removedText"
                }
            )
        }.onError {
            AppLog.put("扫描 Download 失败\n${it.localizedMessage}", it)
            context.toastOnUi("扫描 Download 失败\n${it.localizedMessage}")
        }.onFinally {
            scanningLiveData.postValue(false)
        }
    }
}
