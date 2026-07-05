# Changelog

## [2.1](https://github.com/DaKlok/BibleLockScreen/releases/tag/2.1) — Release 2.1 (2026-07-05)

# Changelog

## 🆕 Developer Logs

- Added a new **`AppLogger`** object — a lightweight, file-backed logger that persists structured log lines (`DEBUG` / `INFO` / `WARN` / `ERROR`) to app storage.
- Added an in-app **Developer Logs** viewer (`DeveloperLogSheet`) with:
  - Filter tabs: `All`, `ERROR`, `WARN`, `INFO`, `DEBUG`
  - Copy-all and clear-all actions
  - Color-coded rows per log level
- Instrumented key components with logging so real device behavior is traceable without `adb logcat`:
  - `ScreenOffReceiver` — logs receipt of `ACTION_SCREEN_OFF`, wallpaper cycling decisions, cache hits/misses, and success/failure of applying the wallpaper.
  - `DailyVerseWorker` — logs `doWork()` start, success/failure, and (later) rescheduling.
- Added **crash / kill detection**: on each app start, `AppLogger` compares the previous session's state and reports either:
  - A previous session that ended in an uncaught crash, or
  - The process having been killed by the system while backgrounded (with no clean shutdown).
- **Fix:** the "process was killed" log entry used to report the timestamp of *reopening* the app, not the time the process was actually last alive. It now uses the last recorded heartbeat (`onStop()`) as the entry's timestamp instead of "now".
- **Fix:** the "process was killed" message was always shown as a `WARN`, implying it broke scheduled wallpaper updates — which is no longer true for interval-based scheduling (see AlarmManager section below). It's now:
  - `INFO` (reassuring, "this is normal") when only interval-based scheduling is active, since that survives process death.
  - `WARN` only when "change wallpaper on screen off" is enabled, since that feature genuinely depends on the app process staying alive.
- Added a distinct **Material 3 Expressive green** highlight for the specific log lines that represent an actual wallpaper change (`Success: Wallpaper applied`, `✓ Wallpaper set successfully`), so the single most important event in the log visually stands out from routine lifecycle/scheduling noise.

## ⏰ Exact-time wallpaper scheduling (major rework)

**Problem:** scheduled wallpaper changes were drifting later and later over time (e.g. showing up at 20:43 instead of 20:00), because the app relied on `WorkManager`'s `PeriodicWorkRequest` / delayed `OneTimeWorkRequest`, which only guarantees "no earlier than" — Android's Doze mode and battery/App-Standby throttling are free to push real execution back, and that slack never shrinks back down.

- Replaced `PeriodicWorkRequest`-based scheduling with a **self-rescheduling chain of exact `AlarmManager` alarms** (`AlarmManager.setExactAndAllowWhileIdle`), the one Android API explicitly exempted from Doze deferral.
- Added **`WallpaperAlarmReceiver`** — a small, manifest-registered `BroadcastReceiver` that receives the exact alarm and hands the actual rendering work off to `WorkManager` (`DailyVerseWorker`), combining exact timing with WorkManager's more robust execution model.
- Added **`BootReceiver`** — exact alarms don't survive a device reboot (unlike WorkManager jobs), so this re-arms them on `BOOT_COMPLETED`.
- `DailyVerseWorker` now re-arms its *own* next alarm at the end of every run (success or failure) via `rescheduleNext()`, recomputing the delay fresh each time instead of relying on a fixed periodic interval.
- Added a **graceful fallback**: if the exact-alarm permission isn't granted (or gets revoked), the app falls back to a delayed `WorkManager` job with the same delay and logs a warning, instead of silently doing nothing.
- Manifest changes: added `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED` permissions, and registered both new receivers.
- Updated the in-app diagnostic check (previously inspecting `WorkManager`'s job state) to instead check whether an exact alarm is currently armed for each schedule ("DailyBibleWallpaper", "WallpaperCycling").

## 🕒 Local-time alignment fixes

**Problem:** interval-based scheduling ("every N hours") was computed from **UTC epoch hours** instead of the device's local time. On a UTC+2 device, "every 3 hours" landed on 17:00 / 20:00 instead of the expected 15:00 / 18:00 / 21:00 / 00:00 (offset drift by the device's UTC offset).

