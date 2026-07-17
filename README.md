# Glass Photos

A working Android photos app built with `io.github.kashif-mehmood-km:backdrop` (liquid glass /
iOS-style frosted UI) for Compose. Reads your **real** device photo library via `MediaStore` —
no placeholders, no mock data.

## What it does

- **Library tab** — full photo grid, grouped by day ("Today", "Yesterday", full dates), sourced
  live from `MediaStore.Images`.
- **Search tab** — glass search field + an albums grid derived from your device's actual folders
  (Camera, Screenshots, WhatsApp Images, etc.), filterable by name.
- **Tap a photo** → full-screen glass detail viewer: swipeable pager between photos, pinch-to-zoom,
  swipe-down-to-dismiss (with live glass-scale/fade tied to drag distance), and a bottom glass
  action bar (share / favorite / delete).
- **Share** uses a real `ACTION_SEND` intent chooser.
- **Delete** uses the real `MediaStore.createDeleteRequest` (API 30+) / `RecoverableSecurityException`
  flow, so it shows the actual Android system confirmation dialog and genuinely removes the file —
  it's not a fake local-only removal.
- **Permission gating** — a glass "Allow Access" screen on first launch, using the correct
  permission for the OS version (`READ_MEDIA_IMAGES` on 13+, `READ_EXTERNAL_STORAGE` below).

Every glass surface (top bar, bottom tab bar, detail action bar, search field, permission button)
uses the `backdrop` library's real refraction/blur/vibrancy pipeline — sticky/scrolling surfaces
intentionally use the leaner effect stack (`blur` + `colorControls`, no `vibrancy`/`lens`) per the
library's own perf guidance, since those backdrops re-record every scroll frame.

## Project layout

```
app/src/main/kotlin/com/glassphotos/app/
├── MainActivity.kt              # permission + MediaStore delete flow (IntentSenderRequest)
├── GlassPhotosApp.kt
├── data/
│   └── PhotoRepository.kt       # MediaStore query, day-grouping, album-grouping
├── glass/
│   ├── DragGestures.kt          # inspectDragGestures (no-slop pointer pipeline)
│   └── InteractiveHighlight.kt  # press/offset spring tracker used by GlassButton
└── ui/
    ├── GlassPhotosRoot.kt       # tab nav + detail-viewer overlay wiring
    ├── PermissionScreen.kt
    ├── LibraryScreen.kt         # day-sectioned grid
    ├── SearchScreen.kt          # search field + albums grid
    ├── PhotoDetailScreen.kt     # pager + zoom + swipe-to-dismiss + action bar
    └── glasscomponents/
        ├── GlassButton.kt
        ├── GlassTopBar.kt
        ├── GlassBottomBar.kt
        ├── GlassSearchField.kt
        └── GlassDetailActionBar.kt
```

## Build

Matches your ALiquidGlassDemo toolchain: Kotlin 2.4.0, AGP 8.13.2, Gradle 8.13, Compose BOM
2025.09.00, minSdk 26 / targetSdk 36.

**Locally (Termux won't compile this — needs the Android SDK):**
```bash
./gradlew :app:assembleDebug
```
APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

**Via GitHub Actions (recommended, matches your usual workflow):**
Push this to a repo — `.github/workflows/build.yml` is already wired to build the debug APK on
every push and upload it as a workflow artifact. No local compile needed.

## Known gaps / next steps

- No "For You" / memories tab yet — bottom bar currently ships Library + Search only, easy to
  extend with the same `GlassBottomBar` component.
- Album tap (`onAlbumClick`) is stubbed — wiring it to an album-filtered `LibraryScreen` is a
  ~10-line change (filter `allPhotos` by `bucketName` before grouping).
- No favorites persistence — `favoriteIds` lives in Compose state only, resets on process death.
  Trivial to back with a `SharedPreferences` or small Room table if you want it durable.
- Video/GIF items are intentionally excluded (`MediaStore.Images` only) — swap to a combined
  images+video query if you want the full camera roll.
