# Watch TXT Reader Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the current app into a single-purpose offline Android watch TXT reader that reads `/Download/**/*.txt` and reuses the existing Legado reading engine.

**Architecture:** Keep the current package name, Room database, local TXT parser, and `ReadBookActivity` reading path. Add a strict Download TXT retention policy, clean non-target data at scan/startup boundaries, then prune online/media/Web/RSS entry points, dependencies, assets, and source directories in compile-verified stages.

**Tech Stack:** Kotlin, Android XML/ViewBinding, Room, Coroutines, Gradle Android Plugin, JUnit4 local unit tests.

## Global Constraints

- Keep the current package name and Room database so existing watch installs can upgrade in place.
- Support only `/Download/**/*.txt` files.
- Treat the app as fully offline: no network permission and no network-facing product feature.
- Keep only watch-oriented UI plus an About screen for version, license, and privacy information.
- Reuse `ReadBookActivity`, `ReadBook`, `ReadBookViewModel`, `ReadBookConfig`, local TXT parsing, pagination, and progress storage.
- Do not add a new reading engine, UI framework, network import fallback, cloud sync, Web UI, EPUB/PDF/UMD/MOBI/archive/manga/audio/video support, or compatibility UI for old online features.
- Each task must end with a commit after the listed verification passes.

---

## File Structure

- Create `app/src/main/java/io/legado/app/model/localBook/WatchTxtRetentionPolicy.kt`: pure local filesystem policy for deciding which books are valid Download TXT books.
- Modify `app/src/main/java/io/legado/app/model/localBook/WatchTxtShelfCleaner.kt`: delegate retain/remove decisions to `WatchTxtRetentionPolicy`.
- Modify `app/src/main/java/io/legado/app/model/localBook/WatchTxtImporter.kt`: remove every non-retained book during Download scan, not only missing Download TXT books.
- Create `app/src/test/java/io/legado/app/model/localBook/WatchTxtRetentionPolicyTest.kt`: local unit tests for retention decisions.
- Modify `app/src/test/java/io/legado/app/model/localBook/WatchTxtImporterTest.kt`: assert non-target books are removed during scan.
- Create `app/src/main/java/io/legado/app/model/localBook/WatchOnlyDataCleaner.kt`: one-shot cleaner for source/RSS/rule/TTS/server/cookie/cache data no longer used by the offline watch product.
- Modify `app/src/main/java/io/legado/app/data/dao/CacheDao.kt`: add `clearAll()` for full offline cleanup.
- Modify `app/src/main/java/io/legado/app/data/dao/CookieDao.kt`: add `clearAll()` for full offline cleanup.
- Modify `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfViewModel.kt`: run `WatchOnlyDataCleaner` and Download scan together.
- Modify `app/src/main/java/io/legado/app/App.kt`: keep only local watch startup work.
- Modify `app/src/main/java/io/legado/app/help/DefaultData.kt`: import only local TXT TOC defaults on version changes.
- Modify `app/src/main/AndroidManifest.xml`: keep only offline watch activities, permission activity, reader, TOC, About, and local storage permissions.
- Create `app/src/main/java/io/legado/app/ui/watch/about/WatchAboutActivity.kt`: small offline About page.
- Create `app/src/main/res/layout/activity_watch_about.xml`: black watch-friendly About page.
- Modify `README.md` and `app/src/main/assets/privacyPolicy.md`: describe the offline watch TXT product.
- Delete online/Web/RSS/media source areas after the offline Manifest is in place.
- Modify `app/build.gradle`, `settings.gradle`, `gradle/libs.versions.toml`: prune unused plugins, modules, and dependencies after source references are removed.
- Delete `modules/web`, embedded Web assets, Cronet assets, and online default data after retained code no longer references them.

---

### Task 1: Download TXT Retention Policy

**Files:**
- Create: `app/src/main/java/io/legado/app/model/localBook/WatchTxtRetentionPolicy.kt`
- Modify: `app/src/main/java/io/legado/app/model/localBook/WatchTxtShelfCleaner.kt`
- Test: `app/src/test/java/io/legado/app/model/localBook/WatchTxtRetentionPolicyTest.kt`

**Interfaces:**
- Consumes: `WatchTxtFileFilter.isTxtFileName(name: String): Boolean`, `Book`, and `Book.isLocal`.
- Produces: `WatchTxtRetentionPolicy.retainedPath(downloadDir: File, book: Book): String?`, `WatchTxtRetentionPolicy.shouldKeep(downloadDir: File, book: Book): Boolean`, `WatchTxtShelfCleaner.booksToRemove(downloadDir: File, books: Iterable<Book>): List<Book>`.

