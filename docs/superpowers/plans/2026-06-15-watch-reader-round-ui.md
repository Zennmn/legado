# Watch Reader Round UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the currently reachable phone-style reader controls with a minimal round-watch UI for local TXT reading on the China-market OPPO Watch X.

**Architecture:** Keep the existing `ReadBookActivity`, pagination engine, local TXT import, and OLED defaults. Add watch-specific reader menu, chapter list, and settings surfaces, then route the old reader callbacks to no-op or watch UI so search, replace, book info, read-aloud, phone menu, text action, and phone first-run dialogs are no longer reachable from normal watch use.

**Tech Stack:** Kotlin, Android XML layouts, ViewBinding, existing Legado `ReadBook`/`ReadBookConfig`/Room pipeline, JUnit4 local unit tests, Android Gradle Plugin.

---

## File Structure

- Create `app/src/main/java/io/legado/app/ui/watch/WatchReaderControls.kt`: pure clamp and formatting helpers for font, line spacing, brightness, and progress text.
- Create `app/src/test/java/io/legado/app/ui/watch/WatchReaderControlsTest.kt`: local unit tests for control bounds.
- Create `app/src/main/java/io/legado/app/ui/book/read/WatchReadMenu.kt`: round-watch replacement for `ReadMenu`, exposing the same callback surface to reduce `ReadBookActivity` churn.
- Create `app/src/main/res/layout/view_watch_read_menu.xml`: pure-black safe-area reader menu with only exit, TOC, settings, font size, brightness, and OLED reset controls.
- Create `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocActivityResult.kt`: Activity result contract returning the same extras expected by `ReadBookActivity`.
- Create `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocActivity.kt`: simple round-watch chapter list activity.
- Create `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocViewModel.kt`: loads local chapters from Room.
- Create `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocAdapter.kt`: compact chapter row adapter.
- Create `app/src/main/res/layout/activity_watch_toc.xml`: pure-black TOC surface with circular safe padding.
- Create `app/src/main/res/layout/item_watch_toc.xml`: compact chapter row.
- Create `app/src/main/java/io/legado/app/ui/book/read/config/WatchReaderSettingsDialog.kt`: round-watch settings dialog for font, line spacing, brightness, and OLED reset.
- Create `app/src/main/res/layout/dialog_watch_reader_settings.xml`: compact centered settings panel.
- Modify `app/src/main/res/layout/activity_book_read.xml`: replace `ReadMenu` with `WatchReadMenu`; keep `SearchMenu` hidden so existing binding references still compile.
- Modify `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt`: use watch TOC, suppress phone menu, block old callbacks, block text action menu, keep no-network cleanup.
- Modify `app/src/main/java/io/legado/app/ui/book/read/BaseReadBookActivity.kt`: allow watch builds to skip the phone click-region onboarding dialog.
- Modify `app/src/main/java/io/legado/app/ui/watch/WatchReaderDefaults.kt`: disable text selection and force watch reader defaults before first reader UI work.
- Modify `app/src/main/res/values/strings.xml`: add watch reader labels.
- Modify `app/src/main/res/values/styles.xml`: add reusable watch menu and settings text styles.

## Task 1: Watch Reader Control Logic

**Files:**
- Create: `app/src/test/java/io/legado/app/ui/watch/WatchReaderControlsTest.kt`
- Create: `app/src/main/java/io/legado/app/ui/watch/WatchReaderControls.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/io/legado/app/ui/watch/WatchReaderControlsTest.kt`:

```kotlin
package io.legado.app.ui.watch

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchReaderControlsTest {

    @Test
    fun textSizeIsClampedToWatchRange() {
        assertEquals(16, WatchReaderControls.nextTextSize(16, -1))
        assertEquals(22, WatchReaderControls.nextTextSize(20, 2))
        assertEquals(32, WatchReaderControls.nextTextSize(32, 1))
    }

    @Test
    fun lineSpacingIsClampedToWatchRange() {
        assertEquals(2, WatchReaderControls.nextLineSpacing(2, -2))
        assertEquals(10, WatchReaderControls.nextLineSpacing(8, 2))
        assertEquals(20, WatchReaderControls.nextLineSpacing(20, 4))
    }

    @Test
    fun brightnessIsClampedToReadableRange() {
        assertEquals(8, WatchReaderControls.nextBrightness(8, -16))
        assertEquals(144, WatchReaderControls.nextBrightness(128, 16))
        assertEquals(255, WatchReaderControls.nextBrightness(248, 16))
    }

    @Test
    fun progressTextHandlesEmptyChapterLists() {
        assertEquals("第 1/1 章", WatchReaderControls.chapterProgressText(0, 0))
        assertEquals("第 3/12 章", WatchReaderControls.chapterProgressText(2, 12))
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.ui.watch.WatchReaderControlsTest"
```

Expected: FAIL because `WatchReaderControls` is unresolved.

- [ ] **Step 3: Implement the helper**

Create `app/src/main/java/io/legado/app/ui/watch/WatchReaderControls.kt`:

```kotlin
package io.legado.app.ui.watch

object WatchReaderControls {

    const val MIN_TEXT_SIZE = 16
    const val MAX_TEXT_SIZE = 32
    const val MIN_LINE_SPACING = 2
    const val MAX_LINE_SPACING = 20
    const val MIN_BRIGHTNESS = 8
    const val MAX_BRIGHTNESS = 255

    fun nextTextSize(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)
    }

    fun nextLineSpacing(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_LINE_SPACING, MAX_LINE_SPACING)
    }

    fun nextBrightness(current: Int, delta: Int): Int {
        return (current + delta).coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
    }

    fun chapterProgressText(index: Int, total: Int): String {
        val safeTotal = total.coerceAtLeast(1)
        val safeIndex = index.coerceIn(0, safeTotal - 1) + 1
        return "第 $safeIndex/$safeTotal 章"
    }
}
```

