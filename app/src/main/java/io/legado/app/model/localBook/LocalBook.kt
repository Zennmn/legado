package io.legado.app.model.localBook

import android.net.Uri
import androidx.core.net.toUri
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.EmptyFileException
import io.legado.app.exception.TocEmptyException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import splitties.init.appCtx

object LocalBook {

    @Throws(FileNotFoundException::class, SecurityException::class)
    fun getBookInputStream(book: Book): InputStream {
        val file = book.file()
        if (!file.isFile) throw FileNotFoundException("${file.path} 文件不存在")
        return FileInputStream(file)
    }

    fun getLastModified(book: Book): Result<Long> = runCatching {
        val file = book.file()
        if (file.exists()) file.lastModified() else throw FileNotFoundException("${file.path} 文件不存在")
    }

    @Throws(TocEmptyException::class)
    fun getChapterList(book: Book): ArrayList<BookChapter> {
        val list = ArrayList(LinkedHashSet(TextFile.getChapterList(book)))
        if (list.isEmpty()) throw TocEmptyException(appCtx.getString(R.string.chapter_list_empty))
        list.forEachIndexed { index, chapter ->
            chapter.index = index
            if (chapter.title.isEmpty()) chapter.title = "无标题章节"
        }
        book.durChapterTitle = list.getOrElse(book.durChapterIndex.coerceAtMost(list.lastIndex)) { list.last() }
            .getDisplayTitle()
        book.latestChapterTitle = list.last().getDisplayTitle()
        book.totalChapterNum = list.size
        book.latestChapterTime = getLastModified(book).getOrDefault(System.currentTimeMillis())
        return list
    }

    fun getContent(book: Book, chapter: BookChapter): String? {
        return try {
            TextFile.getContent(book, chapter)
        } catch (e: Exception) {
            AppLog.put("获取本地书籍内容失败\n${e.localizedMessage}", e)
            "获取本地书籍内容失败\n${e.localizedMessage}"
        }.takeUnless { it.isNullOrEmpty() && !chapter.isVolume }
    }

    fun importFile(uri: Uri): Book {
        val file = uri.toFile()
        if (!file.isFile || file.length() == 0L) throw EmptyFileException("Unexpected empty File")
        val bookUrl = file.absolutePath
        val existing = appDb.bookDao.getBook(bookUrl)
        val book = existing ?: Book(
            type = BookType.text or BookType.local,
            bookUrl = bookUrl,
            origin = BookType.localTag,
            originName = file.name,
            name = file.nameWithoutExtension,
            author = "",
            latestChapterTime = file.lastModified(),
            order = appDb.bookDao.minOrder - 1
        )
        book.type = BookType.text or BookType.local
        book.origin = BookType.localTag
        book.bookUrl = bookUrl
        book.originName = file.name
        if (book.name.isBlank()) book.name = file.nameWithoutExtension
        book.latestChapterTime = file.lastModified()
        if (existing == null) {
            appDb.bookDao.insert(book)
        } else {
            appDb.bookChapterDao.delByBook(bookUrl)
            appDb.bookDao.update(book)
        }
        return book
    }

    fun deleteBook(book: Book, deleteOriginal: Boolean) {
        appDb.bookChapterDao.delByBook(book.bookUrl)
        if (deleteOriginal) runCatching { book.file().delete() }
    }

    fun isOnBookShelf(fileName: String): Boolean = appDb.bookDao.hasFile(fileName)

    fun mergeBook(localBook: Book, onLineBook: Book?): Book = localBook

    private fun Book.file(): File {
        val uri = bookUrl.toUri()
        return if (uri.scheme == "file") File(uri.path ?: bookUrl) else File(bookUrl)
    }

    private fun Uri.toFile(): File {
        return if (scheme == "file" || scheme == null) File(path ?: toString()) else File(toString())
    }
}