- [ ] **Step 1: Write the failing retention policy tests**

Create `app/src/test/java/io/legado/app/model/localBook/WatchTxtRetentionPolicyTest.kt`:

```kotlin
package io.legado.app.model.localBook

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WatchTxtRetentionPolicyTest {

    @Test
    fun keepsExistingVisibleTxtInsideDownload() {
        val root = Files.createTempDirectory("watch-retain-").toFile()
        try {
            val file = File(root, "book.txt").apply { writeText("hello") }
            val book = localTxtBook(file)

            assertTrue(WatchTxtRetentionPolicy.shouldKeep(root, book))
            assertEquals(file.canonicalPath, WatchTxtRetentionPolicy.retainedPath(root, book))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsMissingTxtInsideDownload() {
        val root = Files.createTempDirectory("watch-retain-missing-").toFile()
        try {
            val missing = File(root, "missing.txt")

            assertFalse(WatchTxtRetentionPolicy.shouldKeep(root, localTxtBook(missing)))
            assertEquals(missing.canonicalPath, WatchTxtRetentionPolicy.retainedPath(root, localTxtBook(missing)))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsTxtOutsideDownload() {
        val root = Files.createTempDirectory("watch-retain-root-").toFile()
        val outside = Files.createTempDirectory("watch-retain-outside-").toFile()
        try {
            val file = File(outside, "outside.txt").apply { writeText("outside") }

            assertFalse(WatchTxtRetentionPolicy.shouldKeep(root, localTxtBook(file)))
            assertNull(WatchTxtRetentionPolicy.retainedPath(root, localTxtBook(file)))
        } finally {
            root.deleteRecursively()
            outside.deleteRecursively()
        }
    }

    @Test
    fun rejectsNonTxtInsideDownload() {
        val root = Files.createTempDirectory("watch-retain-non-txt-").toFile()
        try {
            val file = File(root, "book.epub").apply { writeText("epub") }

            assertFalse(WatchTxtRetentionPolicy.shouldKeep(root, localBook(file, BookType.local)))
            assertNull(WatchTxtRetentionPolicy.retainedPath(root, localBook(file, BookType.local)))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsContentUriLegacyBooks() {
        val root = Files.createTempDirectory("watch-retain-content-").toFile()
        try {
            val book = Book(
                bookUrl = "content://downloads/book.txt",
                originName = "book.txt",
                name = "book",
                type = BookType.local or BookType.text
            )

            assertFalse(WatchTxtRetentionPolicy.shouldKeep(root, book))
            assertNull(WatchTxtRetentionPolicy.retainedPath(root, book))
        } finally {
            root.deleteRecursively()
        }
    }

    private fun localTxtBook(file: File): Book {
        return localBook(file, BookType.local or BookType.text)
    }

    private fun localBook(file: File, type: Int): Book {
        return Book(
            bookUrl = file.absolutePath,
            originName = file.name,
            name = file.nameWithoutExtension,
            type = type
        )
    }
}
```

- [ ] **Step 2: Run the new test to verify RED**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.WatchTxtRetentionPolicyTest"
```

Expected: FAIL with unresolved reference `WatchTxtRetentionPolicy`.

- [ ] **Step 3: Add the retention policy**

Create `app/src/main/java/io/legado/app/model/localBook/WatchTxtRetentionPolicy.kt`:

```kotlin
package io.legado.app.model.localBook

import io.legado.app.data.entities.Book
import io.legado.app.help.book.isLocal
import java.io.File
import java.net.URI

object WatchTxtRetentionPolicy {

    fun shouldKeep(downloadDir: File, book: Book): Boolean {
        val path = retainedPath(downloadDir, book) ?: return false
        return File(path).isFile
    }

    fun retainedPath(downloadDir: File, book: Book): String? {
        if (!book.isLocal || !WatchTxtFileFilter.isTxtFileName(book.originName)) {
            return null
        }
        val bookPath = pathFromBookUrl(book.bookUrl)?.normalizedPath() ?: return null
        val downloadPath = downloadDir.normalizedPath()
        if (bookPath == downloadPath || !bookPath.startsWith(downloadPath + File.separator)) {
            return null
        }
        return bookPath
    }

    private fun pathFromBookUrl(bookUrl: String): String? {
        if (bookUrl.startsWith("content://", ignoreCase = true)) {
            return null
        }
        return if (bookUrl.startsWith("file://", ignoreCase = true)) {
            runCatching { File(URI(bookUrl)).path }.getOrNull()
        } else {
            bookUrl.takeIf { it.isNotBlank() }
        }
    }

    private fun String.normalizedPath(): String {
        return runCatching { File(this).canonicalPath }
            .getOrElse { File(this).absolutePath }
    }