- [ ] **Step 4: Run tests and verify GREEN**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.ui.watch.WatchReaderControlsTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/test/java/io/legado/app/ui/watch/WatchReaderControlsTest.kt app/src/main/java/io/legado/app/ui/watch/WatchReaderControls.kt
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "test: add watch reader controls"
```

## Task 2: Watch Reader Menu

**Files:**
- Create: `app/src/main/java/io/legado/app/ui/book/read/WatchReadMenu.kt`
- Create: `app/src/main/res/layout/view_watch_read_menu.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/styles.xml`

- [ ] **Step 1: Add watch menu strings**

Add these strings before `</resources>` in `app/src/main/res/values/strings.xml`:

```xml
    <string name="watch_reader_menu">阅读</string>
    <string name="watch_reader_exit">退出</string>
    <string name="watch_reader_toc">目录</string>
    <string name="watch_reader_settings">设置</string>
    <string name="watch_text_smaller">A-</string>
    <string name="watch_text_larger">A+</string>
    <string name="watch_brightness_down">暗</string>
    <string name="watch_brightness_up">亮</string>
    <string name="watch_oled_reset">纯黑</string>
```

- [ ] **Step 2: Add the watch menu layout**

Create `app/src/main/res/layout/view_watch_read_menu.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/watch_menu_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/watch_oled_black"
    android:clickable="true"
    android:focusable="true"
    android:paddingStart="26dp"
    android:paddingTop="24dp"
    android:paddingEnd="26dp"
    android:paddingBottom="24dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tv_book_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:ellipsize="end"
            android:gravity="center"
            android:includeFontPadding="false"
            android:maxLines="1"
            android:textColor="@color/watch_text_primary"
            android:textSize="18sp"
            android:textStyle="bold"
            tools:text="本地小说" />

        <TextView
            android:id="@+id/tv_progress"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:gravity="center"
            android:includeFontPadding="false"
            android:textColor="@color/watch_text_secondary"
            android:textSize="12sp"
            tools:text="第 3/12 章" />

        <GridLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="18dp"
            android:columnCount="3"
            android:rowCount="3">

            <TextView
                android:id="@+id/btn_exit"
                style="@style/WatchReaderMenuButton"
                android:text="@string/watch_reader_exit" />

            <TextView
                android:id="@+id/btn_toc"
                style="@style/WatchReaderMenuButton"
                android:text="@string/watch_reader_toc" />

            <TextView
                android:id="@+id/btn_settings"
                style="@style/WatchReaderMenuButton"
                android:text="@string/watch_reader_settings" />

            <TextView
                android:id="@+id/btn_text_down"
                style="@style/WatchReaderMenuButton"
                android:text="@string/watch_text_smaller" />

            <TextView
                android:id="@+id/tv_text_size"
                style="@style/WatchReaderMenuValue"
                tools:text="22" />

            <TextView
                android:id="@+id/btn_text_up"
                style="@style/WatchReaderMenuButton"
                android:text="@string/watch_text_larger" />

            <TextView
                android:id="@+id/btn_brightness_down"
                style="@style/WatchReaderMenuButton"
                android:text="@string/watch_brightness_down" />

            <TextView
                android:id="@+id/tv_brightness"
                style="@style/WatchReaderMenuValue"
                tools:text="128" />

            <TextView
                android:id="@+id/btn_brightness_up"
                style="@style/WatchReaderMenuButton"
                android:text="@string/watch_brightness_up" />
        </GridLayout>

        <TextView
            android:id="@+id/btn_oled"
            android:layout_width="96dp"
            android:layout_height="40dp"
            android:layout_marginTop="12dp"
            android:gravity="center"
            android:text="@string/watch_oled_reset"
            android:textColor="@color/watch_text_primary"
            android:textSize="14sp" />
    </LinearLayout>
</FrameLayout>
```

Add these styles to `app/src/main/res/values/styles.xml` before `</resources>`:

```xml
    <style name="WatchReaderMenuButton">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">42dp</item>
        <item name="android:layout_columnWeight">1</item>
        <item name="android:layout_margin">3dp</item>
        <item name="android:background">?android:attr/selectableItemBackground</item>
        <item name="android:gravity">center</item>
        <item name="android:includeFontPadding">false</item>
        <item name="android:textColor">@color/watch_text_primary</item>
        <item name="android:textSize">14sp</item>
    </style>

    <style name="WatchReaderMenuValue">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">42dp</item>
        <item name="android:layout_columnWeight">1</item>
        <item name="android:layout_margin">3dp</item>
        <item name="android:gravity">center</item>
        <item name="android:includeFontPadding">false</item>
        <item name="android:textColor">@color/watch_text_secondary</item>
        <item name="android:textSize">13sp</item>
    </style>
```

- [ ] **Step 3: Add the watch menu class**

Create `app/src/main/java/io/legado/app/ui/book/read/WatchReadMenu.kt`:

```kotlin
package io.legado.app.ui.book.read

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import io.legado.app.constant.EventBus
import io.legado.app.databinding.ViewWatchReadMenuBinding
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.watch.WatchReaderControls
import io.legado.app.ui.watch.WatchReaderDefaults
import io.legado.app.utils.activity
import io.legado.app.utils.gone
import io.legado.app.utils.postEvent
import io.legado.app.utils.visible

class WatchReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var canShowMenu: Boolean = false
    private val binding = ViewWatchReadMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private val callBack: CallBack get() = activity as CallBack
    private var onMenuOutEnd: (() -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        bindEvent()
        gone()
    }

    fun runMenuIn(anim: Boolean = false) {
        callBack.onMenuShow()
        upBookView()
        upNumbers()
        visible()
        canShowMenu = true
        callBack.upSystemUiVisibility()
    }

    fun runMenuOut(anim: Boolean = false, onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        gone()
        canShowMenu = false
        callBack.onMenuHide()
        callBack.upSystemUiVisibility()
        this.onMenuOutEnd?.invoke()
        this.onMenuOutEnd = null
    }

    fun reset() {
        upBookView()
        upNumbers()
    }

    fun refreshMenuColorFilter() = Unit

    fun upBrightnessState() {
        upNumbers()
        applyBrightnessToWindow()
    }

    fun upBookView() = binding.run {
        val book = ReadBook.book
        tvBookName.text = book?.name.orEmpty().ifBlank { book?.originName.orEmpty() }
        tvProgress.text = WatchReaderControls.chapterProgressText(
            index = ReadBook.durChapterIndex,
            total = book?.simulatedTotalChapterNum() ?: ReadBook.simulatedChapterSize
        )
    }

    fun upSeekBar() {
        upBookView()
    }

    fun setAutoPage(autoPage: Boolean) = Unit

    fun setSeekPage(seek: Int) = Unit

    private fun bindEvent() = binding.run {
        watchMenuRoot.setOnClickListener { runMenuOut() }
        btnExit.setOnClickListener {
            runMenuOut {
                activity?.finish()
            }
        }
        btnToc.setOnClickListener {
            runMenuOut {
                callBack.openChapterList()
            }
        }
        btnSettings.setOnClickListener {
            runMenuOut {
                callBack.showMoreSetting()
            }
        }
        btnTextDown.setOnClickListener { changeTextSize(-1) }
        btnTextUp.setOnClickListener { changeTextSize(1) }
        btnBrightnessDown.setOnClickListener { changeBrightness(-16) }
        btnBrightnessUp.setOnClickListener { changeBrightness(16) }
        btnOled.setOnClickListener {
            WatchReaderDefaults.apply()
            postEvent(EventBus.UP_CONFIG, arrayListOf(0, 1, 2, 6, 9, 11))
            upNumbers()
            applyBrightnessToWindow()
        }
    }

    private fun changeTextSize(delta: Int) {
        val config = ReadBookConfig.durConfig
        config.textSize = WatchReaderControls.nextTextSize(config.textSize, delta)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
        upNumbers()
    }

    private fun changeBrightness(delta: Int) {
        AppConfig.readBrightness = WatchReaderControls.nextBrightness(AppConfig.readBrightness, delta)
        applyBrightnessToWindow()
        upNumbers()
    }

    private fun applyBrightnessToWindow() {
        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = (AppConfig.readBrightness / 255f)
                .coerceAtLeast(0.004f)
                .coerceAtMost(1f)
            window.attributes = params
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun upNumbers() = binding.run {
        tvTextSize.text = ReadBookConfig.durConfig.textSize.toString()
        tvBrightness.text = AppConfig.readBrightness.toString()
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun openSourceEditActivity()
        fun openBookInfoActivity()
        fun showReadStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showHelp()
        fun showLogin()
        fun payAction()
        fun disableSource()
        fun skipToChapter(index: Int)
        fun onMenuShow()
        fun onMenuHide()
    }
}
```

- [ ] **Step 4: Build**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: Kotlin compilation succeeds.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/ui/book/read/WatchReadMenu.kt app/src/main/res/layout/view_watch_read_menu.xml app/src/main/res/values/strings.xml app/src/main/res/values/styles.xml
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "feat: add watch reader menu"
```

## Task 3: Watch Chapter List

**Files:**
- Create: `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocActivityResult.kt`
- Create: `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocActivity.kt`
- Create: `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocViewModel.kt`
- Create: `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocAdapter.kt`
- Create: `app/src/main/res/layout/activity_watch_toc.xml`
- Create: `app/src/main/res/layout/item_watch_toc.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add strings**

Add these strings before `</resources>` in `app/src/main/res/values/strings.xml`:

```xml
    <string name="watch_toc_title">目录</string>
    <string name="watch_toc_empty">没有目录</string>
```

- [ ] **Step 2: Add layouts**

Create `app/src/main/res/layout/activity_watch_toc.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<io.legado.app.ui.watch.EdgeSwipeBackLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/swipe_back_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/watch_oled_black"
    android:paddingStart="24dp"
    android:paddingTop="22dp"
    android:paddingEnd="24dp"
    android:paddingBottom="22dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tv_title"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:includeFontPadding="false"
            android:text="@string/watch_toc_title"
            android:textColor="@color/watch_text_primary"
            android:textSize="18sp"
            android:textStyle="bold" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/recycler_view"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_marginTop="12dp"
            android:layout_weight="1"
            android:clipToPadding="false"
            android:paddingBottom="12dp"
            tools:listitem="@layout/item_watch_toc" />

        <TextView
            android:id="@+id/tv_empty"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_marginTop="12dp"
            android:layout_weight="1"
            android:gravity="center"
            android:text="@string/watch_toc_empty"
            android:textColor="@color/watch_text_secondary"
            android:textSize="14sp"
            android:visibility="gone" />
    </LinearLayout>
