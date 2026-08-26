package com.daklok.biblelockscreen.strings

import java.util.Locale

// --- TRANSLATIONS ---
// AppStrings declares every field with a default value — and that default
// is always the English text. This is what makes partial translations
// work: a StringsXX.kt file only needs to pass the fields it has actually
// translated; any field it omits silently falls back to English instead of
// failing to compile.
//
// Since English *is* the default, StringsEN.kt itself is just
// `AppStrings()` with nothing overridden — there's no separate English
// file to keep in sync. Translators should start from
// StringsTemplate.kt.txt instead, which has one `<TODO> // "English text"`
// line per field for reference.
//
// Actual text otherwise lives in per-language files: StringsSK.kt (source
// language, Slovak), StringsCZ.kt, etc.
//
// To add a new translation:
//   1. Copy StringsTemplate.kt.txt to StringsXX.kt in this package
//      (XX = uppercase language code) and follow the instructions at the
//      top of that file.
//   2. Register the language in `availableLanguages` below.
//   3. Add a case to the `when` block in MainActivity that maps the code to the instance.
//
// If you add a brand-new string field (not just a new language), add it
// below with its English text as the default — that's the one place
// English text for a field lives now.

// Every default value below IS the English text — there's no separate
// StringsEN.kt to keep in sync anymore, this is the single source of truth
// for it. When you change a string's English wording, change it here.
class AppStrings {
    var test: String = "Generate"
    var generatingBtn: String = "Generating..."
    var generating: String = "Generating wallpaper..."
    var done: String = "Done"
    var updateTime: String = "Time of daily update"
    var tapToEdit: String = "Tap to edit position and size"
    var appName: String = "Bible Lock Screen"
    var settings: String = "App Settings"
    var dailyWallpaper: String = "Daily Wallpaper Change"
    var active: String = "Active (every day at %s:00)"
    var inactive: String = "Disabled"
    var textCustomization: String = "Text Customization"
    var bold: String = "Bold"
    var shadow: String = "Shadow"
    var textSize: String = "Text Size"
    var textWidth: String = "Text Width"
    var textHeight: String = "Text Position"
    var textAlpha: String = "Transparency"
    var bgBlur: String = "Background Blur"
    var bgDarknessLabel: String = "Background Darkness"
    var anotherPhoto: String = "Change Photo"
    var selectPhotoFirst: String = "Select a photo first"
    var appearance: String = "Appearance"
    var system: String = "System"
    var light: String = "Light"
    var dark: String = "Dark"
    var dynamicColor: String = "Material You (System Colors)"
    var haptics: String = "Haptic Feedback"
    var hapticsDesc: String = "Vibrations when editing the widget"
    var support: String = "Support"
    var supportDesc: String = "If you like the app and want to support its development, you can buy me a coffee ☕"
    var donate: String = "Donate"
    var close: String = "Close"
    var dragHint: String = "Drag dots for size and width\nDrag the center to move"
    var cancel: String = "Cancel"
    var clickToSelect: String = "Tap to select photo"
    var loading: String = "Loading verse..."
    var appLanguage: String = "App Language"
    var verseLanguage: String = "Verse Language"
    var language: String = "Language"
    var dailyWorkerOn: String = "Enabled!\nWallpaper will change tomorrow at %s:00."
    var dailyWorkerOff: String = "Daily change disabled."
    var fontModern: String = "Modern"
    var fontBook: String = "Book"
    var fontMono: String = "Typewriter"
    var fontCursive: String = "Cursive"
    var fontLight: String = "Light"
    var fontCondensed: String = "Condensed"
    var langSk: String = "Slovenčina"
    var langEn: String = "English"
    var langCz: String = "Čeština"

