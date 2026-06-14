package io.legado.app.ui.watch

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.model.localBook.WatchTxtImportResult
import io.legado.app.model.localBook.WatchTxtImporter
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchBookshelfViewModel(application: Application) : BaseViewModel(application) {

    val booksFlow: Flow<List<Book>> = appDb.bookDao.flowLocal()
        .map { books ->
            books
                .filter { it.originName.endsWith(".txt", ignoreCase = true) }
                .sortedWith(compareByDescending<Book> { it.durChapterTime }.thenBy { it.name })
        }

    val scanResultLiveData = MutableLiveData<WatchTxtImportResult>()
    val scanningLiveData = MutableLiveData<Boolean>()

    fun scanDownload(importer: WatchTxtImporter = WatchTxtImporter()) {
        execute(context = IO) {
            importer.scanAndImport()
        }.onStart {
            scanningLiveData.postValue(true)
        }.onSuccess {
            scanResultLiveData.postValue(it)
            if (it.scannedCount == 0) {
                context.toastOnUi("Download 里没有 txt 文件")
            } else {
                context.toastOnUi("已扫描 ${it.scannedCount} 个 txt 文件")
            }
        }.onError {
            AppLog.put("扫描 Download 失败\n${it.localizedMessage}", it)
            context.toastOnUi("扫描 Download 失败\n${it.localizedMessage}")
        }.onFinally {
            scanningLiveData.postValue(false)
        }
    }
}