    private fun File.normalizedPath(): String {
        return runCatching { canonicalPath }
            .getOrElse { absolutePath }
    }
}
```

- [ ] **Step 4: Replace `WatchTxtShelfCleaner` with the policy wrapper**

Replace `app/src/main/java/io/legado/app/model/localBook/WatchTxtShelfCleaner.kt` with:

```kotlin
package io.legado.app.model.localBook

import io.legado.app.data.entities.Book
import java.io.File

object WatchTxtShelfCleaner {

    fun booksToRemove(downloadDir: File, books: Iterable<Book>): List<Book> {
        return books.filterNot { WatchTxtRetentionPolicy.shouldKeep(downloadDir, it) }
    }

    fun staleDownloadTxtBooks(downloadDir: File, books: Iterable<Book>): List<Book> {
        return booksToRemove(downloadDir, books)
    }

    fun isMissingDownloadTxtBook(downloadDir: File, book: Book): Boolean {
        val path = WatchTxtRetentionPolicy.retainedPath(downloadDir, book) ?: return false
        return !File(path).isFile
    }
}
```

- [ ] **Step 5: Run retention tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.WatchTxtRetentionPolicyTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/model/localBook/WatchTxtRetentionPolicy.kt app/src/main/java/io/legado/app/model/localBook/WatchTxtShelfCleaner.kt app/src/test/java/io/legado/app/model/localBook/WatchTxtRetentionPolicyTest.kt
git commit -m "feat: add watch txt retention policy"
```

---

### Task 2: Apply Retention During Download Scan

**Files:**
- Modify: `app/src/main/java/io/legado/app/model/localBook/WatchTxtImporter.kt`
- Modify: `app/src/test/java/io/legado/app/model/localBook/WatchTxtImporterTest.kt`

**Interfaces:**
- Consumes: `WatchTxtShelfCleaner.booksToRemove(downloadDir: File, books: Iterable<Book>): List<Book>`.
- Produces: `WatchTxtImporter.scanAndImport(): WatchTxtImportResult` where `removedCount` includes all non-retained books removed during scan.

- [ ] **Step 1: Replace the non-target cleanup test**

In `app/src/test/java/io/legado/app/model/localBook/WatchTxtImporterTest.kt`, replace `scanAndImportKeepsNonDownloadAndNonTxtBooksWhenPruning()` with:

```kotlin
    @Test
    fun scanAndImportRemovesNonDownloadAndNonTxtBooksWhenPruning() {
        val root = Files.createTempDirectory("watch-txt-import-remove-legacy-").toFile()
        val otherRoot = Files.createTempDirectory("watch-txt-import-other-").toFile()
        val removed = arrayListOf<String>()
        try {
            val outsideTxt = File(otherRoot, "outside.txt").apply { writeText("outside") }
            val localPdf = File(root, "legacy.pdf").apply { writeText("pdf") }

            val importer = WatchTxtImporter(
                downloadDirProvider = { root },
                importFile = {},
                shelfBooksProvider = {
                    listOf(
                        localTxtBook(outsideTxt),
                        localBook(localPdf, BookType.local)
                    )
                },
                removeBookFromShelf = { removed.add(it.originName) }
            )

            val result = importer.scanAndImport()

            assertEquals(listOf("outside.txt", "legacy.pdf"), removed.sorted())
            assertEquals(2, result.removedCount)
            assertEquals(0, result.importedCount)
            assertEquals(0, result.scannedCount)
        } finally {
            root.deleteRecursively()
            otherRoot.deleteRecursively()
        }
    }
```

- [ ] **Step 2: Run importer tests to verify RED**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.WatchTxtImporterTest"
```

Expected: FAIL because `WatchTxtImporter` still removes only stale Download TXT books.

- [ ] **Step 3: Use `booksToRemove()` in the importer**

In `app/src/main/java/io/legado/app/model/localBook/WatchTxtImporter.kt`, replace this block:

```kotlin
        WatchTxtShelfCleaner.staleDownloadTxtBooks(downloadDir, shelfBooksProvider())
            .forEach { book ->
                runCatching {
                    removeBookFromShelf(book)
                }.onSuccess {
                    removedCount += 1
                }
            }
```

with:

```kotlin
        WatchTxtShelfCleaner.booksToRemove(downloadDir, shelfBooksProvider())
            .forEach { book ->
                runCatching {
                    removeBookFromShelf(book)
                }.onSuccess {
                    removedCount += 1
                }
            }
```

- [ ] **Step 4: Run local book tests to verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/model/localBook/WatchTxtImporter.kt app/src/test/java/io/legado/app/model/localBook/WatchTxtImporterTest.kt
git commit -m "feat: prune non-target books during watch scan"
```