</io.legado.app.ui.watch.EdgeSwipeBackLayout>
```

Create `app/src/main/res/layout/item_watch_toc.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="52dp"
    android:background="?android:attr/selectableItemBackground"
    android:gravity="center_vertical"
    android:orientation="vertical"
    android:paddingStart="8dp"
    android:paddingEnd="8dp">

    <TextView
        android:id="@+id/tv_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:ellipsize="end"
        android:includeFontPadding="false"
        android:maxLines="1"
        android:textColor="@color/watch_text_primary"
        android:textSize="15sp"
        tools:text="第一章" />

    <TextView
        android:id="@+id/tv_index"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:includeFontPadding="false"
        android:textColor="@color/watch_text_secondary"
        android:textSize="11sp"
        tools:text="第 1 章" />
</LinearLayout>
```

- [ ] **Step 3: Add the result contract**

Create `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocActivityResult.kt`:

```kotlin
package io.legado.app.ui.watch.toc

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class WatchTocActivityResult : ActivityResultContract<String, Array<Any>?>() {

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, WatchTocActivity::class.java)
            .putExtra("bookUrl", input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Array<Any>? {
        if (resultCode != RESULT_OK || intent == null) {
            return null
        }
        return arrayOf(
            intent.getIntExtra("index", 0),
            intent.getIntExtra("chapterPos", 0),
            intent.getBooleanExtra("chapterChanged", false),
            0,
            0
        )
    }
}
```

- [ ] **Step 4: Add ViewModel and adapter**

Create `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocViewModel.kt`:

```kotlin
package io.legado.app.ui.watch.toc

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.simulatedTotalChapterNum
import kotlinx.coroutines.Dispatchers.IO

data class WatchTocState(
    val book: Book,
    val chapters: List<BookChapter>
)

class WatchTocViewModel(application: Application) : BaseViewModel(application) {

    val tocLiveData = MutableLiveData<WatchTocState>()
    val errorLiveData = MutableLiveData<String>()

    fun load(bookUrl: String) {
        execute(context = IO) {
            val book = appDb.bookDao.getBook(bookUrl)
                ?: throw NoStackTraceException("找不到书籍")
            val end = (book.simulatedTotalChapterNum() - 1).coerceAtLeast(0)
            val chapters = appDb.bookChapterDao.getChapterList(bookUrl, 0, end)
            WatchTocState(book, chapters)
        }.onSuccess {
            tocLiveData.postValue(it)
        }.onError {
            errorLiveData.postValue(it.localizedMessage ?: "目录加载失败")
        }
    }
}
```

Create `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocAdapter.kt`:

```kotlin
package io.legado.app.ui.watch.toc

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ItemWatchTocBinding

class WatchTocAdapter(
    private val onClick: (BookChapter) -> Unit
) : RecyclerView.Adapter<WatchTocAdapter.Holder>() {

    private val chapters = arrayListOf<BookChapter>()
    var currentChapterIndex: Int = 0

    fun submitList(items: List<BookChapter>) {
        chapters.clear()
        chapters.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemWatchTocBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount(): Int = chapters.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(chapters[position])
    }

    inner class Holder(
        private val binding: ItemWatchTocBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chapter: BookChapter) = binding.run {
            tvTitle.text = chapter.title.ifBlank { "第 ${chapter.index + 1} 章" }
            tvIndex.text = if (chapter.index == currentChapterIndex) {
                "当前 · 第 ${chapter.index + 1} 章"
            } else {
                "第 ${chapter.index + 1} 章"
            }
            root.setOnClickListener { onClick(chapter) }
        }
    }
}
```

- [ ] **Step 5: Add the activity**

Create `app/src/main/java/io/legado/app/ui/watch/toc/WatchTocActivity.kt`:

```kotlin
package io.legado.app.ui.watch.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ActivityWatchTocBinding
import io.legado.app.utils.gone
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible

class WatchTocActivity :
    VMBaseActivity<ActivityWatchTocBinding, WatchTocViewModel>(
        theme = Theme.Dark,
        imageBg = false
    ) {

    override val binding by viewBinding(ActivityWatchTocBinding::inflate)
    override val viewModel by viewModels<WatchTocViewModel>()

    private val adapter by lazy {
        WatchTocAdapter(::selectChapter)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
        observe()
        viewModel.load(intent.getStringExtra("bookUrl").orEmpty())
    }

    private fun initView() = binding.run {
        swipeBackLayout.setOnEdgeBack { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this@WatchTocActivity)
        recyclerView.adapter = adapter
    }

    private fun observe() {
        viewModel.tocLiveData.observe(this) { state ->
            adapter.currentChapterIndex = state.book.durChapterIndex
            adapter.submitList(state.chapters)
            binding.recyclerView.visible(state.chapters.isNotEmpty())
            binding.tvEmpty.visible(state.chapters.isEmpty())
            val currentPosition = state.chapters.indexOfFirst { it.index >= state.book.durChapterIndex }
            if (currentPosition >= 0) {
                binding.recyclerView.scrollToPosition(currentPosition)
            }
        }
        viewModel.errorLiveData.observe(this) {
            binding.recyclerView.gone()
            binding.tvEmpty.visible()
            toastOnUi(it)
        }
    }

    private fun selectChapter(chapter: BookChapter) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra("index", chapter.index)
                .putExtra("chapterPos", 0)
                .putExtra("chapterChanged", true)
        )
        finish()
    }
}
```

- [ ] **Step 6: Register the activity**

Add this activity near `WatchBookshelfActivity` in `app/src/main/AndroidManifest.xml`:

```xml
        <activity
            android:name=".ui.watch.toc.WatchTocActivity"
            android:configChanges="locale|keyboardHidden|orientation|screenSize|smallestScreenSize|screenLayout|uiMode"
            android:exported="false"
            android:screenOrientation="portrait" />
