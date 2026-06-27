# Watch TXT Reader Refactor Design

## Goal

Turn this repository into a single-purpose Android watch app for offline TXT reading.

The app will read `.txt` files under the public Download directory, show them in a watch-first bookshelf, and use the existing Legado reading engine for pagination, reading progress, chapter parsing, and reader configuration. The final product must not expose network reading, book sources, RSS, Web UI, WebDAV, media playback, online updates, or remote import features.

## Decisions

- Keep the current package name and Room database so the existing tested watch build can be upgraded in place.
- Reuse the existing reading engine instead of rewriting pagination and TXT parsing.
- Support only `/Download/**/*.txt` files.
- Treat the app as fully offline. No network permission and no retained network-facing product feature.
- On startup, clean non-target data and keep only Download TXT books and their reading state.
- Keep only watch-oriented UI plus an About screen for version, license, and privacy information.
- Use compile-driven hard pruning: delete obsolete entry points, dependencies, and source directories in stages, using compiler errors to identify the smallest replacement needed.

## Product Surface

### Kept

- Startup flow into the watch bookshelf.
- Storage permission request and Download TXT scan.
- Watch bookshelf list.
- Click to read a TXT book.
- Long-press to remove a book from the shelf.
- Automatic cleanup when a Download TXT file no longer exists.
- Reader page, chapter list, reader menu, and compact reader settings.
- Reading progress, bookmarks if already supported by the retained reader flow, and TXT chapter parsing.
- About screen with accurate offline-app privacy text.

### Removed Or Disabled

- Online book sources and source editor.
- RSS sources and RSS reader.
- Web service, WebSocket debug API, Web bookshelf, and Vue web module.
- WebDAV sync and backup.
- Firebase analytics/performance.
- Cronet and runtime native downloads.
- Online update checks and rule subscription updates.
- Remote import, URL import, network downloads, and remote cover fetching.
- Audio, video, manga, browser, and other phone-oriented reader surfaces.
- Main phone navigation and complex settings pages.

## Architecture

### App Startup

`App.onCreate()` should retain only initialization required for the watch TXT reader:

- Crash/log handling that does not require network.
- Theme/day-night initialization.
- Lifecycle callback registration.
- Shared preference configuration needed by the reader.
- Room database availability.
- Reader configuration cleanup that is strictly local.

It should remove or skip initialization for Cronet, GMS TLS provider, global URL stream handlers, Rhino, source sorting, WebDAV sync, online rule updates, network cache cleanup, and any feature not reachable in the offline watch app.

`WelcomeActivity` remains the launcher and applies `WatchReaderDefaults`, then opens `WatchBookshelfActivity`.

### Bookshelf

`WatchBookshelfActivity` owns the watch entry surface.

Flow:

1. Apply watch defaults.
2. Request storage permission.
3. Scan `Environment.DIRECTORY_DOWNLOADS` recursively for visible `.txt` files.
4. Clean database books that are not valid Download TXT books.
5. Import new TXT files through the retained local book pipeline.
6. Observe Room bookshelf data and display only retained Download TXT books.
7. Before opening a book, verify its file still exists; if missing, remove it and show a short message.

The scan must continue when one file fails to import, and must report enough feedback for a watch screen without adding a separate report UI.

### Reading

`ReadBookActivity`, `ReadBook`, `ReadBookViewModel`, `ReadBookConfig`, local TXT parsing, and pagination remain the core reading path.

The reader must be constrained to watch-safe behavior:

- No source switching.
- No online content fetching.
- No text selection menu unless needed by the watch UX.
- No phone-style first-run click-area onboarding.
- No read-aloud, audio, video, browser, search-source, or remote callbacks from normal watch use.
- Use watch menu, watch TOC, and compact settings.

### Data Model

Retain existing Room database and migrations to preserve upgrade compatibility.

Keep data needed for:

- `Book` records that map to valid Download TXT files.
- `BookChapter` records for retained books.
- Reading progress and related local reading state.
- Reader configuration.
- Minimal groups if required by existing DAO/query assumptions.