---

### Task 3: Clean Offline-Obsolete Database Data

**Files:**
- Modify: `app/src/main/java/io/legado/app/data/dao/CacheDao.kt`
- Modify: `app/src/main/java/io/legado/app/data/dao/CookieDao.kt`
- Create: `app/src/main/java/io/legado/app/model/localBook/WatchOnlyDataCleaner.kt`
- Modify: `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfViewModel.kt`

**Interfaces:**
- Consumes: Room DAOs exposed through `appDb` and `WatchTxtShelfCleaner.booksToRemove()`.
- Produces: `WatchOnlyDataCleaner.clean(downloadDir: File): WatchOnlyDataCleanResult` and full cache/cookie clear DAO methods.

- [ ] **Step 1: Add full-clear DAO methods**

In `app/src/main/java/io/legado/app/data/dao/CacheDao.kt`, add this method after `clearDeadline()`:

```kotlin
    @Query("delete from caches")
    fun clearAll()
```

In `app/src/main/java/io/legado/app/data/dao/CookieDao.kt`, add this method after `deleteOkHttp()`:

```kotlin
    @Query("delete from cookies")
    fun clearAll()
```

- [ ] **Step 2: Add the watch-only data cleaner**

Create `app/src/main/java/io/legado/app/model/localBook/WatchOnlyDataCleaner.kt`:

```kotlin
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
```

- [ ] **Step 3: Run compile to verify DAO code generation**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: PASS.

- [ ] **Step 4: Run cleaner before scan**

In `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfViewModel.kt`, add this import:

```kotlin
import io.legado.app.model.localBook.WatchOnlyDataCleaner
```

In `scanDownload()`, replace:

```kotlin
            importer.scanAndImport()
```

with:

```kotlin
            WatchOnlyDataCleaner.clean()
            importer.scanAndImport()
```

- [ ] **Step 5: Run watch and local book tests**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.*" --tests "io.legado.app.ui.watch.*"
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/data/dao/CacheDao.kt app/src/main/java/io/legado/app/data/dao/CookieDao.kt app/src/main/java/io/legado/app/model/localBook/WatchOnlyDataCleaner.kt app/src/main/java/io/legado/app/ui/watch/WatchBookshelfViewModel.kt
git commit -m "feat: clean obsolete data for watch txt mode"
```

---

### Task 4: Local-Only Startup Defaults

**Files:**
- Modify: `app/src/main/java/io/legado/app/App.kt`
- Modify: `app/src/main/java/io/legado/app/help/DefaultData.kt`

**Interfaces:**
- Consumes: existing app startup lifecycle.
- Produces: startup with no Cronet, no WebDAV, no Rhino, no URL handler, no source sorting, and no default RSS/TTS/dict import.

- [ ] **Step 1: Restrict default data imports**

In `app/src/main/java/io/legado/app/help/DefaultData.kt`, replace `upVersion()` with:

```kotlin
    fun upVersion() {
        if (LocalConfig.versionCode < AppConst.appInfo.versionCode) {
            Coroutine.async {
                if (LocalConfig.needUpTxtTocRule) {
                    importDefaultTocRules()
                }
            }.onError {
                it.printOnDebug()
            }
        }
    }
```

- [ ] **Step 2: Remove unused default import functions after compile confirms no references**

Delete these functions from `DefaultData.kt`:

```kotlin
    fun importDefaultHttpTTS() {
        appDb.httpTTSDao.deleteDefault()
        appDb.httpTTSDao.insert(*httpTTS.toTypedArray())
    }

    fun importDefaultRssSources() {
        appDb.rssSourceDao.deleteDefault()
        appDb.rssSourceDao.insert(*rssSources.toTypedArray())
    }

    fun importDefaultDictRules() {
        appDb.dictRuleDao.insert(*dictRules.toTypedArray())
    }
