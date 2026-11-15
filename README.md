# Sequence 1.4 — native Android rebuild

A native Kotlin / Jetpack Compose rebuild of the original WebView-based **Sequence** watch-order app.
No WebView, no JavaScript bridge, no network permission. The catalogue and all generated artwork are
bundled, and progress is stored locally in `SharedPreferences` using the same `sequence-progress` JSON
shape as the web build.

The UI is Material 3 rather than a port of the web layout: wallpaper-based dynamic colour on
Android 12+, collapsing large top app bars, a real `NavigationBar`, animated tab changes, M3 bottom
sheets, segmented buttons, swipe-to-mark-seen with haptics, and edge-to-edge insets.

## Ready to install

- `Sequence-1.4.apk` — signed release build (`com.sequence.watchorder`, versionCode 150,
  versionName `1.4`).
- Minimum Android 8.0 (API 26), targets Android 15 (API 35).
- Rendered previews of every screen are in `screenshots/`.

## Versions

Each change ships under a new name so builds never get confused:

| Version | Name | What changed |
| --- | --- | --- |
| 1.3 | `Sequence-1.3.apk` | Original WebView build |
| 1.3-native | `Sequence-1.3-native.apk` | First native Kotlin/Compose port |
| 1.4 | `Sequence-1.4.apk` | Material 3 redesign: dynamic colour, collapsing bars, nav bar, motion, sheets |
| next | `Sequence-1.5.apk` | Any further change bumps the version again |

The `versionName` in `app/build.gradle.kts` is the single source of truth; the settings screen and the
APK name both follow it.

> The original app used the same application id with a different signing key. Uninstall the old
> WebView app first; Android will reject an in-place update signed by another key.

## What is included

- **Home** — genre chips, demo order comparison, overall screen/print progress, continue shelf,
  up-next rows with one-tap seen checks, keep-reading shelf, genre shelves, and a media-player style
  mini bar above the navigation.
- **Library** — saved and started franchises, grid/list views, progress filters, and sort by recent,
  A–Z, most left, or furthest along.
- **Upcoming** — future releases grouped by month, plus releases from the last 90 days with seen toggles.
- **Search** — franchises, films/series, books and comics, with genre tiles for browsing.
- **Franchise detail** — hero art, expandable blurb, release/story ordering, watch filters, book format
  filters, series/special markers, optional-entry markers, swipe-right to toggle seen, per-title sheets,
  "mark everything up to here", reading paths, cover checkmarks, and a next-up FAB.
- **Settings** — order mode, clear all progress (confirmed), and about/offline notices.
- **Undo snackbar** after progress and save changes, matching the original behaviour.
- All 20 poster and 135 cover artworks were rasterized from the original generator's SVG output to
  WebP and are served from `assets/art`.

## Native UI details

- **Colour** — `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+; a fixed Sequence
  blue Material 3 scheme below that. No hard-coded surface greys in screens.
- **Chrome** — `LargeTopAppBar` with `exitUntilCollapsed` scroll on Home, Library and Upcoming; a
  compact bar on detail and settings; `NavigationBar` with selected-item indicator and labels.
- **Motion** — `AnimatedContent` slides between tabs and cross-fades into detail/settings; collapsing
  bars animate; chips and segmented buttons use M3 state animations.
- **Components** — cards for artwork, `FilterChip` rows, `ListItem` rows, `SingleChoiceSegmentedButtonRow`
  for watch/books and release/story, `SwipeToDismissBox` to mark an episode seen, `ExtendedFloatingActionButton`
  for "Next up", `ModalBottomSheet` for every order/filter/detail sheet.
- **Insets** — edge-to-edge; every screen consumes the `Scaffold` content padding so nothing hides
  under the status bar, the navigation bar or the mini player.
- **Haptics** — a haptic tick on every seen/read toggle.

## Source layout

```
app/src/main/java/com/sequence/watchorder/
  MainActivity.kt          edge-to-edge Compose entry point
  App.kt                   app shell, top bars, bottom nav, mini bar, snackbar, back handling
  SheetHost.kt             order/filter/more/sort/confirm and entry/work bottom sheets
  MainViewModel.kt         UI state, navigation, progress mutations and undo
  data/
    Models.kt              catalogue and progress models
    Catalog.kt             assets/sequence.json parser
    Logic.kt               ordering, progress, feeds, library and search logic
    ProgressStore.kt       SharedPreferences persistence (sequence-progress JSON)
  ui/
    Theme.kt               Material 3 colour schemes (dynamic + fallback) and accent palettes
    Components.kt          cards, chips, rows, sheet controls, check circles
    Icons.kt               original Material SVG paths as Compose ImageVectors
    Art.kt                 bundled WebP loader with an in-memory LruCache
    HomeScreen.kt
    LibraryScreen.kt
    UpcomingScreen.kt
    SearchScreen.kt
    DetailScreen.kt
    SettingsScreen.kt
