package com.daklok.biblelockscreen.strings

import java.util.Locale

// --- TRANSLATIONS ---
// Source language: Slovak (skStrings). All other languages override these defaults.
// To add a new translation:
//   1. Create a new file StringsXX.kt in this package (XX = uppercase language code).
//   2. Define: val xxStrings = AppStrings(...),
//   3. Register the language in `availableLanguages` below.
//   4. Add a case to the `when` block in MainActivity that maps the code to the instance.

data class AppStrings(
    val test: String = "Generovať",
    val generatingBtn: String = "Generuje sa...",
    val generating: String = "Tapeta sa generuje...",
    val done: String = "Hotovo",

    val updateTime: String = "Čas dennej aktualizácie",

    val tapToEdit: String = "Kliknutím upravíš polohu a veľkosť",
    val appName: String = "Bible Lock Screen",
    val settings: String = "Nastavenia aplikácie",
    val dailyWallpaper: String = "Denná zmena tapety",
    val active: String = "Aktívne (každý deň o %s:00)",
    val inactive: String = "Vypnuté",

    val textCustomization: String = "Prispôsobenie textu",
    val bold: String = "Tučné",
    val shadow: String = "Tieň",
    val textSize: String = "Veľkosť písma",
    val textWidth: String = "Šírka textu",
    val textHeight: String = "Výška textu",
    val textAlpha: String = "Priehľadnosť",
    val bgBlur: String = "Rozmazanie pozadia",
    val bgDarknessLabel: String = "Stmavenie pozadia",
    val anotherPhoto: String = "Iná fotka",
    val selectPhotoFirst: String = "Najskôr vyber fotku",
    val appearance: String = "Vzhľad",
    val system: String = "Systém",
    val light: String = "Svetlý",
    val dark: String = "Tmavý",
    val dynamicColor: String = "Material You (Farby systému)",
    val haptics: String = "Haptická odozva",
    val hapticsDesc: String = "Vibrácie pri upravovaní widgetu",
    val support: String = "Podpora",
    val supportDesc: String = "Ak sa ti aplikácia páči a chceš podporiť jej vývoj, môžeš mi kúpiť kávu ☕",
    val donate: String = "Prispieť",
    val close: String = "Zavrieť",
    val dragHint: String = "Ťahaj body pre veľkosť a šírku\nŤahaj do stredu pre presun",

    val cancel: String = "Zrušiť",
    val clickToSelect: String = "Klikni pre výber fotky",
    val loading: String = "Načítavam verš...",
    val appLanguage: String = "Jazyk aplikácie",
    val verseLanguage: String = "Jazyk veršov",
    val language: String = "Jazyk",
    val dailyWorkerOn: String = "Zapnuté!\nTapeta sa zmení zajtra o %s:00.",
    val dailyWorkerOff: String = "Denná zmena bola vypnutá.",
    val fontModern: String = "Moderný",
    val fontBook: String = "Knižný",

    val fontMono: String = "Strojový",
    val fontCursive: String = "Písaný",
    val fontLight: String = "Tenký",
    val fontCondensed: String = "Úzky",
    val langSk: String = "Slovenčina",
    val langEn: String = "English",
    val langCz: String = "Čeština",

    // Battery strings
    val batteryWarningTitle: String = "Upozornenie na batériu",
    val batteryWarningDesc: String = "Pre správne fungovanie automatickej zmeny tapety na pozadí, prosím, vypnite optimalizáciu batérie pre túto aplikáciu.",
    val batteryWarningButton: String = "Vypnúť optimalizáciu",

    // Custom Verse strings
    val customVerseTitle: String = "Vlastný text",
    val customVerseHint: String = "Text verša...",
    val customRefHint: String = "Súradnice (napr. Ján 3:16)",
    val applyCustom: String = "Použiť",

    // Automatic Wallpaper Change strings
    val autoWallpaper: String = "Automatická zmena tapety",
    val autoWallpaperIntervalLabel: String = "Interval zmeny",
    val autoWallpaperEvery1h: String = "Každú hodinu",
    val autoWallpaperEvery2h: String = "Každé 2 hodiny",
    val autoWallpaperEvery3h: String = "Každé 3 hodiny",
    val autoWallpaperEvery6h: String = "Každých 6 hodín",
    val autoWallpaperEvery12h: String = "Každých 12 hodín",
    val autoWallpaperEvery24h: String = "Každých 24 hodín (denne)",
    val autoWallpaperOnScreenOff: String = "Pri vypnutí obrazovky",
    val autoWallpaperOnScreenOffDesc: String = "Verš sa zmení pri každom zamknutí telefónu",
    val autoWallpaperTimeLabel: String = "Čas dennej zmeny",
    val autoWallpaperActiveHourly: String = "Aktívne (každých %sh)",
    val autoWallpaperActiveDaily: String = "Aktívne (denne o %s:00)",
    val autoWallpaperActiveScreenOff: String = "Aktívne (pri zamknutí)",
    val autoWorkerOn: String = "Zapnuté! Tapeta sa zmení podľa nastaveného intervalu.",
    val autoWorkerOff: String = "Automatická zmena bola vypnutá.",

    // Color picker
    val colorPickerTitle: String = "Vybrať farbu",
    val colorPickerBrightness: String = "Jas",
    val colorPickerVerseColor: String = "Farba verša",

    // Wallpaper target
    val wallpaperTargetLabel: String = "Použiť tapetu na",
    val wallpaperTargetLock: String = "Zamknutá",
    val wallpaperTargetHome: String = "Domov",
    val wallpaperTargetBoth: String = "Oboje",

    // Verse database section
    val vdbTitle: String = "Databázy veršov",
    val vdbSubtitle: String = "Vytvor, importuj alebo exportuj zbierky",
    val vdbManage: String = "Spravovať",
    val vdbNewDatabase: String = "Nová databáza",
    val vdbEditPrefix: String = "Upraviť ·",
    val vdbImportExport: String = "Import / Export",
    val vdbCreateNew: String = "Vytvoriť novú",
    val vdbSectionCustom: String = "Vlastné",
    val vdbSectionBuiltIn: String = "Vstavaná",
    val vdbHint: String = "Vlastnú DB vyberieš cez Nastavenia → Jazyk veršov",
    val vdbDeleteTitle: String = "Zmazať",
    val vdbDeleteText: String = "verzov bude natrvalo odstránených.",
    val vdbDelete: String = "Zmazať",
    val vdbDeleted: String = "Zmazané",
    val vdbVerses: String = "veršov",
    val vdbCustomLabel: String = "Vlastné",
    val vdbCodeLabel: String = "Kód",
    val vdbCodePlaceholder: String = "napr. KJV",
    val vdbCodeHint: String = "Vyber v Nastavenia → Jazyk veršov",
    val vdbUpdate: String = "Aktualizovať",
    val vdbSave: String = "Uložiť",
    val vdbNoVerses: String = "Žiadne verše — klepni na + nižšie",
    val vdbVersesFilled: String = "vyplnených",
    val vdbVerse: String = "verš",
    val vdbVersePlural: String = "veršov",
    val vdbClearAll: String = "Vymazať všetko",
    val vdbNoVersesYet: String = "Zatiaľ žiadne verše",
    val vdbAddFirstVerse: String = "Klepni na tlačidlo nižšie a pridaj prvý verš",
    val vdbAddVerse: String = "Pridať verš",
    val vdbVerseText: String = "Text verša",
    val vdbReference: String = "Súradnice",
    val vdbReferencePlaceholder: String = "napr. Ján 3:16",
    val vdbVerseCard: String = "Verš",
    val vdbUpdated: String = "Aktualizovaná",
    val vdbCreated: String = "Vytvorená",
    val vdbErrorCode: String = "Zadaj kód databázy",
    val vdbErrorVerse: String = "Pridaj aspoň jeden verš",
    val vdbErrorFailed: String = "Chyba",
    val vdbImportTitle: String = "Import",
    val vdbImportJson: String = "Importovať JSON súbor",
    val vdbImportDesc: String = "Vyber verse .json zo zariadenia",
    val vdbBrowse: String = "Prehľadávať",
    val vdbImporting: String = "Importujem...",
    val vdbImportCodeTitle: String = "Kód databázy",
    val vdbImportCodeDesc: String = "Zadaj krátky kód pre túto databázu (napr. KJV). Prepíše vstavanú databázu ak sa kód zhoduje.",
    val vdbImport: String = "Importovať",
    val vdbImportSuccess: String = "Importovaných %d veršov ako",
    val vdbImportFailed: String = "Import zlyhal",
    val vdbExportCustom: String = "Export — Vlastné",
    val vdbExportBuiltIn: String = "Export — Vstavaná",
    val vdbExport: String = "Exportovať",
    val vdbExporting: String = "Ukladám do Downloads…",
    val vdbExportDone: String = "Uložené do Downloads",
    val vdbExportFailed: String = "Export zlyhal",
    val vdbJsonFormat: String = "Formát JSON",

    // Backup & Restore
    val backupTitle: String = "Záloha a obnova",
    val backupExport: String = "Zálohovať nastavenia",
    val backupExportDesc: String = "Uloží všetky nastavenia, tapetu a databázy do jedného súboru",
    val backupImport: String = "Obnoviť zo zálohy",
    val backupImportDesc: String = "Nahradí všetky aktuálne nastavenia, tapetu a databázy",
    val backupExporting: String = "Zálohujem…",
    val backupImporting: String = "Obnovujem…",
    val backupExportSuccess: String = "Záloha uložená · %d nastavení, %d databáz",
    val backupImportSuccess: String = "Záloha obnovená · %d nastavení, %d databáz. Reštartujem…",
    val backupExportFailed: String = "Záloha zlyhala",
    val backupImportFailed: String = "Obnova zlyhala",
    val backupConfirmTitle: String = "Obnoviť zo zálohy?",
    val backupConfirmDesc: String = "Toto nahradí všetky tvoje aktuálne nastavenia, tapetu a databázy. Akciu nie je možné vrátiť späť.",

    // Wallpaper gallery screen
    val wpScreenTitle: String = "Tapety",
    val wpScreenSubtitle: String = "Spravuj svoje tapety a automatické striedanie",
    val wpGalleryEmpty: String = "Žiadne tapety",
    val wpGalleryEmptyDesc: String = "Pridaj svoju prvú tapetu — vyber fotku z galérie.",
    val wpAdd: String = "Pridať tapetu",
    val wpSetActive: String = "Použiť",
    val wpActive: String = "Aktívna",
    val wpDelete: String = "Odstrániť",
    val wpDeleteConfirm: String = "Odstrániť túto tapetu?",
    val wpDeleteConfirmDesc: String = "Tapeta bude natrvalo odstránená z aplikácie.",
    val wpCycleTitle: String = "Automatické striedanie",
    val wpCycleDesc: String = "Striedaj tapety automaticky podľa intervalu",
    val wpCycleInterval: String = "Interval striedania",
    val wpCycleDailyHour: String = "Čas dennej zmeny",
    val wpCycleOnScreenOff: String = "Pri zamknutí",
    val wpCycleOnScreenOffDesc: String = "Zmení tapetu pri každom zamknutí telefónu",
    val wpNightMode: String = "Nočný režim",
    val wpNightModeDesc: String = "Použiť inú tapetu v noci",
    val wpNightStart: String = "Začiatok noci",
    val wpNightEnd: String = "Koniec noci",
    val wpActiveBadge: String = "Aktívna",
    val wpNightBadge: String = "Nočná",
    val wpCycleModeVerse: String = "Pri zmene verša",
    val wpCycleModeInterval: String = "Vlastný interval",
    val wpCycleModeScreenOff: String = "Pri zamknutí",
    val wpCycleModeDayNight: String = "Deň / Noc",
    val wpCycleModeVerseDesc: String = "Tapeta sa zmení spolu s veršom",
    val wpCycleModeDayNightDesc: String = "Automaticky prepínať medzi dennou a nočnou tapetou",
    val wpCycleDayWallpaper: String = "Denná tapeta",
    val wpCycleNightWallpaper: String = "Nočná tapeta",
    val wpCycleModeIntervalDesc: String = "Zmena v pevných intervaloch",
    val wpCycleModeScreenOffDesc: String = "Zmena pri každom zamknutí",
    val wpDualLockWarning: String = "Zmena tapety pri vypnutom displeji môže trvať až 3 sekundy.",
    val wpDayStart: String = "Začiatok dňa",
    val wpDayEnd: String = "Koniec dňa",

    val wpViewAll: String = "Zobraziť všetky",
    val wpViewAllTitle: String = "Všetky tapety",
    val wpSelectMode: String = "Vybrať",
    val wpDeleteSelected: String = "Odstrániť vybrané",
    val wpSelected: String = "vybraných",
    val wpDeleteAllConfirm: String = "Odstrániť vybrané tapety?",
    val wpDeleteAllConfirmDesc: String = "Vybrané tapety budú natrvalo odstránené z aplikácie.",
    val wpPageHint: String = "Potiahni doľava pre tapety →",

    // Verse language source toggle (in Settings)
    val vdbSourceDefault: String = "Vstavané",
    val vdbSourceCustom: String = "Vlastné",
    val vdbEmptyCustom: String = "Zatiaľ žiadne vlastné databázy",
    val vdbEmptyCustomDesc: String = "Vytvor si vlastnú zbierku veršov — napríklad pre KJV alebo iný preklad.",
    val vdbEmptyCustomCta: String = "Vytvoriť novú databázu",

    // Duplicate-code warnings + import conflict dialog
    val vdbWarningCodeBuiltin: String = "Kód \"%s\" je už použitý vstavanou databázou. Môžeš ho použiť — vlastná DB bude zdieľať kód.",
    val vdbWarningCodeCustom: String = "Kód \"%s\" už existuje ako vlastná databáza. Uloženie ju prepíše.",
    val vdbImportConflictTitle: String = "Kód už existuje",
    val vdbImportConflictDesc: String = "Vlastná databáza s kódom \"%s\" už existuje. Zadaj iný kód, alebo nech \"%s\" na prepísanie.",
    val vdbImportConflictAction: String = "Importovať s týmto kódom",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    val vdbOverwriteTitle: String = "Prepísať databázu?",
    val vdbOverwriteDesc: String = "Vlastná databáza s kódom \"%s\" už existuje. Uloženie natrvalo prepíše jej verše. Túto akciu nie je možné vrátiť späť.",
    val vdbOverwriteConfirm: String = "Prepísať",

    // --- Share verse as image ---
    val share: String = "Zdieľať",
    val shareDialogTitle: String = "Zdieľať verš",
    val shareDialogDesc: String = "Vyberte akciu",
    val shareAction: String = "Zdieľať",
    val saveToGalleryAction: String = "Uložiť do galérie",
    val shareSuccess: String = "Verš zdieľaný",
    val shareFailed: String = "Nepodarilo sa zdieľať verš",
    val saveSuccess: String = "Verš uložený do galérie",
    val saveFailed: String = "Nepodarilo sa uložiť do galérie",
    val shareImageDesc: String = "Obrázok s veršom na zdieľanie"
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