```

Run this search:

```powershell
rg "DefaultData\.(httpTTS|rssSources|coverRule|dictRules)|importDefaultHttpTTS|importDefaultRssSources|importDefaultDictRules" app/src/main/java
```

Expected: matches only inside `app/src/main/java/io/legado/app/help/DefaultData.kt`. Then delete the lazy properties `httpTTS`, `rssSources`, `coverRule`, and `dictRules` from `DefaultData.kt`.

- [ ] **Step 3: Replace online-heavy startup work**

In `app/src/main/java/io/legado/app/App.kt`, replace the body of `onCreate()` with:

```kotlin
    override fun onCreate() {
        super.onCreate()
        CrashHandler(this)
        if (isDebuggable) {
            ThreadUtils.setThreadAssertsDisabledForTesting(true)
        }
        oldConfig = Configuration(resources.configuration)
        applyDayNightInit(this)
        registerActivityLifecycleCallbacks(LifecycleHelp)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(AppConfig)
        Coroutine.async {
            LogUtils.init(this@App)
            LogUtils.d("App", "onCreate")
            LogUtils.logDeviceInfo()
            createNotificationChannels()
            LiveEventBus.config()
                .lifecycleObserverAlwaysActive(true)
                .autoClear(false)
                .enableLogger(BuildConfig.DEBUG || AppConfig.recordLog)
                .setLogger(EventLogger())
            DefaultData.upVersion()
            AppFreezeMonitor.init(this@App)
            DispatchersMonitor.init()
            appDb.cacheDao.clearDeadline(System.currentTimeMillis())
            BookHelp.clearInvalidCache()
            ReadBookConfig.clearBgAndCache()
            when (AppConfig.chineseConverterType) {
                1 -> {
                    ChineseUtils.fixT2sDict()
                    ChineseUtils.preLoad(true, TransType.TRADITIONAL_TO_SIMPLE)
                }

                2 -> ChineseUtils.preLoad(true, TransType.SIMPLE_TO_TRADITIONAL)
            }
        }
    }
```

- [ ] **Step 4: Remove imports made unused by local-only startup**

In `App.kt`, remove imports for these symbols when the IDE/compiler marks them unused:

```kotlin
import io.legado.app.help.AppWebDav
import io.legado.app.help.RuleBigDataHelp
import io.legado.app.help.http.Cronet
import io.legado.app.help.http.ObsoleteUrlFactory
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.source.SourceHelp
import io.legado.app.model.BookCover
import java.net.URL
import java.util.concurrent.TimeUnit
```

- [ ] **Step 5: Compile startup changes**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/App.kt app/src/main/java/io/legado/app/help/DefaultData.kt
git commit -m "refactor: make startup local only"
```

---

### Task 5: Offline Manifest And Watch About Page

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/io/legado/app/ui/watch/about/WatchAboutActivity.kt`
- Create: `app/src/main/res/layout/activity_watch_about.xml`
- Modify: `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfActivity.kt`
- Modify: `README.md`
- Modify: `app/src/main/assets/privacyPolicy.md`

**Interfaces:**
- Consumes: existing `WelcomeActivity`, `WatchBookshelfActivity`, `WatchTocActivity`, `ReadBookActivity`, and `PermissionActivity`.
- Produces: Manifest with no network permission and only offline watch product components.

- [ ] **Step 1: Add watch About activity**

Create `app/src/main/java/io/legado/app/ui/watch/about/WatchAboutActivity.kt`:

```kotlin
package io.legado.app.ui.watch.about

import android.os.Bundle
import io.legado.app.BuildConfig
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityWatchAboutBinding

class WatchAboutActivity : BaseActivity<ActivityWatchAboutBinding>() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.tvTitle.text = "阅读手表版"
        binding.tvVersion.text = BuildConfig.VERSION_NAME
        binding.tvBody.text = "离线 TXT 阅读器。仅扫描 Download 文件夹下的 txt 文件，不提供书源、RSS、Web 服务、云同步或在线下载。"
        binding.root.setOnClickListener { finish() }
    }
}
```

Create `app/src/main/res/layout/activity_watch_about.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/watch_oled_black"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="32dp">

    <TextView
        android:id="@+id/tv_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:textColor="@color/watch_text_primary"
        android:textSize="18sp"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/tv_version"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:gravity="center"
        android:textColor="@color/watch_text_secondary"
        android:textSize="12sp" />

    <TextView
        android:id="@+id/tv_body"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="18dp"
        android:gravity="center"
        android:lineSpacingExtra="3dp"
        android:textColor="@color/watch_text_primary"
        android:textSize="13sp" />
</LinearLayout>
```

- [ ] **Step 2: Replace Manifest permissions**

In `app/src/main/AndroidManifest.xml`, keep only these permissions at the top of the manifest:

```xml
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission
        android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />
```

There must be no `INTERNET`, `ACCESS_NETWORK_STATE`, or `ACCESS_WIFI_STATE` permission in the file after this step.

- [ ] **Step 3: Keep only offline watch components in Manifest**

Inside `<application>`, keep these activities and provider entries:

```xml
        <activity
            android:name=".ui.welcome.WelcomeActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".ui.watch.WatchBookshelfActivity"
            android:configChanges="locale|keyboardHidden|orientation|screenSize|smallestScreenSize|screenLayout|uiMode"
            android:exported="false"
            android:screenOrientation="portrait" />

        <activity
            android:name=".ui.watch.toc.WatchTocActivity"
            android:configChanges="locale|keyboardHidden|orientation|screenSize|smallestScreenSize|screenLayout|uiMode"
            android:exported="false"
            android:screenOrientation="portrait" />

        <activity
            android:name=".ui.watch.about.WatchAboutActivity"
            android:exported="false"
            android:screenOrientation="portrait" />

        <activity
            android:name=".ui.book.read.ReadBookActivity"
            android:configChanges="locale|keyboardHidden|orientation|screenSize|smallestScreenSize|screenLayout"
            android:exported="false"
            android:launchMode="singleTask" />

        <activity
            android:name="io.legado.app.lib.permission.PermissionActivity"
            android:theme="@style/Activity.Permission" />

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileProvider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

        <meta-data
            android:name="channel"
            android:value="${APP_CHANNEL_VALUE}" />
```

Remove service, receiver, exported file association, API provider, phone launcher aliases, source/RSS/config/browser/media activities, and the `<queries>` block.

- [ ] **Step 4: Add an About entry from the bookshelf**

In `app/src/main/res/layout/activity_watch_bookshelf.xml`, add this `TextView` immediately after the existing `@+id/tv_scan` `TextView` in the top horizontal `LinearLayout`:

```xml
            <TextView
                android:id="@+id/tv_about"
                android:layout_width="wrap_content"
                android:layout_height="40dp"
                android:gravity="center"
                android:minWidth="48dp"
                android:paddingStart="10dp"
                android:paddingEnd="10dp"
                android:text="关于"
                android:textColor="@color/watch_text_secondary"
                android:textSize="13sp" />
```

Add this import to `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfActivity.kt`:

```kotlin
import io.legado.app.ui.watch.about.WatchAboutActivity
import splitties.activities.start
```

In `WatchBookshelfActivity.initView()`, add this click listener after the existing `tvScan.setOnClickListener` block:

```kotlin
        tvAbout.setOnClickListener {
            start<WatchAboutActivity>()
        }
```

- [ ] **Step 5: Update product docs**

Replace `app/src/main/assets/privacyPolicy.md` with:

```markdown
* 本应用是离线手表 TXT 阅读器。
* 本应用不提供书源、RSS、Web 服务、云同步、在线下载或统计服务。
* 本应用只读取手表 Download 文件夹下的 txt 文件，用于导入书架和保存本地阅读进度。
* 存储权限仅用于扫描和读取 Download 文件夹中的 txt 文件。
```

Replace the introduction and feature list in `README.md` with a short offline watch description:

```markdown
# 阅读手表版

离线 TXT 阅读器，面向手表使用。

## 功能

- 扫描 Download 文件夹下的 `.txt` 文件。
- 自动导入本地 TXT 到手表书架。
- 保留阅读进度。
- 提供手表适配的书架、目录、阅读菜单和阅读设置。
- 不提供书源、RSS、Web 服务、云同步或在线下载。
```

- [ ] **Step 6: Compile Manifest and About page changes**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/AndroidManifest.xml app/src/main/java/io/legado/app/ui/watch/about/WatchAboutActivity.kt app/src/main/res/layout/activity_watch_about.xml app/src/main/java/io/legado/app/ui/watch/WatchBookshelfActivity.kt README.md app/src/main/assets/privacyPolicy.md
git commit -m "refactor: restrict app surface to offline watch reader"
```

---

### Task 6: Hard Delete Online, Web, RSS, And Media Source Areas

**Files:**
- Delete source directories and files listed in Step 1.
- Modify retained reader files only to remove references to deleted features.

**Interfaces:**
- Consumes: compile-clean local-only product from Tasks 1-5.
- Produces: source tree where online/RSS/Web/media product code is deleted and debug Kotlin compiles.

- [ ] **Step 1: Delete source areas no longer in the product surface**

Run these commands:

