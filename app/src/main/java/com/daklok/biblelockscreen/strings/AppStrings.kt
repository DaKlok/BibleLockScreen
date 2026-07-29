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
data class AppStrings(
    val test: String = "Generate",
    val generatingBtn: String = "Generating...",
    val generating: String = "Generating wallpaper...",
    val done: String = "Done",
    val updateTime: String = "Time of daily update",
    val tapToEdit: String = "Tap to edit position and size",
    val appName: String = "Bible Lock Screen",
    val settings: String = "App Settings",
    val dailyWallpaper: String = "Daily Wallpaper Change",
    val active: String = "Active (every day at %s:00)",
    val inactive: String = "Disabled",
    val textCustomization: String = "Text Customization",
    val bold: String = "Bold",
    val shadow: String = "Shadow",
    val textSize: String = "Text Size",
    val textWidth: String = "Text Width",
    val textHeight: String = "Text Position",
    val textAlpha: String = "Transparency",
    val bgBlur: String = "Background Blur",
    val bgDarknessLabel: String = "Background Darkness",
    val anotherPhoto: String = "Change Photo",
    val selectPhotoFirst: String = "Select a photo first",
    val appearance: String = "Appearance",
    val system: String = "System",
    val light: String = "Light",
    val dark: String = "Dark",
    val dynamicColor: String = "Material You (System Colors)",
    val haptics: String = "Haptic Feedback",
    val hapticsDesc: String = "Vibrations when editing the widget",
    val support: String = "Support",
    val supportDesc: String = "If you like the app and want to support its development, you can buy me a coffee ☕",
    val donate: String = "Donate",
    val close: String = "Close",
    val dragHint: String = "Drag dots for size and width\nDrag the center to move",
    val cancel: String = "Cancel",
    val clickToSelect: String = "Tap to select photo",
    val loading: String = "Loading verse...",
    val appLanguage: String = "App Language",
    val verseLanguage: String = "Verse Language",
    val language: String = "Language",
    val dailyWorkerOn: String = "Enabled!\nWallpaper will change tomorrow at %s:00.",
    val dailyWorkerOff: String = "Daily change disabled.",
    val fontModern: String = "Modern",
    val fontBook: String = "Book",
    val fontMono: String = "Typewriter",
    val fontCursive: String = "Cursive",
    val fontLight: String = "Light",
    val fontCondensed: String = "Condensed",
    val langSk: String = "Slovenčina",
    val langEn: String = "English",
    val langCz: String = "Čeština",

    // Battery strings
    val batteryWarningTitle: String = "Battery Warning",
    val batteryWarningDesc: String = "For the automatic wallpaper change to work reliably in the background, please disable battery optimization for this app.",
    val batteryWarningButton: String = "Disable Optimization",
    // Custom Verse strings
    val customVerseTitle: String = "Custom Text",
    val customVerseHint: String = "Verse text...",
    val customRefHint: String = "Coordinates (e.g. John 3:16)",
    val applyCustom: String = "Apply",
    // Automatic Wallpaper Change strings
    val autoWallpaper: String = "Automatic Wallpaper Change",
    val autoWallpaperIntervalLabel: String = "Change interval",
    val autoWallpaperEvery1h: String = "Every hour",
    val autoWallpaperEvery2h: String = "Every 2 hours",
    val autoWallpaperEvery3h: String = "Every 3 hours",
    val autoWallpaperEvery6h: String = "Every 6 hours",
    val autoWallpaperEvery12h: String = "Every 12 hours",
    val autoWallpaperEvery24h: String = "Every 24 hours (daily)",
    val autoWallpaperOnScreenOff: String = "On screen off",
    val autoWallpaperOnScreenOffDesc: String = "Verse changes every time you lock the phone",
    val autoWallpaperTimeLabel: String = "Daily change time",
    val autoWallpaperActiveHourly: String = "Active (every %sh)",
    val autoWallpaperActiveDaily: String = "Active (daily at %s:00)",
    val autoWallpaperActiveScreenOff: String = "Active (on lock)",
    val autoWorkerOn: String = "Enabled! Wallpaper will change based on your interval.",
    val autoWorkerOff: String = "Automatic change disabled.",
    // Color picker
    val colorPickerTitle: String = "Select color",
    val colorPickerBrightness: String = "Brightness",
    val colorPickerVerseColor: String = "Verse color",
    // Wallpaper target
    val wallpaperTargetLabel: String = "Apply wallpaper to",
    val wallpaperTargetLock: String = "Lock screen",
    val wallpaperTargetHome: String = "Home screen",
    val wallpaperTargetBoth: String = "Both",
    // Verse database section
    val vdbTitle: String = "Verse databases",
    val vdbSubtitle: String = "Create, import or export collections",
    val vdbManage: String = "Manage",
    val vdbNewDatabase: String = "New database",
    val vdbEditPrefix: String = "Edit ·",
    val vdbImportExport: String = "Import / Export",
    val vdbCreateNew: String = "Create new",
    val vdbSectionCustom: String = "Custom",
    val vdbSectionBuiltIn: String = "Built-in",
    val vdbHint: String = "Select a custom DB via Settings → Verse Language",
    val vdbDeleteTitle: String = "Delete",
    val vdbDeleteText: String = "verses will be permanently removed.",
    val vdbDelete: String = "Delete",
    val vdbDeleted: String = "Deleted",
    val vdbVerses: String = "verses",
    val vdbCustomLabel: String = "Custom",
    val vdbCodeLabel: String = "Code",
    val vdbCodePlaceholder: String = "e.g. KJV",
    val vdbCodeHint: String = "Select this in Settings → Verse Language",
    val vdbUpdate: String = "Update",
    val vdbSave: String = "Save",
    val vdbNoVerses: String = "No verses — tap + below to add",
    val vdbVersesFilled: String = "filled",
    val vdbVerse: String = "verse",
    val vdbVersePlural: String = "verses",
    val vdbClearAll: String = "Clear all",
    val vdbNoVersesYet: String = "No verses yet",
    val vdbAddFirstVerse: String = "Tap the button below to add your first verse",
    val vdbAddVerse: String = "Add verse",
    val vdbVerseText: String = "Verse text",
    val vdbReference: String = "Reference",
    val vdbReferencePlaceholder: String = "e.g. John 3:16",
    val vdbVerseCard: String = "Verse",
    val vdbUpdated: String = "Updated",
    val vdbCreated: String = "Created",
    val vdbErrorCode: String = "Enter a database code",
    val vdbErrorVerse: String = "Add at least one verse",
    val vdbErrorFailed: String = "Failed",
    val vdbImportTitle: String = "Import",
    val vdbImportJson: String = "Import JSON file",
    val vdbImportDesc: String = "Pick a verse .json from your device",
    val vdbBrowse: String = "Browse",
    val vdbImporting: String = "Importing...",
    val vdbImportCodeTitle: String = "Database code",
    val vdbImportCodeDesc: String = "Enter a short code for this database (e.g. KJV). It overrides a built-in database if the code matches.",
    val vdbImport: String = "Import",
    val vdbImportSuccess: String = "Imported %d verses as",
    val vdbImportFailed: String = "Import failed",
    val vdbExportCustom: String = "Export — Custom",
    val vdbExportBuiltIn: String = "Export — Built-in",
    val vdbExport: String = "Export",
    val vdbExporting: String = "Saving to Downloads…",
    val vdbExportDone: String = "Saved to Downloads",
    val vdbExportFailed: String = "Export failed",
    val vdbJsonFormat: String = "JSON format",
    // Backup & Restore
    val backupTitle: String = "Backup & Restore",
    val backupExport: String = "Back up settings",
    val backupExportDesc: String = "Saves all settings, wallpaper, and databases to a single file",
    val backupImport: String = "Restore from backup",
    val backupImportDesc: String = "Replaces all current settings, wallpaper, and databases",
    val backupExporting: String = "Backing up…",
    val backupImporting: String = "Restoring…",
    val backupExportSuccess: String = "Backup saved · %d settings, %d databases",
    val backupImportSuccess: String = "Backup restored · %d settings, %d databases. Restarting…",
    val backupExportFailed: String = "Backup failed",
    val backupImportFailed: String = "Restore failed",
    val backupConfirmTitle: String = "Restore from backup?",
    val backupConfirmDesc: String = "This will replace all your current settings, wallpaper, and databases. This cannot be undone.",
    // Wallpaper gallery screen
    val wpScreenTitle: String = "Wallpapers",
    val wpScreenSubtitle: String = "Manage your wallpapers and auto-cycling",
    val wpGalleryEmpty: String = "No wallpapers",
    val wpGalleryEmptyDesc: String = "Add your first wallpaper — pick a photo from your gallery.",
    val wpAdd: String = "Add wallpaper",
    val wpSetActive: String = "Set active",
    val wpActive: String = "Active",
    val wpDelete: String = "Remove",
    val wpDeleteConfirm: String = "Remove this wallpaper?",
    val wpDeleteConfirmDesc: String = "The wallpaper will be permanently removed from the app.",
    val wpCycleTitle: String = "Auto-cycling",
    val wpCycleDesc: String = "Automatically cycle through wallpapers on a schedule",
    val wpCycleInterval: String = "Cycle interval",
    val wpCycleDailyHour: String = "Daily change time",
    val wpCycleOnScreenOff: String = "On lock",
    val wpCycleOnScreenOffDesc: String = "Change wallpaper every time you lock the phone",
    val wpNightMode: String = "Night mode",
    val wpNightModeDesc: String = "Use a different wallpaper at night",
    val wpNightStart: String = "Night starts",
    val wpNightEnd: String = "Night ends",
    val wpActiveBadge: String = "Active",
    val wpNightBadge: String = "Night",
    val wpCycleModeVerse: String = "On verse change",
    val wpCycleModeInterval: String = "Custom interval",
    val wpCycleModeScreenOff: String = "On every lock",
    val wpCycleModeDayNight: String = "Day / Night",
    val wpCycleModeVerseDesc: String = "Wallpaper changes with the verse",
    val wpCycleModeDayNightDesc: String = "Automatically switch between day and night wallpaper",
    val wpCycleDayWallpaper: String = "Day wallpaper",
    val wpCycleNightWallpaper: String = "Night wallpaper",
    val wpCycleModeIntervalDesc: String = "Change at fixed intervals",
    val wpCycleModeScreenOffDesc: String = "Change on every screen lock",
    val wpDualLockWarning: String = "It could take up to 3 seconds to update wallpaper while screen is off.",
    val wpDayStart: String = "Day starts",
    val wpDayEnd: String = "Day ends",
    val wpViewAll: String = "View all",
    val wpViewAllTitle: String = "All wallpapers",
    val wpSelectMode: String = "Select",
    val wpDeleteSelected: String = "Delete selected",
    val wpSelected: String = "selected",
    val wpDeleteAllConfirm: String = "Delete selected wallpapers?",
    val wpDeleteAllConfirmDesc: String = "The selected wallpapers will be permanently removed from the app.",
    val wpPageHint: String = "Swipe left for wallpapers →",
    // Verse language source toggle (in Settings)
    val vdbSourceDefault: String = "Default",
    val vdbSourceCustom: String = "Custom",
    val vdbSourceFavorites: String = "Favorites",
    val vdbFavoritesCycling: String = "Cycling through %d favorite verses",
    val vdbFavoritesEmptyCta: String = "Add some favorites first",
    val vdbEmptyCustom: String = "No custom databases yet",
    val vdbEmptyCustomDesc: String = "Create your own verse collection — for example KJV or another translation.",
    val vdbEmptyCustomCta: String = "Create new database",
    // Duplicate-code warnings + import conflict dialog
    val vdbWarningCodeBuiltin: String = "Code \"%s\" is already used by a built-in database. You can still use it — the custom DB will share the code.",
    val vdbWarningCodeCustom: String = "Code \"%s\" already exists as a custom database. Saving will overwrite it.",
    val vdbImportConflictTitle: String = "Code already exists",
    val vdbImportConflictDesc: String = "A custom database with code \"%s\" already exists. Enter a different code, or keep \"%s\" to overwrite.",
    val vdbImportConflictAction: String = "Import with this code",
    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    val vdbOverwriteTitle: String = "Overwrite database?",
    val vdbOverwriteDesc: String = "A custom database with code \"%s\" already exists. Saving will permanently replace its verses. This action cannot be undone.",
    val vdbOverwriteConfirm: String = "Overwrite",
    // --- Share verse as image ---
    val share: String = "Share",
    val shareDialogTitle: String = "Share verse",
    val shareDialogDesc: String = "Choose an action",
    val shareAction: String = "Share",
    val shareActionDesc: String = "Send the image via other apps",
    val saveToGalleryAction: String = "Save to gallery",
    val saveToGalleryActionDesc: String = "Save the image to this device",
    val shareSuccess: String = "Verse shared",
    val shareFailed: String = "Failed to share verse",
    val saveSuccess: String = "Verse saved to gallery",
    val saveFailed: String = "Failed to save to gallery",
    val shareImageDesc: String = "Image with verse for sharing",
    // --- Favorite verses ---
    val favorites: String = "Favorites",
    val favoritesTitle: String = "Favorite verses",
    val favoritesEmpty: String = "You don't have any favorite verses yet",
    val favoritesEmptyDesc: String = "Add a verse by tapping the star in the preview.",
    val addToFavorites: String = "Add to favorites",
    val removeFromFavorites: String = "Remove from favorites",
    val favoritesCount: String = "%d favorites",
    val setAsWallpaper: String = "Set as wallpaper",
    val confirmRemoveFavoriteTitle: String = "Remove from favorites?",
    val confirmRemoveFavoriteDesc: String = "This verse will be removed from your favorites.",
    val addedToFavorites: String = "Verse added to favorites",
    val removedFromFavorites: String = "Verse removed from favorites",
    val addedOn: String = "Added %s"
)

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