```

- [ ] **Step 7: Build**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: Kotlin compilation succeeds.

- [ ] **Step 8: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/ui/watch/toc app/src/main/res/layout/activity_watch_toc.xml app/src/main/res/layout/item_watch_toc.xml app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "feat: add watch chapter list"
```

## Task 4: Watch Reader Settings

**Files:**
- Create: `app/src/main/java/io/legado/app/ui/book/read/config/WatchReaderSettingsDialog.kt`
- Create: `app/src/main/res/layout/dialog_watch_reader_settings.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/styles.xml`

- [ ] **Step 1: Add settings strings**

Add these strings before `</resources>` in `app/src/main/res/values/strings.xml`:

```xml
    <string name="watch_setting_text_size">字号</string>
    <string name="watch_setting_line_spacing">行距</string>
    <string name="watch_setting_brightness">亮度</string>
    <string name="watch_setting_reset_black">重置纯黑</string>
    <string name="watch_minus">-</string>
    <string name="watch_plus">+</string>
```

- [ ] **Step 2: Add the settings layout**

Create `app/src/main/res/layout/dialog_watch_reader_settings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/watch_oled_black"
    android:orientation="vertical"
    android:paddingStart="20dp"
    android:paddingTop="18dp"
    android:paddingEnd="20dp"
    android:paddingBottom="18dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:includeFontPadding="false"
        android:text="@string/watch_reader_settings"
        android:textColor="@color/watch_text_primary"
        android:textSize="17sp"
        android:textStyle="bold" />

    <LinearLayout
        android:id="@+id/row_text_size"
        style="@style/WatchReaderSettingsRow">

        <TextView
            style="@style/WatchReaderSettingsLabel"
            android:text="@string/watch_setting_text_size" />

        <TextView
            android:id="@+id/btn_text_down"
            style="@style/WatchReaderSettingsButton"
            android:text="@string/watch_minus" />

        <TextView
            android:id="@+id/tv_text_size"
            style="@style/WatchReaderSettingsValue"
            tools:text="22" />

        <TextView
            android:id="@+id/btn_text_up"
            style="@style/WatchReaderSettingsButton"
            android:text="@string/watch_plus" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/row_line_spacing"
        style="@style/WatchReaderSettingsRow">

        <TextView
            style="@style/WatchReaderSettingsLabel"
            android:text="@string/watch_setting_line_spacing" />

        <TextView
            android:id="@+id/btn_line_down"
            style="@style/WatchReaderSettingsButton"
            android:text="@string/watch_minus" />

        <TextView
            android:id="@+id/tv_line_spacing"
            style="@style/WatchReaderSettingsValue"
            tools:text="8" />

        <TextView
            android:id="@+id/btn_line_up"
            style="@style/WatchReaderSettingsButton"
            android:text="@string/watch_plus" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/row_brightness"
        style="@style/WatchReaderSettingsRow">

        <TextView
            style="@style/WatchReaderSettingsLabel"
            android:text="@string/watch_setting_brightness" />

        <TextView
            android:id="@+id/btn_brightness_down"
            style="@style/WatchReaderSettingsButton"
            android:text="@string/watch_minus" />

        <TextView
            android:id="@+id/tv_brightness"
            style="@style/WatchReaderSettingsValue"
            tools:text="128" />

        <TextView
            android:id="@+id/btn_brightness_up"
            style="@style/WatchReaderSettingsButton"
            android:text="@string/watch_plus" />
    </LinearLayout>

    <TextView
        android:id="@+id/btn_reset_black"
        android:layout_width="match_parent"
        android:layout_height="42dp"
        android:layout_marginTop="10dp"
        android:background="?android:attr/selectableItemBackground"
        android:gravity="center"
        android:text="@string/watch_setting_reset_black"
        android:textColor="@color/watch_text_primary"
        android:textSize="14sp" />
</LinearLayout>
```

Add these styles to `app/src/main/res/values/styles.xml` before `</resources>`:

```xml
    <style name="WatchReaderSettingsRow">
        <item name="android:layout_width">match_parent</item>
        <item name="android:layout_height">46dp</item>
        <item name="android:layout_marginTop">8dp</item>
        <item name="android:gravity">center_vertical</item>
        <item name="android:orientation">horizontal</item>
    </style>

    <style name="WatchReaderSettingsLabel">
        <item name="android:layout_width">0dp</item>
        <item name="android:layout_height">match_parent</item>
        <item name="android:layout_weight">1</item>
        <item name="android:gravity">center_vertical</item>
        <item name="android:includeFontPadding">false</item>
        <item name="android:textColor">@color/watch_text_primary</item>
        <item name="android:textSize">14sp</item>
    </style>

    <style name="WatchReaderSettingsButton">
        <item name="android:layout_width">40dp</item>
        <item name="android:layout_height">40dp</item>
        <item name="android:background">?android:attr/selectableItemBackground</item>
        <item name="android:gravity">center</item>
        <item name="android:includeFontPadding">false</item>
        <item name="android:textColor">@color/watch_text_primary</item>
        <item name="android:textSize">18sp</item>
    </style>

    <style name="WatchReaderSettingsValue">
        <item name="android:layout_width">42dp</item>
        <item name="android:layout_height">match_parent</item>
        <item name="android:gravity">center</item>
        <item name="android:includeFontPadding">false</item>
        <item name="android:textColor">@color/watch_text_secondary</item>
        <item name="android:textSize">13sp</item>
    </style>
```

