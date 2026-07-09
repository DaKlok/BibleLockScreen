package com.daklok.biblelockscreen.strings

import java.util.Locale

// --- TRANSLATIONS ---
// AppStrings only declares the fields (plus a few values that are shared
// across all languages, like appName). Actual text lives in per-language
// files: StringsSK.kt (source language, Slovak), StringsEN.kt, etc.
// To add a new translation:
//   1. Create a new file StringsXX.kt in this package (XX = uppercase language code).
//   2. Define: val xxStrings = AppStrings(...), passing every field declared below.
//   3. Register the language in `availableLanguages` below.
//   4. Add a case to the `when` block in MainActivity that maps the code to the instance.

data class AppStrings(
    val test: String,
    val generatingBtn: String,
    val generating: String,
    val done: String,

    val updateTime: String,

    val tapToEdit: String,
    val appName: String = "Bible Lock Screen",
    val settings: String,
    val dailyWallpaper: String,
    val active: String,
    val inactive: String,

    val textCustomization: String,
    val bold: String,
    val shadow: String,
    val textSize: String,
    val textWidth: String,
    val textHeight: String,
    val textAlpha: String,
    val bgBlur: String,
    val bgDarknessLabel: String,
    val anotherPhoto: String,
    val selectPhotoFirst: String,
    val appearance: String,
    val system: String,
    val light: String,
    val dark: String,
    val dynamicColor: String,
    val haptics: String,
    val hapticsDesc: String,
    val support: String,
    val supportDesc: String,
    val donate: String,
    val close: String,
    val dragHint: String,

    val cancel: String,
    val clickToSelect: String,
    val loading: String,
    val appLanguage: String,
    val verseLanguage: String,
    val language: String,
    val dailyWorkerOn: String,
    val dailyWorkerOff: String,
    val fontModern: String,
    val fontBook: String,

    val fontMono: String,
    val fontCursive: String,
    val fontLight: String,
    val fontCondensed: String,
    val langSk: String = "Slovenčina",
    val langEn: String = "English",
    val langCz: String = "Čeština",

    // Battery strings
    val batteryWarningTitle: String,
    val batteryWarningDesc: String,
    val batteryWarningButton: String,

    // Custom Verse strings
    val customVerseTitle: String,
    val customVerseHint: String,
    val customRefHint: String,
    val applyCustom: String,

    // Automatic Wallpaper Change strings
    val autoWallpaper: String,
    val autoWallpaperIntervalLabel: String,
    val autoWallpaperEvery1h: String,
    val autoWallpaperEvery2h: String,
    val autoWallpaperEvery3h: String,
    val autoWallpaperEvery6h: String,
    val autoWallpaperEvery12h: String,
    val autoWallpaperEvery24h: String,
    val autoWallpaperOnScreenOff: String,
    val autoWallpaperOnScreenOffDesc: String,
    val autoWallpaperTimeLabel: String,
    val autoWallpaperActiveHourly: String,
    val autoWallpaperActiveDaily: String,
    val autoWallpaperActiveScreenOff: String,
    val autoWorkerOn: String,
    val autoWorkerOff: String,

    // Color picker
    val colorPickerTitle: String,
    val colorPickerBrightness: String,
    val colorPickerVerseColor: String,

    // Wallpaper target
    val wallpaperTargetLabel: String,
    val wallpaperTargetLock: String,
    val wallpaperTargetHome: String,
    val wallpaperTargetBoth: String,

    // Verse database section
    val vdbTitle: String,
    val vdbSubtitle: String,
    val vdbManage: String,
    val vdbNewDatabase: String,
    val vdbEditPrefix: String,
    val vdbImportExport: String,
    val vdbCreateNew: String,
    val vdbSectionCustom: String,
    val vdbSectionBuiltIn: String,
    val vdbHint: String,
    val vdbDeleteTitle: String,
    val vdbDeleteText: String,
    val vdbDelete: String,
    val vdbDeleted: String,
    val vdbVerses: String,
    val vdbCustomLabel: String,
    val vdbCodeLabel: String,
    val vdbCodePlaceholder: String,
    val vdbCodeHint: String,
    val vdbUpdate: String,
    val vdbSave: String,
    val vdbNoVerses: String,
    val vdbVersesFilled: String,
    val vdbVerse: String,
    val vdbVersePlural: String,
    val vdbClearAll: String,
    val vdbNoVersesYet: String,
    val vdbAddFirstVerse: String,
    val vdbAddVerse: String,
    val vdbVerseText: String,
    val vdbReference: String,
    val vdbReferencePlaceholder: String,
    val vdbVerseCard: String,
    val vdbUpdated: String,
    val vdbCreated: String,
    val vdbErrorCode: String,
    val vdbErrorVerse: String,
    val vdbErrorFailed: String,
    val vdbImportTitle: String,
    val vdbImportJson: String,
    val vdbImportDesc: String,
    val vdbBrowse: String,
    val vdbImporting: String,
    val vdbImportCodeTitle: String,
    val vdbImportCodeDesc: String,
    val vdbImport: String,
    val vdbImportSuccess: String,
    val vdbImportFailed: String,
    val vdbExportCustom: String,
    val vdbExportBuiltIn: String,
    val vdbExport: String,
    val vdbExporting: String,
    val vdbExportDone: String,
    val vdbExportFailed: String,
    val vdbJsonFormat: String,

    // Backup & Restore
    val backupTitle: String,
    val backupExport: String,
    val backupExportDesc: String,
    val backupImport: String,
    val backupImportDesc: String,
    val backupExporting: String,
    val backupImporting: String,
    val backupExportSuccess: String,
    val backupImportSuccess: String,
    val backupExportFailed: String,
    val backupImportFailed: String,
    val backupConfirmTitle: String,
    val backupConfirmDesc: String,

    // Wallpaper gallery screen
    val wpScreenTitle: String,
    val wpScreenSubtitle: String,
    val wpGalleryEmpty: String,
    val wpGalleryEmptyDesc: String,
    val wpAdd: String,
    val wpSetActive: String,
    val wpActive: String,
    val wpDelete: String,
    val wpDeleteConfirm: String,
    val wpDeleteConfirmDesc: String,
    val wpCycleTitle: String,
    val wpCycleDesc: String,
    val wpCycleInterval: String,
    val wpCycleDailyHour: String,
    val wpCycleOnScreenOff: String,
    val wpCycleOnScreenOffDesc: String,
    val wpNightMode: String,
    val wpNightModeDesc: String,
    val wpNightStart: String,
    val wpNightEnd: String,
    val wpActiveBadge: String,
    val wpNightBadge: String,
    val wpCycleModeVerse: String,
    val wpCycleModeInterval: String,
    val wpCycleModeScreenOff: String,
    val wpCycleModeDayNight: String,
    val wpCycleModeVerseDesc: String,
    val wpCycleModeDayNightDesc: String,
    val wpCycleDayWallpaper: String,
    val wpCycleNightWallpaper: String,
    val wpCycleModeIntervalDesc: String,
    val wpCycleModeScreenOffDesc: String,
    val wpDualLockWarning: String,
    val wpDayStart: String,
    val wpDayEnd: String,

    val wpViewAll: String,
    val wpViewAllTitle: String,
    val wpSelectMode: String,
    val wpDeleteSelected: String,
    val wpSelected: String,
    val wpDeleteAllConfirm: String,
    val wpDeleteAllConfirmDesc: String,
    val wpPageHint: String,

    // Verse language source toggle (in Settings)
    val vdbSourceDefault: String,
    val vdbSourceCustom: String,
    val vdbEmptyCustom: String,
    val vdbEmptyCustomDesc: String,
    val vdbEmptyCustomCta: String,

    // Duplicate-code warnings + import conflict dialog
    val vdbWarningCodeBuiltin: String,
    val vdbWarningCodeCustom: String,
    val vdbImportConflictTitle: String,
    val vdbImportConflictDesc: String,
    val vdbImportConflictAction: String,

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    val vdbOverwriteTitle: String,
    val vdbOverwriteDesc: String,
    val vdbOverwriteConfirm: String,

    // --- Share verse as image ---
    val share: String,
    val shareDialogTitle: String,
    val shareDialogDesc: String,
    val shareAction: String,
    val shareActionDesc: String,
    val saveToGalleryAction: String,
    val saveToGalleryActionDesc: String,
    val shareSuccess: String,
    val shareFailed: String,
    val saveSuccess: String,
    val saveFailed: String,
    val shareImageDesc: String,

    // --- Favorite verses ---
    val favorites: String,
    val favoritesTitle: String,
    val favoritesEmpty: String,
    val favoritesEmptyDesc: String,
    val addToFavorites: String,
    val removeFromFavorites: String,
    val favoritesCount: String,
    val setAsWallpaper: String,
    val confirmRemoveFavoriteTitle: String,
    val confirmRemoveFavoriteDesc: String,
    val addedToFavorites: String,
    val removedFromFavorites: String,
    val addedOn: String
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