- Rewrote `computeSlotInitialDelayMs()` to align to **local** wall-clock hour boundaries (via `Calendar`) instead of UTC epoch-hour arithmetic. Fixes the 2h/3h/6h interval offset issue.
- **Fix:** the 12-hour interval option silently ignored the user-selected anchor hour and fell through to the same (buggy, UTC-based) slot logic used for 2h/3h/6h. It now correctly honors the chosen hour, same as the 24h option, via `computeDailyCycleInitialDelayMs(hour, 12)`.
- **Fix (critical):** `computeDailyCycleInitialDelayMs()` only added the interval **once** when computing the next occurrence of the anchor hour. With a 12h interval and anchor hour 7 (07:00/19:00), if the current time was already past 19:00 (e.g. 19:48), the single addition still landed in the past — producing a **negative delay**. A negative/past trigger time made `AlarmManager` fire immediately, and since the worker rescheduled itself using the same buggy function, this became a **tight refire loop** (wallpaper changing almost every second). Fixed by looping the addition until the target time is strictly in the future.
- Added a **defense-in-depth safety net** in `scheduleExactWallpaperAlarm()`: any trigger time that isn't at least 60 seconds in the future is now clamped and logged as an `ERROR`, so a similar bug can never again cause a silent refire loop — it will visibly show up in Developer Logs instead.
- `LocalBibleProvider.getVerseForInterval()`: the sub-24h verse "time slot" index was also based on raw UTC epoch hours, which could disagree with the (now local-time-based) alarm schedule — e.g. changing the anchor hour from 22:00 to 23:00 could still land in the same UTC slot and show the same verse. Switched the slot calculation to use local wall-clock hour + a local day counter, consistent with the scheduling fix above.
- Note: image cycling (`WallpaperManager.cycleToNext()`) was **not affected** by any of the above — it's a simple round-robin counter advanced once per triggered run, with no time-of-day math involved. It only inherits correctness from the alarm-scheduling fix (i.e. it now advances at the correct local time), but never had a UTC/local slot bug of its own.

## 🎨 Material 3 color/theming fixes

**Problem:** with Material You (dynamic color) enabled, several nested backgrounds that were supposed to look different rendered as the *same* color — most noticeably the settings-area background and the area behind the Pixel 6 lock-screen preview. The same issue showed up in the plain light theme too, even with Material You off.

- **Root cause:** these surfaces used the classic `background` / `surface` / `surfaceVariant` roles, which are often very close in tone under dynamic color, and in the static (non-dynamic) light scheme `background` and `surface` default to the *exact same* tone unless explicitly overridden.
- Migrated the nested UI containers to Material 3's newer **surface container ladder**, which is specifically designed to keep each nesting level a deliberately distinct tone:
  - Screen background (area behind the Pixel 6 preview) → stays `background`
  - Settings-area wrapper → `surface` → **`surfaceContainerLow`**
  - Individual settings cards (`SettingsCard`, incl. verse settings) → `surfaceVariant` → **`surfaceContainerHigh`**
  - Pixel 6 preview `Card` → previously no explicit color (defaulted to `surface`) → now explicitly **`surfaceContainerHighest`**
- **Follow-up fix:** after the above change, the *static* dark theme (Material You off) looked different from before — because the newly-used `surfaceContainer*` roles weren't explicitly set in the custom `DarkColorScheme`, so they silently fell back to Material 3's baseline neutral defaults instead of this app's custom dark palette (`BackgroundDark` / `SurfaceDark` / `SurfaceVariantDark`). Fixed by explicitly pinning `surfaceContainerLowest/Low/(default)/High/Highest` in `Theme.kt`, built from the same custom dark palette so the static dark theme's original look is preserved while the light/dynamic-color contrast fix still applies.


## 📄 Manifest

