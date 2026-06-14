# Watch TXT Reader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the current Android app into a bookshelf-first, pure-local TXT reader for the China-market OPPO Watch X.

**Architecture:** Keep the existing Kotlin/XML/ViewBinding stack and local `Book`/Room/`LocalBook` reader pipeline. Add a small watch-specific shell (`WatchBookshelfActivity`) instead of refactoring the phone `MainActivity`, then route the launcher to it and remove user-visible network paths from the manifest and menus.

**Tech Stack:** Kotlin, Android XML layouts, ViewBinding, Room, coroutines/Flow, JUnit4 local unit tests, Android Gradle Plugin.

---

## File Structure

- Create `app/src/main/java/io/legado/app/model/localBook/WatchTxtFileFilter.kt`: pure Kotlin filename and file-list filtering for `.txt`.
- Create `app/src/main/java/io/legado/app/model/localBook/WatchTxtImporter.kt`: imports filtered TXT files from public `Download` via `LocalBook.importFile`.
- Create `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfActivity.kt`: single-screen round-watch bookshelf shell.
- Create `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfViewModel.kt`: observes local books and triggers `Download` scans.
- Create `app/src/main/java/io/legado/app/ui/watch/WatchBookAdapter.kt`: compact pure-black list adapter.
- Create `app/src/main/java/io/legado/app/ui/watch/EdgeSwipeBackDetector.kt`: pure gesture decision helper.
- Create `app/src/main/java/io/legado/app/ui/watch/EdgeSwipeBackLayout.kt`: view wrapper that calls back when the detector fires.
- Create `app/src/main/java/io/legado/app/ui/watch/WatchReaderDefaults.kt`: applies OLED pure-black and watch reader defaults.
- Create `app/src/main/res/layout/activity_watch_bookshelf.xml`: round-watch bookshelf layout.
- Create `app/src/main/res/layout/item_watch_book.xml`: compact book row.
- Create `app/src/main/res/values/watch_colors.xml`: pure black and watch text colors.
- Modify `app/src/main/java/io/legado/app/ui/welcome/WelcomeActivity.kt`: route launcher to `WatchBookshelfActivity`, not phone `MainActivity`.
- Modify `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt`: install left-edge swipe-back and apply watch reader defaults.
- Modify `app/src/main/res/layout/activity_book_read.xml`: wrap reader content in `EdgeSwipeBackLayout`.
- Modify `app/src/main/AndroidManifest.xml`: remove network permissions/components and register `WatchBookshelfActivity`.
- Delete `app/src/main/res/xml/network_security_config.xml`: no network security config is needed after the manifest no longer references it.
- Modify `app/src/main/res/values/strings.xml`: add watch-specific text strings.
- Add tests under `app/src/test/java/io/legado/app/model/localBook/` and `app/src/test/java/io/legado/app/ui/watch/`.

## Task 0: Repository Baseline

**Files:**
- Track existing project files so subsequent implementation commits are reviewable.

- [ ] **Step 1: Confirm only tool caches are ignored**

Run:

```powershell
git status --short
git check-ignore -v .codegraph .superpowers
```

Expected:

```text
?? app/
?? modules/
?? build.gradle
?? settings.gradle
.gitignore:21:.codegraph/ .codegraph
.gitignore:22:.superpowers/ .superpowers
```

- [ ] **Step 2: Add the existing project as a baseline**

Run:

```powershell
git add .github CHANGELOG.md English.md LICENSE README.md api.md app avd.bat avd.sh build.gradle gradle.properties gradle gradlew gradlew.bat modules package.json settings.gradle
git status --short
```

Expected:

```text
A  app/src/main/AndroidManifest.xml
A  modules/book/build.gradle
A  build.gradle
A  settings.gradle
```

- [ ] **Step 3: Commit the baseline**

Run:

```powershell
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "chore: import legado baseline"
```

Expected: commit succeeds and `git status --short` is clean except future implementation edits.

## Task 1: TXT Filtering Tests And Pure Filter

