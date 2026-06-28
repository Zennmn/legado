# Remove Unnecessary Dependencies and Dead Code Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove ~15 unnecessary dependencies from build.gradle and dead code from App.kt to simplify the offline TXT reader app.

**Architecture:** Incrementally remove dependencies, verify compile after each batch, then clean up dead code and run final verification.

**Tech Stack:** Android, Gradle, Kotlin

## Global Constraints

- Keep Rhino module (`:modules:rhino`) - TextFile.kt uses it for TXT TOC rule evaluation
- Keep JsTest.kt deletion - it's not part of watch TXT reader tests
- Verify compile passes after each dependency removal
- If removal causes compile error, restore dependency and note which code needs it

---

### Task 1: Remove Dependencies Batch 1 (Network/HTML/Compression)

**Files:**
- Modify: `app/build.gradle:222-251`
- Test: Run compile to verify

**Interfaces:**
- Consumes: Current build.gradle dependency list
- Produces: Updated build.gradle with batch 1 removed

- [ ] **Step 1: Remove first batch of dependencies**

Remove these lines from `app/build.gradle`:
```groovy
implementation(libs.jsoup)
implementation(libs.json.path)
implementation(libs.okhttp)
implementation(libs.libarchive)
implementation(libs.hutool.crypto)
```

- [ ] **Step 2: Run compile to verify**

Run: `.\gradlew.bat :app:compileAppDebugKotlin`
Expected: PASS - if FAIL, restore the failing dependency

---

### Task 2: Remove Dependencies Batch 2 (Editor/Markdown/SVG)

**Files:**
- Modify: `app/build.gradle`
- Test: Run compile to verify

**Interfaces:**
- Consumes: Updated build.gradle from Task 1
- Produces: Updated build.gradle with batch 2 removed

- [ ] **Step 1: Remove second batch of dependencies**

Remove these lines from `app/build.gradle`:
```groovy
implementation(platform(libs.soraEditor.bom))
implementation(libs.soraEditor.core)
implementation(libs.soraEditor.language.textmate)
implementation(libs.markwon.core)
implementation(libs.markwon.image.glide)
implementation(libs.markwon.ext.tables)
implementation(libs.markwon.html)
implementation(libs.androidsvg)
implementation(libs.glide.svg)
```

- [ ] **Step 2: Run compile to verify**

Run: `.\gradlew.bat :app:compileAppDebugKotlin`
Expected: PASS - if FAIL, restore the failing dependency

---

### Task 3: Remove Dependencies Batch 3 (UI/Utility)

**Files:**
- Modify: `app/build.gradle`
- Test: Run compile to verify

**Interfaces:**
- Consumes: Updated build.gradle from Task 2
- Produces: Updated build.gradle with batch 3 removed

- [ ] **Step 1: Remove third batch of dependencies**

Remove these lines from `app/build.gradle`:
```groovy
implementation(libs.zxing.lite)
implementation(libs.colorpicker)
implementation(libs.commons.text)
implementation(libs.glide.recyclerview)
implementation(libs.flexbox)
implementation(libs.glide.okhttp)
```

- [ ] **Step 2: Run compile to verify**

Run: `.\gradlew.bat :app:compileAppDebugKotlin`
Expected: PASS - if FAIL, restore the failing dependency

---

### Task 4: Remove Rhino Module from settings.gradle (Conditional)

**Files:**
- Modify: `settings.gradle:52`
- Test: Run compile to verify

**Interfaces:**
- Consumes: Current settings.gradle
- Produces: Updated settings.gradle (if compile passes)

- [ ] **Step 1: Check if Rhino is still needed**

TextFile.kt uses `com.script.rhino.RhinoScriptEngine` - the module MUST be kept.
Skip this task entirely - Rhino is required.

---

### Task 5: Clean Up App.kt Dead Code

**Files:**
- Modify: `app/src/main/java/io/legado/app/App.kt`
- Test: Run compile to verify

**Interfaces:**
- Consumes: Current App.kt with dead code
- Produces: Clean App.kt without dead code

- [ ] **Step 1: Remove installGmsTlsProvider() method**