```powershell
git rm -r app/src/main/java/io/legado/app/api
git rm -r app/src/main/java/io/legado/app/web
git rm -r app/src/main/java/io/legado/app/model/webBook
git rm -r app/src/main/java/io/legado/app/model/rss
git rm -r app/src/main/java/io/legado/app/model/remote
git rm app/src/main/java/io/legado/app/model/RuleUpdate.kt
git rm app/src/main/java/io/legado/app/model/CheckSource.kt
git rm app/src/main/java/io/legado/app/model/Download.kt
git rm app/src/main/java/io/legado/app/model/AudioPlay.kt
git rm app/src/main/java/io/legado/app/model/VideoPlay.kt
git rm app/src/main/java/io/legado/app/model/ReadManga.kt
git rm app/src/main/java/io/legado/app/model/BookCover.kt
git rm app/src/main/java/io/legado/app/service/WebService.kt
git rm app/src/main/java/io/legado/app/service/WebTileService.kt
git rm app/src/main/java/io/legado/app/service/DownloadService.kt
git rm app/src/main/java/io/legado/app/service/CheckSourceService.kt
git rm app/src/main/java/io/legado/app/service/CacheBookService.kt
git rm app/src/main/java/io/legado/app/service/AudioPlayService.kt
git rm app/src/main/java/io/legado/app/service/VideoPlayService.kt
git rm app/src/main/java/io/legado/app/service/TTSReadAloudService.kt
git rm app/src/main/java/io/legado/app/service/HttpReadAloudService.kt
git rm -r app/src/main/java/io/legado/app/ui/rss
git rm -r app/src/main/java/io/legado/app/ui/browser
git rm -r app/src/main/java/io/legado/app/ui/video
git rm -r app/src/main/java/io/legado/app/ui/login
git rm -r app/src/main/java/io/legado/app/ui/book/source
git rm -r app/src/main/java/io/legado/app/ui/book/search
git rm -r app/src/main/java/io/legado/app/ui/book/explore
git rm -r app/src/main/java/io/legado/app/ui/book/cache
git rm -r app/src/main/java/io/legado/app/ui/book/changecover
git rm -r app/src/main/java/io/legado/app/ui/book/changesource
git rm -r app/src/main/java/io/legado/app/ui/book/audio
```

- [ ] **Step 2: Compile to list retained-code references**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: FAIL listing references from retained files to deleted features.

- [ ] **Step 3: Remove deleted-feature references from retained reading code**

Apply these local-only replacements first, then rerun the compile command.

In `app/src/main/java/io/legado/app/utils/ContextExtensions.kt`, replace `startActivityForBook()` with:

```kotlin
fun Context.startActivityForBook(
    book: Book,
    configIntent: Intent.() -> Unit = {},
) {
    val intent = Intent(this, ReadBookActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.putExtra("bookUrl", book.bookUrl)
    intent.apply(configIntent)
    startActivity(intent)
}
```

Remove imports from `ContextExtensions.kt` for deleted `AudioPlayActivity`, `VideoPlayerActivity`, and `ReadMangaActivity`.

In `app/src/main/java/io/legado/app/ui/book/read/ReadBookViewModel.kt`, remove automatic online source fallback by replacing:

```kotlin
        if (!book.isLocal && ReadBook.bookSource == null) {
            autoChangeSource(book.name, book.author)
            return
        }
```

with:

```kotlin
        if (!book.isLocal) {
            ReadBook.upMsg("仅支持 Download 文件夹下的本地 txt")
            return
        }
```

Run this search:

```powershell
rg "autoChangeSource\(" app/src/main/java/io/legado/app/ui/book/read/ReadBookViewModel.kt
```

Expected: only the `autoChangeSource` function declaration remains. Delete the whole `autoChangeSource` function from `ReadBookViewModel.kt`.

In `app/src/main/java/io/legado/app/model/CacheBook.kt`, remove network download fallback by replacing calls to `WebBook.getContent(...)` with a local failure string:

```kotlin
            downloadFinish(chapter, "离线手表版不支持在线缓存")
            onSuccess(chapter)
            return
```

- [ ] **Step 4: Repeat compile and remove one retained reference at a time**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: PASS before committing. For each failure, only edit the retained file named by the compiler and remove the deleted-feature branch. Do not add compatibility shims for online features.