    // Battery strings
    var batteryWarningTitle: String = "Battery Warning"
    var batteryWarningDesc: String = "For the automatic wallpaper change to work reliably in the background, please disable battery optimization for this app."
    var batteryWarningButton: String = "Disable Optimization"
    // Custom Verse strings
    var customVerseTitle: String = "Custom Text"
    var customVerseHint: String = "Verse text..."
    var customRefHint: String = "Coordinates (e.g. John 3:16)"
    var applyCustom: String = "Apply"
    // Automatic Wallpaper Change strings
    var autoWallpaper: String = "Automatic Wallpaper Change"
    var autoWallpaperIntervalLabel: String = "Change interval"
    var autoWallpaperEvery1h: String = "Every hour"
    var autoWallpaperEvery2h: String = "Every 2 hours"
    var autoWallpaperEvery3h: String = "Every 3 hours"
    var autoWallpaperEvery6h: String = "Every 6 hours"
    var autoWallpaperEvery12h: String = "Every 12 hours"
    var autoWallpaperEvery24h: String = "Every 24 hours (daily)"
    var autoWallpaperOnScreenOff: String = "On screen off"
    var autoWallpaperOnScreenOffDesc: String = "Verse changes every time you lock the phone"
    var autoWallpaperTimeLabel: String = "Daily change time"
    var autoWallpaperActiveHourly: String = "Active (every %sh)"
    var autoWallpaperActiveDaily: String = "Active (daily at %s:00)"
    var autoWallpaperActiveScreenOff: String = "Active (on lock)"
    var autoWorkerOn: String = "Enabled! Wallpaper will change based on your interval."
    var autoWorkerOff: String = "Automatic change disabled."
    // Color picker
    var colorPickerTitle: String = "Select color"
    var colorPickerBrightness: String = "Brightness"
    var colorPickerVerseColor: String = "Verse color"
    // Wallpaper target
    var wallpaperTargetLabel: String = "Apply wallpaper to"
    var wallpaperTargetLock: String = "Lock screen"
    var wallpaperTargetHome: String = "Home screen"
    var wallpaperTargetBoth: String = "Both"
    // Verse database section
    var vdbTitle: String = "Verse databases"
    var vdbSubtitle: String = "Create, import or export collections"
    var vdbManage: String = "Manage"
    var vdbNewDatabase: String = "New database"
    var vdbEditPrefix: String = "Edit ·"
    var vdbImportExport: String = "Import / Export"
    var vdbCreateNew: String = "Create new"
    var vdbSectionCustom: String = "Custom"
    var vdbSectionBuiltIn: String = "Built-in"
    var vdbHint: String = "Tap \"Use\" on a custom database to make it your active verse source — or pick it later via Settings → Verse Language."
    var vdbUse: String = "Use"
    var vdbUsed: String = "Activated"
    var vdbDeleteTitle: String = "Delete"
    var vdbDeleteText: String = "verses will be permanently removed."
    var vdbDelete: String = "Delete"
    var vdbDeleted: String = "Deleted"
    var vdbVerses: String = "verses"
    var vdbCustomLabel: String = "Custom"
    var vdbCodeLabel: String = "Code"
    var vdbCodePlaceholder: String = "e.g. KJV"
    var vdbCodeHint: String = "Select this in Settings → Verse Language"
    var vdbUpdate: String = "Update"
    var vdbSave: String = "Save"
    var vdbNoVerses: String = "No verses — tap + below to add"
    var vdbVersesFilled: String = "filled"
    var vdbVerse: String = "verse"
    var vdbVersePlural: String = "verses"
    var vdbClearAll: String = "Clear all"
    var vdbNoVersesYet: String = "No verses yet"
    var vdbAddFirstVerse: String = "Tap the button below to add your first verse"
    var vdbAddVerse: String = "Add verse"
    var vdbVerseText: String = "Verse text"
    var vdbReference: String = "Reference"
    var vdbReferencePlaceholder: String = "e.g. John 3:16"
    var vdbVerseCard: String = "Verse"
    var vdbUpdated: String = "Updated"
    var vdbCreated: String = "Created"
    var vdbErrorCode: String = "Enter a database code"
    var vdbErrorVerse: String = "Add at least one verse"
    var vdbErrorFailed: String = "Failed"
    var vdbImportTitle: String = "Import"
    var vdbImportJson: String = "Import JSON file"
    var vdbImportDesc: String = "Pick a verse .json from your device"
    var vdbBrowse: String = "Browse"
    var vdbImporting: String = "Importing..."
    var vdbImportCodeTitle: String = "Database code"
    var vdbImportCodeDesc: String = "Enter a short code for this database (e.g. KJV). It overrides a built-in database if the code matches."
    var vdbImport: String = "Import"
    var vdbImportSuccess: String = "Imported %d verses as"
    var vdbImportFailed: String = "Import failed"
    var vdbExportCustom: String = "Export — Custom"
    var vdbExportBuiltIn: String = "Export — Built-in"
    var vdbExport: String = "Export"
    var vdbExporting: String = "Saving to Downloads…"
    var vdbExportDone: String = "Saved to Downloads"
    var vdbExportFailed: String = "Export failed"
    var vdbJsonFormat: String = "JSON format"
    // Backup & Restore
    var backupTitle: String = "Backup & Restore"
    var backupExport: String = "Back up settings"
    var backupExportDesc: String = "Saves all settings, wallpaper, and databases to a single file"
    var backupImport: String = "Restore from backup"
    var backupImportDesc: String = "Replaces all current settings, wallpaper, and databases"
    var backupExporting: String = "Backing up…"
    var backupImporting: String = "Restoring…"
    var backupExportSuccess: String = "Backup saved · %d settings, %d databases"
    var backupImportSuccess: String = "Backup restored · %d settings, %d databases. Restarting…"
    var backupExportFailed: String = "Backup failed"
    var backupImportFailed: String = "Restore failed"
    var backupConfirmTitle: String = "Restore from backup?"
    var backupConfirmDesc: String = "This will replace all your current settings, wallpaper, and databases. This cannot be undone."
    // Wallpaper gallery screen
    var wpScreenTitle: String = "Wallpapers"
    var wpScreenSubtitle: String = "Manage your wallpapers and auto-cycling"
    var wpGalleryEmpty: String = "No wallpapers"
    var wpGalleryEmptyDesc: String = "Add your first wallpaper — pick a photo from your gallery."
    var wpAdd: String = "Add wallpaper"
    var wpSetActive: String = "Set active"
    var wpActive: String = "Active"
    var wpDelete: String = "Remove"
    var wpDeleteConfirm: String = "Remove this wallpaper?"
    var wpDeleteConfirmDesc: String = "The wallpaper will be permanently removed from the app."
    var wpCycleTitle: String = "Auto-cycling"
    var wpCycleDesc: String = "Automatically cycle through wallpapers on a schedule"
    var wpCycleInterval: String = "Cycle interval"
    var wpCycleDailyHour: String = "Daily change time"
    var wpCycleOnScreenOff: String = "On lock"
    var wpCycleOnScreenOffDesc: String = "Change wallpaper every time you lock the phone"
    var wpNightMode: String = "Night mode"
    var wpNightModeDesc: String = "Use a different wallpaper at night"
    var wpNightStart: String = "Night starts"
    var wpNightEnd: String = "Night ends"
    var wpActiveBadge: String = "Active"
    var wpNightBadge: String = "Night"
    var wpCycleModeVerse: String = "On verse change"
    var wpCycleModeInterval: String = "Custom interval"
    var wpCycleModeScreenOff: String = "On every lock"
    var wpCycleModeDayNight: String = "Day / Night"
    var wpCycleModeVerseDesc: String = "Wallpaper changes with the verse"
    var wpCycleModeDayNightDesc: String = "Automatically switch between day and night wallpaper"
    var wpCycleDayWallpaper: String = "Day wallpaper"
    var wpCycleNightWallpaper: String = "Night wallpaper"
    var wpCycleModeIntervalDesc: String = "Change at fixed intervals"
    var wpCycleModeScreenOffDesc: String = "Change on every screen lock"
    var wpDualLockWarning: String = "It could take up to 3 seconds to update wallpaper while screen is off."
    var wpDayStart: String = "Day starts"
    var wpDayEnd: String = "Day ends"
    var wpViewAll: String = "View all"
    var wpViewAllTitle: String = "All wallpapers"
    var wpSelectMode: String = "Select"
    var wpDeleteSelected: String = "Delete selected"
    var wpSelected: String = "selected"
    var wpDeleteAllConfirm: String = "Delete selected wallpapers?"
    var wpDeleteAllConfirmDesc: String = "The selected wallpapers will be permanently removed from the app."
    var wpPageHint: String = "Swipe left for wallpapers →"
    // Verse language source toggle (in Settings)
    var vdbSourceDefault: String = "Default"
    var vdbSourceCustom: String = "Custom"
    var vdbSourceFavorites: String = "Favorites"
    var vdbFavoritesCycling: String = "Cycling through %d favorite verses"
    var vdbFavoritesEmptyCta: String = "Add some favorites first"
    var vdbEmptyCustom: String = "No custom databases yet"
    var vdbEmptyCustomDesc: String = "Create your own verse collection — for example KJV or another translation."
    var vdbEmptyCustomCta: String = "Create new database"
    // Duplicate-code warnings + import conflict dialog
    var vdbWarningCodeBuiltin: String = "Code \"%s\" is already used by a built-in database. You can still use it — the custom DB will share the code."
    var vdbWarningCodeCustom: String = "Code \"%s\" already exists as a custom database. Saving will overwrite it."
    var vdbImportConflictTitle: String = "Code already exists"
    var vdbImportConflictDesc: String = "A custom database with code \"%s\" already exists. Enter a different code, or keep \"%s\" to overwrite."
    var vdbImportConflictAction: String = "Import with this code"
    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    var vdbOverwriteTitle: String = "Overwrite database?"
    var vdbOverwriteDesc: String = "A custom database with code \"%s\" already exists. Saving will permanently replace its verses. This action cannot be undone."
    var vdbOverwriteConfirm: String = "Overwrite"
    // --- Share verse as image ---
    var share: String = "Share"
    var shareDialogTitle: String = "Share verse"
    var shareDialogDesc: String = "Choose an action"
    var shareAction: String = "Share"
    var shareActionDesc: String = "Send the image via other apps"
    var saveToGalleryAction: String = "Save to gallery"
    var saveToGalleryActionDesc: String = "Save the image to this device"
    var shareSuccess: String = "Verse shared"
    var shareFailed: String = "Failed to share verse"
    var saveSuccess: String = "Verse saved to gallery"
    var saveFailed: String = "Failed to save to gallery"
    var shareImageDesc: String = "Image with verse for sharing"
    // --- Favorite verses ---
    var favorites: String = "Favorites"
    var favoritesTitle: String = "Favorite verses"
    var favoritesEmpty: String = "You don't have any favorite verses yet"
    var favoritesEmptyDesc: String = "Add a verse by tapping the star in the preview."
    var addToFavorites: String = "Add to favorites"
    var removeFromFavorites: String = "Remove from favorites"
    var favoritesCount: String = "%d favorites"
    var setAsWallpaper: String = "Set as wallpaper"
    var confirmRemoveFavoriteTitle: String = "Remove from favorites?"
    var confirmRemoveFavoriteDesc: String = "This verse will be removed from your favorites."
    var addedToFavorites: String = "Verse added to favorites"
    var removedFromFavorites: String = "Verse removed from favorites"
    var addedOn: String = "Added %s"
    // Subtitle under the Favorites page header — same pattern as wpScreenSubtitle.
    var favoritesSubtitle: String = "Tap a verse to make it your wallpaper"
    // --- Favorite card style (Settings → Appearance) ---
    var favCardStyleLabel: String = "Favorite card style"
    var favCardStyleQuote: String = "Detailed"
    var favCardStyleCompact: String = "Compact"
    var favCardStyleHero: String = "Quote"
    var favCardStyleQuoteDesc: String = "Verse with an action bar"
    var favCardStyleCompactDesc: String = "Compact list row"
    var favCardStyleHeroDesc: String = "Large, poster-like quote"
    // --- Favorite multi-select (bulk delete) ---
    var favSelectMode: String = "Select"
    var favDeleteSelected: String = "Remove selected"
    var favSelected: String = "selected"
    var favSelectAll: String = "Select all"
    var favDeselectAll: String = "Deselect all"
    var favDeleteConfirm: String = "Remove selected verses?"
    var favDeleteConfirmDesc: String = "These verses will be removed from your favorites."
    var favRemovedCount: String = "%d verses removed"
}