Remove or ignore data for:

- Network books.
- Local books outside Download.
- Non-TXT local books.
- Book sources.
- RSS sources and records.
- Replace rules.
- HTTP TTS.
- Rule subscriptions.
- WebDAV settings.
- Remote server settings.
- Cached network responses.

The cleanup should be conservative about what it keeps: a book is retained only if it resolves to a local filesystem path, the path is inside the canonical Download directory, the filename is visible and ends with `.txt`, and the file still exists. `content://` legacy imports are not retained.

## Deletion Strategy

Use staged hard pruning. Each stage should compile before moving to the next stage.

### Stage 1: Product Boundary

- Keep Manifest free of network permissions.
- Remove or disable phone and online entry points.
- Update README, app strings, About, and privacy policy to describe an offline watch TXT reader.
- Remove automatic network tasks from app startup and main view-model flows.

### Stage 2: Data Cleanup

- Add a dedicated Download TXT retention policy.
- Clean non-target books and obsolete rule/source data at startup or scan time.
- Add local unit tests for retain/delete decisions.

### Stage 3: Entry Point Pruning

- Delete unreachable Activity, Service, receiver, provider, and menu registrations for online/RSS/Web/media features.
- Remove layouts and resources that become unreferenced after entry point pruning.

### Stage 4: Dependency Pruning

- Remove Firebase, Cronet, NanoHTTPD, Media3, GSYVideoPlayer, Danmaku, network-only Glide integrations, and other dependencies once no retained code references them.
- Remove `modules:web` from product concerns.
- Remove `modules:rhino` only after all JS/rule execution references are gone.

### Stage 5: Source Pruning

- Delete source directories for web book parsing, RSS, source management, Web API, Web service, WebView browser, remote books, media playback, and online-only helpers.
- Replace accidental references with the smallest local-only code path.

### Stage 6: Final Verification

- Build debug APK.
- Run retained local unit tests.
- Perform watch manual checks: permission, empty Download, scan, open TXT, progress restore, delete missing file, long-press remove, TOC, reader settings, About.

## Error Handling

- Missing storage permission: remain on bookshelf and show a short permission message.
- Download directory missing or empty: show an empty bookshelf and a short message.
- TXT import failure: continue scanning remaining files and report failure count.
- Missing file when opening: remove the stale book and show a short message.
- Data cleanup failure: log locally and continue scan where possible.
- Reader failure: keep existing local reader error display and local log behavior.

## Testing

Retain and extend local unit tests only where they pay for the pruning risk.

Required tests:

- `WatchTxtFileFilterTest`: visible `.txt` detection, recursive list behavior, sorting.
- `WatchTxtImporterTest`: import success, per-file failure handling, missing Download TXT cleanup.
- `WatchReaderControlsTest`: watch text/line/brightness bounds, round display defaults, watch-safe click actions.
- New cleanup policy tests: keep valid Download TXT, reject non-TXT, reject outside Download, reject missing file, reject `content://`.

Verification commands by stage:

```powershell
.\gradlew.bat :app:testAppDebugUnitTest --tests "io.legado.app.model.localBook.*" --tests "io.legado.app.ui.watch.*"
.\gradlew.bat :app:compileAppDebugKotlin
.\gradlew.bat :app:assembleAppDebug
```

## Non-Goals

- No new reading engine.
- No new UI framework.
- No network import fallback.
- No support for EPUB, PDF, UMD, MOBI, archives, manga, audio, or video.
- No Web UI.
- No cloud sync or backup.
- No compatibility UI for old Legado online features.

## Success Criteria

- The installed app presents itself as an offline watch TXT reader.
- It requests no network permission.
- A clean install can read TXT files from Download.
- An upgrade install keeps valid Download TXT progress and removes non-target data.
- Online source/RSS/Web/media features are not reachable from UI, Manifest, or active startup tasks.
- Debug APK builds after pruning.
- Watch/localBook unit tests pass.
