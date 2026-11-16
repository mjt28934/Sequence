# Sequence 2.4 — 49 franchises, films and games

A native Kotlin / Jetpack Compose rebuild of the original WebView-based **Sequence** watch-order app.
No WebView, no JavaScript bridge, no network permission. The catalogue and all artwork are
bundled, and progress is stored locally in `SharedPreferences` using the same `sequence-progress` JSON
shape as the web build.

The UI is Material 3 rather than a port of the web layout: wallpaper-based dynamic colour on
Android 12+, collapsing large top app bars, a real `NavigationBar`, animated tab changes, M3 bottom
sheets, segmented buttons, and edge-to-edge insets.

## Ready to install

- `Sequence-2.0.apk` — signed release build (`com.sequence.watchorder`, versionCode 220,
  versionName `2.0`), the latest published release.
- `Sequence-2.4.apk` — local build of the current source (versionCode 260, versionName `2.4`); it is
  not published yet.
- `Sequence-2.3.apk` — local build of the previous mini-bar removal (versionCode 250,
  versionName `2.3`); it is not published either.
- `Sequence-2.2.apk` — local build of the earlier Home and swipe pass (versionCode 240,
  versionName `2.2`); it is not published either.
- Minimum Android 8.0 (API 26), targets Android 15 (API 35).
- Rendered previews of every screen are in `screenshots/`.

## Versions

Each change ships under a new name so builds never get confused:

| Version | Name | What changed |
| --- | --- | --- |
| 1.3 | `Sequence-1.3.apk` | Original WebView build |
| 1.3-native | `Sequence-1.3-native.apk` | First native Kotlin/Compose port |
| 1.4 | `Sequence-1.4.apk` | Material 3 redesign: dynamic colour, collapsing bars, nav bar, motion, sheets |
| 1.5 | `Sequence-1.5.apk` | Real film posters replace the generated franchise art |
| 1.6 | `Sequence-1.6.apk` | 17 more franchises (37 total), 116 more titles, 22 more books, real covers, and a motion pass over every screen |
| 2.0 | `Sequence-2.0.apk` | Tags tab, inline Home search, appearance and layout settings, shared-axis transitions, a bottom bar that never re-creates, sticky upcoming months, and a reduced-motion-aware polish pass |
| 2.1 | `Sequence-2.1.apk` | 12 more franchises (49 total): Scream, Final Destination, Saw, Rambo, The Godfather, Underworld, and six game series. Adds a Game kind, game filtering and played/playing copy. Unreleased. |
| 2.2 | `Sequence-2.2.apk` | Cleaner Home: the continue hero and up-next rows are gone, so the totals, continue shelf, keep-reading shelf and franchise grid carry the page. Rows are marked seen by swiping right and unmarked by swiping left, replacing the seen circles. Unreleased. |
| 2.3 | `Sequence-2.3.apk` | Removes the mini continue strip above the navigation bar, leaving the tab bar as the only pinned chrome. Unreleased. |
| 2.4 | `Sequence-2.4.apk` | Removes the swipe-to-mark-seen gesture from detail and Upcoming. Rows are plain tappable rows again; marks stay available in the entry sheets and "mark everything up to here". Unreleased. |

Versions 1.7 through 1.10 were internal builds; their changes are part of 2.0.

The `versionName` in `app/build.gradle.kts` is the single source of truth; the settings screen and the
APK name both follow it.

> The original app used the same application id with a different signing key. Uninstall the old
> WebView app first; Android will reject an in-place update signed by another key.

## What is included

- **Home** — overall seen/read progress, a continue shelf, a keep-reading row, the full franchise grid
  with a hide-finished empty state, and a media-player style navigation bar. A finished franchise
  carries a tick badge on its poster, and a new install skips the explainer and goes straight to the
  catalogue. Game entries say "played" instead of "watched".
- **Library** — saved and started franchises, grid/list views, progress filters, and sort by recent,
  A–Z, most left, or furthest along. Rows animate into place as the filter changes, and the bar links
  straight to settings.
- **Upcoming** — future releases grouped under sticky month headers, plus releases from the last 90
  days; every row opens its franchise and title sheet.
- **Tags** — genre shelves with cover collages, and a genre view of franchise posters that animates
  as the filter changes.
- **Search** — franchises, films, series, games, books and comics, with a match count on every result
  section; it opens inline on Home and closing it keeps your place.
- **Franchise detail** — hero art with a scroll parallax, expandable blurb, release/story ordering,
  watch filters that include a games option for mixed franchises, book format filters, series/game/
  special markers, optional-entry markers, per-title sheets, "mark everything up to here", reading
  paths, cover checkmarks, and a next-up FAB.
- **Settings** — start tab, hide finished franchises, theme and vibration, reduce motion, library
  layout and sort, watching order, export/import, clear all progress (confirmed), and reset settings.
- **Undo snackbar** after progress and save changes, matching the original behaviour.
- **Real artwork** for all 49 franchises and 156 of 157 books and comics, served from `assets/art`.

## Artwork

Each franchise banner is 960x600 and shows the actual theatrical poster or game box art: the artwork
sits on the right at full sharpness over a blurred, darkened copy of itself, which keeps the 16:10 crop
the layouts use and leaves the left side clear for the title text laid over it. Bond is the exception:
its landscape UK quad posters are used full-bleed, so the set reads as one run of films.

Posters and box art come from the lead image of each film's or game's English Wikipedia article and are
re-composed locally by `buildtools/fetch-posters.mjs` (Sharp), then written straight into
`app/src/main/assets/art/posters`. Source images are kept out of git; only the finished banners ship.
`buildtools/check-posters.mjs` re-reads every banner and fails if one is missing, too small or flat.
See `POSTERS.md` for the exact page behind each one.