- [ ] **Step 3: Add the settings dialog**

Create `app/src/main/java/io/legado/app/ui/book/read/config/WatchReaderSettingsDialog.kt`:

```kotlin
package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.databinding.DialogWatchReaderSettingsBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.ui.watch.WatchReaderControls
import io.legado.app.ui.watch.WatchReaderDefaults
import io.legado.app.utils.dpToPx
import io.legado.app.utils.postEvent
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.math.min

class WatchReaderSettingsDialog : BaseDialogFragment(R.layout.dialog_watch_reader_settings) {

    private val binding by viewBinding(DialogWatchReaderSettingsBinding::bind)

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            setBackgroundDrawableResource(R.color.transparent)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.gravity = Gravity.CENTER
            attributes = attr
            val width = min(resources.displayMetrics.widthPixels - 48.dpToPx(), 260.dpToPx())
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        bindEvent()
        upValues()
    }

    private fun bindEvent() = binding.run {
        btnTextDown.setOnClickListener { changeTextSize(-1) }
        btnTextUp.setOnClickListener { changeTextSize(1) }
        btnLineDown.setOnClickListener { changeLineSpacing(-1) }
        btnLineUp.setOnClickListener { changeLineSpacing(1) }
        btnBrightnessDown.setOnClickListener { changeBrightness(-16) }
        btnBrightnessUp.setOnClickListener { changeBrightness(16) }
        btnResetBlack.setOnClickListener {
            WatchReaderDefaults.apply()
            postEvent(EventBus.UP_CONFIG, arrayListOf(0, 1, 2, 6, 9, 11))
            applyBrightnessToWindow()
            upValues()
        }
    }

    private fun changeTextSize(delta: Int) {
        val config = ReadBookConfig.durConfig
        config.textSize = WatchReaderControls.nextTextSize(config.textSize, delta)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
        upValues()
    }

    private fun changeLineSpacing(delta: Int) {
        val config = ReadBookConfig.durConfig
        config.lineSpacingExtra = WatchReaderControls.nextLineSpacing(config.lineSpacingExtra, delta)
        ReadBookConfig.save()
        postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6, 9, 11))
        upValues()
    }

    private fun changeBrightness(delta: Int) {
        AppConfig.readBrightness = WatchReaderControls.nextBrightness(AppConfig.readBrightness, delta)
        applyBrightnessToWindow()
        upValues()
    }

    private fun applyBrightnessToWindow() {
        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = (AppConfig.readBrightness / 255f)
                .coerceAtLeast(0.004f)
                .coerceAtMost(1f)
            window.attributes = params
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun upValues() = binding.run {
        tvTextSize.text = ReadBookConfig.durConfig.textSize.toString()
        tvLineSpacing.text = ReadBookConfig.durConfig.lineSpacingExtra.toString()
        tvBrightness.text = AppConfig.readBrightness.toString()
    }
}
```

- [ ] **Step 4: Build**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: Kotlin compilation succeeds.

- [ ] **Step 5: Commit**

Run:

```powershell
git add app/src/main/java/io/legado/app/ui/book/read/config/WatchReaderSettingsDialog.kt app/src/main/res/layout/dialog_watch_reader_settings.xml app/src/main/res/values/strings.xml app/src/main/res/values/styles.xml
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "feat: add watch reader settings"
```

## Task 5: Wire Watch UI Into Reader And Cut Old Entrypoints

**Files:**
- Modify: `app/src/main/res/layout/activity_book_read.xml`
- Modify: `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt`
- Modify: `app/src/main/java/io/legado/app/ui/book/read/BaseReadBookActivity.kt`
- Modify: `app/src/main/java/io/legado/app/ui/watch/WatchReaderDefaults.kt`

- [ ] **Step 1: Replace the reader menu view**

In `app/src/main/res/layout/activity_book_read.xml`, replace the `ReadMenu` tag:

```xml
    <io.legado.app.ui.book.read.ReadMenu
        android:id="@+id/read_menu"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone" />
```

with:

```xml
    <io.legado.app.ui.book.read.WatchReadMenu
        android:id="@+id/read_menu"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone" />
```

Keep the existing `SearchMenu` view below it with `android:visibility="gone"` so generated binding code and search-cleanup code still compile.

- [ ] **Step 2: Skip phone first-run reader dialogs for watch UI**

In `app/src/main/java/io/legado/app/ui/book/read/BaseReadBookActivity.kt`, add this property near `protected val menuLayoutIsVisible`:

```kotlin
    protected open val useWatchReaderUi: Boolean = false
```

Change the first-run dialog condition in `onActivityCreated` from:

```kotlin
        if (!LocalConfig.readHelpVersionIsLast) {
```

to:

```kotlin
        if (!useWatchReaderUi && !LocalConfig.readHelpVersionIsLast) {
```

- [ ] **Step 3: Tighten watch defaults**