Remove lines 89-120 from App.kt (the unused `installGmsTlsProvider` method):
```kotlin
    /**
     * 尝试在安装了GMS的设备上(GMS或者MicroG)使用GMS内置的Conscrypt
     * 作为首选JCE提供程序，而使Okhttp在低版本Android上
     * 能够启用TLSv1.3
     * https://f-droid.org/zh_Hans/2020/05/29/android-updates-and-tls-connections.html
     * https://developer.android.google.cn/reference/javax/net/ssl/SSLSocket
     *
     * @param context
     * @return
     */
    private fun installGmsTlsProvider(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return
        }
        try {
            val gmsPackageName = "com.google.android.gms"
            val appInfo = packageManager.getApplicationInfo(gmsPackageName, 0)
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                return
            }
            val gms = context.createPackageContext(
                gmsPackageName,
                CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY
            )
            gms.classLoader
                .loadClass("com.google.android.gms.common.security.ProviderInstallerImpl")
                .getMethod("insertProvider", Context::class.java)
                .invoke(null, gms)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
```

- [ ] **Step 2: Remove unused notification channels**

Remove `readAloudChannel` and `webChannel` definitions from `createNotificationChannels()`:
```kotlin
        val readAloudChannel = NotificationChannel(
            channelIdReadAloud,
            getString(R.string.read_aloud),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val webChannel = NotificationChannel(
            channelIdWeb,
            getString(R.string.web_service),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
```

Update `notificationManager.createNotificationChannels()` to only include `downloadChannel`:
```kotlin
        notificationManager.createNotificationChannels(
            listOf(downloadChannel)
        )
```

Remove unused imports:
```kotlin
import io.legado.app.constant.AppConst.channelIdReadAloud
import io.legado.app.constant.AppConst.channelIdWeb
```

- [ ] **Step 3: Run compile to verify**

Run: `.\gradlew.bat :app:compileAppDebugKotlin`
Expected: PASS

---

### Task 6: Delete JsTest.kt

**Files:**
- Delete: `app/src/test/java/io/legado/app/JsTest.kt`

**Interfaces:**
- Consumes: JsTest.kt file
- Produces: File deleted

- [ ] **Step 1: Delete JsTest.kt**

Delete the file `app/src/test/java/io/legado/app/JsTest.kt`

- [ ] **Step 2: Run compile to verify**

Run: `.\gradlew.bat :app:compileAppDebugKotlin`
Expected: PASS

---

### Task 7: Final Verification and Commit

**Files:**
- All modified files

**Interfaces:**
- Consumes: All changes from Tasks 1-6
- Produces: Verified, committed changes

- [ ] **Step 1: Run full compile**

Run: `.\gradlew.bat :app:compileAppDebugKotlin`
Expected: PASS

- [ ] **Step 2: Run watch TXT reader tests**

Run: `.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.*" --tests "io.legado.app.ui.watch.*"`
Expected: PASS

- [ ] **Step 3: Build APK**

Run: `.\gradlew.bat :app:assembleAppDebug`
Expected: PASS

- [ ] **Step 4: Commit changes**

Run: `git add -A && git commit -m "chore: remove unnecessary dependencies and dead code"`

---

## Summary of Dependencies to Remove

| Dependency | Reason for Removal |
|------------|-------------------|
| `libs.jsoup` | HTML parsing not needed for offline TXT reader |
| `libs.json.path` | JSON path parsing not needed |
| `libs.okhttp` | HTTP client not needed for offline reader |
| `libs.libarchive` | Archive compression not needed |
| `libs.hutool.crypto` | Encryption not needed |
| `libs.soraEditor.*` | Code editor not needed |
| `libs.markwon.*` | Markdown rendering not needed |
| `libs.androidsvg` | SVG rendering not needed |
| `libs.glide.svg` | SVG for Glide not needed |
| `libs.zxing.lite` | QR code scanning not needed |
| `libs.colorpicker` | Color picker UI not needed |
| `libs.commons.text` | Apache commons text not needed |
| `libs.glide.recyclerview` | Glide RecyclerView integration not needed |
| `libs.flexbox` | FlexBox layout not needed |
| `libs.glide.okhttp` | Glide OkHttp integration not needed |

## Dependencies to Keep

| Dependency | Reason |
|------------|--------|
| `:modules:rhino` | TextFile.kt uses RhinoScriptEngine for TXT TOC rule evaluation |
| `libs.glide.glide` | Image loading for book covers |
| `libs.gson` | JSON serialization |
| All AndroidX libs | Core Android functionality |
| Room libs | Database |
| Kotlin/Coroutines libs | Core language support |

## Dead Code to Remove

1. `installGmsTlsProvider()` method - never called, TLS provider installation not needed
2. `readAloudChannel` notification channel - read aloud feature not in TXT reader
3. `webChannel` notification channel - web service feature not in TXT reader