// --- LANGUAGE DETECTION ---
fun getDefaultAppLanguage(): String {
    val sysLang = Locale.getDefault().language.uppercase()
    return when (sysLang) {
        "CS" -> "CZ"
        "SK", "EN", "CZ", "ES", "IT", "FR", "DE", "HU", "PL" -> sysLang
        else -> "EN"
    }
}

// --- AVAILABLE LANGUAGES (display order for the language picker) ---
val availableLanguages = listOf(
    "EN" to "English",
    "SK" to "Slovenčina",
    "CZ" to "Čeština",
    "ES" to "Español",
    "IT" to "Italiano",
    "FR" to "Français",
    "DE" to "Deutsch",
    "HU" to "Magyar",
    "PL" to "Polski"
)

// ─────────────────────────────────────────────────────────────────────────────
// LOCALIZED LANGUAGE NAMES (top-level, NOT a field of AppStrings)
// ─────────────────────────────────────────────────────────────────────────────
// Historical context: AppStrings used to be a `data class`, and adding a
// `Map<String, String>` field would have pushed its synthetic default-args
// constructor past the JVM method-parameter limit (255) and triggered a
// `VerifyError: StringsENKt.<clinit>` crash. We've since converted AppStrings
// to a regular `class` with `var` fields, so technically this map COULD be
// moved back in as another `var` field.
//
// We keep it as a top-level val anyway, because:
//   - It's a fundamentally different shape (a Map<String, String> lookup
//     table) than a single UI string field.
//   - It needs to be looked up by active app language code, not accessed
//     as a property of the active AppStrings instance — `langLabel(appLang, ...)`
//     takes the appLang code precisely so it can pick the right map regardless
//     of which AppStrings instance the caller happens to hold.
//
// Translator workflow: a translator adds their own `xxLocalizedLangNames`
// val in StringsXX.kt and registers it in `localizedLangNamesFor()` below.
// If they skip it, the picker falls back to English `enLocalizedLangNames`.