In `app/src/main/java/io/legado/app/ui/watch/WatchReaderDefaults.kt`, add this preference write inside `apply()` after the existing `showRss` line:

```kotlin
        appCtx.putPrefBoolean(PreferKey.textSelectAble, false)
```

- [ ] **Step 4: Update reader imports and implemented interface**

In `app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt`, add imports:

```kotlin
import io.legado.app.ui.book.read.config.WatchReaderSettingsDialog
import io.legado.app.ui.watch.toc.WatchTocActivityResult
```

Remove these imports when they become unused:

```kotlin
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.read.config.MoreConfigDialog
import io.legado.app.ui.book.read.config.ReadAloudDialog
import io.legado.app.ui.book.read.config.ReadStyleDialog
import io.legado.app.ui.book.searchContent.SearchContentActivity
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.replace.ReplaceRuleActivity
```

Change the activity interface list from:

```kotlin
    ReadMenu.CallBack,
```

to:

```kotlin
    WatchReadMenu.CallBack,
```

Add this property near `override val isScroll`:

```kotlin
    override val useWatchReaderUi: Boolean = true
```

- [ ] **Step 5: Use watch TOC contract and remove phone Activity launchers**

Replace the TOC contract:

```kotlin
    private val tocActivity =
        registerForActivityResult(TocActivityResult()) {
            it?.let {
                viewModel.openChapter(it[0] as Int, it[1] as Int)
            }
        }
```

with:

```kotlin
    private val tocActivity =
        registerForActivityResult(WatchTocActivityResult()) {
            it?.let {
                viewModel.openChapter(it[0] as Int, it[1] as Int)
            }
        }
```

Delete these launcher properties because the watch reader no longer exposes these screens:

```kotlin
    private val replaceActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                viewModel.replaceRuleChanged()
            }
        }
    private val searchContentActivity =
        registerForActivityResult(StartActivityContract(SearchContentActivity::class.java)) {
            val data = it.data ?: return@registerForActivityResult
            val key = data.getLongExtra("key", System.currentTimeMillis())
            val index = data.getIntExtra("index", 0)
            val searchResult = IntentData.get<SearchResult>("searchResult$key")
            val searchResultList = IntentData.get<List<SearchResult>>("searchResultList$key")
            if (searchResult != null && searchResultList != null) {
                viewModel.searchContentQuery = searchResult.query
                binding.searchMenu.upSearchResultList(searchResultList)
                isShowingSearchResult = true
                viewModel.searchResultIndex = index
                binding.searchMenu.updateSearchResultIndex(index)
                binding.searchMenu.selectedSearchResult?.let { currentResult ->
                    ReadBook.saveCurrentBookProgress()
                    skipToSearch(currentResult)
                    showActionMenu()
                }
            }
        }
    private val bookInfoActivity =
        registerForActivityResult(StartActivityContract(BookInfoActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_DELETED)
                super.finish()
            } else {
                ReadBook.loadOrUpContent()
            }
        }
```

- [ ] **Step 6: Apply watch defaults before base reader UI setup**

In `onActivityCreated`, move `WatchReaderDefaults.apply()` before `super.onActivityCreated(savedInstanceState)`:

```kotlin
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        WatchReaderDefaults.apply()
        super.onActivityCreated(savedInstanceState)
        (binding.root as EdgeSwipeBackLayout).setOnEdgeBack {
            finish()
        }
```

- [ ] **Step 7: Remove phone overflow reader menu**

Replace `onCompatCreateOptionsMenu` with:

```kotlin
    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        return false
    }
```

Replace `onPrepareOptionsMenu` with:

```kotlin
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        return false
    }
```

At the start of `onMenuOpened`, add:

```kotlin
        if (watchLocalOnly) {
            return false
        }
```

- [ ] **Step 8: Block text selection action menu**

At the start of `showTextActionMenu()`, add:

```kotlin
        if (watchLocalOnly) {
            binding.readView.cancelSelect()
            return
        }
```

At the start of `onMenuItemSelected(itemId: Int)`, add:

```kotlin
        if (watchLocalOnly) {
            return true
        }
```

- [ ] **Step 9: Route old reader callbacks to watch behavior**

Replace these methods in `ReadBookActivity.kt`:

```kotlin
    override fun showReadAloudDialog() {
        showDialogFragment<ReadAloudDialog>()
    }

    override fun openBookInfoActivity() {
        ReadBook.book?.let {
            bookInfoActivity.launch {
                putExtra("name", it.name)
                putExtra("author", it.author)
            }
        }
    }

    override fun openReplaceRule() {
        replaceActivity.launch(Intent(this, ReplaceRuleActivity::class.java))
    }

    override fun openSearchActivity(searchWord: String?) {
        val book = ReadBook.book ?: return
        searchContentActivity.launch {
            putExtra("bookUrl", book.bookUrl)
            putExtra("searchWord", searchWord ?: viewModel.searchContentQuery)
            putExtra("searchResultIndex", viewModel.searchResultIndex)
            viewModel.searchResultList?.first()?.let {
                if (it.query == viewModel.searchContentQuery) {
                    IntentData.put("searchResultList", viewModel.searchResultList)
                }
            }
        }
    }

    override fun showReadStyle() {
        showDialogFragment<ReadStyleDialog>()
    }

    override fun showMoreSetting() {
        showDialogFragment<MoreConfigDialog>()
    }

    override fun showSearchSetting() {
        showDialogFragment<MoreConfigDialog>()
    }
```

