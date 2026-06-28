package io.legado.app.help.book

import android.os.Build
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.constant.AppPattern.spaceRegex
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.utils.ChineseUtils
import io.legado.app.utils.escapeRegex
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx
import java.util.regex.Pattern

class ContentProcessor private constructor(
    private val bookName: String,
    private val bookOrigin: String
) {

    companion object {
        private val processors = hashMapOf<String, ContentProcessor>()
        private val isAndroid8 = Build.VERSION.SDK_INT in 26..27

        fun get(book: Book) = get(book.name, book.origin)

        fun get(bookName: String, bookOrigin: String): ContentProcessor {
            return processors.getOrPut(bookName + bookOrigin) {
                ContentProcessor(bookName, bookOrigin)
            }
        }

        fun upReplaceRules() = Unit
    }

    val removeSameTitleCache = hashSetOf<String>()

    fun upReplaceRules() = Unit
    fun getTitleReplaceRules(): List<Nothing> = emptyList()
    fun getContentReplaceRules(): List<Nothing> = emptyList()

    fun getContent(
        book: Book,
        chapter: BookChapter,
        content: String,
        includeTitle: Boolean = true,
        useReplace: Boolean = true,
        chineseConvert: Boolean = true,
        reSegment: Boolean = true
    ): BookContent {
        var mContent = content
        var sameTitleRemoved = false
        if (content != "null") {
            val fileName = chapter.getFileName("nr")
            if (!removeSameTitleCache.contains(fileName)) {
                try {
                    val name = Pattern.quote(book.name)
                    val title = chapter.title.escapeRegex().replace(spaceRegex, "\\s*")
                    val matcher = Pattern.compile("^(\\s|\\p{Punct}|${name})*${title}(\\s)*")
                        .matcher(mContent)
                    if (matcher.find()) {
                        mContent = mContent.substring(matcher.end())
                        sameTitleRemoved = true
                    }
                } catch (e: Exception) {
                    AppLog.put("去除重复标题出错\n${e.localizedMessage}", e)
                }
            }
            if (reSegment && book.getReSegment()) {
                mContent = ContentHelp.reSegment(mContent, chapter.title)
            }
            if (chineseConvert) {
                try {
                    when (AppConfig.chineseConverterType) {
                        1 -> mContent = ChineseUtils.t2s(mContent)
                        2 -> mContent = ChineseUtils.s2t(mContent)
                    }
                } catch (_: Exception) {
                    appCtx.toastOnUi("简繁转换出错")
                }
            }
            if (AppConfig.adaptSpecialStyle) {
                val useHtmlMap = mutableMapOf<String, String>()
                mContent = AppPattern.useHtmlRegex.replace(mContent) { matchResult ->
                    val placeholder = "特殊格式的占位不应该被看见${useHtmlMap.size}。"
                    useHtmlMap[placeholder] = "\n${matchResult.value.replace("\n", "")}\n"
                    placeholder
                }
                useHtmlMap.forEach { (placeholder, originalContent) ->
                    mContent = mContent.replace(placeholder, originalContent)
                }
            }
        }
        if (includeTitle) {
            mContent = chapter.getDisplayTitle() + "\n" + mContent
        }
        if (isAndroid8) {
            mContent = mContent.replace('\u00A0', ' ')
        }
        val contents = arrayListOf<String>()
        mContent.split("\n").forEach { str ->
            val paragraph = str.trim { it.code <= 0x20 || it == '　' }
            if (paragraph.isNotEmpty()) {
                contents.add(if (contents.isEmpty() && includeTitle) paragraph else "${ReadBookConfig.paragraphIndent}$paragraph")
            }
        }
        return BookContent(sameTitleRemoved, contents, null)
    }
}
