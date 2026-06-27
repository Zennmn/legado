package io.legado.app.model.localBook

import android.os.Environment
import io.legado.app.data.appDb
import java.io.File

data class WatchOnlyDataCleanResult(
    val removedBooks: Int,
    val removedBookSources: Int,
    val removedRssSources: Int,
    val removedReplaceRules: Int,
    val removedRuleSubs: Int,
    val removedHttpTts: Int,
    val removedDictRules: Int,
    val removedServers: Int
)

object WatchOnlyDataCleaner {

    fun clean(
        downloadDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    ): WatchOnlyDataCleanResult {
        val booksToRemove = WatchTxtShelfCleaner.booksToRemove(downloadDir, appDb.bookDao.all)
        booksToRemove.forEach { book ->
            LocalBook.deleteBook(book, false)
            appDb.bookChapterDao.delByBook(book.bookUrl)
            appDb.bookDao.delete(book)
        }

        val bookSources = appDb.bookSourceDao.all
        val rssSources = appDb.rssSourceDao.all
        val replaceRules = appDb.replaceRuleDao.all
        val ruleSubs = appDb.ruleSubDao.all
        val httpTts = appDb.httpTTSDao.all
        val dictRules = appDb.dictRuleDao.all
        val servers = appDb.serverDao.all

        if (bookSources.isNotEmpty()) appDb.bookSourceDao.delete(*bookSources.toTypedArray())
        if (rssSources.isNotEmpty()) appDb.rssSourceDao.delete(*rssSources.toTypedArray())
        if (replaceRules.isNotEmpty()) appDb.replaceRuleDao.delete(*replaceRules.toTypedArray())
        if (ruleSubs.isNotEmpty()) appDb.ruleSubDao.delete(*ruleSubs.toTypedArray())
        if (httpTts.isNotEmpty()) appDb.httpTTSDao.delete(*httpTts.toTypedArray())
        if (dictRules.isNotEmpty()) appDb.dictRuleDao.delete(*dictRules.toTypedArray())
        if (servers.isNotEmpty()) appDb.serverDao.delete(*servers.toTypedArray())
        appDb.cookieDao.clearAll()
        appDb.cacheDao.clearAll()

        return WatchOnlyDataCleanResult(
            removedBooks = booksToRemove.size,
            removedBookSources = bookSources.size,
            removedRssSources = rssSources.size,
            removedReplaceRules = replaceRules.size,
            removedRuleSubs = ruleSubs.size,
            removedHttpTts = httpTts.size,
            removedDictRules = dictRules.size,
            removedServers = servers.size
        )
    }
}