with:

```kotlin
    override fun showReadAloudDialog() = Unit

    override fun openBookInfoActivity() = Unit

    override fun openReplaceRule() = Unit

    override fun openSearchActivity(searchWord: String?) = Unit

    override fun showReadStyle() {
        showDialogFragment<WatchReaderSettingsDialog>()
    }

    override fun showMoreSetting() {
        showDialogFragment<WatchReaderSettingsDialog>()
    }

    override fun showSearchSetting() {
        showDialogFragment<WatchReaderSettingsDialog>()
    }
```

Replace `onClickReadAloud()` with:

```kotlin
    override fun onClickReadAloud() = Unit
```

Replace `showHelp()` with:

```kotlin
    override fun showHelp() = Unit
```

- [ ] **Step 10: Keep no-network cleanup consistent**

In `onDestroy()`, replace:

```kotlin
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
```

with:

```kotlin
        if (!watchLocalOnly && !BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
```

- [ ] **Step 11: Build**

Run:

```powershell
.\gradlew.bat :app:compileAppDebugKotlin
```

Expected: Kotlin compilation succeeds.

- [ ] **Step 12: Commit**

Run:

```powershell
git add app/src/main/res/layout/activity_book_read.xml app/src/main/java/io/legado/app/ui/book/read/ReadBookActivity.kt app/src/main/java/io/legado/app/ui/book/read/BaseReadBookActivity.kt app/src/main/java/io/legado/app/ui/watch/WatchReaderDefaults.kt
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "feat: route reader to watch UI"
```

## Task 6: Verification

**Files:**
- No new files.

- [ ] **Step 1: Run focused unit tests**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.ui.watch.WatchReaderControlsTest"
```

Expected: PASS.

- [ ] **Step 2: Run all app unit tests**

Run:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleAppDebug
```

Expected: `BUILD SUCCESSFUL` and an APK under `app/build/outputs/apk/app/debug/`.

- [ ] **Step 4: Verify old network surface stays absent**

Run:

```powershell
Select-String -Path app\build\intermediates\merged_manifests\appDebug\processAppDebugManifest\AndroidManifest.xml -Pattern "android.permission.INTERNET|ACCESS_NETWORK_STATE|ACCESS_WIFI_STATE|WebService|DownloadService|ReaderProvider|OnLineImport"
```

Expected: no output.

- [ ] **Step 5: Verify old reader UI classes are not referenced from watch menu layout**

Run:

```powershell
Select-String -Path app\src\main\res\layout\activity_book_read.xml -Pattern "io.legado.app.ui.book.read.ReadMenu"
```

Expected: no output.

Run:

```powershell
Select-String -Path app\src\main\res\layout\activity_book_read.xml -Pattern "io.legado.app.ui.book.read.WatchReadMenu"
```

Expected:

```text
app\src\main\res\layout\activity_book_read.xml:...:<io.legado.app.ui.book.read.WatchReadMenu
```

- [ ] **Step 6: Manual round-watch smoke test**

On the OPPO Watch X or a round emulator:

```text
1. Put a .txt file in /sdcard/Download.
2. Launch the app.
3. Open the book from the watch bookshelf.
4. Tap center to open the reader menu.
5. Confirm only exit, TOC, settings, font, brightness, and pure-black controls are visible.
6. Confirm no search, replacement, book info, read-aloud, online refresh, or old overflow menu is reachable.
7. Open TOC and choose a chapter.
8. Open settings and adjust text size, line spacing, brightness, and pure-black reset.
9. Left-edge right swipe returns from settings/TOC/reader.
10. Confirm text and controls are not clipped by the round display edge.
```

- [ ] **Step 7: Commit verification-only fixes**

Run this only when Task 6 required small compile or resource fixes:

```powershell
git add app/src/main app/src/test
git -c user.name="Codex" -c user.email="codex@example.local" commit -m "fix: complete watch round UI verification"
```

Expected: no commit is created when Tasks 1-5 pass without additional fixes.

## Self-Review

Spec coverage:
- Main reader menu no longer uses phone `ReadMenu`: Task 2 and Task 5.
- Only watch-appropriate reader controls remain: Task 2, Task 4, Task 5.
- TOC is watch-specific and not the phone `TitleBar + TabLayout + ViewPager` screen: Task 3 and Task 5.
- Settings are watch-specific and not `ReadStyleDialog`/`MoreConfigDialog`: Task 4 and Task 5.
- Search, replace, book info, read-aloud, text action menu, phone overflow menu, and first-run phone click-region UI are blocked from normal watch use: Task 5.
- OLED pure black and left-swipe-back remain part of the watch flow: Task 2, Task 3, Task 4, Task 5, Task 6.
- No internet/network surface regression is checked: Task 6.

Placeholder scan:
- The plan contains no placeholder markers, no unresolved file paths, and no steps that ask the implementer to invent missing behavior.

Type consistency:
- `WatchReaderControls`, `WatchReadMenu`, `WatchTocActivityResult`, `WatchTocActivity`, `WatchTocViewModel`, `WatchTocAdapter`, and `WatchReaderSettingsDialog` are defined before any task uses them.
- `ReadBookActivity` continues to call `binding.readMenu.runMenuIn()`, `binding.readMenu.runMenuOut()`, `binding.readMenu.upBookView()`, `binding.readMenu.upSeekBar()`, `binding.readMenu.setAutoPage()`, and `binding.readMenu.upBrightnessState()` because `WatchReadMenu` implements those methods.
