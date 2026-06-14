# Watch TXT Reader Design

## Goal

Transform the current Legado Android app into a pure local TXT reader for the China-market OPPO Watch X. The first version targets a round OLED watch experience: local `Download` scanning, a simplified bookshelf-first home screen, pure black reading, and no user-visible network features.

## Confirmed Scope

The app becomes a local TXT reader, not a general Legado client on a smaller screen.

In scope:
- Scan public `Download` for `.txt` files.
- Automatically import new TXT files into the local bookshelf.
- Show a simplified round-watch bookshelf first on launch.
- Read TXT files with existing local text parsing and progress storage.
- Keep `.txt` file association as a secondary import/open path.
- Default to OLED pure black UI and reader background.
- Add left-edge swipe-back navigation.
- Disable product-layer networking and remove network permissions.

Out of scope for the first version:
- Online book sources, search, discovery, RSS, remote books, WebDAV, web service, online import, online update checks, cache/download workflows, HTTP TTS, video/audio/manga entry points.
- Wear OS APIs. The target device is the China-market OPPO Watch X, not Wear OS.
- Deep dependency and dead-code deletion. Network-heavy code may remain compiled in phase one if no user path can trigger it and the app has no network permission.
- Complex circular text layout. Round-screen support is handled with safe insets, large text, and simplified UI first.

## Existing Project Findings

Local TXT support already exists. `ImportBookActivity` lists local files, `ImportBookViewModel.addToBookshelf()` calls `LocalBook.importFiles()`, and `LocalBook.importFile()` creates local text books with `BookType.text or BookType.local`. TXT content and chapters are handled by `TextFile`.

File association already supports `.txt` through `FileAssociationActivity`, and the manifest currently accepts many formats. For the watch TXT build, this should be reduced to `.txt`/`text/plain`.

The current main app is not suitable for the watch target as-is. `MainActivity` sets up a `ViewPager` with bottom navigation for bookshelf, discovery, RSS, and my/config. `MainActivity.onPostCreate()` also performs privacy/version/help flows, WebDAV backup sync, rule subscription updates, automatic book TOC updates, and delayed post-load work. These paths need to be cut down for the offline watch app.

## Product Flow

Launch flow:
1. `WelcomeActivity` may remain as a short transition.
2. The app enters `MainActivity`.
3. `MainActivity` shows a single simplified bookshelf view instead of the current multi-tab phone UI.
4. On first display and on manual refresh, the app scans the public `Download` directory.
5. New `.txt` files are imported automatically.
6. Existing imported files are not duplicated.
7. The bookshelf displays local TXT books and reading progress.

Bookshelf behavior:
- The home screen is bookshelf-first, not continue-reading-first.
- Each row should fit a round watch: large tap target, book name, and progress. File time or size can be secondary if it fits without crowding.
- Primary action is opening a book.
- Secondary actions are refresh scan, settings, and remove book.
- If no TXT files are found, show an empty state with a refresh action and concise text telling the user to put TXT files in `Download`.

Import behavior:
- Default scan directory is public `Download`.
- Only `.txt` is accepted.
- Non-TXT files are ignored silently or with a low-noise summary, not shown as import candidates.
- If a source file is deleted after import, the first version does not automatically delete the bookshelf record. Opening the missing book should show a local error with an option to remove it.
- File association remains as a fallback: opening a `.txt` from another app should import/read it.

## Reader Experience

The reader reuses existing `ReadBookActivity`, `ReadBook`, `ReadBookConfig`, and `TextFile` behavior where possible. The first version should adapt defaults and available controls rather than replace the pagination engine.

OLED defaults:
- Reading background is true black `#000000`.
- Bookshelf/settings surfaces also default to true black.
- Body text uses soft near-white or light gray for contrast without harsh full-white glare.
- Existing theme background images, patterned backgrounds, and decorative reader backgrounds are disabled for this watch profile.

Round-screen defaults:
- Use safe inner padding so text and controls are not clipped by round corners.
- Use larger default text than phone defaults.
- Keep line spacing compact enough for the small display.
- Avoid dense toolbars and small icon-only clusters.

Reader controls:
- Tap left side for previous page.
- Tap right side for next page.
- Tap center for the minimal reader menu.
- If volume/physical key page turning works on the device, keep it.
- Reader menu only exposes watch-appropriate local actions: back to bookshelf, TOC, font/brightness/progress, and remove/delete where appropriate.
- No change source, online refresh, cache/download, web, image, audio, or HTTP read-aloud actions should be visible.

