package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.legado.app.constant.AppPattern
import io.legado.app.constant.BookType
import io.legado.app.constant.PageAnim
import io.legado.app.data.appDb
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.nio.charset.Charset
import java.time.LocalDate
import kotlin.math.max

@Parcelize
@TypeConverters(Book.Converters::class)
@Entity(
    tableName = "books",
    indices = [Index(value = ["name", "author"], unique = true)]
)
data class Book(
    @PrimaryKey
    @ColumnInfo(defaultValue = "")
    var bookUrl: String = "",
    @ColumnInfo(defaultValue = "")
    var tocUrl: String = "",
    @ColumnInfo(defaultValue = BookType.localTag)
    var origin: String = BookType.localTag,
    @ColumnInfo(defaultValue = "")
    var originName: String = "",
    @ColumnInfo(defaultValue = "")
    var name: String = "",
    @ColumnInfo(defaultValue = "")
    var author: String = "",
    var kind: String? = null,
    var customTag: String? = null,
    var coverUrl: String? = null,
    var customCoverUrl: String? = null,
    var intro: String? = null,
    var customIntro: String? = null,
    var charset: String? = null,
    @ColumnInfo(defaultValue = "0")
    var type: Int = BookType.text,
    @ColumnInfo(defaultValue = "0")
    var group: Long = 0,
    var latestChapterTitle: String? = null,
    @ColumnInfo(defaultValue = "0")
    var latestChapterTime: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    var lastCheckTime: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    var lastCheckCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var totalChapterNum: Int = 0,
    var durChapterTitle: String? = null,
    @ColumnInfo(defaultValue = "0")
    var durChapterIndex: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var durVolumeIndex: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var chapterInVolumeIndex: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var durChapterPos: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var durChapterTime: Long = System.currentTimeMillis(),
    var wordCount: String? = null,
    @ColumnInfo(defaultValue = "1")
    var canUpdate: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    var order: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var originOrder: Int = 0,
    var variable: String? = null,
    var readConfig: ReadConfig? = null,
    @ColumnInfo(defaultValue = "0")
    var syncTime: Long = 0L
) : Parcelable {

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    val variableMap: HashMap<String, String> by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: hashMapOf()
    }

    @Ignore
    @IgnoredOnParcel
    var infoHtml: String? = null

    @Ignore
    @IgnoredOnParcel
    var tocHtml: String? = null

    @Ignore
    @IgnoredOnParcel
    private var folderName: String? = null

    @get:Ignore
    @IgnoredOnParcel
    val lastChapterIndex: Int get() = totalChapterNum - 1

    @IgnoredOnParcel
    val config: ReadConfig
        get() {
            if (readConfig == null) readConfig = ReadConfig()
            return readConfig!!
        }

    override fun equals(other: Any?): Boolean = other is Book && other.bookUrl == bookUrl
    override fun hashCode(): Int = bookUrl.hashCode()

    fun getRealAuthor(): String = author.replace(AppPattern.authorRegex, "")
    fun getUnreadChapterNum(): Int = max(simulatedTotalChapterNum() - durChapterIndex - 1, 0)
    fun getDisplayCover(): String? = customCoverUrl.takeUnless { it.isNullOrEmpty() } ?: coverUrl
    fun getDisplayIntro(): String? = customIntro.takeUnless { it.isNullOrEmpty() } ?: intro
    fun upCustomIntro() { customIntro = intro }
    fun fileCharset(): Charset = Charset.forName(charset ?: "UTF-8")

    fun setReverseToc(reverseToc: Boolean) { config.reverseToc = reverseToc }
    fun getReverseToc(): Boolean = config.reverseToc
    fun setUseReplaceRule(useReplaceRule: Boolean) { config.useReplaceRule = useReplaceRule }
    fun getUseReplaceRule(): Boolean = config.useReplaceRule ?: AppConfig.replaceEnableDefault
    fun setReSegment(reSegment: Boolean) { config.reSegment = reSegment }
    fun getReSegment(): Boolean = config.reSegment
    fun setPageAnim(pageAnim: Int?) { config.pageAnim = pageAnim }
    fun getPageAnim(): Int = (config.pageAnim ?: ReadBookConfig.pageAnim).takeIf { it >= 0 }
        ?: ReadBookConfig.pageAnim
    fun setImageStyle(imageStyle: String?) { config.imageStyle = imageStyle }
    fun getImageStyle(): String? = config.imageStyle
    fun setTtsEngine(ttsEngine: String?) { config.ttsEngine = ttsEngine }
    fun getTtsEngine(): String? = config.ttsEngine
    fun setSplitLongChapter(limitLongContent: Boolean) { config.splitLongChapter = limitLongContent }
    fun getSplitLongChapter(): Boolean = config.splitLongChapter
    fun setReadSimulating(readSimulating: Boolean) { config.readSimulating = readSimulating }
    fun getReadSimulating(): Boolean = config.readSimulating
    fun setStartDate(startDate: LocalDate?) { config.startDate = startDate }
    fun getStartDate(): LocalDate? = if (config.readSimulating) config.startDate ?: LocalDate.now() else LocalDate.now()
    fun setStartChapter(startChapter: Int) { config.startChapter = startChapter }
    fun getStartChapter(): Int = if (config.readSimulating) config.startChapter ?: 0 else durChapterIndex
    fun setDailyChapters(dailyChapters: Int) { config.dailyChapters = dailyChapters }
    fun getDailyChapters(): Int = config.dailyChapters
    fun setOpenCredits(openCredits: Int) { config.openCredits = openCredits }
    fun getOpenCredits(): Int = config.openCredits
    fun setCloseCredits(closeCredits: Int) { config.closeCredits = closeCredits }
    fun getCloseCredits(): Int = config.closeCredits
    fun setPlayMode(playMode: Int) { config.playMode = playMode }
    fun getPlayMode(): Int = config.playMode
    fun setPlaySpeed(playSpeed: Float) { config.playSpeed = playSpeed }
    fun getPlaySpeed(): Float = config.playSpeed
    fun getDelTag(tag: Long): Boolean = config.delTag and tag == tag
    fun addDelTag(tag: Long) { config.delTag = config.delTag or tag }
    fun removeDelTag(tag: Long) { config.delTag = config.delTag and tag.inv() }

    fun getFolderName(): String {
        folderName?.let { return it }
        folderName = name.take(9) + bookUrl.hashCode().toString().replace("-", "")
        return folderName!!
    }

    fun toReplaceBook(): Any? = null

    fun migrateTo(newBook: Book, toc: List<BookChapter>): Book {
        if (toc.isNotEmpty()) {
            newBook.durChapterIndex = durChapterIndex.coerceIn(0, toc.lastIndex)
            newBook.durChapterTitle = toc[newBook.durChapterIndex].getDisplayTitle()
            newBook.durChapterPos = durChapterPos
        }
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

    fun createBookMark(): Bookmark = Bookmark(bookName = name, bookAuthor = author)

    fun save() {
        if (appDb.bookDao.has(bookUrl)) appDb.bookDao.update(this) else appDb.bookDao.insert(this)
    }

    fun delete() {
        if (ReadBook.book?.bookUrl == bookUrl) ReadBook.book = null
        appDb.bookDao.delete(this)
    }

    companion object {
        const val hTag = 2L
        const val rubyTag = 4L
        const val imgStyleDefault = "DEFAULT"
        const val imgStyleFull = "FULL"
        const val imgStyleText = "TEXT"
        const val imgStyleSingle = "SINGLE"
    }

    @Parcelize
    data class ReadConfig(
        var reverseToc: Boolean = false,
        var pageAnim: Int? = null,
        var reSegment: Boolean = false,
        var imageStyle: String? = null,
        var useReplaceRule: Boolean? = null,
        var delTag: Long = 0L,
        var ttsEngine: String? = null,
        var splitLongChapter: Boolean = true,
        var readSimulating: Boolean = false,
        var startDate: LocalDate? = null,
        var startChapter: Int? = null,
        var dailyChapters: Int = 3,
        var openCredits: Int = 0,
        var closeCredits: Int = 0,
        var playMode: Int = 0,
        var playSpeed: Float = 1.0f
    ) : Parcelable

    class Converters {
        @TypeConverter
        fun readConfigToString(config: ReadConfig?): String = GSON.toJson(config)

        @TypeConverter
        fun stringToReadConfig(json: String?): ReadConfig? = GSON.fromJsonObject<ReadConfig>(json).getOrNull()
    }
}
