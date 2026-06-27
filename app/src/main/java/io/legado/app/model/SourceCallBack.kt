package io.legado.app.model

import android.app.Activity
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import kotlinx.coroutines.CoroutineScope

object SourceCallBack {
    const val START_READ = "startRead"
    const val END_READ = "endRead"
    const val SAVE_READ = "saveRead"
    const val ADD_BOOK_SHELF = "addBookShelf"
    const val CLICK_CUSTOM_BUTTON = "clickCustomButton"

    fun callBackBook(event: String, source: Any?, book: Book?, chapter: BookChapter? = null, content: String? = null) = Unit
    fun callBackBtn(activity: Activity, event: String, source: Any?, book: Book?, chapter: BookChapter?) = Unit
    fun callBackSource(scope: CoroutineScope, event: String, source: Any?) = Unit
}
