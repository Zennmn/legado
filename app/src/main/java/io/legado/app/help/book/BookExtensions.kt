@file:Suppress("unused")

package io.legado.app.help.book

import android.net.Uri
import androidx.core.net.toUri
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.model.localBook.LocalBook
import java.io.File
import java.time.LocalDate
import java.time.Period.between
import kotlin.math.max
import kotlin.math.min

val Book.isAudio: Boolean get() = isType(BookType.audio)
val Book.isVideo: Boolean get() = isType(BookType.video)
val Book.isImage: Boolean get() = isType(BookType.image)
val Book.isLocal: Boolean get() = isType(BookType.local) || origin == BookType.localTag
val Book.isLocalTxt: Boolean get() = isLocal && originName.endsWith(".txt", true)
val Book.isEpub: Boolean get() = false
val Book.isUmd: Boolean get() = false
val Book.isPdf: Boolean get() = false
val Book.isMobi: Boolean get() = false
val Book.isOnLineTxt: Boolean get() = false
val Book.isWebFile: Boolean get() = false
val Book.isUpError: Boolean get() = false
val Book.isArchive: Boolean get() = false
val Book.isNotShelf: Boolean get() = isType(BookType.notShelf)
val Book.archiveName: String get() = originName

fun Book.contains(word: String?): Boolean {
    if (word.isNullOrEmpty()) return true
    return name.contains(word) || author.contains(word) || originName.contains(word)
}

fun Book.getLocalUri(): Uri = if (bookUrl.startsWith("file://")) bookUrl.toUri() else Uri.fromFile(File(bookUrl))
fun Book.getArchiveUri(): Uri? = null
fun Book.cacheLocalUri(uri: Uri) = Unit
fun Book.removeLocalUriCache() = Unit
fun Book.getRemoteUrl(): String? = null

fun Book.setType(vararg types: Int) {
    type = 0
    addType(*types)
}

fun Book.addType(vararg types: Int) {
    types.forEach { type = type or it }
}

fun Book.removeType(vararg types: Int) {
    types.forEach { type = type and it.inv() }
}

fun Book.removeAllBookType() = removeType(BookType.allBookType)
fun Book.clearType() { type = 0 }
fun Book.isType(bookType: Int): Boolean = type and bookType > 0
fun Book.upType() { if (type == 0) type = BookType.text or BookType.local }

fun Book.sync(oldBook: Book) {
    val curBook = appDb.bookDao.getBook(oldBook.bookUrl) ?: return
    durChapterTime = curBook.durChapterTime
    durChapterPos = curBook.durChapterPos
    durChapterIndex = curBook.durChapterIndex
    canUpdate = curBook.canUpdate
    readConfig = curBook.readConfig
}

fun Book.update() = appDb.bookDao.update(this)
fun Book.primaryStr(): String = origin + bookUrl

fun Book.updateTo(newBook: Book): Book {
    newBook.durChapterIndex = durChapterIndex
    newBook.durChapterTitle = durChapterTitle
    newBook.durChapterPos = durChapterPos
    newBook.durChapterTime = durChapterTime
    newBook.group = group
    newBook.order = order
    newBook.customCoverUrl = customCoverUrl
    newBook.customIntro = customIntro
    newBook.customTag = customTag
    newBook.canUpdate = canUpdate
    newBook.readConfig = readConfig
    return newBook
}

fun Book.hasVariable(key: String): Boolean = variableMap.containsKey(key)

fun Book.getFolderNameNoCache(): String = name.take(9) + bookUrl.hashCode().toString().replace("-", "")
fun Book.getBookSource(): Any? = null
fun Book.isLocalModified(): Boolean = isLocal && LocalBook.getLastModified(this).getOrDefault(0L) > latestChapterTime
fun Book.releaseHtmlData() {
    infoHtml = null
    tocHtml = null
}

fun Book.isSameNameAuthor(other: Any?): Boolean = other is Book && name == other.name && author == other.author
fun Book.getExportFileName(suffix: String): String = "$name 作者：${getRealAuthor()}.$suffix"
fun Book.getExportFileName(suffix: String, epubIndex: Int, jsStr: String? = null): String =
    "$name 作者：${getRealAuthor()} [$epubIndex].$suffix"

fun Book.simulatedTotalChapterNum(): Int {
    return if (readSimulating()) {
        val start = config.startDate ?: LocalDate.now()
        val daysPassed = between(start, LocalDate.now()).days + 1
        val chaptersToUnlock = max(0, (config.startChapter ?: 0) + (daysPassed * config.dailyChapters))
        min(totalChapterNum, chaptersToUnlock)
    } else {
        totalChapterNum
    }
}

fun Book.readSimulating(): Boolean = config.readSimulating
fun tryParesExportFileName(jsStr: String): Boolean = true