- [ ] **Step 5: Run watch/local tests**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.*" --tests "io.legado.app.ui.watch.*"
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java app/src/test/java
git commit -m "refactor: delete online and media source areas"
```

---

### Task 7: Prune Dependencies, Modules, And Assets

**Files:**
- Modify: `settings.gradle`
- Modify: `app/build.gradle`
- Modify: `gradle/libs.versions.toml`
- Delete: `modules/web`
- Delete: `app/src/main/assets/web`
- Delete: `app/src/main/assets/cronet.json`
- Delete: online default data assets listed below

**Interfaces:**
- Consumes: compile-clean local-only source tree from Tasks 1-6.
- Produces: Gradle configuration without Web, Firebase, Cronet, Web server, media, and online parser dependencies.

- [ ] **Step 1: Remove Web module directory**

Run:

```powershell
git rm -r modules/web
```

- [ ] **Step 2: Remove embedded Web and Cronet assets**

Run:

```powershell
git rm -r app/src/main/assets/web
git rm app/src/main/assets/cronet.json
git rm app/src/main/assets/defaultData/bookSources.json
git rm app/src/main/assets/defaultData/rssSources.json
git rm app/src/main/assets/defaultData/httpTTS.json
git rm app/src/main/assets/defaultData/coverRule.json
git rm app/src/main/assets/defaultData/dictRules.json
git rm app/src/main/assets/defaultData/directLinkUpload.json
```

Keep these local assets:

```text
app/src/main/assets/defaultData/txtTocRule.json
app/src/main/assets/defaultData/readConfig.json
app/src/main/assets/defaultData/keyboardAssists.json
app/src/main/assets/defaultData/themeConfig.json
app/src/main/assets/privacyPolicy.md
app/src/main/assets/LICENSE.md
```

- [ ] **Step 3: Remove unused Gradle dependencies from `app/build.gradle`**

Delete dependency lines for these aliases and file trees:

```groovy
implementation(libs.media.media)
implementation(libs.media3.exoplayer)
implementation(libs.media3.datasource.okhttp)
implementation(libs.gsyVideoPlayer.java)
implementation(libs.gsyVideoPlayer.exo2)
implementation(libs.danmakuFlameMaster)
implementation(libs.jsoup)
implementation(libs.json.path)
implementation(libs.jsoupxpath)
implementation(project(path: ':modules:rhino'))
implementation(libs.okhttp)
implementation(fileTree(dir: 'cronetlib', include: ['*.jar', '*.aar']))
implementation(libs.protobuf.javalite)
implementation(libs.glide.okhttp)
implementation(libs.nanohttpd.nanohttpd)
implementation(libs.nanohttpd.websocket)
implementation(libs.libarchive)
implementation(libs.hutool.crypto)
implementation platform(libs.firebase.bom)
implementation libs.firebase.analytics
implementation libs.firebase.perf
implementation(libs.lyricViewx)
```

Also remove this plugin if no Firebase dependency remains:

```groovy
alias libs.plugins.google.services
```

- [ ] **Step 4: Remove Rhino module from settings after references are gone**

In `settings.gradle`, remove:

```groovy
include ':modules:rhino'
```

Keep `:modules:book` until TXT parsing compiles without it.

- [ ] **Step 5: Compile and remove unused version catalog entries**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: PASS. If retained watch/TXT reader code needs one of the removed dependencies, restore only that dependency line and rerun this command until it passes.

- [ ] **Step 6: Commit after compile passes**

Run after `:app:compileAppDebugKotlin` passes:

```powershell
git add settings.gradle app/build.gradle gradle/libs.versions.toml app/src/main/assets modules
git commit -m "refactor: prune online dependencies and assets"
```

---

### Task 8: Final Build, Manual Checklist, And Cleanup

**Files:**
- Modify only files required by final compile, lint, or package errors.
- Do not modify unrelated dirty files unless they are required for the final watch product.

**Interfaces:**
- Consumes: tasks 1-7.
- Produces: debug APK and final verification evidence.

- [ ] **Step 1: Run retained unit tests**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.*" --tests "io.legado.app.ui.watch.*"
```

Expected: PASS.

- [ ] **Step 2: Compile Kotlin**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: PASS.

- [ ] **Step 3: Build debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleAppDebug
```

Expected: `BUILD SUCCESSFUL` and a debug APK under `app/build/outputs/apk/app/debug/`.

- [ ] **Step 4: Verify no network permission in merged manifest**

Run:

```powershell
Select-String -Path "app/build/intermediates/merged_manifest/appDebug/processAppDebugManifest/AndroidManifest.xml" -Pattern "INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE"
```

Expected: no matches.

- [ ] **Step 5: Manual watch checklist**

Install the APK on the target watch and verify:

```text
1. Fresh install opens the watch bookshelf.
2. Permission prompt explains storage access.
3. Empty Download shows an empty bookshelf message.
4. Adding a .txt file under Download and rescanning imports it.
5. Opening the TXT enters the watch reader.
6. Reader progress restores after leaving and reopening the book.
7. TOC opens and returns to the selected chapter.
8. Reader settings change text size, line spacing, brightness, and OLED reset.
9. Deleting the TXT file removes the stale shelf item on scan or open.
10. Long-press removes a book from the shelf without deleting the original TXT.
11. About screen opens and describes offline TXT behavior.
```

- [ ] **Step 6: Commit final cleanup**

Run:

```powershell
git status --short
git add README.md app/src/main app/build.gradle settings.gradle gradle/libs.versions.toml
git commit -m "chore: verify offline watch txt reader build"
```

If `git status --short` shows IDE files such as `.project`, `.settings`, `.classpath`, or `opencode.json`, do not add them.
