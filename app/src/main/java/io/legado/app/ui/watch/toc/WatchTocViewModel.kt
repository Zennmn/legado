package io.legado.app.ui.watch.toc

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.simulatedTotalChapterNum
import kotlinx.coroutines.Dispatchers.IO

data class WatchTocState(
    val book: Book,
    val chapters: List<BookChapter>
)

class WatchTocViewModel(application: Application) : BaseViewModel(application) {

    val tocLiveData = MutableLiveData<WatchTocState>()
    val errorLiveData = MutableLiveData<String>()

    fun load(bookUrl: String) {
        execute(context = IO) {
            val book = appDb.bookDao.getBook(bookUrl)
                ?: throw NoStackTraceException("找不到书籍")
            val end = (book.simulatedTotalChapterNum() - 1).coerceAtLeast(0)
            val chapters = appDb.bookChapterDao.getChapterList(bookUrl, 0, end)
            WatchTocState(book, chapters)
        }.onSuccess {
            tocLiveData.postValue(it)
        }.onError {
            errorLiveData.postValue(it.localizedMessage ?: "目录加载失败")
        }
    }
}