app/src/test/java/com/sequence/watchorder/
    ScreenshotTest.kt      Paparazzi renders of every screen against the real catalogue
```

Static data lives in `app/src/main/assets/sequence.json`; artwork lives in
`app/src/main/assets/art/posters` and `app/src/main/assets/art/covers`.

## Screen previews without a device

Paparazzi renders the real composables and the real catalogue on the JVM. Recorded renders go to
`screenshots/` and to `app/build/reports/paparazzi/debug/images`:

```powershell
$env:JAVA_HOME = "C:\Projects\Orderly\.toolchain\jdk17"
.\gradlew.bat :app:testDebugUnitTest --tests "com.sequence.watchorder.ScreenshotTest" "-Ppaparazzi.test.record=true"
```

Quote `"-Ppaparazzi.test.record=true"` in PowerShell, otherwise `-P` is split into a task name.
The test builds `MainViewModel(catalog, progress)` through an internal state-only constructor, so no
device, emulator or `SharedPreferences` is involved; artwork is decoded straight from `assets/art`.

## Build

The project now uses the Android Studio SDK at:

```
C:\Users\MJT\AppData\Roaming\Local\Android\Sdk
```

It contains platform 35, build-tools 35.0.0, platform-tools and `cmdline-tools/latest`; the path is
recorded in `local.properties`. `.toolchain` still provides JDK 17 and Gradle 8.11.1 (Android Studio
can use its bundled JBR instead). Maven access uses the Aliyun Google/Central mirrors configured in
`settings.gradle.kts` because `dl.google.com` was unavailable in the build environment.

Everything needed is already on disk, so the build works with no network at all:

```powershell
$env:JAVA_HOME = "C:\Projects\Orderly\.toolchain\jdk17"
.\gradlew.bat --offline clean :app:assembleRelease
```

```powershell
$env:JAVA_HOME = "C:\Projects\Orderly\.toolchain\jdk17"

# Prefer the checked-in wrapper when it can download Gradle 8.11.1:
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease

# Or use the bundled Gradle without any download:
& "C:\Projects\Orderly\.toolchain\gradle-8.11.1\bin\gradle.bat" :app:assembleDebug
& "C:\Projects\Orderly\.toolchain\gradle-8.11.1\bin\gradle.bat" :app:assembleRelease
```

Outputs:

- `app/build/outputs/apk/debug/app-debug.apk` (`com.sequence.watchorder.debug`, `1.4-debug`)
- `app/build/outputs/apk/release/app-release.apk` (signed with `sequence-release.jks`)
- `Sequence-1.4.apk` — the release build copied to the project root for installing

### Android Studio

Open `C:\Projects\Orderly` directly. Settings → Build, Execution, Deployment → Build Tools → Gradle:

- **Gradle JDK:** point at `C:\Projects\Orderly\.toolchain\jdk17` (Android Studio's own JBR is JDK 25,
  which AGP 8.7.3 does not support). `gradle.properties` already pins `org.gradle.java.home` to the
  JDK 17 in `.toolchain`, so this only matters if you edit that line.
- **Gradle wrapper:** the checked-in wrapper is Gradle 8.11.1 and its distribution is pre-downloaded
  into `%USERPROFILE%\.gradle\wrapper\dists`, so the first sync needs no Gradle download.
- **SDK:** `local.properties` points at `C:\Users\MJT\AppData\Roaming\Local\Android\Sdk`, which holds
  platform 35, build-tools 35.0.0, platform-tools and `cmdline-tools/latest`. No emulator or system
  image is installed there.

## Signing

Nothing secret is committed. `app/build.gradle.kts` reads the signing config from `local.properties`
(git-ignored) or from environment variables, and skips signing when neither is present:

```properties
sequence.storeFile=sequence-release.jks
sequence.storePassword=…
sequence.keyAlias=…
sequence.keyPassword=…
```

The equivalent CI variables are `SEQUENCE_STORE_FILE`, `SEQUENCE_STORE_PASSWORD`,
`SEQUENCE_KEY_ALIAS` and `SEQUENCE_KEY_PASSWORD`. Use your own keystore for any public build; the
local `sequence-release.jks` is not in the repository.

## Data and behaviour notes

- `sequence.json` contains 20 franchises, 309 entries and 135 books/comics, with data checked
  through 2026-11-09 (the original bundle's date).
- Upcoming titles do not count toward progress and cannot be marked seen.
- Release order sorts by premiere date; story order uses each franchise's era order and story index.
- Library membership = saved, or at least one entry seen, or at least one book read.
- Clearing all progress also clears the saved and recent lists; order mode and library sort/view are
  retained, matching the original confirm sheet.
- Everything is local: there is no `INTERNET` permission in the manifest.