**Files:**
- Create: `app/src/test/java/io/legado/app/model/localBook/WatchTxtFileFilterTest.kt`
- Create: `app/src/main/java/io/legado/app/model/localBook/WatchTxtFileFilter.kt`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/io/legado/app/model/localBook/WatchTxtFileFilterTest.kt`:

```kotlin
package io.legado.app.model.localBook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WatchTxtFileFilterTest {

    @Test
    fun acceptsOnlyVisibleTxtNames() {
        assertTrue(WatchTxtFileFilter.isTxtFileName("book.txt"))
        assertTrue(WatchTxtFileFilter.isTxtFileName("BOOK.TXT"))
        assertTrue(WatchTxtFileFilter.isTxtFileName("小说.Txt"))

        assertFalse(WatchTxtFileFilter.isTxtFileName(".hidden.txt"))
        assertFalse(WatchTxtFileFilter.isTxtFileName("book.epub"))
        assertFalse(WatchTxtFileFilter.isTxtFileName("book.txt.bak"))
        assertFalse(WatchTxtFileFilter.isTxtFileName("folder"))
    }

    @Test
    fun listTxtFilesReturnsSortedFilesOnly() {
        val root = createTempDir(prefix = "watch-txt-filter-")
        try {
            File(root, "b.txt").writeText("b")
            File(root, "a.TXT").writeText("a")
            File(root, "c.epub").writeText("c")
            File(root, ".hidden.txt").writeText("hidden")
            File(root, "folder.txt").mkdir()

            val names = WatchTxtFileFilter.listTxtFiles(root).map { it.name }

            assertEquals(listOf("a.TXT", "b.txt"), names)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun missingDirectoryReturnsEmptyList() {
        val root = File(createTempDir(prefix = "watch-txt-filter-missing-"), "missing")

        assertEquals(emptyList<File>(), WatchTxtFileFilter.listTxtFiles(root))
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.WatchTxtFileFilterTest"
```

Expected: FAIL because `WatchTxtFileFilter` is unresolved.

- [ ] **Step 3: Implement the filter**

Create `app/src/main/java/io/legado/app/model/localBook/WatchTxtFileFilter.kt`:

```kotlin
package io.legado.app.model.localBook

import java.io.File
import java.util.Locale

object WatchTxtFileFilter {

    fun isTxtFileName(name: String): Boolean {
        if (name.startsWith(".")) return false
        return name.lowercase(Locale.ROOT).endsWith(".txt")
    }

    fun listTxtFiles(root: File): List<File> {
        return root.listFiles()
            ?.asSequence()
            ?.filter { it.isFile }
            ?.filter { isTxtFileName(it.name) }
            ?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            ?.toList()
            ?: emptyList()
    }
}
```

- [ ] **Step 4: Run tests and verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.WatchTxtFileFilterTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/test/java/io/legado/app/model/localBook/WatchTxtFileFilterTest.kt app/src/main/java/io/legado/app/model/localBook/WatchTxtFileFilter.kt
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "test: add watch txt file filter"
```

## Task 2: Edge Swipe-Back Detector

**Files:**
- Create: `app/src/test/java/io/legado/app/ui/watch/EdgeSwipeBackDetectorTest.kt`
- Create: `app/src/main/java/io/legado/app/ui/watch/EdgeSwipeBackDetector.kt`

- [ ] **Step 1: Write failing tests**

Create `app/src/test/java/io/legado/app/ui/watch/EdgeSwipeBackDetectorTest.kt`:

```kotlin
package io.legado.app.ui.watch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeSwipeBackDetectorTest {

    private val detector = EdgeSwipeBackDetector(
        edgeWidthPx = 32f,
        minDistancePx = 64f,
        maxVerticalDriftPx = 48f
    )

    @Test
    fun detectsRightSwipeStartingAtLeftEdge() {
        assertTrue(detector.shouldBack(startX = 12f, startY = 100f, endX = 96f, endY = 112f))
    }

    @Test
    fun ignoresSwipeThatStartsAwayFromLeftEdge() {
        assertFalse(detector.shouldBack(startX = 40f, startY = 100f, endX = 140f, endY = 100f))
    }

    @Test
    fun ignoresShortHorizontalMovement() {
        assertFalse(detector.shouldBack(startX = 12f, startY = 100f, endX = 60f, endY = 100f))
    }

    @Test
    fun ignoresMostlyVerticalMovement() {
        assertFalse(detector.shouldBack(startX = 12f, startY = 100f, endX = 120f, endY = 180f))
    }

    @Test
    fun ignoresLeftwardMovement() {
        assertFalse(detector.shouldBack(startX = 12f, startY = 100f, endX = 2f, endY = 100f))
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.ui.watch.EdgeSwipeBackDetectorTest"
```

Expected: FAIL because `EdgeSwipeBackDetector` is unresolved.

- [ ] **Step 3: Implement the detector**

Create `app/src/main/java/io/legado/app/ui/watch/EdgeSwipeBackDetector.kt`:

```kotlin
package io.legado.app.ui.watch

import kotlin.math.abs

class EdgeSwipeBackDetector(
    private val edgeWidthPx: Float,
    private val minDistancePx: Float,
    private val maxVerticalDriftPx: Float
) {

    fun shouldBack(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ): Boolean {
        val dx = endX - startX
        val dy = endY - startY
        return startX <= edgeWidthPx &&
            dx >= minDistancePx &&
            abs(dy) <= maxVerticalDriftPx &&
            dx > abs(dy)
    }
}
```

- [ ] **Step 4: Run tests and verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.ui.watch.EdgeSwipeBackDetectorTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/test/java/io/legado/app/ui/watch/EdgeSwipeBackDetectorTest.kt app/src/main/java/io/legado/app/ui/watch/EdgeSwipeBackDetector.kt
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "test: add watch edge swipe detector"
```

## Task 3: TXT Importer And Watch Bookshelf ViewModel

**Files:**
- Create: `app/src/main/java/io/legado/app/model/localBook/WatchTxtImporter.kt`
- Create: `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfViewModel.kt`

- [ ] **Step 1: Write failing importer tests**

Create `app/src/test/java/io/legado/app/model/localBook/WatchTxtImporterTest.kt`:

```kotlin
package io.legado.app.model.localBook

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class WatchTxtImporterTest {

    @Test
    fun scanAndImportImportsOnlyTxtFiles() {
        val root = createTempDir(prefix = "watch-txt-import-")
        val imported = arrayListOf<String>()
        try {
            File(root, "b.txt").writeText("b")
            File(root, "a.TXT").writeText("a")
            File(root, "cover.jpg").writeText("jpg")

            val importer = WatchTxtImporter(
                downloadDirProvider = { root },
                importFile = { imported.add(it.name) }
            )

            val result = importer.scanAndImport()

            assertEquals(listOf("a.TXT", "b.txt"), imported)
            assertEquals(2, result.importedCount)
            assertEquals(2, result.scannedCount)
            assertEquals(emptyList<String>(), result.failedFiles)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun scanAndImportRecordsFailuresAndContinues() {
        val root = createTempDir(prefix = "watch-txt-import-fail-")
        try {
            File(root, "a.txt").writeText("a")
            File(root, "b.txt").writeText("b")

            val importer = WatchTxtImporter(
                downloadDirProvider = { root },
                importFile = {
                    if (it.name == "a.txt") error("boom")
                }
            )

            val result = importer.scanAndImport()

            assertEquals(1, result.importedCount)
            assertEquals(2, result.scannedCount)
            assertEquals(listOf("a.txt"), result.failedFiles)
        } finally {
            root.deleteRecursively()
        }
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.WatchTxtImporterTest"
```

Expected: FAIL because `WatchTxtImporter` is unresolved.

- [ ] **Step 3: Implement the importer**

Create `app/src/main/java/io/legado/app/model/localBook/WatchTxtImporter.kt`:

```kotlin
package io.legado.app.model.localBook

import android.net.Uri
import android.os.Environment
import java.io.File

data class WatchTxtImportResult(
    val scannedCount: Int,
    val importedCount: Int,
    val failedFiles: List<String>
)

class WatchTxtImporter(
    private val downloadDirProvider: () -> File = {
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    },
    private val importFile: (File) -> Unit = { file ->
        LocalBook.importFile(Uri.fromFile(file))
    }
) {

    fun scanAndImport(): WatchTxtImportResult {
        val files = WatchTxtFileFilter.listTxtFiles(downloadDirProvider())
        var importedCount = 0
        val failedFiles = arrayListOf<String>()
        files.forEach { file ->
            runCatching {
                importFile(file)
            }.onSuccess {
                importedCount += 1
            }.onFailure {
                failedFiles.add(file.name)
            }
        }
        return WatchTxtImportResult(
            scannedCount = files.size,
            importedCount = importedCount,
            failedFiles = failedFiles
        )
    }
}
```

- [ ] **Step 4: Run importer tests and verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.WatchTxtImporterTest"
```

Expected: PASS.

- [ ] **Step 5: Add the ViewModel**

Create `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfViewModel.kt`:

```kotlin
package io.legado.app.ui.watch

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.model.localBook.WatchTxtImportResult
import io.legado.app.model.localBook.WatchTxtImporter
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchBookshelfViewModel(application: Application) : BaseViewModel(application) {

    val booksFlow: Flow<List<Book>> = appDb.bookDao.flowLocal()
        .map { books ->
            books
                .filter { it.originName.endsWith(".txt", ignoreCase = true) }
                .sortedWith(compareByDescending<Book> { it.durChapterTime }.thenBy { it.name })
        }

    val scanResultLiveData = MutableLiveData<WatchTxtImportResult>()
    val scanningLiveData = MutableLiveData<Boolean>()

    fun scanDownload(importer: WatchTxtImporter = WatchTxtImporter()) {
        execute(context = IO) {
            importer.scanAndImport()
        }.onStart {
            scanningLiveData.postValue(true)
        }.onSuccess {
            scanResultLiveData.postValue(it)
            if (it.scannedCount == 0) {
                context.toastOnUi("Download 里没有 txt 文件")
            } else {
                context.toastOnUi("已扫描 ${it.scannedCount} 个 txt 文件")
            }
        }.onError {
            AppLog.put("扫描 Download 失败\n${it.localizedMessage}", it)
            context.toastOnUi("扫描 Download 失败\n${it.localizedMessage}")
        }.onFinally {
            scanningLiveData.postValue(false)
        }
    }
}
```

- [ ] **Step 6: Build to verify ViewModel compiles**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: Kotlin compilation succeeds.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/test/java/io/legado/app/model/localBook/WatchTxtImporterTest.kt app/src/main/java/io/legado/app/model/localBook/WatchTxtImporter.kt app/src/main/java/io/legado/app/ui/watch/WatchBookshelfViewModel.kt
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "feat: add watch txt importer"
```

## Task 4: Watch Bookshelf UI

**Files:**
- Create: `app/src/main/java/io/legado/app/ui/watch/WatchBookAdapter.kt`
- Create: `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfActivity.kt`
- Create: `app/src/main/res/layout/activity_watch_bookshelf.xml`
- Create: `app/src/main/res/layout/item_watch_book.xml`
- Create: `app/src/main/res/values/watch_colors.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add watch strings and colors**

Append these strings inside `app/src/main/res/values/strings.xml` before `</resources>`:

```xml
    <string name="watch_bookshelf">书架</string>
    <string name="watch_scan_download">扫描 Download</string>
    <string name="watch_empty_title">Download 里没有 TXT</string>
    <string name="watch_empty_summary">把 .txt 文件放到 Download 后点刷新</string>
    <string name="watch_remove_book">移除书籍</string>
```

Create `app/src/main/res/values/watch_colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="watch_oled_black">#000000</color>
    <color name="watch_text_primary">#EAEAEA</color>
    <color name="watch_text_secondary">#A8A8A8</color>
    <color name="watch_row_pressed">#1AFFFFFF</color>
</resources>
```

- [ ] **Step 2: Add layouts**

Create `app/src/main/res/layout/activity_watch_bookshelf.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<io.legado.app.ui.watch.EdgeSwipeBackLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/swipe_back_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/watch_oled_black"
    android:paddingStart="18dp"
    android:paddingTop="18dp"
    android:paddingEnd="18dp"
    android:paddingBottom="18dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center_horizontal"
        android:orientation="vertical">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/tv_title"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:includeFontPadding="false"
                android:text="@string/watch_bookshelf"
                android:textColor="@color/watch_text_primary"
                android:textSize="20sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/tv_scan"
                android:layout_width="wrap_content"
                android:layout_height="40dp"
                android:gravity="center"
                android:minWidth="64dp"
                android:paddingStart="12dp"
                android:paddingEnd="12dp"
                android:text="@string/refresh"
                android:textColor="@color/watch_text_primary"
                android:textSize="14sp" />
        </LinearLayout>

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_view"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_marginTop="12dp"
            android:layout_weight="1"
            android:clipToPadding="false"
            android:overScrollMode="never"
            tools:listitem="@layout/item_watch_book" />

        <TextView
            android:id="@+id/tv_empty"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:gravity="center"
            android:text="@string/watch_empty_summary"
            android:textColor="@color/watch_text_secondary"
            android:textSize="15sp"
            android:visibility="gone" />
    </LinearLayout>
</io.legado.app.ui.watch.EdgeSwipeBackLayout>
```

Create `app/src/main/res/layout/item_watch_book.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:background="?android:attr/selectableItemBackground"
    android:clickable="true"
    android:focusable="true"
    android:gravity="center_vertical"
    android:orientation="vertical"
    android:paddingStart="10dp"
    android:paddingEnd="10dp">

    <TextView
        android:id="@+id/tv_name"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:ellipsize="end"
        android:includeFontPadding="false"
        android:maxLines="1"
        android:textColor="@color/watch_text_primary"
        android:textSize="17sp"
        tools:text="三体" />

    <TextView
        android:id="@+id/tv_progress"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="6dp"
        android:ellipsize="end"
        android:includeFontPadding="false"
        android:maxLines="1"
        android:textColor="@color/watch_text_secondary"
        android:textSize="12sp"
        tools:text="第 1 章 · 12%" />
</LinearLayout>
```

- [ ] **Step 3: Add `EdgeSwipeBackLayout`**

Create `app/src/main/java/io/legado/app/ui/watch/EdgeSwipeBackLayout.kt`:

```kotlin
package io.legado.app.ui.watch

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.utils.dpToPx

class EdgeSwipeBackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val detector = EdgeSwipeBackDetector(
        edgeWidthPx = 32.dpToPx().toFloat(),
        minDistancePx = 64.dpToPx().toFloat(),
        maxVerticalDriftPx = 48.dpToPx().toFloat()
    )
    private var startX = 0f
    private var startY = 0f
    private var onBack: (() -> Unit)? = null

    fun setOnEdgeBack(action: () -> Unit) {
        onBack = action
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
            }

            MotionEvent.ACTION_UP -> {
                if (detector.shouldBack(startX, startY, ev.x, ev.y)) {
                    onBack?.invoke()
                    return true
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
```

- [ ] **Step 4: Add adapter**

Create `app/src/main/java/io/legado/app/ui/watch/WatchBookAdapter.kt`:

```kotlin
package io.legado.app.ui.watch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemWatchBookBinding
import java.text.NumberFormat
import java.util.Locale

class WatchBookAdapter(
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<WatchBookAdapter.Holder>() {

    private val books = arrayListOf<Book>()
    private val percentFormat = NumberFormat.getPercentInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    fun submitList(items: List<Book>) {
        books.clear()
        books.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemWatchBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = books.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(books[position])
    }

    inner class Holder(
        private val binding: ItemWatchBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) = binding.run {
            tvName.text = book.name.ifBlank { book.originName }
            tvProgress.text = progressText(book)
            root.setOnClickListener { onBookClick(book) }
        }
    }

    private fun progressText(book: Book): String {
        val chapter = book.durChapterTitle.orEmpty().ifBlank { "未开始" }
        val progress = if (book.totalChapterNum > 0) {
            percentFormat.format((book.durChapterIndex + 1).toDouble() / book.totalChapterNum.toDouble())
        } else {
            "0%"
        }
        return "$chapter · $progress"
    }
}
```

- [ ] **Step 5: Add activity**

Create `app/src/main/java/io/legado/app/ui/watch/WatchBookshelfActivity.kt`:

```kotlin
package io.legado.app.ui.watch

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityWatchBookshelfBinding
import io.legado.app.utils.gone
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WatchBookshelfActivity :
    VMBaseActivity<ActivityWatchBookshelfBinding, WatchBookshelfViewModel>(
        theme = Theme.Dark,
        imageBg = false
    ) {

    override val binding by viewBinding(ActivityWatchBookshelfBinding::inflate)
    override val viewModel by viewModels<WatchBookshelfViewModel>()

    private val adapter by lazy {
        WatchBookAdapter { book ->
            startActivityForBook(book)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        WatchReaderDefaults.apply()
        initView()
        observeBooks()
        viewModel.scanDownload()
    }

    private fun initView() = binding.run {
        swipeBackLayout.setOnEdgeBack { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this@WatchBookshelfActivity)
        recyclerView.adapter = adapter
        tvScan.setOnClickListener {
            viewModel.scanDownload()
        }
    }

    private fun observeBooks() {
        lifecycleScope.launch {
            viewModel.booksFlow
                .catch {
                    binding.tvEmpty.visible()
                    binding.recyclerView.gone()
                }
                .collectLatest { books ->
                    adapter.submitList(books)
                    binding.recyclerView.visible(books.isNotEmpty())
                    binding.tvEmpty.visible(books.isEmpty())
                }
        }
    }
}
```

- [ ] **Step 6: Build**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: Kotlin compilation succeeds.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/ui/watch app/src/main/res/layout/activity_watch_bookshelf.xml app/src/main/res/layout/item_watch_book.xml app/src/main/res/values/watch_colors.xml app/src/main/res/values/strings.xml
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "feat: add watch bookshelf"
```

## Task 5: Route Launcher To Watch Bookshelf And Apply Reader Defaults

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/welcome/WelcomeActivity.kt`
- Create: `app/src/main/java/io/legado/app/ui/watch/WatchReaderDefaults.kt`
- Modify: `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt`

- [ ] **Step 1: Add watch reader defaults**

Create `app/src/main/java/io/legado/app/ui/watch/WatchReaderDefaults.kt`:

```kotlin
package io.legado.app.ui.watch

import android.graphics.Color
import io.legado.app.constant.PageAnim
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.utils.putPrefBoolean
import splitties.init.appCtx

object WatchReaderDefaults {

    fun apply() {
        AppConfig.isNightTheme = true
        AppConfig.isEInkMode = false
        appCtx.putPrefBoolean(PreferKey.showMangaUi, false)
        appCtx.putPrefBoolean(PreferKey.showDiscovery, false)
        appCtx.putPrefBoolean(PreferKey.showRss, false)
        appCtx.putPrefBoolean(PreferKey.autoRefresh, false)
        appCtx.putPrefBoolean(PreferKey.autoCheckNewBackup, false)

        ReadBookConfig.configList.forEach(::applyConfig)
        applyConfig(ReadBookConfig.shareConfig)
        ReadBookConfig.save()
    }

    private fun applyConfig(config: ReadBookConfig.Config) {
        config.bgType = 0
        config.bgTypeNight = 0
        config.bgTypeEInk = 0
        config.bgStr = "#000000"
        config.bgStrNight = "#000000"
        config.bgStrEInk = "#000000"
        config.setCurBg(0, "#000000")
        config.setCurTextColor(Color.rgb(234, 234, 234))
        config.setCurTextAccentColor(Color.rgb(150, 220, 180))
        config.setCurPageAnim(PageAnim.noAnim)
        config.textSize = 22
        config.lineSpacingExtra = 8
        config.paragraphSpacing = 1
        config.paddingLeft = 18
        config.paddingRight = 18
        config.paddingTop = 14
        config.paddingBottom = 14
        config.headerPaddingLeft = 18
        config.headerPaddingRight = 18
        config.footerPaddingLeft = 18
        config.footerPaddingRight = 18
        config.showHeaderLine = false
        config.showFooterLine = false
    }
}
```

- [ ] **Step 2: Compile defaults**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: Kotlin compilation succeeds.

- [ ] **Step 3: Route welcome to watch activity**

Modify imports in `app/src/main/java/io/legado/app/ui/welcome/WelcomeActivity.kt`:

```kotlin
import io.legado.app.ui.watch.WatchBookshelfActivity
```

Remove these imports if unused:

```kotlin
import io.legado.app.data.appDb
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.main.MainActivity
```

Replace `startMainActivity()` with:

```kotlin
    private fun startMainActivity() {
        WatchReaderDefaults.apply()
        startActivity<WatchBookshelfActivity>()
        finish()
    }
```

Also import:

```kotlin
import io.legado.app.ui.watch.WatchReaderDefaults
```

- [ ] **Step 4: Install reader swipe-back and defaults**

In `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt`, import:

```kotlin
import io.legado.app.ui.watch.EdgeSwipeBackLayout
import io.legado.app.ui.watch.WatchReaderDefaults
```

In `onActivityCreated`, immediately after `super.onActivityCreated(savedInstanceState)`, add:

```kotlin
WatchReaderDefaults.apply()
(binding.root as EdgeSwipeBackLayout).setOnEdgeBack {
    finish()
}
```

Modify `app/src/main/res/layout/activity_book_read.xml` so the root view is `EdgeSwipeBackLayout` instead of `FrameLayout`:

```diff
-<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
+<io.legado.app.ui.watch.EdgeSwipeBackLayout xmlns:android="http://schemas.android.com/apk/res/android"
     android:layout_width="match_parent"
     android:layout_height="match_parent"
     android:orientation="vertical">
-</FrameLayout>
+</io.legado.app.ui.watch.EdgeSwipeBackLayout>
```

Leave all child views inside the root unchanged.

- [ ] **Step 5: Build**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: Kotlin compilation succeeds.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/ui/welcome/WelcomeActivity.kt app/src/main/java/io/legado/app/ui/watch/WatchReaderDefaults.kt app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt app/src/main/res/layout/activity_book_read.xml
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "feat: route app to watch reader shell"
```

## Task 6: Manifest Offline Product Layer

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Delete: `app/src/main/res/xml/network_security_config.xml`

- [ ] **Step 1: Remove network/update permissions**

Edit `app/src/main/AndroidManifest.xml` and remove these permission lines:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

Keep storage permissions needed for public `Download` access.

In the `<application>` tag, remove this attribute:

```xml
android:networkSecurityConfig="@xml/network_security_config"
```

Delete the now-unreferenced file:

```text
app/src/main/res/xml/network_security_config.xml
```

- [ ] **Step 2: Register `WatchBookshelfActivity`**

Add near the current main activity declarations:

```xml
        <activity
            android:name=".ui.watch.WatchBookshelfActivity"
            android:configChanges="locale|keyboardHidden|orientation|screenSize|smallestScreenSize|screenLayout|uiMode"
            android:exported="false"
            android:screenOrientation="portrait" />
```

- [ ] **Step 3: Remove online-facing components**

Remove the complete activity, service, and provider blocks whose `android:name` is one of these values:

```text
.ui.association.OnLineImportActivity
.ui.association.OpenUrlConfirmActivity
.service.CheckSourceService
.service.CacheBookService
.service.WebService
.service.WebTileService
.service.DownloadService
.api.ReaderProvider
```

- [ ] **Step 4: Reduce file association to TXT**

In `FileAssociationActivity` intent filters, keep only:

```xml
<data android:mimeType="text/plain" />
<data android:pathAdvancedPattern=".*\.[tT][xX][tT]" />
<data android:pathPattern=".*\.txt" />
<data android:pathPattern=".*\.TXT" />
```

Remove JSON, EPUB, PDF, MOBI, AZW, ZIP, RAR, and 7z patterns from those filters.

- [ ] **Step 5: Build merged manifest**

Run:

```powershell
.\gradlew.bat :app:processAppDebugMainManifest
```

Expected: manifest processing succeeds.

- [ ] **Step 6: Verify no Internet permission**

Run:

```powershell
Select-String -Path app\build\intermediates\merged_manifests\appDebug\processAppDebugMainManifest\AndroidManifest.xml -Pattern "android.permission.INTERNET"
```

Expected: no output.

- [ ] **Step 7: Commit**

Run:

```powershell
git add -A app/src/main/AndroidManifest.xml app/src/main/res/xml/network_security_config.xml
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "chore: remove online manifest surface"
```

## Task 7: Hide Online Reader/Menu Paths

**Files:**
- Modify: `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt`

- [ ] **Step 1: Add the watch-local guard**

In `ReadBookActivity`, add this class property near `private var syncDialog: AlertDialog? = null`:

```kotlin
    private val watchLocalOnly = true
```

- [ ] **Step 2: Stop reader lifecycle network sync**

In `onResume()`, wrap the network listener registration block:

```kotlin
        if (!watchLocalOnly) {
            networkChangedListener.register()
            networkChangedListener.onNetworkChanged = {
                if (AppConfig.syncBookProgressPlus && NetworkUtils.isAvailable() && !justInitData && ReadBook.inBookshelf) {
                    ReadBook.syncProgress({ progress -> sureNewProgress(progress) })
                }
            }
        }
```

In `onPause()`, change:

```kotlin
        if (!BuildConfig.DEBUG && ReadBook.inBookshelf) {
```

to:

```kotlin
        if (!watchLocalOnly && !BuildConfig.DEBUG && ReadBook.inBookshelf) {
```

Change:

```kotlin
        if (!BuildConfig.DEBUG) {
```

to:

```kotlin
        if (!watchLocalOnly && !BuildConfig.DEBUG) {
```

Change:

```kotlin
        networkChangedListener.unRegister()
```

to:

```kotlin
        if (!watchLocalOnly) {
            networkChangedListener.unRegister()
        }
```

- [ ] **Step 3: Add watch-local menu helpers**

In `ReadBookActivity`, add these helpers near `private fun upMenu()`:

```kotlin

    private fun Menu.hideWatchOnlineItems() {
        listOf(
            R.id.menu_get_progress,
            R.id.menu_cover_progress,
            R.id.menu_simulated_reading,
            R.id.menu_image_style,
            R.id.menu_effective_replaces,
            R.id.menu_log,
            R.id.menu_help
        ).forEach { id ->
            findItem(id)?.isVisible = false
        }
    }

    private fun isWatchBlockedMenuItem(itemId: Int): Boolean {
        return watchLocalOnly && itemId in setOf(
            R.id.menu_change_source,
            R.id.menu_book_change_source,
            R.id.menu_chapter_change_source,
            R.id.menu_refresh,
            R.id.menu_refresh_dur,
            R.id.menu_refresh_after,
            R.id.menu_refresh_all,
            R.id.menu_download,
            R.id.menu_get_progress,
            R.id.menu_cover_progress,
            R.id.menu_simulated_reading,
            R.id.menu_image_style,
            R.id.menu_effective_replaces,
            R.id.menu_log,
            R.id.menu_help
        )
    }
```

- [ ] **Step 4: Skip online visibility and WebDAV checks**

In `upMenu()`, change:

```kotlin
        val onLine = !book.isLocal
```

to:

```kotlin
        val onLine = !watchLocalOnly && !book.isLocal
```

After the `for (i in 0 until menu.size)` loop and before the `lifecycleScope.launch` block that checks `AppWebDav.isOk`, add:

```kotlin
        if (watchLocalOnly) {
            menu.hideWatchOnlineItems()
            return
        }
```

This keeps TXT local actions visible and prevents `AppWebDav.isOk` from running in the watch-local reader.

- [ ] **Step 5: Block hidden online handlers**

At the start of `onCompatOptionsItemSelected`, before the `when (item.itemId)` block, add:

```kotlin
        if (isWatchBlockedMenuItem(item.itemId)) {
            return true
        }
```

- [ ] **Step 6: Build**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: compilation succeeds.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "chore: hide online reader actions"
```

## Task 8: Final Build And Manifest Verification

**Files:**
- No new files unless previous tasks expose compile fixes.

- [ ] **Step 1: Run local unit tests**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest
```

Expected: all unit tests pass, including:

```text
WatchTxtFileFilterTest
WatchTxtImporterTest
EdgeSwipeBackDetectorTest
```

- [ ] **Step 2: Build debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleAppDebug
```

Expected: build succeeds and creates an app debug APK under `app/build/outputs/apk/app/debug/`.

- [ ] **Step 3: Verify merged manifest does not include network permission**

Run:

```powershell
Select-String -Path app\build\intermediates\merged_manifests\appDebug\processAppDebugMainManifest\AndroidManifest.xml -Pattern "INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE|WebService|OnLineImport|ReaderProvider"
```

Expected: no output.

- [ ] **Step 4: Commit final verification fixes**

If any small fixes were required during verification:

```powershell
git add app/src/main
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "fix: complete watch txt reader verification"
```

If no fixes were required, do not create an empty commit.

## Task 9: Manual Device Smoke Test

**Files:**
- No code changes unless the smoke test finds defects.

- [ ] **Step 1: Install the debug APK**

Run:

```powershell
.\gradlew.bat :app:installAppDebug
```

Expected: install succeeds on the connected OPPO Watch X or Android test device.

- [ ] **Step 2: Put a TXT in public Download**

Use `adb` if available:

```powershell
adb shell mkdir -p /sdcard/Download
Set-Content -Path C:\tmp\watch-test.txt -Value "第一章 测试`n这是一段测试文字。"
adb push C:\tmp\watch-test.txt /sdcard/Download/watch-test.txt
```

Expected: `adb push` reports one file pushed.

- [ ] **Step 3: Launch and verify behavior**

Manual expected results:

```text
App opens to the pure-black watch bookshelf.
The bookshelf scans Download and shows watch-test.
Tapping watch-test opens the reader.
Reader background is true black.
Text is readable on the round display.
Left/right tap page navigation works.
Left-edge right swipe returns to bookshelf.
No network prompt or online error appears in airplane mode.
```

- [ ] **Step 4: Record follow-up defects**

If a defect appears, write a failing test first when it is logic-level. For visual/layout defects, capture a screenshot and create a focused fix commit:

```powershell
git add app/src/main
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "fix: adjust watch reader smoke test issue"
```

## Self-Review

Spec coverage:
- Local `Download` TXT scanning: Tasks 1, 3, 4, 9.
- Bookshelf-first round-watch UI: Task 4.
- OLED true black defaults: Tasks 4, 5.
- Left-edge swipe-back: Tasks 2, 4, 5, 9.
- Product-layer offline mode and no network permissions: Tasks 5, 6, 7, 8.
- TXT-only file association: Task 6.
- Verification: Tasks 8 and 9.

Readiness scan:
- No unfinished markers or unspecified implementation steps remain.

Type consistency:
- `WatchTxtFileFilter`, `WatchTxtImporter`, `WatchTxtImportResult`, `EdgeSwipeBackDetector`, `EdgeSwipeBackLayout`, `WatchBookshelfViewModel`, `WatchBookAdapter`, `WatchBookshelfActivity`, and `WatchReaderDefaults` are defined before use.