Book covers follow the same rule: `buildtools/fetch-covers.mjs` takes the lead image of the book's
English Wikipedia article and crops it to a 2:3 cover. 1.6 adds 22 covers for the new franchises;
`bk-bd-12` has no usable cover (its only scan is a landscape wraparound jacket) and falls back to the
placeholder tile.

## UI and motion

- **One motion switch** — `ui/Motion.kt` holds `Motion.enabled`, which follows the Reduce motion
  setting in Settings. Every timed animation checks it; snapshot tests turn it off so renders are
  settled frames rather than half-faded ones.
- **One set of timings** — `SeqMotion` in the same file holds the press, short, standard, navigation
  and progress durations plus the two springs, so a press, a badge and a tab change feel related.
- **Press feedback** — cards, poster tiles, cover tiles, genre tiles, library rows, watch rows, search
  results and the continue hero shrink slightly while pressed, driven by the same `InteractionSource`
  as the ripple.
- **Entrance** — Home sections fade and lift in with a short stagger the first time they are shown;
  a saveable `EnterTracker` means returning to the tab or scrolling back up never replays them.
- **Art** — a poster or cover decoded on demand fades in with a 3% overscale that settles, so cards do
  not pop into place mid-scroll. Already-cached art appears instantly.
- **Progress and badges** — progress strips animate to their new value, seen/read controls ease their
  fill and colour, and check badges spring in when a mark lands.
- **Lists** — lazy lists animate item placement and fades as filters and marks change; under Reduce
  motion the rows simply settle in place.
- **Detail hero** — the artwork trails the page as it scrolls and grows just enough to avoid exposing
  an edge; the blurb resizes smoothly when it expands.
- **Tabs and destinations** — `AnimatedContent` slides tabs with the same 48dp / 250ms move in both
  directions and cross-fades into detail and settings; the bar itself never re-creates.

## Native UI details

- **Colour** — `dynamicDarkColorScheme` / `dynamicLightColorScheme` on Android 12+; a fixed Sequence
  blue Material 3 scheme below that. No hard-coded surface greys in screens.
- **Chrome** — `LargeTopAppBar` with `exitUntilCollapsed` scroll on Home, Library and Upcoming; a
  compact bar on detail and settings; `NavigationBar` with selected-item indicator and labels.
- **Components** — cards for artwork, `FilterChip` rows, `ListItem` rows, `SingleChoiceSegmentedButtonRow`
  for watch/books and release/story, `ExtendedFloatingActionButton` for "Next up", `ModalBottomSheet`
  for every order/filter/detail sheet.
- **Insets** — edge-to-edge; every screen consumes the `Scaffold` content padding so nothing hides
  under the status bar or the navigation bar.
- **Haptics** — a haptic tick on read toggles.

## Source layout

```
app/src/main/java/com/sequence/watchorder/
  MainActivity.kt          edge-to-edge Compose entry point
  App.kt                   app shell, top bars, bottom nav, snackbar, back handling
  SheetHost.kt             order/filter/more/sort/confirm and entry/work bottom sheets
  MainViewModel.kt         UI state, navigation, progress mutations and undo
  data/
    Models.kt              catalogue and progress models
    Catalog.kt             assets/sequence.json parser
    Logic.kt               ordering, progress, feeds, library and search logic
    ProgressStore.kt       SharedPreferences persistence (sequence-progress JSON)
  ui/
    Theme.kt               Material 3 colour schemes (dynamic + fallback) and accent palettes
    Motion.kt              shared motion: Motion.enabled, SeqMotion timings, press scale, section moves
    Components.kt          cards, chips, rows, sheet controls
    Icons.kt               original Material SVG paths as Compose ImageVectors
    Art.kt                 bundled WebP loader with a heap-sized LruCache and fade-in
    Feedback.kt            haptic ticks for read toggles
    HomeScreen.kt
    LibraryScreen.kt
    TagsScreen.kt
    UpcomingScreen.kt
    SearchResults.kt       inline Home search field and result rows
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

Two small checkers guard against renders that look fine at a glance but are actually blank:

```powershell
node buildtools/check-shots.mjs screenshots/*.png   # mean luma, spread and mid-tone share per render
node buildtools/diff-shots.mjs old.png new.png      # tile-by-tile detail, old vs new
```

`check-shots.mjs` flags a render with almost no mid-tones, which is what a screen of missing posters
or covers produces. `ScreenshotTest` pins the art cache to 320 MB so no asset is evicted between
snapshots.

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

- `app/build/outputs/apk/debug/app-debug.apk` (`com.sequence.watchorder.debug`, `2.4-debug`)
- `app/build/outputs/apk/release/app-release.apk` (signed with `sequence-release.jks`)
- `Sequence-2.4.apk` — the current release build copied to the project root (not published)

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

- `sequence.json` contains 49 franchises, 495 entries and 157 books/comics, with data checked
  through 2026-11-16. Release dates come from Wikidata (`P577`, earliest day-precision value), with
  six titles given hand-checked dates where Wikidata only had a premiere or a stale scheduling claim.
- Game entries use `kind: "game"`. They count and filter like other entries, are labelled "Game" in
  rows and sheets, and game-only franchises say "played" instead of "watched".
- Upcoming titles do not count toward progress and cannot be marked seen.
- Release order sorts by premiere date; story order uses each franchise's era order and story index.
- Library membership = saved, or at least one entry seen, or at least one book read.
- Clearing all progress also clears the saved and recent lists; order mode and library sort/view are
  retained, matching the original confirm sheet.
- Everything is local: there is no `INTERNET` permission in the manifest.