- Added `SCHEDULE_EXACT_ALARM` permission (for exact wallpaper-change alarms).
- Added `RECEIVE_BOOT_COMPLETED` permission (to re-arm alarms after reboot).
- Registered `WallpaperAlarmReceiver` (not exported — only triggered by the app's own alarms).
- Registered `BootReceiver` (exported, filtered to `BOOT_COMPLETED`).

## [2.0](https://github.com/DaKlok/BibleLockScreen/releases/tag/2.0) — Release 2.0 (2026-07-01)

Implement a wallpaper gallery management system, auto-cycling functionality, and a paginated UI for the home screen.

- **Wallpaper Management Engine**:
    - Introduced `WallpaperManager` to manage multiple user wallpapers stored in a dedicated internal directory.
    - Implemented `WallpaperSettings` to handle auto-cycling configurations, including time-based intervals, "on-lock" triggers.
    - Added a migration bridge to automatically import existing legacy wallpapers into the new gallery system on first launch.
- **Gallery & Configuration UI**:
    - Created `WallpaperScreen` providing a thumbnail-based gallery with support for adding, deleting, and activating wallpapers.
    - Integrated Material 3 components for auto-cycling settings, including sliders for time intervals and daily change hours.
    - Implemented a `HorizontalPager` in `MainActivity` to allow seamless swiping between the lock-screen preview and the wallpaper management gallery.
    - Added a dynamic page indicator and a one-time "swipe hint" to improve feature discoverability.
- **Backup & Restore Enhancements**:
    - Updated `SettingsBackupManager` to bundle the entire wallpaper gallery directory into the backup ZIP.
    - Refactored preference restoration to clear existing state before importing, ensuring a clean replacement of settings.
    - Improved preference serialization to explicitly handle value types (Int, Long, Float), preventing data loss during GSON restoration.
 - **Wallpaper Caching & Performance**:
    - Introduced `WallpaperCacheManager` to pre-render the next wallpaper (image + verse text) as a PNG, reducing the wallpaper swap time during screen-off events
    - Refactored `ScreenOffReceiver` to perform asynchronous wallpaper updates, prioritizing the file-based cache with a fallback to full rendering.
    - Integrated pre-rendering triggers in `DailyVerseWorker` to ensure the cache is refreshed after every successful wallpaper update.
- **Wallpaper Rendering**:
    - Updated `WallpaperUtils` to calculate screen dimensions using the minimum and maximum values of the display metrics for width and height, respectively.
    - This ensures that wallpapers are always generated in portrait mode even if the device is currently in landscape orientation, preventing the system from stretching or cropping the bitmap on the lock screen.
- **UI & Localization**:
    - Added extensive localized strings for all new wallpaper management and cycling features across nine languages.
    - Updated the main preview to support parallax scaling and alpha transitions during vertical scrolling.

## [1.9](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.9) — 1.9 (2026-06-20)

Implement full-app backup and restore functionality, overhaul the custom verse database UI, and enhance language selection logic.
- **Backup & Restore**:
    - Introduced `SettingsBackupManager` to bundle shared preferences, user wallpapers, and custom verse databases into a single ZIP file.
    - Integrated export and import actions in the Settings UI with progress indicators and safety confirmation dialogs.
    - Implemented automatic Activity recreation upon successful restoration to refresh application state.
- **Custom Database Improvements**:
    - Redesigned `VerseDatabaseScreen` with a modern Material 3 layout, including a sticky bottom save bar and improved card styling.
    - Added real-time conflict detection for language codes with prominent warning cards for existing built-in or custom databases.
    - Replaced the modal import dialog with an inline panel featuring live validation and an overwrite confirmation flow.
- **Enhanced Language Selection**:
    - Introduced `VerseLanguagePicker` using a segmented toggle to clearly separate "Default" (built-in) and "Custom" sources.
    - Replaced standard dropdowns with a rich `LanguagePickerDialog` featuring code badges, subtitles, and expressive iconography.
    - Updated `LocalBibleProvider` and `DailyVerseWorker` to track and respect the verse source, resolving issues where custom codes overlapped with built-in assets.
- **UI & Localization**:
    - Added extensive localized strings for all new backup, restore, and database management features across nine languages.
    - Improved empty-state illustrations and call-to-action buttons within the database management sheets.

## [1.8](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.8) — Release 1.8 (2026-06-15)

Changelog:
Implement custom verse database management, allowing users to create, import, and export verse collections.

- **Verse Management Engine**:
    - Introduced `VerseJsonManager` to handle JSON-based storage of custom verse databases in the internal file directory.
    - Implemented functionality to list custom databases, save manual entries, and import/export collections via the system file picker and Downloads folder.
- **Database Management UI**:
    - Added `VerseDatabaseScreen` providing a comprehensive interface for managing collections.
    - Created a "Create/Edit" workflow for manual entry of verse text and references with real-time validation and haptic feedback.
    - Integrated an "Import/Export" screen supporting JSON file picking and status indicators for background operations.
- **Integration & Data Logic**:
    - Updated `LocalBibleProvider` to prioritize custom user databases over bundled assets when loading verses.
    - Enhanced the "Verse Language" setting in `MainActivity` to dynamically include custom databases (marked with a ★) alongside built-in languages.
    - Added a new "Verse databases" entry point in the main settings UI.
 - **Localization**:
    - Replaced hardcoded strings in `VerseDatabaseScreen.kt` with localized properties from the `AppStrings` system.
    - Added comprehensive translations for all verse database management features (titles, labels, buttons, and notifications) across all supported languages: Slovak, English, Czech, Spanish, Italian, French, German, Hungarian, and Polish.

## [1.7](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.7) — Release 1.7 (2026-04-19)

Changelog:
- **Wallpaper Configuration**:
    - Added a new `wallpaper_target` setting allowing users to apply wallpapers to the Lock screen, Home screen, or both.
    - Updated `DailyVerseWorker` to respect the selected wallpaper target using `WallpaperManager` flags.
- **UI & Localization**:
    - Introduced localization strings for "Wallpaper target" labels across all supported languages (Slovak, English, Czech, Spanish, Italian, French, German, Hungarian, and Polish).
    - Removed unused `com.github.skydoves:colorpicker-compose` dependency in favor of the internal implementation.

## [1.6](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.6) — Release 1.6 (2026-04-17)

Changelog:
- **New Color Picker**: 
    - Replaced the external `skydoves:colorpicker-compose` dependency with a custom-built `ColorPickerDialog` and internal HSV wheel implementation.
    - Added support for HEX and RGB input fields for precise color selection.
    - Integrated a brightness slider and a real-time text preview within the picker.
    - Included a row of common color presets (White, Black, Cream, Golden, etc.) for quick access.
- **UI & UX Improvements**:
    - Added haptic feedback when selecting colors.
    - Improved the color selection logic to trigger an immediate background worker update if a custom image is set.
- **Technical Changes**:
    - Implemented internal HSV-to-RGB conversion and drawing logic using Compose `Canvas`.
    - Added comprehensive color utility functions for HEX parsing and HSV transformations.
    - Standardized preset colors across the application for design consistency.

## [1.5](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.5) — Release 1.5 (2026-04-11)

Changelog:

**--- USER INTERFACE & EXPERIENCE ---**
- **Modernized Design Language**: Overhauled `MainActivity` with rounded cards, pill-shaped drag handles, and improved spacing for a cleaner aesthetic.
- **Polished Top Bar**: Reimagined the top navigation with a new icon and a sleek "Settings" button style.
- **Fluid Animations**: 
    - Added **spring-based transitions** for settings and menu expansions.
    - Introduced a **"swipe hint"** animation on the main handle to improve feature discoverability.
    - Integrated a loading spinner directly into the **"Generate"** button for real-time feedback.
- **Enhanced Color Picker**: Redesigned the color picker row with animated selection rings and a fresh custom color button.
- **Editor Refinement**: Updated the `Full-Screen Editor` with a gradient-shaded bottom bar and high-contrast interactive drag handles.

**--- ENHANCED CUSTOMIZATION ---**
- **Unified Controls**: Grouped blur, size, width, position, and transparency sliders into a single **"Text Customization"** card to reduce clutter.
- **Precision Layout**: Introduced a **Vertical Offset slider** for text positioning and localized the label across all supported languages.
- **Visual Hierarchy**: Added icons to section headers and redesigned the photo selection placeholder with a prominent bordered card style.
- **Dynamic Backgrounds**: Added a **"Background Darkness"** slider with haptic feedback, allowing you to control vignette intensity via direct alpha mapping in `WallpaperUtils`.

**--- AUTOMATION & SCHEDULING ---**
- **New Screen-Off Mode**: Introduced `ScreenOffReceiver` to detect device locking and trigger a silent verse update via an expedited `WorkManager` request.
- **Sequential Rotation**: Added a persistent counter to ensure a fresh verse is shown every time the screen cycles.
- **Smart Scheduling**: Refactored `LocalBibleProvider` to support time-interval-based selection (1h, 2h, 3h, 6h, 12h, 24h).
- **Slot Synchronization**: Implemented UTC epoch-based synchronization to ensure consistent verse rotation across devices.

**--- LOCALIZATION & THEME ---**
- **Global Support**: Added comprehensive localized strings for new features across **9 languages** (SK, EN, CZ, ES, IT, FR, DE, HU, PL).
- **Theme Consistency**: Updated `use_dynamic_color` to default to **false** for better initial consistency; updated `ColorPickerRow` to use localized dialog actions.

**--- MAINTENANCE & BUG FIXES ---**
- **System Compliance**: Updated `ScreenOffReceiver` to register and unregister dynamically, adhering to modern Android broadcast requirements.
- **Tactile Feedback**: Refined haptic triggers and adjusted slider step logic for a more responsive user experience.
- **Worker Logic**: Updated `DailyVerseWorker` to fully respect the new interval and screen-off logic.

## [1.4](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.4) — Release 1.4 (2026-04-08)

Changelog:

- **Settings & Persistence**:
    - Migrated app settings (font, colors, language, offsets) to persist via `SharedPreferences`, ensuring user preferences are restored on launch.
    - Replaced the settings `AlertDialog` with a modern `ModalBottomSheet` featuring categorized sections (Language, Appearance, Haptics, Daily Wallpaper, Support).
    - Improved data synchronization between the background worker and the main UI preview.
- **UI & UX Enhancements**:
    - Implemented a custom animated in-app notification system (`AppNotification`) to replace standard Snapbars for actions like saving and generation.
    - Redesigned settings cards with a consistent `surfaceVariant` style and improved iconography.
    - Refined the `EnhancedSlider` component to accept external haptic feedback providers.
    - Added a lifecycle observer to refresh the wallpaper preview automatically when the app returns to the foreground.

## [1.3](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.3) — Release 1.3 (2026-04-07)

Changelog:
- **Custom Verse Support**:
    - Added a new feature allowing users to input and apply custom verse text and references instead of using the "Verse of the Day."
    - Implemented persistence for custom verses in `SharedPreferences`.
    - Updated `DailyVerseWorker` to respect the custom verse setting when generating wallpapers in the background.
    - Automatically disables custom verse usage when the daily automatic update is toggled on.
- **UI & UX Enhancements**:
    - Introduced `AnimatedVisibility` for settings sections (Battery Warning and Custom Verse Editor) for smoother transitions.
    - Added a tap-to-dismiss gesture to clear focus from text input fields.
    - Improved the interactive editor with a "snapping" mechanism for text width, size, and vertical position, providing haptic feedback at default values.
- **Localization**:
    - Added new strings for "Custom Text," "Verse text," "Coordinates," and "Apply" across all supported languages (SK, EN, CZ, ES, IT, FR, DE, HU, PL).
- **Code Quality**:
    - Cleaned up redundant imports and whitespace.
    - Improved state management for font picking and color selection.

## [1.2](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.2) — Release 1.2 (2026-04-05)

Removed Internet permission

## [1.1](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.1) — Release 1.1 (2026-04-04)

Full changelog:

- Verses are now stored in a local json file, making the app work offline
- BLS now stores a copy of the chosen picture in its data folder, this makes the app work even if the chosen picture gets deleted
- Added a battery optimization warning
Minor UX improvements:
- Added a new popup to indicate where to acces the full screen editor
- Improved some animations
- Remade the "Test" button
- Added an option to change the "Daily Wallpaper Change" time to the settings menu

## [1.0.0](https://github.com/DaKlok/BibleLockScreen/releases/tag/1.0.0) — Release (2026-03-29)

_No release notes were written for this release._