Swipe-back:
- Add left-edge swipe-back for watch navigation.
- Reading page: left-edge right swipe returns to the bookshelf.
- Bookshelf: left-edge right swipe exits or backs to the root state if a nested state exists.
- Settings/TOC: left-edge right swipe returns to the previous screen.
- The gesture must start from the left edge so ordinary page gestures and tap zones do not conflict with it.

## Offline Product Layer

The first version removes network capability at the product layer and from Android permissions.

Manifest and components:
- Remove `android.permission.INTERNET`.
- Remove `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `DOWNLOAD_WITHOUT_NOTIFICATION`, `REQUEST_INSTALL_PACKAGES`, and other permissions only needed by online/update/download/web-service features.
- Disable or remove manifest declarations for WebService, WebTileService, CheckSourceService, DownloadService, online import scheme handlers, and exported providers that expose online source APIs.
- Keep the local TXT file association activity, reduced to TXT/text MIME handling.

Startup behavior:
- Do not call version update checks.
- Do not call WebDAV backup sync.
- Do not call rule subscription updates.
- Do not automatically update online book TOCs.
- Do not run delayed online post-load work.

Visible UI:
- Remove search, discovery, RSS, remote book, add URL, cache/download, web service, online import/export, and online source management entry points from the watch UI.
- Keep only local TXT bookshelf, local reader, minimal settings, and local file-open/import affordances.

Implementation note:
- Phase one should not spend effort deleting every network-related Kotlin file or Gradle dependency. The practical success condition is that the APK has no network permission, no startup network work, and no reachable network UI path.
- A future cleanup phase may remove OkHttp/Cronet/NanoHTTPD/Firebase/WebDAV/RSS/source dependencies once the watch app behavior is stable.

## Architecture Approach

Use the existing XML/ViewBinding architecture and MVVM-style ViewModels. Do not introduce Compose for this phase.

Preferred implementation units:
- A watch-specific bookshelf screen or simplified variant of the existing bookshelf fragments.
- A local TXT scanner component that scans `Download`, filters `.txt`, and imports via `LocalBook.importFile()`/`LocalBook.importFiles()`.
- A watch profile/defaults initializer for offline mode, pure black reader config, and simplified feature flags.
- A reusable edge-swipe-back helper for Activities/Fragments that need it.
- Manifest/menu/layout resource edits that remove online user paths.

Boundaries:
- Keep TXT scanning/import logic out of Activities where possible.
- Keep UI state in ViewModel-style classes consistent with the existing project.
- Reuse Room DAOs and local book entities instead of inventing a new storage model.
- Avoid broad refactors unrelated to making the watch TXT app work.

## Testing And Verification

Automated tests should cover:
- `Download` scanning accepts `.txt`.
- Scanner ignores non-TXT files.
- Repeated scans do not duplicate imported books.
- Left-edge swipe-back detection is distinct from normal page interaction.

Build verification:
- `./gradlew :app:assembleDebug` passes.
- The merged manifest or built APK contains no `android.permission.INTERNET`.
- The merged manifest does not expose Web/RSS/online import/remote download services.

Manual verification:
- Put `a.txt` in public `Download`.
- Start the app.
- The simplified watch bookshelf appears and imports/displays `a.txt`.
- Opening the book shows a true black OLED reader.
- Page navigation works.
- Left-edge right swipe returns from reader to bookshelf.
- Opening a `.txt` through Android "open with" still imports/reads locally.
- With no network available, startup shows no network prompts and no online errors.

## Risks And Mitigations

Risk: Legado online features are deeply interwoven.
Mitigation: Do phase-one product-layer offline conversion first. Do not deep-delete dependencies until the watch reader is stable.

Risk: Android public `Download` access differs by OS version and device policy.
Mitigation: Prefer platform storage APIs already present in the project where possible. If direct file access is blocked, fall back to the existing document/file handling path while preserving the user-facing `Download` convention.

Risk: Round screen text clipping.
Mitigation: Use conservative reader safe padding and verify on a round/small emulator or device screenshot.

Risk: Swipe-back conflicts with page turns.
Mitigation: Trigger swipe-back only from the left edge with horizontal movement thresholds; keep normal tap zones intact.

Risk: The repository was initialized after project files already existed.
Mitigation: Commit only the design and ignore-file changes for this spec step, not the entire untracked project.
