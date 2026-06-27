package io.legado.app.model

import android.content.Context
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Semaphore

object CacheBook {
    fun getOrCreate(bookUrl: String): CacheBookModel? = null
    fun getOrCreate(bookSource: Any?, book: Book): CacheBookModel = CacheBookModel(book)
    fun start(context: Context, book: Book, start: Int, end: Int) = Unit
    fun stop(context: Context) = Unit
    fun close() = Unit

    class CacheBookModel(private val book: Book) {
        fun download(scope: CoroutineScope, chapter: BookChapter, semaphore: Semaphore? = null) = Unit
        suspend fun downloadAwait(chapter: BookChapter): String = "离线手表版不支持在线缓存"
    }
}