/** English fallback — used when no localized map exists for the active appLang. */
val enLocalizedLangNames: Map<String, String> = mapOf(
    "EN" to "English",
    "SK" to "Slovak",
    "CZ" to "Czech",
    "ES" to "Spanish",
    "IT" to "Italian",
    "FR" to "French",
    "DE" to "German",
    "HU" to "Hungarian",
    "PL" to "Polish"
)

/**
 * Returns the localized-language-names map for the active app language.
 *
 * Falls back to English when [appLang] has no per-language map — mirrors
 * the existing "missing string → English default" fallback pattern.
 */
fun localizedLangNamesFor(appLang: String): Map<String, String> = when (appLang) {
    "EN" -> enLocalizedLangNames
    "SK" -> skLocalizedLangNames
    "CZ" -> czLocalizedLangNames
    "ES" -> esLocalizedLangNames
    "IT" -> itLocalizedLangNames
    "FR" -> frLocalizedLangNames
    "DE" -> deLocalizedLangNames
    "HU" -> huLocalizedLangNames
    "PL" -> plLocalizedLangNames
    else -> enLocalizedLangNames
}

/**
 * Renders a language picker entry as "Native name (Localized name)" —
 * e.g. with appLang = "SK", code = "IT", nativeName = "Italiano" →
 * "Italiano (Taliančina)".
 *
 * When the localized name equals the native name (e.g. Slovak viewing
 * "Slovenčina"), the redundant parenthetical is omitted. Also falls back to
 * English if [appLang] has no per-language map.
 */
fun langLabel(appLang: String, code: String, nativeName: String): String {
    val map = localizedLangNamesFor(appLang)
    val localized = map[code]
    return if (!localized.isNullOrBlank() && localized != nativeName) {
        "$nativeName ($localized)"
    } else {
        nativeName
    }
}