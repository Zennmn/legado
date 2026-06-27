package io.legado.app.data.entities

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.ChineseUtils
import io.legado.app.utils.MD5Utils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "chapters",
    primaryKeys = ["url", "bookUrl"],
    indices = [Index(value = ["bookUrl"], unique = false),
        Index(value = ["bookUrl", "index"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = Book::class,
        parentColumns = ["bookUrl"],
        childColumns = ["bookUrl"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BookChapter(
    var url: String = "",
    var title: String = "",
    var isVolume: Boolean = false,
    var baseUrl: String = "",
    var bookUrl: String = "",
    var index: Int = 0,
    var isVip: Boolean = false,
    var isPay: Boolean = false,
    var resourceUrl: String? = null,
    var tag: String? = null,
    var wordCount: String? = null,
    var start: Long? = null,
    var end: Long? = null,
    var startFragmentId: String? = null,
    var endFragmentId: String? = null,
    var variable: String? = null,
    var imgUrl: String? = null
) : Parcelable {

    @Ignore
    @IgnoredOnParcel
    private var titleMD5: String? = null

    fun update() {
        appDb.bookChapterDao.update(this)
    }

    override fun hashCode(): Int = url.hashCode()
    override fun equals(other: Any?): Boolean = other is BookChapter && other.url == url
    fun primaryStr(): String = bookUrl + url

    fun getDisplayTitle(
        replaceRules: List<*>? = null,
        useReplace: Boolean = true,
        chineseConvert: Boolean = true,
        replaceBook: Any? = null
    ): String {
        var displayTitle = title.replace(AppPattern.rnRegex, "")
        if (chineseConvert) {
            when (AppConfig.chineseConverterType) {
                1 -> displayTitle = ChineseUtils.t2s(displayTitle)
                2 -> displayTitle = ChineseUtils.s2t(displayTitle)
            }
        }
        return displayTitle
    }

    private fun ensureTitleMD5Init() {
        if (titleMD5 == null) titleMD5 = MD5Utils.md5Encode16(title)
    }

    @SuppressLint("DefaultLocale")
    fun getFileName(suffix: String = "nb"): String {
        ensureTitleMD5Init()
        return String.format("%05d-%s.%s", index, titleMD5, suffix)
    }

    @SuppressLint("DefaultLocale")
    fun getFontName(): String {
        ensureTitleMD5Init()
        return String.format("%05d-%s.ttf", index, titleMD5)
    }
}
