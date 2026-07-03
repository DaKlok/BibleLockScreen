package com.daklok.biblelockscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.request.CachePolicy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.daklok.biblelockscreen.ui.theme.BibleLockScreenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// --- TRANSLATIONS ---
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
    val vdbOverwriteConfirm: String = "Prepísať"
)

val skStrings = AppStrings()

val enStrings = AppStrings(
    updateTime = "Time of daily update",
    generatingBtn = "Generating...",
    tapToEdit = "Tap to edit position and size",
    settings = "App Settings",
    dailyWallpaper = "Daily Wallpaper Change",
    active = "Active (every day at %s:00)",
    inactive = "Disabled",
    textCustomization = "Text Customization",
    bold = "Bold",
    shadow = "Shadow",
    textSize = "Text Size",

    textWidth = "Text Width",
    textHeight = "Text Position",
    textAlpha = "Transparency",
    bgBlur = "Background Blur",
    bgDarknessLabel = "Background Darkness",
    anotherPhoto = "Change Photo",
    test = "Generate",
    selectPhotoFirst = "Select a photo first",
    appearance = "Appearance",
    system = "System",
    light = "Light",
    dark = "Dark",
    dynamicColor = "Material You (System Colors)",
    haptics = "Haptic Feedback",
    hapticsDesc = "Vibrations when editing the widget",
    support = "Support",

    supportDesc = "If you like the app and want to support its development, you can buy me a coffee ☕",
    donate = "Donate",
    close = "Close",
    dragHint = "Drag dots for size and width\nDrag the center to move",
    cancel = "Cancel",
    done = "Done",
    clickToSelect = "Tap to select photo",
    loading = "Loading verse...",
    appLanguage = "App Language",
    verseLanguage = "Verse Language",
    language = "Language",
    dailyWorkerOn = "Enabled!\nWallpaper will change tomorrow at %s:00.",
    dailyWorkerOff = "Daily change disabled.",
    generating = "Generating wallpaper...",
    fontModern = "Modern",
    fontBook = "Book",
    fontMono = "Typewriter",
    fontCursive = "Cursive",
    fontLight = "Light",
    fontCondensed = "Condensed",
    batteryWarningTitle = "Battery Warning",
    batteryWarningDesc = "For the automatic wallpaper change to work reliably in the background, please disable battery optimization for this app.",
    batteryWarningButton = "Disable Optimization",
    customVerseTitle = "Custom Text",
    customVerseHint = "Verse text...",
    customRefHint = "Coordinates (e.g. John 3:16)",
    applyCustom = "Apply",
    autoWallpaper = "Automatic Wallpaper Change",
    autoWallpaperIntervalLabel = "Change interval",
    autoWallpaperEvery1h = "Every hour",
    autoWallpaperEvery2h = "Every 2 hours",
    autoWallpaperEvery3h = "Every 3 hours",
    autoWallpaperEvery6h = "Every 6 hours",
    autoWallpaperEvery12h = "Every 12 hours",
    autoWallpaperEvery24h = "Every 24 hours (daily)",
    autoWallpaperOnScreenOff = "On screen off",
    autoWallpaperOnScreenOffDesc = "Verse changes every time you lock the phone",
    autoWallpaperTimeLabel = "Daily change time",
    autoWallpaperActiveHourly = "Active (every %sh)",
    autoWallpaperActiveDaily = "Active (daily at %s:00)",
    autoWallpaperActiveScreenOff = "Active (on lock)",
    autoWorkerOn = "Enabled! Wallpaper will change based on your interval.",
    autoWorkerOff = "Automatic change disabled.",
    colorPickerTitle = "Select color",
    colorPickerBrightness = "Brightness",
    colorPickerVerseColor = "Verse color",
    wallpaperTargetLabel = "Apply wallpaper to",
    wallpaperTargetLock = "Lock screen",
    wallpaperTargetHome = "Home screen",
    wallpaperTargetBoth = "Both",
    vdbTitle = "Verse databases",
    vdbSubtitle = "Create, import or export collections",
    vdbManage = "Manage",
    vdbNewDatabase = "New database",
    vdbEditPrefix = "Edit ·",
    vdbImportExport = "Import / Export",
    vdbCreateNew = "Create new",
    vdbSectionCustom = "Custom",
    vdbSectionBuiltIn = "Built-in",
    vdbHint = "Select a custom DB via Settings → Verse Language",
    vdbDeleteTitle = "Delete",
    vdbDeleteText = "verses will be permanently removed.",
    vdbDelete = "Delete",
    vdbDeleted = "Deleted",
    vdbVerses = "verses",
    vdbCustomLabel = "Custom",
    vdbCodeLabel = "Code",
    vdbCodePlaceholder = "e.g. KJV",
    vdbCodeHint = "Select this in Settings → Verse Language",
    vdbUpdate = "Update",
    vdbSave = "Save",
    vdbNoVerses = "No verses — tap + below to add",
    vdbVersesFilled = "filled",
    vdbVerse = "verse",
    vdbVersePlural = "verses",
    vdbClearAll = "Clear all",
    vdbNoVersesYet = "No verses yet",
    vdbAddFirstVerse = "Tap the button below to add your first verse",
    vdbAddVerse = "Add verse",
    vdbVerseText = "Verse text",
    vdbReference = "Reference",
    vdbReferencePlaceholder = "e.g. John 3:16",
    vdbVerseCard = "Verse",
    vdbUpdated = "Updated",
    vdbCreated = "Created",
    vdbErrorCode = "Enter a database code",
    vdbErrorVerse = "Add at least one verse",
    vdbErrorFailed = "Failed",
    vdbImportTitle = "Import",
    vdbImportJson = "Import JSON file",
    vdbImportDesc = "Pick a verse .json from your device",
    vdbBrowse = "Browse",
    vdbImporting = "Importing...",
    vdbImportCodeTitle = "Database code",
    vdbImportCodeDesc = "Enter a short code for this database (e.g. KJV). It overrides a built-in database if the code matches.",
    vdbImport = "Import",
    vdbImportSuccess = "Imported %d verses as",
    vdbImportFailed = "Import failed",
    vdbExportCustom = "Export — Custom",
    vdbExportBuiltIn = "Export — Built-in",
    vdbExport = "Export",
    vdbExporting = "Saving to Downloads…",
    vdbExportDone = "Saved to Downloads",
    vdbExportFailed = "Export failed",
    vdbJsonFormat = "JSON format",

    // Backup & Restore
    backupTitle = "Backup & Restore",
    backupExport = "Back up settings",
    backupExportDesc = "Saves all settings, wallpaper, and databases to a single file",
    backupImport = "Restore from backup",
    backupImportDesc = "Replaces all current settings, wallpaper, and databases",
    backupExporting = "Backing up…",
    backupImporting = "Restoring…",
    backupExportSuccess = "Backup saved · %d settings, %d databases",
    backupImportSuccess = "Backup restored · %d settings, %d databases. Restarting…",
    backupExportFailed = "Backup failed",
    backupImportFailed = "Restore failed",
    backupConfirmTitle = "Restore from backup?",
    backupConfirmDesc = "This will replace all your current settings, wallpaper, and databases. This cannot be undone.",

    // Wallpaper gallery screen
    wpScreenTitle = "Wallpapers",
    wpScreenSubtitle = "Manage your wallpapers and auto-cycling",
    wpGalleryEmpty = "No wallpapers",
    wpGalleryEmptyDesc = "Add your first wallpaper — pick a photo from your gallery.",
    wpAdd = "Add wallpaper",
    wpSetActive = "Set active",
    wpActive = "Active",
    wpDelete = "Remove",
    wpDeleteConfirm = "Remove this wallpaper?",
    wpDeleteConfirmDesc = "The wallpaper will be permanently removed from the app.",
    wpCycleTitle = "Auto-cycling",
    wpCycleDesc = "Automatically cycle through wallpapers on a schedule",
    wpCycleInterval = "Cycle interval",
    wpCycleDailyHour = "Daily change time",
    wpCycleOnScreenOff = "On lock",
    wpCycleOnScreenOffDesc = "Change wallpaper every time you lock the phone",
    wpNightMode = "Night mode",
    wpNightModeDesc = "Use a different wallpaper at night",
    wpNightStart = "Night starts",
    wpNightEnd = "Night ends",
    wpActiveBadge = "Active",
    wpNightBadge = "Night",
    wpCycleModeVerse = "On verse change",
    wpCycleModeInterval = "Custom interval",
    wpCycleModeScreenOff = "On every lock",
    wpCycleModeDayNight = "Day / Night",
    wpCycleModeVerseDesc = "Wallpaper changes with the verse",
    wpCycleModeDayNightDesc = "Automatically switch between day and night wallpaper",
    wpCycleDayWallpaper = "Day wallpaper",
    wpCycleNightWallpaper = "Night wallpaper",
    wpCycleModeIntervalDesc = "Change at fixed intervals",
    wpCycleModeScreenOffDesc = "Change on every screen lock",
    wpDualLockWarning = "It could take up to 3 seconds to update wallpaper while screen is off.",
    wpDayStart = "Day starts",
    wpDayEnd = "Day ends",

    wpViewAll = "View all",
    wpViewAllTitle = "All wallpapers",
    wpSelectMode = "Select",
    wpDeleteSelected = "Delete selected",
    wpSelected = "selected",
    wpDeleteAllConfirm = "Delete selected wallpapers?",
    wpDeleteAllConfirmDesc = "The selected wallpapers will be permanently removed from the app.",
    wpPageHint = "Swipe left for wallpapers →",

    // Verse language source toggle (in Settings)
    vdbSourceDefault = "Default",
    vdbSourceCustom = "Custom",
    vdbEmptyCustom = "No custom databases yet",
    vdbEmptyCustomDesc = "Create your own verse collection — for example KJV or another translation.",
    vdbEmptyCustomCta = "Create new database",

    // Duplicate-code warnings + import conflict dialog
    vdbWarningCodeBuiltin = "Code \"%s\" is already used by a built-in database. You can still use it — the custom DB will share the code.",
    vdbWarningCodeCustom = "Code \"%s\" already exists as a custom database. Saving will overwrite it.",
    vdbImportConflictTitle = "Code already exists",
    vdbImportConflictDesc = "A custom database with code \"%s\" already exists. Enter a different code, or keep \"%s\" to overwrite.",
    vdbImportConflictAction = "Import with this code",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    vdbOverwriteTitle = "Overwrite database?",
    vdbOverwriteDesc = "A custom database with code \"%s\" already exists. Saving will permanently replace its verses. This action cannot be undone.",
    vdbOverwriteConfirm = "Overwrite"
)

val czStrings = AppStrings(
    updateTime = "Čas denní aktualizace",
    settings = "Nastavení aplikace",
    dailyWallpaper = "Denní změna tapety",
    active = "Aktivní (každý den v %s:00)",
    inactive = "Vypnuto",
    textCustomization = "Přizpůsobení textu",
    bold = "Tučné",
    shadow = "Stín",
    textSize = "Velikost písma",
    textWidth = "Šířka textu",
    textHeight = "Výška textu",
    textAlpha = "Průhlednost",
    bgBlur = "Rozmazání pozadí",
    bgDarknessLabel = "Ztmavení pozadí",
    anotherPhoto = "Jiná fotka",
    test = "Generovat",
    selectPhotoFirst = "Nejdříve vyber fotku",
    appearance = "Vzhled",
    system = "Systém",
    light = "Světlý",
    dark = "Tmavý",
    dynamicColor = "Material You (Barvy systému)",
    haptics = "Haptická odezva",
    hapticsDesc = "Vibrace při úpravě widgetu",
    support = "Podpora",
    supportDesc = "Pokud se ti aplikace líbí a chceš podpořit její vývoj, můžeš mi koupit kávu ☕",
    donate = "Přispět",

    close = "Zavřít",
    dragHint = "Potažením bodů změň velikost\nTahem ve středu widget přesuň",
    cancel = "Zrušit",
    done = "Hotovo",
    clickToSelect = "Klikni pro výběr fotky",
    loading = "Načítám verš...",
    appLanguage = "Jazyk aplikace",
    verseLanguage = "Jazyk veršů",
    language = "Jazyk",
    dailyWorkerOn = "Zapnuto!\nTapeta se změní zítra v %s:00.",
    dailyWorkerOff = "Denní změna byla vypnuta.",
    generating = "Tapeta se generuje...",
    fontModern = "Moderní",

    fontBook = "Knižní",
    fontMono = "Strojový",
    fontCursive = "Psaný",
    fontLight = "Tenký",
    fontCondensed = "Úzký",
    generatingBtn = "Generuje se...",
    tapToEdit = "Kliknutím upravíš polohu a velikost",
    batteryWarningTitle = "Upozornění na baterii",
    batteryWarningDesc = "Aby automatická změna tapety fungovala spolehlivě na pozadí, vypněte prosím optimalizaci baterie pro tuto aplikaci.",
    batteryWarningButton = "Vypnout optimalizaci",
    customVerseTitle = "Vlastní text",
    customVerseHint = "Text verše...",
    customRefHint = "Souřadnice (např. Jan 3:16)",
    applyCustom = "Použít",
    autoWallpaper = "Automatická změna tapety",
    autoWallpaperIntervalLabel = "Interval změny",
    autoWallpaperEvery1h = "Každou hodinu",
    autoWallpaperEvery2h = "Každé 2 hodiny",
    autoWallpaperEvery3h = "Každé 3 hodiny",
    autoWallpaperEvery6h = "Každých 6 hodin",
    autoWallpaperEvery12h = "Každých 12 hodin",
    autoWallpaperEvery24h = "Každých 24 hodin (denně)",
    autoWallpaperOnScreenOff = "Při vypnutí obrazovky",
    autoWallpaperOnScreenOffDesc = "Verš se změní při každém zamknutí telefonu",
    autoWallpaperTimeLabel = "Čas denní změny",
    autoWallpaperActiveHourly = "Aktivní (každých %sh)",
    autoWallpaperActiveDaily = "Aktivní (denně v %s:00)",
    autoWallpaperActiveScreenOff = "Aktivní (při zamknutí)",
    autoWorkerOn = "Zapnuto! Tapeta se změní dle nastaveného intervalu.",
    autoWorkerOff = "Automatická změna byla vypnuta.",
    colorPickerTitle = "Vybrat barvu",
    colorPickerBrightness = "Jas",
    colorPickerVerseColor = "Barva verše",

    // Wallpaper target
    wallpaperTargetLabel = "Použít tapetu na",
    wallpaperTargetLock = "Zamčená",
    wallpaperTargetHome = "Domů",
    wallpaperTargetBoth= "Oboje",

    // Verse database section
    vdbTitle = "Databáze veršů",
    vdbSubtitle = "Vytvoř, importuj nebo exportuj kolekce",
    vdbManage = "Spravovat",
    vdbNewDatabase = "Nová databáze",
    vdbEditPrefix = "Upravit ·",
    vdbImportExport = "Import / Export",
    vdbCreateNew = "Vytvořit novou",
    vdbSectionCustom = "Vlastní",
    vdbSectionBuiltIn = "Vestavěná",
    vdbHint = "Vlastní DB vyberete v Nastavení → Jazyk veršů",
    vdbDeleteTitle = "Smazat",
    vdbDeleteText = "veršů bude trvale odstraněno.",
    vdbDelete = "Smazat",
    vdbDeleted = "Smazáno",
    vdbVerses = "veršů",
    vdbCustomLabel = "Vlastní",
    vdbCodeLabel = "Kód",
    vdbCodePlaceholder = "např. KJV",
    vdbCodeHint = "Vyber v Nastavení → Jazyk veršů",
    vdbUpdate = "Aktualizovat",
    vdbSave = "Uložit",
    vdbNoVerses = "Žádné verše — klepni na + níže",
    vdbVersesFilled = "vyplněno",
    vdbVerse = "verš",
    vdbVersePlural = "veršů",
    vdbClearAll = "Vymazat vše",
    vdbNoVersesYet = "Zatím žádné verše",
    vdbAddFirstVerse = "Klepnutím na tlačítko níže přidáš první verš",
    vdbAddVerse = "Přidat verš",
    vdbVerseText = "Text verše",
    vdbReference = "Souřadnice",
    vdbReferencePlaceholder = "např. Jan 3:16",
    vdbVerseCard = "Verš",
    vdbUpdated = "Aktualizováno",
    vdbCreated = "Vytvořeno",
    vdbErrorCode = "Zadej kód databáze",
    vdbErrorVerse = "Přidej alespoň jeden verš",
    vdbErrorFailed = "Chyba",
    vdbImportTitle = "Import",
    vdbImportJson = "Importovat JSON soubor",
    vdbImportDesc = "Vyber verse .json ze zařízení",
    vdbBrowse = "Procházet",
    vdbImporting = "Importuji...",
    vdbImportCodeTitle = "Kód databáze",
    vdbImportCodeDesc = "Zadej krátký kód pro tuto databázi (např. KJV). Přepíše vestavěnou databázi, pokud se kód shoduje.",
    vdbImport = "Importovat",
    vdbImportSuccess = "Importováno %d veršů jako",
    vdbImportFailed = "Import selhal",
    vdbExportCustom = "Export — Vlastní",
    vdbExportBuiltIn = "Export — Vestavěná",
    vdbExport = "Exportovat",
    vdbExporting = "Ukládám do Downloads…",
    vdbExportDone = "Uloženo do Downloads",
    vdbExportFailed = "Export selhal",
    vdbJsonFormat = "Formát JSON",

    // Backup & Restore
    backupTitle = "Záloha a obnova",
    backupExport = "Zálohovat nastavení",
    backupExportDesc = "Uloží všechna nastavení, tapetu a databáze do jednoho souboru",
    backupImport = "Obnovit ze zálohy",
    backupImportDesc = "Nahradí všechna aktuální nastavení, tapetu a databáze",
    backupExporting = "Zálohuji…",
    backupImporting = "Obnovuji…",
    backupExportSuccess = "Záloha uložena · %d nastavení, %d databází",
    backupImportSuccess = "Záloha obnovena · %d nastavení, %d databází. Restartuji…",
    backupExportFailed = "Záloha selhala",
    backupImportFailed = "Obnova selhala",
    backupConfirmTitle = "Obnovit ze zálohy?",
    backupConfirmDesc = "Toto nahradí všechna tvá aktuální nastavení, tapetu a databáze. Akci nelze vrátit zpět.",

    // Wallpaper gallery screen
    wpScreenTitle = "Tapety",
    wpScreenSubtitle = "Spravuj své tapety a automatické střídání",
    wpGalleryEmpty = "Žádné tapety",
    wpGalleryEmptyDesc = "Přidej svou první tapetu — vyber fotku z galerie.",
    wpAdd = "Přidat tapetu",
    wpSetActive = "Použít",
    wpActive = "Aktivní",
    wpDelete = "Odstranit",
    wpDeleteConfirm = "Odstranit tuto tapetu?",
    wpDeleteConfirmDesc = "Tapeta bude trvale odstraněna z aplikace.",
    wpCycleTitle = "Automatické střídání",
    wpCycleDesc = "Strídej tapety automaticky podle intervalu",
    wpCycleInterval = "Interval střídání",
    wpCycleDailyHour = "Čas denní změny",
    wpCycleOnScreenOff = "Při zamčení",
    wpCycleOnScreenOffDesc = "Změní tapetu při každém zamčení telefonu",
    wpNightMode = "Noční režim",
    wpNightModeDesc = "Použít jinou tapetu v noci",
    wpNightStart = "Začátek noci",
    wpNightEnd = "Konec noci",
    wpActiveBadge = "Aktivní",
    wpNightBadge = "Noční",
    wpCycleModeVerse = "Při změně verše",
    wpCycleModeInterval = "Vlastní interval",
    wpCycleModeScreenOff = "Při zamčení",
    wpCycleModeDayNight = "Den / Noc",
    wpCycleModeVerseDesc = "Tapeta se změní spolu s veršem",
    wpCycleModeDayNightDesc = "Automaticky přepínat mezi denní a noční tapetou",
    wpCycleDayWallpaper = "Denní tapeta",
    wpCycleNightWallpaper = "Noční tapeta",
    wpCycleModeIntervalDesc = "Změna v pevných intervalech",
    wpCycleModeScreenOffDesc = "Změna při každém zamčení",
    wpDualLockWarning = "Změna tapety při vypnutém displeji může trvat až 3 sekundy.",
    wpDayStart = "Začátek dne",
    wpDayEnd = "Konec dne",

    wpViewAll = "Zobrazit vše",
    wpViewAllTitle = "Všechny tapety",
    wpSelectMode = "Vybrat",
    wpDeleteSelected = "Smazat vybrané",
    wpSelected = "vybraných",
    wpDeleteAllConfirm = "Smazat vybrané tapety?",
    wpDeleteAllConfirmDesc = "Vybrané tapety budou trvale odstraněny z aplikace.",
    wpPageHint = "Potáhni doleva pro tapety →",

    // Verse language source toggle (in Settings)
    vdbSourceDefault = "Vestavěné",
    vdbSourceCustom = "Vlastní",
    vdbEmptyCustom = "Zatím žádné vlastní databáze",
    vdbEmptyCustomDesc = "Vytvoř si vlastní sbírku veršů — třeba pro KJV nebo jiný překlad.",
    vdbEmptyCustomCta = "Vytvořit novou databázi",

    // Duplicate-code warnings + import conflict dialog
    vdbWarningCodeBuiltin = "Kód \"%s\" je již použit vestavěnou databází. Můžeš ho použít — vlastní DB bude sdílet kód.",
    vdbWarningCodeCustom = "Kód \"%s\" již existuje jako vlastní databáze. Uložení ji přepíše.",
    vdbImportConflictTitle = "Kód již existuje",
    vdbImportConflictDesc = "Vlastní databáze s kódem \"%s\" již existuje. Zadej jiný kód, nebo nech \"%s\" pro přepsání.",
    vdbImportConflictAction = "Importovat s tímto kódem",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    vdbOverwriteTitle = "Přepsat databázi?",
    vdbOverwriteDesc = "Vlastní databáze s kódem \"%s\" již existuje. Uložení trvale přepíše její verše. Tuto akci nelze vrátit zpět.",
    vdbOverwriteConfirm = "Přepsat"
)

val esStrings = AppStrings(
    updateTime = "Hora de actualización diaria",
    settings = "Ajustes de la App",
    dailyWallpaper = "Cambio diario de fondo",
    active = "Activo (todos los días a las %s:00)",
    inactive = "Desactivado",
    textCustomization = "Personalización de texto",
    bold = "Negrita",
    shadow = "Sombra",
    textSize = "Tamaño del texto",
    textWidth = "Ancho del texto",
    textHeight = "Altura del texto",
    textAlpha = "Transparencia",

    bgBlur = "Desenfocar fondo",
    bgDarknessLabel = "Oscurecer el fondo",
    anotherPhoto = "Cambiar foto",
    test = "Generar",
    selectPhotoFirst = "Selecciona una foto primero",
    appearance = "Apariencia",
    system = "Sistema",
    light = "Claro",
    dark = "Oscuro",
    dynamicColor = "Material You (Colores del sistema)",
    haptics = "Respuesta háptica",
    hapticsDesc = "Vibraciones al editar el widget",
    support = "Soporte",
    supportDesc = "Si te gusta la app y quieres apoyar su desarrollo, puedes comprarme un café ☕",
    donate = "Donar",
    close = "Cerrar",
    dragHint = "Arrastra los puntos para el tamaño y ancho\nArrastra en el centro para mover",
    cancel = "Cancelar",
    done = "Hecho",
    clickToSelect = "Toca para seleccionar una foto",
    loading = "Cargando versículo...",
    appLanguage = "Idioma de la aplicación",
    verseLanguage = "Idioma del versículo",
    language = "Idioma",
    dailyWorkerOn = "¡Activado!\nEl fondo cambiará mañana a las %s:00.",

    dailyWorkerOff = "Cambio diario desactivado.",
    generating = "Generando fondo...",
    fontModern = "Moderno",
    fontBook = "Libro",
    fontMono = "Máquina de escribir",
    fontCursive = "Cursiva",
    fontLight = "Fino",
    fontCondensed = "Condensada",
    generatingBtn = "Generando...",
    tapToEdit = "Toca para editar la posición y el tamaño",
    batteryWarningTitle = "Advertencia de Batería",
    batteryWarningDesc = "Para que el cambio automático de fondo funcione de manera confiable, desactiva la optimización de batería para esta aplicación.",
    batteryWarningButton = "Desactivar Optimización",
    customVerseTitle = "Texto Personalizado",
    customVerseHint = "Texto del versículo...",
    customRefHint = "Coordenadas (ej. Juan 3:16)",
    applyCustom = "Aplicar",
    autoWallpaper = "Cambio automático de fondo",
    autoWallpaperIntervalLabel = "Intervalo de cambio",
    autoWallpaperEvery1h = "Cada hora",
    autoWallpaperEvery2h = "Cada 2 horas",
    autoWallpaperEvery3h = "Cada 3 horas",
    autoWallpaperEvery6h = "Cada 6 horas",
    autoWallpaperEvery12h = "Cada 12 horas",
    autoWallpaperEvery24h = "Cada 24 horas (diario)",
    autoWallpaperOnScreenOff = "Al apagar la pantalla",
    autoWallpaperOnScreenOffDesc = "El versículo cambia cada vez que bloqueas el teléfono",
    autoWallpaperTimeLabel = "Hora del cambio diario",
    autoWallpaperActiveHourly = "Activo (cada %sh)",
    autoWallpaperActiveDaily = "Activo (diario a las %s:00)",
    autoWallpaperActiveScreenOff = "Activo (al bloquear)",
    autoWorkerOn = "¡Activado! El fondo cambiará según el intervalo configurado.",
    autoWorkerOff = "Cambio automático desactivado.",
    colorPickerTitle = "Seleccionar color",
    colorPickerBrightness = "Brillo",
    colorPickerVerseColor = "Color del versículo",

    // Wallpaper target
    wallpaperTargetLabel = "Aplicar fondo en",
    wallpaperTargetLock = "Bloqueo",
    wallpaperTargetHome = "Inicio",
    wallpaperTargetBoth = "Ambos",

    // Verse database section
    vdbTitle = "Bases de datos de versículos",
    vdbSubtitle = "Crea, importa o exporta colecciones",
    vdbManage = "Gestionar",
    vdbNewDatabase = "Nueva base de datos",
    vdbEditPrefix = "Editar ·",
    vdbImportExport = "Importar / Exportar",
    vdbCreateNew = "Crear nueva",
    vdbSectionCustom = "Personalizada",
    vdbSectionBuiltIn = "Integrada",
    vdbHint = "Selecciona una DB personalizada en Ajustes → Idioma de versículos",
    vdbDeleteTitle = "Eliminar",
    vdbDeleteText = "versículos se eliminarán permanentemente.",
    vdbDelete = "Eliminar",
    vdbDeleted = "Eliminado",
    vdbVerses = "versículos",
    vdbCustomLabel = "Personalizada",
    vdbCodeLabel = "Código",
    vdbCodePlaceholder = "ej. RVR",
    vdbCodeHint = "Selecciónalo en Ajustes → Idioma de versículos",
    vdbUpdate = "Actualizar",
    vdbSave = "Guardar",
    vdbNoVerses = "Sin versículos — toca + abajo",
    vdbVersesFilled = "completados",
    vdbVerse = "versículo",
    vdbVersePlural = "versículos",
    vdbClearAll = "Borrar todo",
    vdbNoVersesYet = "Aún no hay versículos",
    vdbAddFirstVerse = "Toca el botón de abajo para añadir tu primer versículo",
    vdbAddVerse = "Añadir versículo",
    vdbVerseText = "Texto del versículo",
    vdbReference = "Referencia",
    vdbReferencePlaceholder = "ej. Juan 3:16",
    vdbVerseCard = "Versículo",
    vdbUpdated = "Actualizada",
    vdbCreated = "Creada",
    vdbErrorCode = "Introduce un código de base de datos",
    vdbErrorVerse = "Añade al menos un versículo",
    vdbErrorFailed = "Error",
    vdbImportTitle = "Importar",
    vdbImportJson = "Importar archivo JSON",
    vdbImportDesc = "Selecciona un .json de versículos del dispositivo",
    vdbBrowse = "Examinar",
    vdbImporting = "Importando...",
    vdbImportCodeTitle = "Código de base de datos",
    vdbImportCodeDesc = "Introduce un código corto para esta base de datos (ej. RVR). Sustituye la base de datos integrada si el código coincide.",
    vdbImport = "Importar",
    vdbImportSuccess = "Importados %d versículos como",
    vdbImportFailed = "Error al importar",
    vdbExportCustom = "Exportar — Personalizada",
    vdbExportBuiltIn = "Exportar — Integrada",
    vdbExport = "Exportar",
    vdbExporting = "Guardando en Descargas…",
    vdbExportDone = "Guardado en Descargas",
    vdbExportFailed = "Error al exportar",
    vdbJsonFormat = "Formát JSON",

    // Backup & Restore
    backupTitle = "Copia de seguridad y restauración",
    backupExport = "Hacer copia de seguridad",
    backupExportDesc = "Guarda todos los ajustes, fondo de pantalla y bases de datos en un solo archivo",
    backupImport = "Restaurar desde copia de seguridad",
    backupImportDesc = "Reemplaza todos los ajustes, fondo de pantalla y bases de datos actuales",
    backupExporting = "Haciendo copia…",
    backupImporting = "Restaurando…",
    backupExportSuccess = "Copia guardada · %d ajustes, %d bases de datos",
    backupImportSuccess = "Copia restaurada · %d ajustes, %d bases de datos. Reiniciando…",
    backupExportFailed = "La copia de seguridad falló",
    backupImportFailed = "La restauración falló",
    backupConfirmTitle = "¿Restaurar desde copia de seguridad?",
    backupConfirmDesc = "Esto reemplazará todos tus ajustes, fondo de pantalla y bases de datos actuales. No se puede deshacer.",

    // Wallpaper gallery screen
    wpScreenTitle = "Fondos de pantalla",
    wpScreenSubtitle = "Gestiona tus fondos y la rotación automática",
    wpGalleryEmpty = "Sin fondos de pantalla",
    wpGalleryEmptyDesc = "Añade tu primer fondo — elige una foto de la galería.",
    wpAdd = "Añadir fondo",
    wpSetActive = "Usar",
    wpActive = "Activo",
    wpDelete = "Eliminar",
    wpDeleteConfirm = "¿Eliminar este fondo?",
    wpDeleteConfirmDesc = "El fondo se eliminará permanentemente de la app.",
    wpCycleTitle = "Rotación automática",
    wpCycleDesc = "Cambia los fondos automáticamente según un intervalo",
    wpCycleInterval = "Intervalo de rotación",
    wpCycleDailyHour = "Hora de cambio diario",
    wpCycleOnScreenOff = "Al bloquear",
    wpCycleOnScreenOffDesc = "Cambia el fondo cada vez que bloqueas el teléfono",
    wpNightMode = "Modo nocturno",
    wpNightModeDesc = "Usar un fondo diferente por la noche",
    wpNightStart = "Inicio de la noche",
    wpNightEnd = "Fin de la noche",
    wpActiveBadge = "Activo",
    wpNightBadge = "Noche",
    wpCycleModeVerse = "Al cambiar versículo",
    wpCycleModeInterval = "Intervalo personalizado",
    wpCycleModeScreenOff = "Al bloquear",
    wpCycleModeDayNight = "Día / Noc",
    wpCycleModeVerseDesc = "El fondo cambia con el versículo",
    wpCycleModeDayNightDesc = "Cambiar automáticamente entre fondo de día y de noche",
    wpCycleDayWallpaper = "Fondo de día",
    wpCycleNightWallpaper = "Fondo de noche",
    wpCycleModeIntervalDesc = "Cambiar en intervalos fijos",
    wpCycleModeScreenOffDesc = "Cambiar en cada bloqueo",
    wpDualLockWarning = "El cambio de fondo con la pantalla apagada puede tardar hasta 3 segundos.",
    wpDayStart = "El día empieza",
    wpDayEnd = "El día termina",

    wpViewAll = "Ver todo",
    wpViewAllTitle = "Todos los fondos",
    wpSelectMode = "Seleccionar",
    wpDeleteSelected = "Eliminar seleccionados",
    wpSelected = "seleccionados",
    wpDeleteAllConfirm = "¿Eliminar fondos seleccionados?",
    wpDeleteAllConfirmDesc = "Los fondos seleccionados se eliminarán permanentemente de la app.",
    wpPageHint = "Desliza a la izquierda para fondos →",

    // Verse language source toggle (in Settings)
    vdbSourceDefault = "Predeterminadas",
    vdbSourceCustom = "Personalizadas",
    vdbEmptyCustom = "Aún no hay bases de datos personalizadas",
    vdbEmptyCustomDesc = "Crea tu propia colección de versículos, por ejemplo KJV u otra traducción.",
    vdbEmptyCustomCta = "Crear nueva base de datos",

    // Duplicate-code warnings + import conflict dialog
    vdbWarningCodeBuiltin = "El código \"%s\" ya lo usa una base de datos integrada. Puedes usarlo — la base personalizada compartirá el código.",
    vdbWarningCodeCustom = "El código \"%s\" ya existe como base de datos personalizada. Guardar la sobrescribirá.",
    vdbImportConflictTitle = "El código ya existe",
    vdbImportConflictDesc = "Ya existe una base de datos personalizada con el código \"%s\". Introduce un código distinto, o deja \"%s\" para sobrescribir.",
    vdbImportConflictAction = "Importar con este código",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    vdbOverwriteTitle = "¿Sobrescribir base de datos?",
    vdbOverwriteDesc = "Ya existe una base de datos personalizada con el código \"%s\". Guardar reemplazará permanentemente sus versículos. Esta acción no se puede deshacer.",
    vdbOverwriteConfirm = "Sobrescribir"
)

val itStrings = AppStrings(
    updateTime = "Ora della modifica giornaliera",
    settings = "Impostazioni App",
    dailyWallpaper = "Sfondo giornaliero",
    active = "Attivo (ogni giorno alle %s:00)",
    inactive = "Inattivo",
    textCustomization = "Personalizzazione testo",
    bold = "Grassetto",
    shadow = "Ombra",
    textSize = "Dimensione testo",
    textWidth = "Larghezza testo",
    textHeight = "Altezza testo",
    textAlpha = "Trasparenza",
    bgBlur = "Sfocatura sfondo",
    bgDarknessLabel = "Oscuramento dello sfondo",
    anotherPhoto = "Cambia foto",
    test = "Generare",
    selectPhotoFirst = "Seleziona prima una foto",
    appearance = "Aspetto",
    system = "Sistema",
    light = "Chiaro",
    dark = "Scuro",
    dynamicColor = "Material You (Colori di sistema)",
    haptics = "Feedback tattile",
    hapticsDesc = "Vibrazioni durante la modifica",
    support = "Supporto",
    supportDesc = "Se ti piace l'app e vuoi supportarne lo sviluppo, puoi offrirmi un caffè ☕",
    donate = "Dona",

    close = "Chiudi",
    dragHint = "Trascina i punti per la dimensione e la larghezza\nTrascina al centro per spostare",
    cancel = "Annulla",
    done = "Fatto",
    clickToSelect = "Tocca per selezionare una foto",
    loading = "Caricamento versetto...",
    appLanguage = "Lingua dell'app",
    verseLanguage = "Lingua del versetto",
    language = "Lingua",
    dailyWorkerOn = "Attivato!\nLo sfondo cambierà domani alle %s:00.",
    dailyWorkerOff = "Cambio giornaliero disattivato.",
    generating = "Generazione sfondo...",

    fontModern = "Moderno",
    fontBook = "Libro",
    fontMono = "Macchina da scrivere",
    fontCursive = "Corsivo",
    fontLight = "Sottile",
    fontCondensed = "Condensato",
    generatingBtn = "Generazione...",
    tapToEdit = "Tocca per cambiare posizione e dimensione",
    batteryWarningTitle = "Avviso Batteria",
    batteryWarningDesc = "Affinché il cambio automatico dello sfondo funzioni in modo affidabile, disabilita l'ottimizzazione della batteria per questa app.",
    batteryWarningButton = "Disabilita Ottimizzazione",
    customVerseTitle = "Testo Personalizzato",
    customVerseHint = "Testo del versetto...",
    customRefHint = "Coordinate (es. Giovanni 3:16)",
    applyCustom = "Applica",
    autoWallpaper = "Cambio automatico sfondo",
    autoWallpaperIntervalLabel = "Intervallo di cambio",
    autoWallpaperEvery1h = "Ogni ora",
    autoWallpaperEvery2h = "Ogni 2 ore",
    autoWallpaperEvery3h = "Ogni 3 ore",
    autoWallpaperEvery6h = "Ogni 6 ore",
    autoWallpaperEvery12h = "Ogni 12 ore",
    autoWallpaperEvery24h = "Ogni 24 ore (giornaliero)",
    autoWallpaperOnScreenOff = "Allo spegnimento schermo",
    autoWallpaperOnScreenOffDesc = "Il versetto cambia ogni volta che blocchi il telefono",
    autoWallpaperTimeLabel = "Orario cambio giornaliero",
    autoWallpaperActiveHourly = "Attivo (ogni %sh)",
    autoWallpaperActiveDaily = "Attivo (giornaliero alle %s:00)",
    autoWallpaperActiveScreenOff = "Attivo (al blocco)",
    autoWorkerOn = "Attivato! Lo sfondo cambierà in base all'intervallo impostato.",
    autoWorkerOff = "Cambio automatico disattivato.",
    colorPickerTitle = "Seleziona colore",
    colorPickerBrightness = "Luminosità",
    colorPickerVerseColor = "Colore del versetto",

    // Wallpaper target
    wallpaperTargetLabel = "Applica sfondo su",
    wallpaperTargetLock = "Blocco",
    wallpaperTargetHome = "Home",
    wallpaperTargetBoth = "Entrambi",

    // Verse database section
    vdbTitle = "Database di versetti",
    vdbSubtitle = "Crea, importa o esporta raccolte",
    vdbManage = "Gestisci",
    vdbNewDatabase = "Nuovo database",
    vdbEditPrefix = "Modifica ·",
    vdbImportExport = "Importa / Esporta",
    vdbCreateNew = "Crea nuovo",
    vdbSectionCustom = "Personalizzato",
    vdbSectionBuiltIn = "Integrato",
    vdbHint = "Seleziona un DB personalizzato in Impostazioni → Lingua versetti",
    vdbDeleteTitle = "Elimina",
    vdbDeleteText = "versetti saranno eliminati definitivamente.",
    vdbDelete = "Elimina",
    vdbDeleted = "Eliminato",
    vdbVerses = "versetti",
    vdbCustomLabel = "Personalizzato",
    vdbCodeLabel = "Codice",
    vdbCodePlaceholder = "es. CEI",
    vdbCodeHint = "Selezionalo in Impostazioni → Lingua versetti",
    vdbUpdate = "Aggiorna",
    vdbSave = "Salva",
    vdbNoVerses = "Nessun versetto — tocca + in basso",
    vdbVersesFilled = "compilati",
    vdbVerse = "versetto",
    vdbVersePlural = "versetti",
    vdbClearAll = "Cancella tutto",
    vdbNoVersesYet = "Ancora nessun versetto",
    vdbAddFirstVerse = "Tocca il pulsante in basso per aggiungere il primo versetto",
    vdbAddVerse = "Aggiungi versetto",
    vdbVerseText = "Testo del versetto",
    vdbReference = "Riferimento",
    vdbReferencePlaceholder = "es. Giovanni 3:16",
    vdbVerseCard = "Versetto",
    vdbUpdated = "Aggiornato",
    vdbCreated = "Creato",
    vdbErrorCode = "Inserisci un codice per il database",
    vdbErrorVerse = "Aggiungi almeno un versetto",
    vdbErrorFailed = "Errore",
    vdbImportTitle = "Importa",
    vdbImportJson = "Importa file JSON",
    vdbImportDesc = "Scegli un .json di versetti dal dispositivo",
    vdbBrowse = "Sfoglia",
    vdbImporting = "Importazione...",
    vdbImportCodeTitle = "Codice database",
    vdbImportCodeDesc = "Inserisci un codice breve per questo database (es. CEI). Sostituisce il database integrato se il codice corrisponde.",
    vdbImport = "Importa",
    vdbImportSuccess = "Importati %d versetti come",
    vdbImportFailed = "Importazione fallita",
    vdbExportCustom = "Esporta — Personalizzato",
    vdbExportBuiltIn = "Esporta — Integrato",
    vdbExport = "Esporta",
    vdbExporting = "Salvataggio in Download…",
    vdbExportDone = "Salvato in Download",
    vdbExportFailed = "Esportazione fallita",
    vdbJsonFormat = "Formato JSON",

    // Backup & Restore
    backupTitle = "Backup e ripristino",
    backupExport = "Esegui backup delle impostazioni",
    backupExportDesc = "Salva tutte le impostazioni, lo sfondo e i database in un unico file",
    backupImport = "Ripristina da backup",
    backupImportDesc = "Sostituisce tutte le impostazioni, lo sfondo e i database attuali",
    backupExporting = "Backup in corso…",
    backupImporting = "Ripristino in corso…",
    backupExportSuccess = "Backup salvato · %d impostazioni, %d database",
    backupImportSuccess = "Backup ripristinato · %d impostazioni, %d database. Riavvio…",
    backupExportFailed = "Backup fallito",
    backupImportFailed = "Ripristino fallito",
    backupConfirmTitle = "Ripristinare dal backup?",
    backupConfirmDesc = "Verranno sostituite tutte le impostazioni, lo sfondo e i database attuali. Operazione irreversibile.",

    // Wallpaper gallery screen
    wpScreenTitle = "Sfondi",
    wpScreenSubtitle = "Gestisci i tuoi sfondi e la rotazione automatica",
    wpGalleryEmpty = "Nessuno sfondo",
    wpGalleryEmptyDesc = "Aggiungi il tuo primo sfondo — scegli una foto dalla galleria.",
    wpAdd = "Aggiungi sfondo",
    wpSetActive = "Usa",
    wpActive = "Attivo",
    wpDelete = "Rimuovi",
    wpDeleteConfirm = "Rimuovere questo sfondo?",
    wpDeleteConfirmDesc = "Lo sfondo verrà rimosso definitivamente dall'app.",
    wpCycleTitle = "Rotazione automatica",
    wpCycleDesc = "Cambia sfondo automaticamente secondo un intervallo",
    wpCycleInterval = "Intervallo di rotazione",
    wpCycleDailyHour = "Ora cambio giornaliero",
    wpCycleOnScreenOff = "Al blocco",
    wpCycleOnScreenOffDesc = "Cambia sfondo ogni volta che blocchi il telefono",
    wpNightMode = "Modalità notturna",
    wpNightModeDesc = "Usa uno sfondo diverso di notte",
    wpNightStart = "Inizio notte",
    wpNightEnd = "Fine notte",
    wpActiveBadge = "Attivo",
    wpNightBadge = "Notte",
    wpCycleModeVerse = "Al cambio versetto",
    wpCycleModeInterval = "Intervallo personalizzato",
    wpCycleModeScreenOff = "Ad ogni blocco",
    wpCycleModeDayNight = "Giorno / Notte",
    wpCycleModeVerseDesc = "Lo sfondo cambia con il versetto",
    wpCycleModeDayNightDesc = "Cambia automaticamente tra sfondo di giorno e di notte",
    wpCycleDayWallpaper = "Sfondo di giorno",
    wpCycleNightWallpaper = "Sfondo di notte",
    wpCycleModeIntervalDesc = "Cambia a intervalli fissi",
    wpCycleModeScreenOffDesc = "Cambia ad ogni blocco",
    wpDualLockWarning = "Il cambio sfondo a schermo spento può richiedere fino a 3 secondi.",
    wpDayStart = "Il giorno inizia",
    wpDayEnd = "Il giorno finisce",

    wpViewAll = "Vedi tutti",
    wpViewAllTitle = "Tutti gli sfondi",
    wpSelectMode = "Seleziona",
    wpDeleteSelected = "Elimina selezionati",
    wpSelected = "selezionati",
    wpDeleteAllConfirm = "Eliminare gli sfondi selezionati?",
    wpDeleteAllConfirmDesc = "Gli sfondi selezionati verranno rimossi definitivamente dall'app.",
    wpPageHint = "Scorri a sinistra per gli sfondi →",

    // Verse language source toggle (in Settings)
    vdbSourceDefault = "Predefinite",
    vdbSourceCustom = "Personalizzate",
    vdbEmptyCustom = "Nessun database personalizzato",
    vdbEmptyCustomDesc = "Crea la tua raccolta di versetti, ad esempio KJV o un'altra traduzione.",
    vdbEmptyCustomCta = "Crea nuovo database",

    // Duplicate-code warnings + import conflict dialog
    vdbWarningCodeBuiltin = "Il codice \"%s\" è già usato da un database integrato. Puoi comunque usarlo — il database personalizzato condividerà il codice.",
    vdbWarningCodeCustom = "Il codice \"%s\" esiste già come database personalizzato. Il salvataggio lo sovrascriverà.",
    vdbImportConflictTitle = "Codice già esistente",
    vdbImportConflictDesc = "Esiste già un database personalizzato con codice \"%s\". Inserisci un codice diverso, oppure lascia \"%s\" per sovrascrivere.",
    vdbImportConflictAction = "Importa con questo codice",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    vdbOverwriteTitle = "Sovrascrivere il database?",
    vdbOverwriteDesc = "Esiste già un database personalizzato con codice \"%s\". Il salvataggio sostituirà definitivamente i suoi versetti. Questa azione non può essere annullata.",
    vdbOverwriteConfirm = "Sovrascrivi"
)

val frStrings = AppStrings(
    updateTime = "Heure de mise à jour quotidienne",
    settings = "Paramètres",
    dailyWallpaper = "Fond d'écran quotidien",
    active = "Actif (tous les jours à %s:00)",
    inactive = "Inactif",
    textCustomization = "Personnalisation du texte",
    bold = "Gras",
    shadow = "Ombre",
    textSize = "Taille du texte",
    textWidth = "Largeur du texte",
    textHeight = "Hauteur du texte",
    textAlpha = "Transparence",
    bgBlur = "Flou d'arrière-plan",
    bgDarknessLabel = "Assombrir l'arrière-plan",
    anotherPhoto = "Changer de photo",
    test = "Générer",
    selectPhotoFirst = "Sélectionnez d'abord une photo",
    appearance = "Apparence",
    system = "Système",
    light = "Clair",
    dark = "Sombre",
    dynamicColor = "Material You (Couleurs système)",
    haptics = "Retour haptique",
    hapticsDesc = "Vibrations lors de l'édition",
    support = "Soutien",
    supportDesc = "Si vous aimez l'application et souhaitez soutenir son développement, vous pouvez m'offrir un café ☕",

    donate = "Faire un don",
    close = "Fermer",
    dragHint = "Faites glisser les points pour la taille et la largeur\nFaites glisser au centre pour déplacer",
    cancel = "Annuler",
    done = "Terminé",
    clickToSelect = "Touchez pour sélectionner une photo",
    loading = "Chargement du verset...",
    appLanguage = "Langue de l'app",
    verseLanguage = "Langue du verset",
    language = "Langue",
    dailyWorkerOn = "Activé !\nLe fond d'écran changera demain à %s:00.",

    dailyWorkerOff = "Changement quotidien désactivé.",
    generating = "Génération du fond d'écran...",
    fontModern = "Moderne",
    fontBook = "Livre",
    fontMono = "Machine à écrire",
    fontCursive = "Cursive",
    fontLight = "Fin",
    fontCondensed = "Condensé",
    generatingBtn = "Génération...",
    tapToEdit = "Touchez pour modifier la position et la taille",
    batteryWarningTitle = "Avertissement de Batterie",
    batteryWarningDesc = "Pour que le changement automatique de fond d'écran fonctionne de manière fiable, veuillez désactiver l'optimisation de la batterie pour cette application.",
    batteryWarningButton = "Désactiver l'Optimisation",
    customVerseTitle = "Texte Personnalisé",
    customVerseHint = "Texte du verset...",
    customRefHint = "Coordonnées (ex. Jean 3:16)",
    applyCustom = "Appliquer",
    autoWallpaper = "Changement automatique du fond",
    autoWallpaperIntervalLabel = "Intervalle de changement",
    autoWallpaperEvery1h = "Chaque heure",
    autoWallpaperEvery2h = "Toutes les 2 heures",
    autoWallpaperEvery3h = "Toutes les 3 heures",
    autoWallpaperEvery6h = "Toutes les 6 heures",
    autoWallpaperEvery12h = "Toutes les 12 heures",
    autoWallpaperEvery24h = "Toutes les 24 heures (quotidien)",
    autoWallpaperOnScreenOff = "À l'extinction de l'écran",
    autoWallpaperOnScreenOffDesc = "Le verset change à chaque verrouillage du téléphone",
    autoWallpaperTimeLabel = "Heure du changement quotidien",
    autoWallpaperActiveHourly = "Actif (toutes les %sh)",
    autoWallpaperActiveDaily = "Actif (quotidien à %s:00)",
    autoWallpaperActiveScreenOff = "Actif (au verrouillage)",
    autoWorkerOn = "Activé ! Le fond changera selon l'intervalle configuré.",
    autoWorkerOff = "Changement automatique désactivé.",
    colorPickerTitle = "Choisir une couleur",
    colorPickerBrightness = "Luminosité",
    colorPickerVerseColor = "Couleur du verset",

    // Wallpaper target
    wallpaperTargetLabel = "Appliquer le fond sur",
    wallpaperTargetLock = "Verrouillage",
    wallpaperTargetHome = "Accueil",
    wallpaperTargetBoth = "Les deux",

    // Verse database section
    vdbTitle = "Bases de données de versets",
    vdbSubtitle = "Crée, importe ou exporte des collections",
    vdbManage = "Gérer",
    vdbNewDatabase = "Nouvelle base de données",
    vdbEditPrefix = "Modifier ·",
    vdbImportExport = "Importer / Exporter",
    vdbCreateNew = "Créer une nouvelle",
    vdbSectionCustom = "Personnalisée",
    vdbSectionBuiltIn = "Intégrée",
    vdbHint = "Sélectionne une DB personnalisée dans Paramètres → Langue des versets",
    vdbDeleteTitle = "Supprimer",
    vdbDeleteText = "versets seront supprimés définitivement.",
    vdbDelete = "Supprimer",
    vdbDeleted = "Supprimé",
    vdbVerses = "versets",
    vdbCustomLabel = "Personnalisée",
    vdbCodeLabel = "Code",
    vdbCodePlaceholder = "ex. LSG",
    vdbCodeHint = "Sélectionne-le dans Paramètres → Langue des versets",
    vdbUpdate = "Mettre à jour",
    vdbSave = "Enregistrer",
    vdbNoVerses = "Aucun verset — appuie sur + ci-dessous",
    vdbVersesFilled = "remplis",
    vdbVerse = "verset",
    vdbVersePlural = "versets",
    vdbClearAll = "Tout effacer",
    vdbNoVersesYet = "Aucun verset pour l'instant",
    vdbAddFirstVerse = "Appuie sur le bouton ci-dessous pour ajouter ton premier verset",
    vdbAddVerse = "Ajouter un verset",
    vdbVerseText = "Texte du verset",
    vdbReference = "Référence",
    vdbReferencePlaceholder = "ex. Jean 3:16",
    vdbVerseCard = "Verset",
    vdbUpdated = "Mise à jour",
    vdbCreated = "Créée",
    vdbErrorCode = "Saisis un code de base de données",
    vdbErrorVerse = "Ajoute au moins un verset",
    vdbErrorFailed = "Erreur",
    vdbImportTitle = "Importer",
    vdbImportJson = "Importer un fichier JSON",
    vdbImportDesc = "Sélectionne un .json de versets depuis l'appareil",
    vdbBrowse = "Parcourir",
    vdbImporting = "Importation...",
    vdbImportCodeTitle = "Code de la base de données",
    vdbImportCodeDesc = "Saisis un code court pour cette base de données (ex. LSG). Elle remplace la base intégrée si le code correspond.",
    vdbImport = "Importer",
    vdbImportSuccess = "Importés %d versets en tant que",
    vdbImportFailed = "Échec de l'importation",
    vdbExportCustom = "Exporter — Personnalisée",
    vdbExportBuiltIn = "Exporter — Intégrée",
    vdbExport = "Exporter",
    vdbExporting = "Enregistrement dans Téléchargements…",
    vdbExportDone = "Enregistré dans Téléchargements",
    vdbExportFailed = "Échec de l'exportation",
    vdbJsonFormat = "Format JSON",

    // Backup & Restore
    backupTitle = "Sauvegarde et restauration",
    backupExport = "Sauvegarder les paramètres",
    backupExportDesc = "Enregistre tous les paramètres, le fond d'écran et les bases de données dans un seul fichier",
    backupImport = "Restaurer depuis une sauvegarde",
    backupImportDesc = "Remplace tous les paramètres, le fond d'écran et les bases de données actuels",
    backupExporting = "Sauvegarde…",
    backupImporting = "Restauration…",
    backupExportSuccess = "Sauvegarde enregistrée · %d paramètres, %d bases de données",
    backupImportSuccess = "Sauvegarde restaurée · %d paramètres, %d bases de données. Redémarrage…",
    backupExportFailed = "Échec de la sauvegarde",
    backupImportFailed = "Échec de la restauration",
    backupConfirmTitle = "Restaurer depuis une sauvegarde ?",
    backupConfirmDesc = "Cela remplacera tous vos paramètres, fond d'écran et bases de données actuels. Action irréversible.",

    // Wallpaper gallery screen
    wpScreenTitle = "Fonds d'écran",
    wpScreenSubtitle = "Gérez vos fonds d'écran et la rotation automatique",
    wpGalleryEmpty = "Aucun fond d'écran",
    wpGalleryEmptyDesc = "Ajoutez votre premier fond — choisissez une photo de la galerie.",
    wpAdd = "Ajouter un fond",
    wpSetActive = "Utiliser",
    wpActive = "Actif",
    wpDelete = "Supprimer",
    wpDeleteConfirm = "Supprimer ce fond d'écran ?",
    wpDeleteConfirmDesc = "Le fond d'écran sera définitivement supprimé de l'app.",
    wpCycleTitle = "Rotation automatique",
    wpCycleDesc = "Change les fonds d'écran automatiquement selon un intervalle",
    wpCycleInterval = "Intervalle de rotation",
    wpCycleDailyHour = "Heure du changement quotidien",
    wpCycleOnScreenOff = "Au verrouillage",
    wpCycleOnScreenOffDesc = "Change le fond d'écran à chaque verrouillage du téléphone",
    wpNightMode = "Mode nuit",
    wpNightModeDesc = "Utiliser un fond d'écran différent la nuit",
    wpNightStart = "Début de la nuit",
    wpNightEnd = "Fin de la nuit",
    wpActiveBadge = "Actif",
    wpNightBadge = "Nuit",
    wpCycleModeVerse = "Au changement de verset",
    wpCycleModeInterval = "Intervalle personnalisé",
    wpCycleModeScreenOff = "À chaque verrouillage",
    wpCycleModeDayNight = "Jour / Nuit",
    wpCycleModeVerseDesc = "Le fond change avec le verset",
    wpCycleModeDayNightDesc = "Basculer automatiquement entre le fond de jour et de nuit",
    wpCycleDayWallpaper = "Fond de jour",
    wpCycleNightWallpaper = "Fond de nuit",
    wpCycleModeIntervalDesc = "Changer à intervalles fixes",
    wpCycleModeScreenOffDesc = "Changer à chaque verrouillage",
    wpDualLockWarning = "Le changement de fond écran éteint peut prendre jusqu'à 3 secondes.",
    wpDayStart = "Le jour commence",
    wpDayEnd = "Le jour finit",

    wpViewAll = "Tout voir",
    wpViewAllTitle = "Tous les fonds",
    wpSelectMode = "Sélectionner",
    wpDeleteSelected = "Supprimer sélectionnés",
    wpSelected = "sélectionnés",
    wpDeleteAllConfirm = "Supprimer fonds sélectionnés ?",
    wpDeleteAllConfirmDesc = "Les fonds sélectionnés seront définitivement supprimés de l'app.",
    wpPageHint = "Glissez à gauche pour les fonds d'écran →",

    // Verse language source toggle (in Settings)
    vdbSourceDefault = "Intégrées",
    vdbSourceCustom = "Personnalisées",
    vdbEmptyCustom = "Aucune base de données personnalisée",
    vdbEmptyCustomDesc = "Créez votre propre collection de versets, par exemple KJV ou une autre traduction.",
    vdbEmptyCustomCta = "Créer une base de données",

    // Duplicate-code warnings + import conflict dialog
    vdbWarningCodeBuiltin = "Le code \"%s\" est déjà utilisé par une base de données intégrée. Vous pouvez l'utiliser — la base personnalisée partagera le code.",
    vdbWarningCodeCustom = "Le code \"%s\" existe déjà comme base de données personnalisée. L'enregistrement l'écrasera.",
    vdbImportConflictTitle = "Le code existe déjà",
    vdbImportConflictDesc = "Une base de données personnalisée avec le code \"%s\" existe déjà. Saisissez un autre code, ou laissez \"%s\" pour écraser.",
    vdbImportConflictAction = "Importer avec ce code",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    vdbOverwriteTitle = "Écraser la base de données ?",
    vdbOverwriteDesc = "Une base de données personnalisée avec le code \"%s\" existe déjà. L'enregistrement remplacera définitivement ses versets. Cette action est irréversible.",
    vdbOverwriteConfirm = "Écraser"
)

val deStrings = AppStrings(
    updateTime = "Zeitliche Aktualisierungszeit",
    settings = "App-Einstellungen",
    dailyWallpaper = "Tägliches Hintergrundbild",
    active = "Aktiv (jeden Tag bei %s:00)",
    inactive = "Inaktiv",
    textCustomization = "Textanpassung",
    bold = "Fett",
    shadow = "Schatten",
    textSize = "Textgröße",
    textWidth = "Textbreite",
    textHeight = "Texthöhe",
    textAlpha = "Transparenz",
    bgBlur = "Hintergrundunschärfe",
    bgDarknessLabel = "Hintergrund abdunkeln",
    anotherPhoto = "Anderes Foto",
    test = "Erzeugen",
    selectPhotoFirst = "Wähle zuerst ein Foto aus",
    appearance = "Erscheinungsbild",
    system = "System",
    light = "Hell",
    dark = "Dunkel",
    dynamicColor = "Material You (Systemfarben)",
    haptics = "Haptisches Feedback",
    hapticsDesc = "Vibrationen beim Bearbeiten",
    support = "Unterstützung",
    supportDesc = "Wenn dir die App gefällt und du ihre Entwicklung unterstützen möchtest, kannst du mir einen Kaffee spendieren ☕",
    donate = "Spenden",
    close = "Schließen",

    dragHint = "Ziehe die Punkte für die Größe und Breite\nZiehe in die Mitte zum Bewegen",
    cancel = "Abbrechen",
    done = "Fertig",
    clickToSelect = "Tippe, um ein Foto auszuwählen",
    loading = "Lade Vers...",
    appLanguage = "App-Sprache",
    verseLanguage = "Vers-Sprache",
    language = "Sprache",
    dailyWorkerOn = "Aktiviert!\nHintergrundbild ändert sich morgen um %s:00.",
    dailyWorkerOff = "Tägliche Änderung deaktiviert.",
    generating = "Hintergrundbild wird generiert...",
    fontModern = "Modern",

    fontBook = "Buch",
    fontMono = "Schreibmaschine",
    fontCursive = "Kursiv",
    fontLight = "Leicht",
    fontCondensed = "Schmal",
    generatingBtn = "Generieren...",
    tapToEdit = "Tippe, um die Position und Größe zu ändern",
    batteryWarningTitle = "Akku-Warnung",
    batteryWarningDesc = "Damit der automatische Hintergrundwechsel zuverlässig funktioniert, deaktivieren Sie bitte die Akku-Optimierung für diese App.",
    batteryWarningButton = "Optimierung Deaktivieren",
    customVerseTitle = "Eigener Text",
    customVerseHint = "Verstext...",
    customRefHint = "Koordinaten (z.B. Johannes 3:16)",
    applyCustom = "Anwenden",
    autoWallpaper = "Automatischer Hintergrundwechsel",
    autoWallpaperIntervalLabel = "Wechselintervall",
    autoWallpaperEvery1h = "Jede Stunde",
    autoWallpaperEvery2h = "Alle 2 Stunden",
    autoWallpaperEvery3h = "Alle 3 Stunden",
    autoWallpaperEvery6h = "Alle 6 Stunden",
    autoWallpaperEvery12h = "Alle 12 Stunden",
    autoWallpaperEvery24h = "Alle 24 Stunden (täglich)",
    autoWallpaperOnScreenOff = "Beim Ausschalten des Bildschirms",
    autoWallpaperOnScreenOffDesc = "Der Vers ändert sich bei jeder Sperrung",
    autoWallpaperTimeLabel = "Uhrzeit des täglichen Wechsels",
    autoWallpaperActiveHourly = "Aktiv (alle %sh)",
    autoWallpaperActiveDaily = "Aktiv (täglich um %s:00)",
    autoWallpaperActiveScreenOff = "Aktiv (bei Sperrung)",
    autoWorkerOn = "Aktiviert! Das Hintergrundbild wechselt nach dem eingestellten Intervall.",
    autoWorkerOff = "Automatischer Wechsel deaktiviert.",
    colorPickerTitle = "Farbe auswählen",
    colorPickerBrightness = "Helligkeit",
    colorPickerVerseColor = "Versfarbe",

    // Wallpaper target
    wallpaperTargetLabel = "Hintergrundbild anwenden auf",
    wallpaperTargetLock = "Sperrbildschirm",
    wallpaperTargetHome = "Startbildschirm",
    wallpaperTargetBoth = "Beide",

    // Verse database section
    vdbTitle = "Vers-Datenbanken",
    vdbSubtitle = "Erstelle, importiere oder exportiere Sammlungen",
    vdbManage = "Verwalten",
    vdbNewDatabase = "Neue Datenbank",
    vdbEditPrefix = "Bearbeiten ·",
    vdbImportExport = "Importieren / Exportieren",
    vdbCreateNew = "Neu erstellen",
    vdbSectionCustom = "Benutzerdefiniert",
    vdbSectionBuiltIn = "Integriert",
    vdbHint = "Wähle eine benutzerdefinierte DB unter Einstellungen → Verssprache",
    vdbDeleteTitle = "Löschen",
    vdbDeleteText = "Verse werden dauerhaft entfernt.",
    vdbDelete = "Löschen",
    vdbDeleted = "Gelöscht",
    vdbVerses = "Verse",
    vdbCustomLabel = "Benutzerdefiniert",
    vdbCodeLabel = "Code",
    vdbCodePlaceholder = "z.B. LUT",
    vdbCodeHint = "Wähle diesen unter Einstellungen → Verssprache",
    vdbUpdate = "Aktualisieren",
    vdbSave = "Speichern",
    vdbNoVerses = "Keine Verse — tippe unten auf +",
    vdbVersesFilled = "ausgefüllt",
    vdbVerse = "Vers",
    vdbVersePlural = "Verse",
    vdbClearAll = "Alle löschen",
    vdbNoVersesYet = "Noch keine Verse",
    vdbAddFirstVerse = "Tippe auf die Schaltfläche unten, um deinen ersten Vers hinzuzufügen",
    vdbAddVerse = "Vers hinzufügen",
    vdbVerseText = "Verstext",
    vdbReference = "Referenz",
    vdbReferencePlaceholder = "z.B. Johannes 3:16",
    vdbVerseCard = "Vers",
    vdbUpdated = "Aktualisiert",
    vdbCreated = "Erstellt",
    vdbErrorCode = "Gib einen Datenbankcode ein",
    vdbErrorVerse = "Füge mindestens einen Vers hinzu",
    vdbErrorFailed = "Fehler",
    vdbImportTitle = "Importieren",
    vdbImportJson = "JSON-Datei importieren",
    vdbImportDesc = "Wähle eine Vers-JSON-Datei vom Gerät",
    vdbBrowse = "Durchsuchen",
    vdbImporting = "Importiere...",
    vdbImportCodeTitle = "Datenbankcode",
    vdbImportCodeDesc = "Gib einen kurzen Code für diese Datenbank ein (z.B. LUT). Sie überschreibt die integrierte Datenbank, wenn der Code übereinstimmt.",
    vdbImport = "Importieren",
    vdbImportSuccess = "%d Verse importiert als",
    vdbImportFailed = "Import fehlgeschlagen",
    vdbExportCustom = "Exportieren — Benutzerdefiniert",
    vdbExportBuiltIn = "Exportieren — Integriert",
    vdbExport = "Exportieren",
    vdbExporting = "Wird in Downloads gespeichert…",
    vdbExportDone = "In Downloads gespeichert",
    vdbExportFailed = "Export fehlgeschlagen",
    vdbJsonFormat = "JSON-Format",

    // Backup & Restore
    backupTitle = "Sicherung & Wiederherstellung",
    backupExport = "Einstellungen sichern",
    backupExportDesc = "Speichert alle Einstellungen, das Hintergrundbild und die Datenbanken in einer Datei",
    backupImport = "Aus Sicherung wiederherstellen",
    backupImportDesc = "Ersetzt alle aktuellen Einstellungen, das Hintergrundbild und die Datenbanken",
    backupExporting = "Sichere…",
    backupImporting = "Stelle wieder her…",
    backupExportSuccess = "Sicherung gespeichert · %d Einstellungen, %d Datenbanken",
    backupImportSuccess = "Sicherung wiederhergestellt · %d Einstellungen, %d Datenbanken. Neustart…",
    backupExportFailed = "Sicherung fehlgeschlagen",
    backupImportFailed = "Wiederherstellung fehlgeschlagen",
    backupConfirmTitle = "Aus Sicherung wiederherstellen?",
    backupConfirmDesc = "Dies ersetzt alle deine aktuellen Einstellungen, das Hintergrundbild und die Datenbanken. Vorgang nicht umkehrbar.",

    // Wallpaper gallery screen
    wpScreenTitle = "Hintergrundbilder",
    wpScreenSubtitle = "Verwalte deine Hintergrundbilder und automatische Rotation",
    wpGalleryEmpty = "Keine Hintergrundbilder",
    wpGalleryEmptyDesc = "Füge dein erstes Hintergrundbild hinzu — wähle ein Foto aus der Galerie.",
    wpAdd = "Hintergrundbild hinzufügen",
    wpSetActive = "Verwenden",
    wpActive = "Aktiv",
    wpDelete = "Entfernen",
    wpDeleteConfirm = "Dieses Hintergrundbild entfernen?",
    wpDeleteConfirmDesc = "Das Hintergrundbild wird dauerhaft aus der App entfernt.",
    wpCycleTitle = "Automatische Rotation",
    wpCycleDesc = "Wechselt Hintergrundbilder automatisch nach einem Intervall",
    wpCycleInterval = "Rotationsintervall",
    wpCycleDailyHour = "Tägliche Wechselzeit",
    wpCycleOnScreenOff = "Beim Sperren",
    wpCycleOnScreenOffDesc = "Wechselt das Hintergrundbild bei jedem Sperren des Telefons",
    wpNightMode = "Nachtmodus",
    wpNightModeDesc = "Nachts ein anderes Hintergrundbild verwenden",
    wpNightStart = "Nacht beginnt",
    wpNightEnd = "Nacht endet",
    wpActiveBadge = "Aktiv",
    wpNightBadge = "Nacht",
    wpCycleModeVerse = "Bei Verswechsel",
    wpCycleModeInterval = "Eigenes Intervall",
    wpCycleModeScreenOff = "Bei jedem Sperren",
    wpCycleModeDayNight = "Tag / Nacht",
    wpCycleModeVerseDesc = "Hintergrundbild wechselt mit dem Vers",
    wpCycleModeDayNightDesc = "Automatisch zwischen Tag- und Nachthintergrund wechseln",
    wpCycleDayWallpaper = "Tag-Hintergrund",
    wpCycleNightWallpaper = "Nacht-Hintergrund",
    wpCycleModeIntervalDesc = "Wechsel in festen Intervallen",
    wpCycleModeScreenOffDesc = "Wechsel bei jedem Sperren",
    wpDualLockWarning = "Der Hintergrundwechsel bei ausgeschaltetem Bildschirm kann bis zu 3 Sekunden dauern.",
    wpDayStart = "Tag beginnt",
    wpDayEnd = "Tag endet",

    wpViewAll = "Alle ansehen",
    wpViewAllTitle = "Alle Hintergrundbilder",
    wpSelectMode = "Auswählen",
    wpDeleteSelected = "Ausgewählte löschen",
    wpSelected = "ausgewählt",
    wpDeleteAllConfirm = "Ausgewählte Hintergrundbilder löschen?",
    wpDeleteAllConfirmDesc = "Die ausgewählten Hintergrundbilder werden dauerhaft aus der App entfernt.",
    wpPageHint = "Wische nach links für Hintergrundbilder →",

    // Verse language source toggle (in Settings)
    vdbSourceDefault = "Standard",
    vdbSourceCustom = "Eigene",
    vdbEmptyCustom = "Noch keine eigenen Datenbanken",
    vdbEmptyCustomDesc = "Erstelle deine eigene Verssammlung, z. B. KJV oder eine andere Übersetzung.",
    vdbEmptyCustomCta = "Neue Datenbank erstellen",

    // Duplicate-code warnings + import conflict dialog
    vdbWarningCodeBuiltin = "Code \"%s\" wird bereits von einer integrierten Datenbank verwendet. Du kannst ihn trotzdem verwenden — die eigene DB teilt sich den Code.",
    vdbWarningCodeCustom = "Code \"%s\" existiert bereits als eigene Datenbank. Speichern überschreibt sie.",
    vdbImportConflictTitle = "Code existiert bereits",
    vdbImportConflictDesc = "Eine eigene Datenbank mit Code \"%s\" existiert bereits. Gib einen anderen Code ein, oder behalte \"%s\" zum Überschreiben.",
    vdbImportConflictAction = "Mit diesem Code importieren",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    vdbOverwriteTitle = "Datenbank überschreiben?",
    vdbOverwriteDesc = "Eine eigene Datenbank mit Code \"%s\" existiert bereits. Speichern überschreibt ihre Verse dauerhaft. Diese Aktion kann nicht rückgängig gemacht werden.",
    vdbOverwriteConfirm = "Überschreiben"
)

val huStrings = AppStrings(
    updateTime = "Napi frissítés időpontja",
    settings = "Alkalmazás beállításai",
    dailyWallpaper = "Napi háttérkép",
    active = "Aktív (minden nap %s:00)",
    inactive = "Inaktív",
    textCustomization = "Szöveg testreszabása",
    bold = "Félkövér",
    shadow = "Árnyék",
    textSize = "Szövegméret",
    textWidth = "Szöveg szélessége",
    textHeight = "Szöveg magassága",
    textAlpha = "Átlátszóság",
    bgBlur = "Háttér elmosása",
    bgDarknessLabel = "A háttér sötétítése",
    anotherPhoto = "Másik fotó",

    test = "Generálni",
    selectPhotoFirst = "Először válassz egy fotót",
    appearance = "Megjelenés",
    system = "Rendszer",
    light = "Világos",
    dark = "Sötét",
    dynamicColor = "Material You (Rendszerszínek)",
    haptics = "Haptikus visszajelzés",
    hapticsDesc = "Rezgés szerkesztéskor",
    support = "Támogatás",
    supportDesc = "Ha tetszik az alkalmazás és szeretnéd támogatni a fejlesztését, meghívhatsz egy kávéra ☕",
    donate = "Adomány",
    close = "Bezárás",

    dragHint = "Húzd a pontokat a méretért és szélességért\nHúzd a közepén a mozgatáshoz",
    cancel = "Mégse",
    done = "Kész",
    clickToSelect = "Koppints a fotó kiválasztásához",
    loading = "Ige betöltése...",
    appLanguage = "Alkalmazás nyelve",
    verseLanguage = "Ige nyelve",
    language = "Nyelv",
    dailyWorkerOn = "Bekapcsolva!\nA háttérkép holnap %s:00-kor változik.",
    dailyWorkerOff = "Napi módosítás kikapcsolva.",
    generating = "Háttérkép generálása...",
    fontModern = "Modern",
    fontBook = "Könyv",

    fontMono = "Írógép",
    fontCursive = "Dőlt",
    fontLight = "Vékony",
    fontCondensed = "Sűrített",
    generatingBtn = "Generálom...",
    tapToEdit = "Koppintson a pozíció és a méret módosításához.",
    batteryWarningTitle = "Akkumulátor Figyelmeztetés",
    batteryWarningDesc = "Annak érdekében, hogy az automatikus háttérkép-váltás megbízhatóan működjön, kérjük, tiltsa le az akkumulátor-optimalizálást ehhez az alkalmazáshoz.",
    batteryWarningButton = "Optimalizálás Letiltása",
    customVerseTitle = "Egyéni szöveg",
    customVerseHint = "Ige szövege...",
    customRefHint = "Koordináták (pl. János 3:16)",
    applyCustom = "Alkalmaz",
    autoWallpaper = "Automatikus háttérkép-csere",
    autoWallpaperIntervalLabel = "Csere időköze",
    autoWallpaperEvery1h = "Minden órában",
    autoWallpaperEvery2h = "Minden 2 órában",
    autoWallpaperEvery3h = "Minden 3 órában",
    autoWallpaperEvery6h = "Minden 6 órában",
    autoWallpaperEvery12h = "Minden 12 órában",
    autoWallpaperEvery24h = "Minden 24 órában (naponta)",
    autoWallpaperOnScreenOff = "Képernyő kikapcsolásakor",
    autoWallpaperOnScreenOffDesc = "Az ige minden zárolásnál változik",
    autoWallpaperTimeLabel = "Napi csere időpontja",
    autoWallpaperActiveHourly = "Aktív (minden %sh)",
    autoWallpaperActiveDaily = "Aktív (naponta %s:00-kor)",
    autoWallpaperActiveScreenOff = "Aktív (zárolásnál)",
    autoWorkerOn = "Bekapcsolva! A háttérkép a beállított időközönként változik.",
    autoWorkerOff = "Automatikus csere kikapcsolva.",
    colorPickerTitle = "Szín kiválasztása",
    colorPickerBrightness = "Fényerő",
    colorPickerVerseColor = "Ige színe",

    // Wallpaper target
    wallpaperTargetLabel = "Háttérkép alkalmazása",
    wallpaperTargetLock = "Zárolás",
    wallpaperTargetHome = "Főképernyő",
    wallpaperTargetBoth = "Mindkettő",

    // Verse database section
    vdbTitle = "Igevers-adatbázisok",
    vdbSubtitle = "Hozz létre, importálj vagy exportálj gyűjteményeket",
    vdbManage = "Kezelés",
    vdbNewDatabase = "Új adatbázis",
    vdbEditPrefix = "Szerkesztés ·",
    vdbImportExport = "Importálás / Exportálás",
    vdbCreateNew = "Új létrehozása",
    vdbSectionCustom = "Egyéni",
    vdbSectionBuiltIn = "Beépített",
    vdbHint = "Egyéni DB kiválasztása: Beállítások → Igevers nyelve",
    vdbDeleteTitle = "Törlés",
    vdbDeleteText = "igevers véglegesen törlődik.",
    vdbDelete = "Törlés",
    vdbDeleted = "Törölve",
    vdbVerses = "igevers",
    vdbCustomLabel = "Egyéni",
    vdbCodeLabel = "Kód",
    vdbCodePlaceholder = "pl. KAR",
    vdbCodeHint = "Válaszd ki a Beállítások → Igevers nyelve menüben",
    vdbUpdate = "Frissítés",
    vdbSave = "Mentés",
    vdbNoVerses = "Nincs igevers — érintsd meg alul a + gombot",
    vdbVersesFilled = "kitöltve",
    vdbVerse = "igevers",
    vdbVersePlural = "igevers",
    vdbClearAll = "Összes törlése",
    vdbNoVersesYet = "Még nincs igevers",
    vdbAddFirstVerse = "Érintsd meg az alábbi gombot az első igevers hozzáadásához",
    vdbAddVerse = "Igevers hozzáadása",
    vdbVerseText = "Igevers szövege",
    vdbReference = "Hivatkozás",
    vdbReferencePlaceholder = "pl. János 3:16",
    vdbVerseCard = "Igevers",
    vdbUpdated = "Frissítve",
    vdbCreated = "Létrehozva",
    vdbErrorCode = "Add meg az adatbázis kódját",
    vdbErrorVerse = "Adj hozzá legalább egy igeverset",
    vdbErrorFailed = "Hiba",
    vdbImportTitle = "Importálás",
    vdbImportJson = "JSON fájl importálása",
    vdbImportDesc = "Válassz egy igevers .json fájlt az eszközről",
    vdbBrowse = "Tallózás",
    vdbImporting = "Importálás...",
    vdbImportCodeTitle = "Adatbázis kódja",
    vdbImportCodeDesc = "Adj meg egy rövid kódot ehhez az adatbázishoz (pl. KAR). Felülírja a beépített adatbázist, ha a kód egyezik.",
    vdbImport = "Importálás",
    vdbImportSuccess = "Importálva %d igevers mint",
    vdbImportFailed = "Importálás sikertelen",
    vdbExportCustom = "Exportálás — Egyéni",
    vdbExportBuiltIn = "Exportálás — Beépített",
    vdbExport = "Exportálás",
    vdbExporting = "Mentés a Letöltések mappába…",
    vdbExportDone = "Mentve a Letöltések mappába",
    vdbExportFailed = "Exportálás sikertelen",
    vdbJsonFormat = "JSON formátum",

    // Backup & Restore
    backupTitle = "Biztonsági mentés és visszaállítás",
    backupExport = "Beállítások mentése",
    backupExportDesc = "Minden beállítást, háttérképet és adatbázist egyetlen fájlba ment",
    backupImport = "Visszaállítás mentésből",
    backupImportDesc = "Lecseréli az összes jelenlegi beállítást, háttérképet és adatbázist",
    backupExporting = "Mentés…",
    backupImporting = "Visszaállítás…",
    backupExportSuccess = "Mentés elmentve · %d beállítás, %d adatbázis",
    backupImportSuccess = "Mentés visszaállítva · %d beállítás, %d adatbázis. Újraindítás…",
    backupExportFailed = "A mentés sikertelen",
    backupImportFailed = "A visszaállítás sikertelen",
    backupConfirmTitle = "Visszaállítás mentésből?",
    backupConfirmDesc = "Ez lecseréli az összes jelenlegi beállításodat, háttérképedet és adatbázisodat. A művelet nem visszavonható.",

    // Wallpaper gallery screen
    wpScreenTitle = "Háttérképek",
    wpScreenSubtitle = "Kezeld a háttérképeidet és az automatikus váltást",
    wpGalleryEmpty = "Nincs háttérkép",
    wpGalleryEmptyDesc = "Add hozzá az első háttérképet — válassz egy fotót a galériából.",
    wpAdd = "Háttérkép hozzáadása",
    wpSetActive = "Használ",
    wpActive = "Aktív",
    wpDelete = "Eltávolít",
    wpDeleteConfirm = "Eltávolítod ezt a háttérképet?",
    wpDeleteConfirmDesc = "A háttérkép véglegesen el lesz távolítva az appból.",
    wpCycleTitle = "Automatikus váltás",
    wpCycleDesc = "A háttérképek automatikusan váltanak egy intervallum szerint",
    wpCycleInterval = "Váltási intervallum",
    wpCycleDailyHour = "Napi váltás ideje",
    wpCycleOnScreenOff = "Záráskor",
    wpCycleOnScreenOffDesc = "Háttérkép váltás minden telefonzáráskor",
    wpNightMode = "Éjszakai mód",
    wpNightModeDesc = "Éjjel másik háttérképet használ",
    wpNightStart = "Éjszaka kezdete",
    wpNightEnd = "Éjszaka vége",
    wpActiveBadge = "Aktív",
    wpNightBadge = "Éjszaka",
    wpCycleModeVerse = "Vers váltáskor",
    wpCycleModeInterval = "Egyéni intervallum",
    wpCycleModeScreenOff = "Minden záráskor",
    wpCycleModeDayNight = "Nappal / Éjszaka",
    wpCycleModeVerseDesc = "A háttérkép a verssel együtt vált",
    wpCycleModeDayNightDesc = "Automatikus váltás nappali és éjszakai háttérkép között",
    wpCycleDayWallpaper = "Nappali háttérkép",
    wpCycleNightWallpaper = "Éjszakai háttérkép",
    wpCycleModeIntervalDesc = "Váltás fix intervallumonként",
    wpCycleModeScreenOffDesc = "Váltás minden záráskor",
    wpDualLockWarning = "A háttérkép váltása kikapcsolt képernyőnél akár 3 másodpercet is igénybe vehet.",
    wpDayStart = "Nappal kezdete",
    wpDayEnd = "Nappal vége",

    wpViewAll = "Összes megtekintése",
    wpViewAllTitle = "Minden háttérkép",
    wpSelectMode = "Kiválasztás",
    wpDeleteSelected = "Kijelöltek törlése",
    wpSelected = "kijelölve",
    wpDeleteAllConfirm = "Kijelölt háttérképek törlése?",
    wpDeleteAllConfirmDesc = "A kijelölt háttérképek véglegesen el lesznek távolítva az appból.",
    wpPageHint = "Húzd balra a háttérképekért →",

    // Verse language source toggle (in Settings)
    vdbSourceDefault = "Beépített",
    vdbSourceCustom = "Saját",
    vdbEmptyCustom = "Még nincsenek saját adatbázisok",
    vdbEmptyCustomDesc = "Hozd létre a saját versgyűjteményedet — például KJV vagy más fordítás.",
    vdbEmptyCustomCta = "Új adatbázis létrehozása",

    // Duplicate-code warnings + import conflict dialog
    vdbWarningCodeBuiltin = "A(z) \"%s\" kódot már használja egy beépített adatbázis. Használhatod — a saját DB megosztja a kódot.",
    vdbWarningCodeCustom = "A(z) \"%s\" kód már létezik saját adatbázisként. A mentés felülírja.",
    vdbImportConflictTitle = "A kód már létezik",
    vdbImportConflictDesc = "Már létezik saját adatbázis \"%s\" kóddal. Adj meg másik kódot, vagy hagyd \"%s\"-t a felülíráshoz.",
    vdbImportConflictAction = "Importálás ezzel a kóddal",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    vdbOverwriteTitle = "Adatbázis felülírása?",
    vdbOverwriteDesc = "Már létezik saját adatbázis \"%s\" kóddal. A mentés véglegesen felülírja a verseit. Ez a művelet nem visszavonható.",
    vdbOverwriteConfirm = "Felülírás"
)

val plStrings = AppStrings(
    updateTime = "Czas aktualizacji dziennie",
    generatingBtn = "generowanie...",
    settings = "Ustawienia aplikacji",
    dailyWallpaper = "Codzienna tapeta",
    active = "Aktywne (codziennie o %s:00)",
    inactive = "Nieaktywne",
    textCustomization = "Dostosowanie tekstu",
    bold = "Pogrubienie",
    shadow = "Cień",
    textSize = "Rozmiar tekstu",
    textWidth = "Szerokość tekstu",
    textHeight = "Wysokość tekstu",
    textAlpha = "Przezroczystość",
    bgBlur = "Rozmycie tła",
    bgDarknessLabel = "Przyciemnianie tła",

    anotherPhoto = "Zmień zdjęcie",
    test = "Stwarzać",
    selectPhotoFirst = "Najpierw wybierz zdjęcie",
    appearance = "Wygląd",
    system = "System",
    light = "Jasny",
    dark = "Ciemny",
    dynamicColor = "Material You (Kolory systemu)",
    haptics = "Wibracje",
    hapticsDesc = "Wibracje podczas edycji",
    support = "Wsparcie",
    supportDesc = "Jeśli podoba Ci się aplikacja i chcesz wesprzeć jej rozwój, możesz postawić mi kawę ☕",
    donate = "Wesprzyj",

    close = "Zamknij",
    dragHint = "Przeciągnij kropki dla rozmiaru i szerokości\nPrzeciągnij środek, aby przesunąć",
    cancel = "Anuluj",
    done = "Gotowe",
    clickToSelect = "Dotknij, aby wybrać zdjęcie",
    loading = "Ładowanie wersetu...",
    appLanguage = "Język aplikacji",
    verseLanguage = "Język wersetu",
    language = "Język",
    dailyWorkerOn = "Włączone!\nTapeta zmieni się jutro o %s:00.",
    dailyWorkerOff = "Codzienna zmiana wyłączona.",
    generating = "Generowanie tapety...",
    fontModern = "Nowoczesny",

    fontBook = "Książka",
    fontMono = "Maszyna do pisania",
    fontCursive = "Kursywa",
    fontLight = "Cienki",
    fontCondensed = "Zwężony",
    tapToEdit = "Dotknij, aby zmienić położenie i wielkość",
    batteryWarningTitle = "Ostrzeżenie o Baterii",
    batteryWarningDesc = "Aby automatyczna zmiana tapety działała niezawodnie w tle, wyłącz optymalizację baterii dla tej aplikacji.",
    batteryWarningButton = "Wyłącz Optymalizację",
    customVerseTitle = "Własny tekst",
    customVerseHint = "Tekst wersetu...",
    customRefHint = "Współrzędne (np. Jan 3:16)",
    applyCustom = "Zastosuj",
    autoWallpaper = "Automatyczna zmiana tapety",
    autoWallpaperIntervalLabel = "Interwał zmiany",
    autoWallpaperEvery1h = "Co godzinę",
    autoWallpaperEvery2h = "Co 2 godziny",
    autoWallpaperEvery3h = "Co 3 godziny",
    autoWallpaperEvery6h = "Co 6 godzin",
    autoWallpaperEvery12h = "Co 12 godzin",
    autoWallpaperEvery24h = "Co 24 godziny (codziennie)",
    autoWallpaperOnScreenOff = "Po wyłączeniu ekranu",
    autoWallpaperOnScreenOffDesc = "Werset zmienia się przy każdym zablokowaniu telefonu",
    autoWallpaperTimeLabel = "Czas codziennej zmiany",
    autoWallpaperActiveHourly = "Aktywne (co %sh)",
    autoWallpaperActiveDaily = "Aktywne (codziennie o %s:00)",
    autoWallpaperActiveScreenOff = "Aktywne (przy blokowaniu)",
    autoWorkerOn = "Włączone! Tapeta zmieni się zgodnie z ustawionym interwałem.",
    autoWorkerOff = "Automatyczna zmiana wyłączona.",
    colorPickerTitle = "Wybierz kolor",
    colorPickerBrightness = "Jasność",
    colorPickerVerseColor = "Kolor wersetu",
    wallpaperTargetLabel = "Zastosuj tapetę na",
    wallpaperTargetLock = "Ekran blokady",
    wallpaperTargetHome = "Ekran główny",
    wallpaperTargetBoth = "Oba",
    vdbTitle = "Bazy danych wersetów",
    vdbSubtitle = "Twórz, importuj lub eksportuj kolekcje",
    vdbManage = "Zarządzaj",
    vdbNewDatabase = "Nowa baza danych",
    vdbEditPrefix = "Edytuj ·",
    vdbImportExport = "Import / Eksport",
    vdbCreateNew = "Utwórz nową",
    vdbSectionCustom = "Własna",
    vdbSectionBuiltIn = "Wbudowana",
    vdbHint = "Wybierz własną bazę w Ustawienia → Język wersetów",
    vdbDeleteTitle = "Usuń",
    vdbDeleteText = "wersetów zostanie trwale usuniętych.",
    vdbDelete = "Usuń",
    vdbDeleted = "Usunięto",
    vdbVerses = "wersetów",
    vdbCustomLabel = "Własna",
    vdbCodeLabel = "Kod",
    vdbCodePlaceholder = "np. BW",
    vdbCodeHint = "Wybierz w Ustawienia → Język wersetów",
    vdbUpdate = "Zaktualizuj",
    vdbSave = "Zapisz",
    vdbNoVerses = "Brak wersetów — dotknij + poniżej",
    vdbVersesFilled = "wypełnionych",
    vdbVerse = "werset",
    vdbVersePlural = "wersetów",
    vdbClearAll = "Wyczyść wszystko",
    vdbNoVersesYet = "Brak wersetów",
    vdbAddFirstVerse = "Dotknij przycisku poniżej, aby dodać pierwszy werset",
    vdbAddVerse = "Dodaj werset",
    vdbVerseText = "Tekst wersetu",
    vdbReference = "Odnośnik",
    vdbReferencePlaceholder = "np. Jana 3:16",
    vdbVerseCard = "Werset",
    vdbUpdated = "Zaktualizowano",
    vdbCreated = "Utworzono",
    vdbErrorCode = "Wprowadź kod bazy danych",
    vdbErrorVerse = "Dodaj co najmniej jeden werset",
    vdbErrorFailed = "Błąd",
    vdbImportTitle = "Import",
    vdbImportJson = "Importuj plik JSON",
    vdbImportDesc = "Wybierz plik .json z wersetami z urządzenia",
    vdbBrowse = "Przeglądaj",
    vdbImporting = "Importowanie...",
    vdbImportCodeTitle = "Kod bazy danych",
    vdbImportCodeDesc = "Wprowadź krótki kod dla tej bazy danych (np. BW). Nadpisuje wbudowaną bazę, jeśli kod jest taki sam.",
    vdbImport = "Importuj",
    vdbImportSuccess = "Zaimportowano %d wersetów jako",
    vdbImportFailed = "Import nie powiódł się",
    vdbExportCustom = "Eksport — Własna",
    vdbExportBuiltIn = "Eksport — Wbudowana",
    vdbExport = "Eksportuj",
    vdbExporting = "Zapisywanie do Pobrane…",
    vdbExportDone = "Zapisano do Pobrane",
    vdbExportFailed = "Eksport nie powiódł się",
    vdbJsonFormat = "Format JSON",

    // Backup & Restore
    backupTitle = "Kopia zapasowa i przywracanie",
    backupExport = "Utwórz kopię zapasową ustawień",
    backupExportDesc = "Zapisuje wszystkie ustawienia, tapetę i bazy danych do jednego pliku",
    backupImport = "Przywróć z kopii zapasowej",
    backupImportDesc = "Zastępuje wszystkie obecne ustawienia, tapetę i bazy danych",
    backupExporting = "Tworzenie kopii…",
    backupImporting = "Przywracanie…",
    backupExportSuccess = "Kopia zapisana · %d ustawień, %d baz danych",
    backupImportSuccess = "Kopia przywrócona · %d ustawień, %d baz danych. Restart…",
    backupExportFailed = "Tworzenie kopii nie powiodło się",
    backupImportFailed = "Przywracanie nie powiodło się",
    backupConfirmTitle = "Przywrócić z kopii zapasowej?",
    backupConfirmDesc = "To zastąpi wszystkie Twoje obecne ustawienia, tapetę i bazy danych. Tej czynności nie można cofnąć.",

    // Wallpaper gallery screen
    wpScreenTitle = "Tapety",
    wpScreenSubtitle = "Zarządzaj tapetami i automatyczną zmianą",
    wpGalleryEmpty = "Brak tapet",
    wpGalleryEmptyDesc = "Dodaj swoją pierwszą tapetę — wybierz zdjęcie z galerii.",
    wpAdd = "Dodaj tapetę",
    wpSetActive = "Użyj",
    wpActive = "Aktywna",
    wpDelete = "Usuń",
    wpDeleteConfirm = "Usunąć tę tapetę?",
    wpDeleteConfirmDesc = "Tapeta zostanie trwale usunięta z aplikacji.",
    wpCycleTitle = "Automatyczna zmiana",
    wpCycleDesc = "Zmieniaj tapety automatycznie według interwału",
    wpCycleInterval = "Interwał zmiany",
    wpCycleDailyHour = "Czas codziennej zmiany",
    wpCycleOnScreenOff = "Przy blokadzie",
    wpCycleOnScreenOffDesc = "Zmienia tapetę przy każdym zablokowaniu telefonu",
    wpNightMode = "Tryb nocny",
    wpNightModeDesc = "Używaj innej tapety w nocy",
    wpNightStart = "Początek nocy",
    wpNightEnd = "Koniec nocy",
    wpActiveBadge = "Aktywna",
    wpNightBadge = "Noc",
    wpCycleModeVerse = "Przy zmianie wersetu",
    wpCycleModeInterval = "Własny interwał",
    wpCycleModeScreenOff = "Przy każdej blokadzie",
    wpCycleModeDayNight = "Dzień / Noc",
    wpCycleModeVerseDesc = "Tapeta zmienia się z wersetem",
    wpCycleModeDayNightDesc = "Automatyczne przełączanie między tapetą dzienną i nocną",
    wpCycleDayWallpaper = "Tapeta dzienna",
    wpCycleNightWallpaper = "Tapeta nocna",
    wpCycleModeIntervalDesc = "Zmiana w stałych odstępach",
    wpCycleModeScreenOffDesc = "Zmiana przy każdej blokadzie",
    wpDualLockWarning = "Zmiana tapety przy wyłączonym ekranie może potrwać do 3 sekund.",
    wpDayStart = "Dzień zaczyna się",
    wpDayEnd = "Dzień kończy się",

    wpViewAll = "View all",
    wpViewAllTitle = "All wallpapers",
    wpSelectMode = "Select",
    wpDeleteSelected = "Delete selected",
    wpSelected = "selected",
    wpDeleteAllConfirm = "Delete selected wallpapers?",
    wpDeleteAllConfirmDesc = "The selected wallpapers will be permanently removed from the app.",
    wpPageHint = "Przesuń w lewo dla tapet →",

    vdbSourceDefault = "Wbudowane",
    vdbSourceCustom = "Własne",
    vdbEmptyCustom = "Brak własnych baz danych",
    vdbEmptyCustomDesc = "Utwórz własną kolekcję wersetów — np. KJV lub inne tłumaczenie.",
    vdbEmptyCustomCta = "Utwórz nową bazę danych",

    // Duplicate-code warnings + import conflict dialog
    vdbWarningCodeBuiltin = "Kod \"%s\" jest już używany przez wbudowaną bazę. Możesz go użyć — własna baza będzie współdzielić kod.",
    vdbWarningCodeCustom = "Kod \"%s\" już istnieje jako własna baza danych. Zapisanie ją nadpisze.",
    vdbImportConflictTitle = "Kod już istnieje",
    vdbImportConflictDesc = "Własna baza danych z kodem \"%s\" już istnieje. Wpisz inny kod, lub zostaw \"%s\" aby nadpisać.",
    vdbImportConflictAction = "Importuj z tym kodem",

    // Overwrite confirmation dialog (CreateEditScreen — Save with existing custom code)
    vdbOverwriteTitle = "Nadpisać bazę danych?",
    vdbOverwriteDesc = "Własna baza danych z kodem \"%s\" już istnieje. Zapisanie trwale zastąpi jej wersety. Tej czynności nie można cofnąć.",
    vdbOverwriteConfirm = "Nadpisz"
)
fun getDefaultAppLanguage(): String {
    val sysLang = Locale.getDefault().language.uppercase()
    return when (sysLang) {
        "CS" -> "CZ"
        "SK", "EN", "CZ", "ES", "IT", "FR", "DE", "HU", "PL" -> sysLang
        else -> "EN"
    }
}



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

// --- CUSTOM IN-APP NOTIFICATION ---
enum class NotificationType { SUCCESS, INFO, ERROR }

data class AppNotification(
    val message: String,
    val type: NotificationType = NotificationType.SUCCESS,
    val id: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    private lateinit var screenOffReceiver: ScreenOffReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Installs a crash handler and detects + logs whether the previous
        // session ended cleanly or was killed by the system / crashed.
        AppLogger.installCrashHandler(this)
        AppLogger.onAppStart(this)

        // Register screen-off receiver dynamically (required for ACTION_SCREEN_OFF on API 26+)
        screenOffReceiver = ScreenOffReceiver()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE) }


            var themeMode by remember { mutableIntStateOf(prefs.getInt("theme_mode", 0)) } // 0=System, 1=Light, 2=Dark
            var useDynamicColor by remember { mutableStateOf(prefs.getBoolean("use_dynamic_color", false)) }

            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true

                else -> isSystemInDarkTheme()
            }

            BibleLockScreenTheme(darkTheme = darkTheme, dynamicColor = useDynamicColor) {
                MainScreen(
                    themeMode = themeMode,

                    useDynamicColor = useDynamicColor,
                    onThemeChange = { newMode ->
                        themeMode = newMode
                        prefs.edit().putInt("theme_mode", newMode).apply()
                    },

                    onDynamicColorChange = { useDynamic ->
                        useDynamicColor = useDynamic
                        prefs.edit().putBoolean("use_dynamic_color", useDynamic).apply()
                    }

                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Refresh the "last known alive" timestamp so that if the process
        // gets killed while backgrounded, the next launch can report
        // roughly how long ago that happened.
        AppLogger.heartbeat(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(screenOffReceiver) } catch (e: Exception) { /* already unregistered */ }

        if (isFinishing) {
            // A real, user-initiated close (back/swipe from recents) — mark
            // this session as having shut down cleanly.
            AppLogger.onAppCleanExit(this)
        } else {
            // Being torn down for a config change / process recreation, not
            // an actual close — don't clear the "session open" flag.
            AppLogger.onAppConfigChangeDestroy(this)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeMode: Int,
    useDynamicColor: Boolean,
    onThemeChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val sysHaptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var activeNotification by remember { mutableStateOf<AppNotification?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isBatteryOptimized by remember { mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName)) }

    val scrollState = rememberScrollState()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var versePair by remember { mutableStateOf<Pair<String, String>?>(null) }

    val prefs = remember { context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE) }
    val defaultSystemLang = remember { getDefaultAppLanguage() }

    var bgDarkness by remember { mutableFloatStateOf(prefs.getFloat("bg_darkness", 0.23f)) }

    // --- STATES ---
    var hasSeenEditHint by remember { mutableStateOf(prefs.getBoolean("has_seen_edit_hint", false)) }
    var hasSeenSwipeHint by remember { mutableStateOf(prefs.getBoolean("has_seen_swipe_hint", false)) }

    LaunchedEffect(scrollState.value) {
        if (scrollState.value > 20 && !hasSeenSwipeHint) {
            hasSeenSwipeHint = true
            prefs.edit().putBoolean("has_seen_swipe_hint", true).apply()
        }
    }
    var dailyHour by remember { mutableIntStateOf(prefs.getInt("daily_hour", 6)) }
    var generationStatus by remember { mutableStateOf("idle") }

    var textSizeMult by remember { mutableFloatStateOf(prefs.getFloat("text_size_mult", 1.0f)) }
    var textWidthMult by remember { mutableFloatStateOf(prefs.getFloat("text_width_mult", 1.0f)) }
    var verticalOffset by remember { mutableFloatStateOf(prefs.getFloat("vertical_offset", 0.0f)) }
    var textColor by remember { mutableIntStateOf(prefs.getInt("text_color", AndroidColor.WHITE)) }
    var textAlpha by remember { mutableFloatStateOf(prefs.getFloat("text_alpha", 1.0f)) }
    var bgBlur by remember { mutableFloatStateOf(prefs.getFloat("bg_blur", 0f)) }
    var isBold by remember { mutableStateOf(prefs.getBoolean("is_bold", true)) }
    var useShadow by remember { mutableStateOf(prefs.getBoolean("use_shadow", true)) }
    var fontFamilyStr by remember { mutableStateOf(prefs.getString("font_family", "sans-serif") ?: "sans-serif") }
    var useHaptics by remember { mutableStateOf(prefs.getBoolean("use_haptics", true)) }
    // 0 = Lock screen only, 1 = Home screen only, 2 = Both
    var wallpaperTarget by remember { mutableIntStateOf(prefs.getInt("wallpaper_target", 0)) }

    var appLang by remember { mutableStateOf(prefs.getString("app_lang", defaultSystemLang) ?: defaultSystemLang) }
    var verseLang by remember { mutableStateOf(prefs.getString("verse_lang", defaultSystemLang) ?: defaultSystemLang) }
    // Track whether the selected verse language comes from a built-in asset
    // (SOURCE_BUILTIN) or a user-created custom database (SOURCE_CUSTOM).
    // This disambiguates the case where a custom DB shares a code with a
    // built-in (e.g. a custom "EN" overriding the built-in "EN").
    var verseLangSource by remember {
        mutableStateOf(
            prefs.getString("verse_lang_source", LocalBibleProvider.SOURCE_BUILTIN)
                ?: LocalBibleProvider.SOURCE_BUILTIN
        )
    }
    // Per-segment "last selected code" memory. When the user flips the
    // Default / Custom segmented toggle, we auto-apply the last code they
    // had chosen in that segment (so e.g. switching Default→Custom→Default
    // restores the built-in language they had picked, e.g. Slovak).
    // Defaults: builtin → app language, custom → first custom DB if any.
    var lastBuiltinCode by remember {
        mutableStateOf(prefs.getString("last_builtin_code", appLang) ?: appLang)
    }
    var lastCustomCode by remember {
        mutableStateOf(prefs.getString("last_custom_code", "") ?: "")
    }

    // Strings
    val strings = when (appLang) {
        "EN" -> enStrings
        "CZ" -> czStrings
        "ES" -> esStrings
        "IT" -> itStrings
        "FR" -> frStrings
        "DE" -> deStrings
        "HU" -> huStrings
        "PL" -> plStrings
        else -> skStrings
    }

    // Edit Mode a Settings
    var isEditing by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showDevMenu by remember { mutableStateOf(false) }
    // Verse-database sheet state — driven by both the "Manage" button in the
    // Verse databases section and the "Create new database" CTA inside the
    // VerseLanguagePicker (when the custom list is empty).
    var showDbSheet by remember { mutableStateOf(false) }
    var dbSheetOpenCreate by remember { mutableStateOf(false) }
    // Hoisted so both the settings sheet (picker + manage button) and the
    // outer VerseDatabaseSheet host can read/refresh the same list.
    var customDbs by remember { mutableStateOf(VerseJsonManager.listCustomDatabases(context)) }
    var isDailyActive by remember { mutableStateOf(prefs.getBoolean("auto_wallpaper_active", false)) }
    var autoIntervalHours by remember { mutableIntStateOf(prefs.getInt("auto_interval_hours", 24)) }
    var changeOnScreenOff by remember { mutableStateOf(prefs.getBoolean("change_on_screen_off", false)) }

    // Backup & Restore state
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    // Custom Haptic Helper
    val performHaptic = { type: HapticFeedbackType ->
        if (useHaptics) {
            sysHaptic.performHapticFeedback(type)
        }
    }

    // Custom Notification Helper
    val showNotification = { message: String, type: NotificationType ->
        activeNotification = AppNotification(message = message, type = type)
        scope.launch {
            kotlinx.coroutines.delay(3000)
            activeNotification = null
        }
    }

    // One-time-per-launch diagnostic: report the state of the scheduled
    // wallpaper WorkManager jobs to the Developer Logs. If auto-wallpaper is
    // supposed to be running but WorkManager has no record of it (or it's
    // CANCELLED), that's a strong sign the system cleared the scheduled job.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val workManager = WorkManager.getInstance(context)
                val dailyInfos = workManager.getWorkInfosForUniqueWork("DailyBibleWallpaper").get()
                if (dailyInfos.isEmpty()) {
                    if (isDailyActive) {
                        AppLogger.w(context, "WorkManager", "Auto-wallpaper is enabled but no scheduled work exists — the system may have cleared it.")
                    }
                } else {
                    dailyInfos.forEach { info ->
                        AppLogger.i(context, "WorkManager", "DailyBibleWallpaper: state=${info.state}, runAttempts=${info.runAttemptCount}")
                    }
                }
                val cyclingInfos = workManager.getWorkInfosForUniqueWork("WallpaperCycling").get()
                cyclingInfos.forEach { info ->
                    AppLogger.i(context, "WorkManager", "WallpaperCycling: state=${info.state}, runAttempts=${info.runAttemptCount}")
                }
            } catch (e: Exception) {
                AppLogger.e(context, "WorkManager", "Failed to read scheduled work status: ${e.message}")
            }
        }
    }

    // Backup & Restore launchers + restore function — must be declared
    // after showNotification and scope, both of which they reference.
    // Export: user picks where to save the backup ZIP
    val backupSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isBackingUp = true
            scope.launch {
                SettingsBackupManager.export(context, uri).fold(
                    onSuccess = { summary ->
                        isBackingUp = false
                        showNotification(
                            strings.backupExportSuccess.format(summary.prefsCount, summary.databaseCount),
                            NotificationType.SUCCESS
                        )
                    },
                    onFailure = {
                        isBackingUp = false
                        showNotification(strings.backupExportFailed, NotificationType.ERROR)
                    }
                )
            }
        }
    }

    // Import: user picks a backup ZIP to restore from
    val backupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirm = true
        }
    }

    fun performRestore(uri: Uri) {
        isRestoring = true
        scope.launch {
            SettingsBackupManager.import(context, uri).fold(
                onSuccess = { summary ->
                    isRestoring = false
                    showNotification(
                        strings.backupImportSuccess.format(summary.prefsCount, summary.databaseCount),
                        NotificationType.SUCCESS
                    )
                    // Recreate the Activity so all `remember { prefs... }`
                    // state re-initializes from the freshly-written prefs,
                    // and the restored wallpaper is picked up.
                    kotlinx.coroutines.delay(800)
                    (context as? android.app.Activity)?.recreate()
                },
                onFailure = {
                    isRestoring = false
                    showNotification(strings.backupImportFailed, NotificationType.ERROR)
                }
            )
        }
    }

    // Function to reload data so it catches background changes (like new verse or worker wallpaper change)
    val reloadPreviewData = {
        val useCustomVerse = prefs.getBoolean("use_custom_verse", false)
        val savedCustomVerse = prefs.getString("custom_verse_text", null)
        val savedCustomRef = prefs.getString("custom_verse_ref", null)

        if (useCustomVerse && !savedCustomVerse.isNullOrEmpty()) {
            versePair = Pair(savedCustomVerse, savedCustomRef ?: "")
        } else {
            scope.launch {
                versePair = LocalBibleProvider.getVerseForInterval(context, verseLang, autoIntervalHours, verseLangSource)
            }
        }

        val localFile = java.io.File(context.filesDir, "user_wallpaper.jpg")
        if (localFile.exists()) {
            val internalUri = Uri.fromFile(localFile).buildUpon()
                .appendQueryParameter("v", System.currentTimeMillis().toString())
                .build()
            imageUri = internalUri
        } else {
            val savedUri = prefs.getString("bg_uri", null)
            if (savedUri != null) imageUri = Uri.parse(savedUri)
        }
    }

    // Obnovíme stav optimalizácie a preview dáta vždy, keď sa užívateľ vráti do aplikácie
    DisposableEffect(lifecycleOwner, verseLang) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
                reloadPreviewData() // Update UI in case midnight has passed while app was backgrounded
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        // One-time migration: if the user already has a user_wallpaper.jpg
        // (from the old single-wallpaper system) but the gallery is empty,
        // import it into the managed wallpapers/ folder so it shows up in
        // the wallpaper screen and gets included in backups.
        val existingActive = java.io.File(context.filesDir, "user_wallpaper.jpg")
        val gallery = WallpaperManager.listWallpapers(context)
        if (existingActive.exists() && gallery.isEmpty()) {
            val id = "wp_${System.currentTimeMillis()}"
            val target = java.io.File(java.io.File(context.filesDir, "wallpapers").also { it.mkdirs() }, "$id.jpg")
            existingActive.copyTo(target, overwrite = true)
            prefs.edit().putString("active_wallpaper_id", id).apply()
        }

        // Initial setup and background worker observer check
        reloadPreviewData()
    }

    // --- SAVE & ACTIONS ---
    fun saveSettings() {
        prefs.edit()
            .putFloat("text_size_mult", textSizeMult)
            .putFloat("text_width_mult", textWidthMult)
            .putFloat("vertical_offset", verticalOffset)
            .putInt("text_color", textColor)
            .putFloat("text_alpha", textAlpha)
            .putFloat("bg_blur", bgBlur)
            .putBoolean("is_bold", isBold)
            .putBoolean("use_shadow", useShadow)
            .putString("font_family", fontFamilyStr)
            .putBoolean("use_haptics", useHaptics)
            .putFloat("bg_darkness", bgDarkness)
            .putInt("wallpaper_target", wallpaperTarget)
            .apply()
    }

    fun toggleAutoWorker(enable: Boolean) {
        performHaptic(HapticFeedbackType.LongPress)
        saveSettings()
        isDailyActive = enable
        // Save master active state so ScreenOffReceiver can check it
        prefs.edit().putBoolean("auto_wallpaper_active", enable).apply()

        if (enable) {
            prefs.edit().putBoolean("use_custom_verse", false).apply()
            versePair = LocalBibleProvider.getVerseForInterval(context, verseLang, autoIntervalHours, verseLangSource)
            if (!changeOnScreenOff) {
                scheduleAutoWallpaper(context, autoIntervalHours, dailyHour)
            }
            showNotification(strings.autoWorkerOn, NotificationType.SUCCESS)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("DailyBibleWallpaper")
            showNotification(strings.autoWorkerOff, NotificationType.INFO)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { sourceUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    // Import the picked photo into the managed wallpapers/
                    // gallery so it survives even if the user deletes the
                    // original from storage. Returns the new wallpaper id.
                    val newId = WallpaperManager.addWallpaper(context, sourceUri)
                    if (newId != null) {
                        // Set it as the active wallpaper (copies to user_wallpaper.jpg)
                        WallpaperManager.setActiveWallpaper(context, newId)
                        prefs.edit().putString("active_wallpaper_id", newId).apply()
                    }

                    // Also keep the legacy copy for backward compatibility
                    val fileName = "user_wallpaper.jpg"
                    val destinationFile = java.io.File(context.filesDir, fileName)

                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        destinationFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val timestamp = System.currentTimeMillis()
                        val internalUriWithCacheBreaker = Uri.fromFile(destinationFile).buildUpon()
                            .appendQueryParameter("v", timestamp.toString())
                            .build()

                        imageUri = null
                        imageUri = internalUriWithCacheBreaker

                        val baseInternalUri = Uri.fromFile(destinationFile).toString()
                        prefs.edit().putString("bg_uri", baseInternalUri).apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler(enabled = isEditing || showSettings || showDbSheet || showDevMenu) {
        if (isEditing) isEditing = false
        else if (showDbSheet) showDbSheet = false
        else if (showDevMenu) showDevMenu = false
        else if (showSettings) scope.launch {
            settingsSheetState.hide()
            showSettings = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Horizontal pager: page 0 = main screen, page 1 = wallpaper gallery
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
        // Sync pager page when user swipes — used by the page indicator
        val scope2 = rememberCoroutineScope()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DevMenuTriggerIcon(
                                onTriggered = {
                                    performHaptic(HapticFeedbackType.LongPress)
                                    showDevMenu = true
                                },
                                onHoldStart = { performHaptic(HapticFeedbackType.LongPress) }
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(strings.appName, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        FilledIconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.padding(end = 8.dp).size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = strings.settings, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            snackbarHost = { },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            // ── Main scrollable Column (NOT inside a pager) ──────────────
            // Only the preview area swipes horizontally; the settings below
            // stay in place and scroll vertically with the rest of the page.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. PREVIEW AREA — wrapped in a HorizontalPager so swiping
                //    left/right switches between the lock-screen preview and
                //    the wallpaper gallery. Only this area swipes; the
                //    settings below do not.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                ) { page ->
                    if (page == 0) {
                        // Parallax zoom-out on the preview only — the wallpaper
                        // screen (page 1) scrolls normally without this effect.
                        Box(
                            modifier = Modifier.graphicsLayer {
                                val scrollOffset = scrollState.value.toFloat()
                                val scale = (1f - (scrollOffset / 1500f)).coerceIn(0.6f, 1f)
                                val alphaVal = (1f - (scrollOffset / 900f)).coerceIn(0.5f, 1f)
                                scaleX = scale
                                scaleY = scale
                                alpha = alphaVal
                                translationY = scrollOffset * 0.5f
                            }
                        ) {
                            Pixel6LockScreenPreview(
                                uri = imageUri,
                                verseText = versePair?.first ?: strings.loading,
                                verseReference = versePair?.second ?: "",
                                textSizeMult = textSizeMult,
                                textWidthMult = textWidthMult,
                                verticalOffset = verticalOffset,
                                textColor = textColor,
                                textAlpha = textAlpha,
                                bgBlur = bgBlur,
                                bgDarkness = bgDarkness,
                                isBold = isBold,
                                useShadow = useShadow,
                                fontFamilyStr = fontFamilyStr,
                                showEditHint = true,
                                strings = strings,
                                showBubbleHint = imageUri != null && !hasSeenEditHint,
                                onClick = {
                                    performHaptic(HapticFeedbackType.LongPress)
                                    launcher.launch("image/*")
                                },
                                onEditClick = {
                                    performHaptic(HapticFeedbackType.LongPress)
                                    isEditing = true
                                    if (!hasSeenEditHint) {
                                        hasSeenEditHint = true
                                        prefs.edit().putBoolean("has_seen_edit_hint", true).apply()
                                    }
                                }
                            )
                        }
                    } else {
                        WallpaperScreen(
                            strings = strings,
                            showNotification = showNotification,
                            onWallpaperChanged = {
                                imageUri = WallpaperManager.activeWallpaperUri(context)
                                reloadPreviewData()
                            }
                        )
                    }
                }

                // ── Page indicator dots (under the preview) ──────────────
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                            label = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .size(width = width, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                                .clickable {
                                    scope2.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                        )
                    }
                }

                // ── Swipe hint (shows on page 0 until user swipes once) ──
                AnimatedVisibility(
                    visible = pagerState.currentPage == 0 && !hasSeenSwipeHint,
                    enter = fadeIn(tween(250)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    ),
                    exit = fadeOut(tween(200)) + slideOutVertically(
                        targetOffsetY = { -it / 2 },
                        animationSpec = tween(200)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            strings.wpPageHint,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. OBLASŤ NASTAVENÍ
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pill drag handle — bounces while user hasn't scrolled yet
                    val showSwipeHint = imageUri != null && !hasSeenSwipeHint && !isEditing
                    val infiniteTransition = rememberInfiniteTransition(label = "pill_bounce")
                    val pillOffsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = if (showSwipeHint) -8f else 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pillOffsetY"
                    )
                    val pillWidth by infiniteTransition.animateFloat(
                        initialValue = 40f,
                        targetValue = if (showSwipeHint) 56f else 40f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pillWidth"
                    )
                    val pillAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.12f,
                        targetValue = if (showSwipeHint) 0.5f else 0.12f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pillAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterHorizontally)
                            .width(pillWidth.dp)
                            .height(4.dp)
                            .offset(y = pillOffsetY.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = pillAlpha),
                                RoundedCornerShape(2.dp)
                            )
                    )

                    // AUTOMATIC WALLPAPER CHANGE — MAIN TOGGLE CARD
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Icon badge
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        color = if (isDailyActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isDailyActive) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            // Title + subtitle — weight(1f) so Switch is never overlapped
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.autoWallpaper,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDailyActive) {
                                        when {
                                            changeOnScreenOff -> strings.autoWallpaperActiveScreenOff
                                            autoIntervalHours < 24 -> String.format(strings.autoWallpaperActiveHourly, autoIntervalHours)
                                            else -> String.format(strings.autoWallpaperActiveDaily, String.format("%02d", dailyHour))
                                        }
                                    } else {
                                        strings.inactive
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDailyActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = isDailyActive,
                                onCheckedChange = { toggleAutoWorker(it) }
                            )
                        }
                    }

                    // AUTO WALLPAPER SUB-MENU (visible only when toggle is on)
                    AnimatedVisibility(
                        visible = isDailyActive,
                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(280)),
                        exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(220))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                            // ── INTERVAL ROW ─────────────────────────────────
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (changeOnScreenOff)
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {

                                    // Header: label on left, current value pill on right
                                    val intervalLabel = when (autoIntervalHours) {
                                        1 -> strings.autoWallpaperEvery1h
                                        2 -> strings.autoWallpaperEvery2h
                                        3 -> strings.autoWallpaperEvery3h
                                        6 -> strings.autoWallpaperEvery6h
                                        12 -> strings.autoWallpaperEvery12h
                                        else -> strings.autoWallpaperEvery24h
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = strings.autoWallpaperIntervalLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (changeOnScreenOff) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        // Pill badge showing selected value
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (changeOnScreenOff)
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                    else
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = intervalLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (changeOnScreenOff)
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                else
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    // Slider snapping to: 1 2 3 6 12 24
                                    val intervalSteps = listOf(1, 2, 3, 6, 12, 24)
                                    val sliderIndex = intervalSteps.indexOf(autoIntervalHours).coerceAtLeast(0).toFloat()
                                    Slider(
                                        value = sliderIndex,
                                        onValueChange = { raw ->
                                            val idx = raw.roundToInt().coerceIn(0, intervalSteps.lastIndex)
                                            val hours = intervalSteps[idx]
                                            if (hours != autoIntervalHours) {
                                                autoIntervalHours = hours
                                                prefs.edit().putInt("auto_interval_hours", hours).apply()
                                                performHaptic(HapticFeedbackType.TextHandleMove)
                                                reloadPreviewData()
                                                if (isDailyActive && !changeOnScreenOff) {
                                                    scheduleAutoWallpaper(context, hours, dailyHour)
                                                }
                                            }
                                        },
                                        valueRange = 0f..(intervalSteps.lastIndex.toFloat()),
                                        steps = intervalSteps.lastIndex - 1,
                                        enabled = !changeOnScreenOff,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            disabledThumbColor = MaterialTheme.colorScheme.outline,
                                            disabledActiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Tick labels below slider
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        intervalSteps.forEach { h ->
                                            Text(
                                                text = if (h < 24) "${h}h" else "24h",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (!changeOnScreenOff && autoIntervalHours == h)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (changeOnScreenOff) 0.4f else 0.7f),
                                                fontWeight = if (!changeOnScreenOff && autoIntervalHours == h) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // ── TIME OF DAY ROW (for 12h and 24h, non screen-off) ──
                            AnimatedVisibility(
                                visible = (autoIntervalHours == 12 || autoIntervalHours == 24) && !changeOnScreenOff,
                                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(200)),
                                exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
                            ) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                        val is12h = autoIntervalHours == 12
                                        val displayHour = if (is12h) dailyHour % 12 else dailyHour
                                        val secondHour = if (is12h) (displayHour + 12) % 24 else displayHour
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = strings.autoWallpaperTimeLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        RoundedCornerShape(20.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (is12h) String.format("%02d:00 / %02d:00", displayHour, secondHour)
                                                    else String.format("%02d:00", dailyHour),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Slider(
                                            value = displayHour.toFloat(),
                                            onValueChange = {
                                                val newHour = it.roundToInt()
                                                if (newHour != displayHour) {
                                                    performHaptic(HapticFeedbackType.TextHandleMove)
                                                }
                                                dailyHour = newHour
                                            },
                                            valueRange = 0f..(if (is12h) 11f else 23f),
                                            steps = if (is12h) 10 else 22,
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        LaunchedEffect(dailyHour) {
                                            prefs.edit().putInt("daily_hour", dailyHour).apply()
                                            if (isDailyActive && (autoIntervalHours == 12 || autoIntervalHours == 24) && !changeOnScreenOff) {
                                                scheduleAutoWallpaper(context, autoIntervalHours, dailyHour)
                                            }
                                        }
                                    }
                                }
                            }

                            // ── ON SCREEN OFF ROW ────────────────────────────
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = if (changeOnScreenOff) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.TouchApp,
                                            contentDescription = null,
                                            tint = if (changeOnScreenOff) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.autoWallpaperOnScreenOff,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = strings.autoWallpaperOnScreenOffDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                    Switch(
                                        checked = changeOnScreenOff,
                                        onCheckedChange = { enabled ->
                                            changeOnScreenOff = enabled
                                            prefs.edit().putBoolean("change_on_screen_off", enabled).apply()
                                            performHaptic(HapticFeedbackType.LongPress)
                                            if (isDailyActive) {
                                                if (enabled) {
                                                    WorkManager.getInstance(context).cancelUniqueWork("DailyBibleWallpaper")
                                                } else {
                                                    scheduleAutoWallpaper(context, autoIntervalHours, dailyHour)
                                                }
                                            }
                                        }
                                    )
                                }
                                // Warning when verse-on-lock is enabled (animated)
                                AnimatedVisibility(
                                    visible = changeOnScreenOff,
                                    enter = expandVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + fadeIn(tween(250)),
                                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Warning, null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                strings.wpDualLockWarning,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }


                    // BATERIOVE UPOZORNENIE
                    AnimatedVisibility(
                        visible = isDailyActive && isBatteryOptimized,
                        enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(250))
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = strings.batteryWarningTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = strings.batteryWarningDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                        contentColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(strings.batteryWarningButton)
                                }
                            }
                        }
                    }

                    // CUSTOM VERSE EDITOR
                    AnimatedVisibility(
                        visible = !isDailyActive,
                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(280)),
                        exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(220))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(strings.customVerseTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }

                            var localVerse by remember(versePair) { mutableStateOf(versePair?.first ?: "") }
                            var localRef by remember(versePair) { mutableStateOf(versePair?.second ?: "") }

                            OutlinedTextField(
                                value = localVerse,
                                onValueChange = { localVerse = it },
                                label = { Text(strings.customVerseHint) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                minLines = 2,
                                maxLines = 4
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = localRef,
                                    onValueChange = { localRef = it },
                                    label = { Text(strings.customRefHint) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (localVerse.isNotBlank()) {
                                            versePair = Pair(localVerse, localRef)
                                            prefs.edit()
                                                .putString("custom_verse_text", localVerse)
                                                .putString("custom_verse_ref", localRef)
                                                .putBoolean("use_custom_verse", true)
                                                .apply()
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            showNotification(strings.done, NotificationType.SUCCESS)
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Icon(Icons.Outlined.Check, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.applyCustom)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // NASTAVENIA
                    if (imageUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TextFormat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(strings.textCustomization, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        // Farba textu
                        ColorPickerRow(selectedColor = textColor, strings = strings) { newColor ->
                            textColor = newColor
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            saveSettings()
                            if (imageUri != null) runOneTimeWorker(context)
                        }

                        // Písmo (Font)
                        FontPickerRow(selectedFont = fontFamilyStr, strings = strings) {
                            fontFamilyStr = it; performHaptic(HapticFeedbackType.TextHandleMove);
                            saveSettings()
                        }

                        // Štýly
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(
                                selected = isBold,
                                onClick = { isBold = !isBold; performHaptic(HapticFeedbackType.TextHandleMove); saveSettings() },
                                label = { Text(strings.bold) },
                                leadingIcon = { Icon(Icons.Outlined.FormatBold, null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = useShadow,
                                onClick = { useShadow = !useShadow; performHaptic(HapticFeedbackType.TextHandleMove); saveSettings() },
                                label = { Text(strings.shadow) },
                                leadingIcon = { Icon(Icons.Default.Hd, null) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Slidery
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                EnhancedSlider(
                                    label = strings.bgBlur,
                                    value = bgBlur,
                                    range = 0f..25f,
                                    defaultVal = 0f,
                                    steps = 24,
                                    icon = Icons.Outlined.BlurOn,
                                    performHaptic = performHaptic,
                                    onValueChange = { bgBlur = it;
                                        saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.textSize,
                                    value = textSizeMult,
                                    range = 0.5f..2.0f,
                                    defaultVal = 1.0f,
                                    steps = 14,
                                    icon = Icons.Default.TextFormat,
                                    performHaptic = performHaptic,
                                    onValueChange = { textSizeMult = it; saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.textWidth,
                                    value = textWidthMult,
                                    range = 0.5f..1.2f,
                                    defaultVal = 1.0f,
                                    steps = 6,
                                    icon = Icons.Default.FormatAlignJustify,
                                    performHaptic = performHaptic,
                                    onValueChange = { textWidthMult = it; saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.textHeight,
                                    value = verticalOffset,
                                    range = -1.0f..1.0f,
                                    defaultVal = 0.0f,
                                    steps = 19,
                                    icon = Icons.Default.SwapVert,
                                    performHaptic = performHaptic,
                                    onValueChange = { verticalOffset = it; saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.textAlpha,
                                    value = textAlpha,
                                    range = 0.2f..1.0f,
                                    defaultVal = 1.0f,
                                    steps = 7,
                                    icon = Icons.Default.Opacity,
                                    performHaptic = performHaptic,
                                    onValueChange = { textAlpha = it;
                                        saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.bgDarknessLabel,
                                    value = bgDarkness,
                                    range = 0f..0.8f,
                                    defaultVal = 0.2f, //0.23 je default
                                    steps = 7,
                                    icon = Icons.Default.BrightnessMedium,
                                    performHaptic = performHaptic,
                                    onValueChange = {
                                        bgDarkness = it
                                        saveSettings()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Akčné tlačidlá
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { launcher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.anotherPhoto, style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                enabled = generationStatus == "idle" && imageUri != null,
                                onClick = {
                                    if (imageUri != null) {
                                        performHaptic(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            generationStatus = "generating"
                                            runOneTimeWorker(context)
                                            kotlinx.coroutines.delay(2000)
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            generationStatus = "Done"
                                            kotlinx.coroutines.delay(2000)
                                            generationStatus = "idle"
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                AnimatedContent(
                                    targetState = generationStatus == "generating",
                                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                                    label = "gen_btn"
                                ) { isGenerating ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isGenerating) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                        }
                                        Text(if (isGenerating) strings.generatingBtn else strings.test, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clickable { launcher.launch("image/*") },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                    Text(strings.selectPhotoFirst, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                } // end settings Column
            } // end main Column
        } // end Scaffold content lambda

        // Mark swipe hint as seen when user leaves page 0
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != 0 && !hasSeenSwipeHint) {
                hasSeenSwipeHint = true
                prefs.edit().putBoolean("has_seen_swipe_hint", true).apply()
            }
        }

        // Nastavenia Aplikácie
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = settingsSheetState,
                dragHandle = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            BottomSheetDefaults.DragHandle()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 8.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.settings,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        settingsSheetState.hide()
                                        showSettings = false
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = strings.close,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {

                    // --- LANGUAGE SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Default.Language,
                        title = strings.language
                    )
                    SettingsCard {
                        LanguageDropdown(
                            label = strings.appLanguage,
                            selectedCode = appLang,
                            options = availableLanguages,
                            showLabel = true,
                            onSelect = {
                                appLang = it
                                prefs.edit().putString("app_lang", it).apply()
                            },
                            dismissLabel = strings.cancel,
                            dialogIcon = Icons.Default.Language
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        // Verse language is now selected via the new picker —
                        // a Default / Custom segmented toggle keeps the two
                        // sources visually separated instead of cramming them
                        // into one long dropdown.
                        VerseLanguagePicker(
                            strings = strings,
                            selectedCode = verseLang,
                            selectedSource = verseLangSource,
                            customDbs = customDbs,
                            onSelect = { code, source ->
                                if (verseLang != code || verseLangSource != source) {
                                    verseLang = code
                                    verseLangSource = source
                                    prefs.edit().putString("verse_lang", code).apply()
                                    prefs.edit().putString("verse_lang_source", source).apply()
                                    // Remember the code per-segment so switching
                                    // back to this segment restores it.
                                    if (source == LocalBibleProvider.SOURCE_BUILTIN) {
                                        lastBuiltinCode = code
                                        prefs.edit().putString("last_builtin_code", code).apply()
                                    } else {
                                        lastCustomCode = code
                                        prefs.edit().putString("last_custom_code", code).apply()
                                    }
                                    versePair = null
                                    scope.launch {
                                        versePair = LocalBibleProvider.getVerseForInterval(context, code, autoIntervalHours, source)
                                    }
                                }
                            },
                            onCreateCustom = {
                                dbSheetOpenCreate = true
                                showDbSheet = true
                            },
                            onSegmentChange = { newSource ->
                                // The user tapped the segmented toggle. Auto-apply
                                // the last-selected code for the new segment.
                                when (newSource) {
                                    LocalBibleProvider.SOURCE_BUILTIN -> {
                                        // Fall back to appLang if no built-in was ever picked.
                                        val code = lastBuiltinCode.ifBlank { appLang }
                                        if (verseLang != code || verseLangSource != newSource) {
                                            verseLang = code
                                            verseLangSource = newSource
                                            prefs.edit().putString("verse_lang", code).apply()
                                            prefs.edit().putString("verse_lang_source", newSource).apply()
                                            lastBuiltinCode = code
                                            prefs.edit().putString("last_builtin_code", code).apply()
                                            versePair = null
                                            scope.launch {
                                                versePair = LocalBibleProvider.getVerseForInterval(context, code, autoIntervalHours, newSource)
                                            }
                                        }
                                    }
                                    LocalBibleProvider.SOURCE_CUSTOM -> {
                                        if (customDbs.isEmpty()) {
                                            // No custom DBs exist — switch the source so the
                                            // picker stays on the Custom segment and shows the
                                            // empty-state CTA. Don't touch verseLang or reload
                                            // verses (there's nothing to load yet).
                                            if (verseLangSource != newSource) {
                                                verseLangSource = newSource
                                                prefs.edit().putString("verse_lang_source", newSource).apply()
                                            }
                                        } else {
                                            // Pick the last custom code, or fall back to the
                                            // first available custom DB.
                                            val code = lastCustomCode.ifBlank { customDbs.first().lang }
                                            if (verseLang != code || verseLangSource != newSource) {
                                                verseLang = code
                                                verseLangSource = newSource
                                                prefs.edit().putString("verse_lang", code).apply()
                                                prefs.edit().putString("verse_lang_source", newSource).apply()
                                                lastCustomCode = code
                                                prefs.edit().putString("last_custom_code", code).apply()
                                                versePair = null
                                                scope.launch {
                                                    versePair = LocalBibleProvider.getVerseForInterval(context, code, autoIntervalHours, newSource)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- VERSE DATABASES SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Default.LibraryBooks,
                        title = strings.vdbTitle
                    )
                    SettingsCard {
                        VerseDatabaseSection(
                            strings = strings,
                            showNotification = showNotification,
                            onDbChanged = {
                                customDbs = VerseJsonManager.listCustomDatabases(context)
                            },
                            // Delegate sheet hosting to MainScreen so both the
                            // "Manage" button and the picker's "Create new"
                            // CTA route through the same showDbSheet state.
                            onManage = {
                                dbSheetOpenCreate = false
                                showDbSheet = true
                            }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- APPEARANCE SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Default.Palette,
                        title = strings.appearance
                    )
                    SettingsCard {
                        val themeOptions = listOf(strings.system, strings.light, strings.dark)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themeOptions.forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = themeMode == index,
                                    onClick = { onThemeChange(index) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Wallpaper,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(strings.wallpaperTargetLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        val targetOptions = listOf(
                            strings.wallpaperTargetLock,
                            strings.wallpaperTargetHome,
                            strings.wallpaperTargetBoth
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            targetOptions.forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = wallpaperTarget == index,
                                    onClick = {
                                        wallpaperTarget = index
                                        saveSettings()
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = targetOptions.size)
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.ColorLens,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(strings.dynamicColor, style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = useDynamicColor, onCheckedChange = { onDynamicColorChange(it) })
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- HAPTICS SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Fingerprint,
                        title = strings.haptics
                    )
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.haptics, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(strings.hapticsDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = useHaptics,
                                onCheckedChange = {
                                    useHaptics = it
                                    saveSettings()
                                    if (it) sysHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- BACKUP & RESTORE SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Settings,
                        title = strings.backupTitle
                    )
                    SettingsCard {
                        // Export
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Upload, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    strings.backupExport,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    strings.backupExportDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                        backupSaver.launch("bible_lockscreen_backup_$dateStr.zip")
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(strings.backupExport, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Import
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Download, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    strings.backupImport,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    strings.backupImportDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                FilledTonalButton(
                                    onClick = { backupPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(strings.backupImport, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    // Restore confirmation dialog
                    if (showRestoreConfirm) {
                        AlertDialog(
                            onDismissRequest = { showRestoreConfirm = false },
                            icon = {
                                Icon(
                                    Icons.Default.Warning, null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            title = { Text(strings.backupConfirmTitle) },
                            text = {
                                Text(
                                    strings.backupConfirmDesc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showRestoreConfirm = false
                                        pendingRestoreUri?.let { performRestore(it) }
                                        pendingRestoreUri = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) { Text(strings.backupImport) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRestoreConfirm = false }) { Text(strings.cancel) }
                            }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- SUPPORT SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Favorite,
                        title = strings.support
                    )
                    SettingsCard {
                        Text(
                            strings.supportDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/daklok"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5E5B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.Favorite, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.donate, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Verse-database sheet — opened either by the "Manage" button in the
        // Verse databases section, or by the "Create new database" CTA inside
        // the VerseLanguagePicker's empty custom state.
        if (showDbSheet) {
            VerseDatabaseSheet(
                strings = strings,
                showNotification = showNotification,
                onDismiss = { showDbSheet = false },
                onDbChanged = {
                    customDbs = VerseJsonManager.listCustomDatabases(context)
                },
                openCreate = dbSheetOpenCreate
            )
        }

        // Developer log sheet — opened by holding the book icon in the top
        // bar for 3 seconds. Shows AppLogger's on-disk log, including any
        // warning about the previous session having been killed by the
        // system rather than closed cleanly.
        if (showDevMenu) {
            DeveloperLogSheet(
                onDismiss = { showDevMenu = false },
                onClear = { AppLogger.clearLogs(context) }
            )
        }

        // Expressive Full-Screen Editor s animáciou
        AnimatedVisibility(
            visible = isEditing,
            enter = scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(250))
        ) {
            FullScreenEditor(
                uri = imageUri,
                verseText = versePair?.first ?: strings.loading,
                verseReference = versePair?.second ?: "",
                initialTextSizeMult = textSizeMult,
                initialTextWidthMult = textWidthMult,
                initialVerticalOffset = verticalOffset,
                textColor = textColor,
                textAlpha = textAlpha,
                bgBlur = bgBlur,
                bgDarkness = bgDarkness,
                isBold = isBold,
                useShadow = useShadow,
                fontFamilyStr = fontFamilyStr,
                strings = strings,
                onSave = { newSize, newWidth, newOffset ->
                    textSizeMult = newSize
                    textWidthMult = newWidth
                    verticalOffset = newOffset
                    saveSettings()
                    isEditing = false
                },
                onDismiss = {
                    isEditing = false
                },
                performHaptic = performHaptic
            )
        }

        // Floating Status Popup
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = generationStatus != "idle",
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }) + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }) + scaleOut(targetScale = 0.9f)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (generationStatus == "generating") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(strings.generating, style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(strings.done, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Custom In-App Notification Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val rememberedNotification = remember { mutableStateOf<AppNotification?>(null) }
            if (activeNotification != null) rememberedNotification.value = activeNotification

            AnimatedVisibility(
                visible = activeNotification != null,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
                exit = fadeOut(tween(350)) + slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(350, easing = FastOutLinearInEasing)
                ) + scaleOut(targetScale = 0.9f, animationSpec = tween(350))
            ) {
                rememberedNotification.value?.let { notification ->
                    val containerColor = when (notification.type) {
                        NotificationType.SUCCESS -> MaterialTheme.colorScheme.secondaryContainer
                        NotificationType.INFO -> MaterialTheme.colorScheme.secondaryContainer
                        NotificationType.ERROR -> MaterialTheme.colorScheme.errorContainer
                    }
                    val contentColor = when (notification.type) {
                        NotificationType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                        NotificationType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
                        NotificationType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                    }
                    val notifIcon = when (notification.type) {
                        NotificationType.SUCCESS -> Icons.Outlined.Check
                        NotificationType.INFO -> Icons.Default.Info
                        NotificationType.ERROR -> Icons.Default.Warning
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = containerColor,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = notifIcon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = notification.message,
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }

    }
}



// --- KOMPONENTY ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(
    label: String,
    selectedCode: String,
    options: List<Pair<String, String>>,
    showLabel: Boolean = true,
    onSelect: (String) -> Unit,
    dismissLabel: String = "Cancel",
    dialogIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedCode }?.second ?: selectedCode

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showLabel) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(selectedLabel)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null)
        }
    }

    if (expanded) {
        LanguagePickerDialog(
            onDismiss = { expanded = false },
            title = label,
            dismissLabel = dismissLabel,
            icon = dialogIcon,
            items = options.map { (code, name) -> LanguagePickerItem(code = code, title = name) },
            selectedCode = selectedCode,
            onSelect = {
                onSelect(it)
                expanded = false
            }
        )

    }
}



@Composable
fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

    }
}



@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )

    }
}



@Composable
fun FullScreenEditor(
    uri: Uri?,
    verseText: String,
    verseReference: String,
    initialTextSizeMult: Float,
    initialTextWidthMult: Float,
    initialVerticalOffset: Float,
    textColor: Int,
    textAlpha: Float,
    bgBlur: Float,
    bgDarkness: Float,
    isBold: Boolean,
    useShadow: Boolean,
    fontFamilyStr: String,
    strings: AppStrings,
    onSave: (Float, Float, Float) -> Unit,
    onDismiss: () -> Unit,
    performHaptic: (HapticFeedbackType) -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    // Lokálny stav
    var localSizeMult by remember { mutableFloatStateOf(initialTextSizeMult) }
    var rawSizeMult by remember { mutableFloatStateOf(initialTextSizeMult) }
    var isSizeSnapped by remember { mutableStateOf(kotlin.math.abs(initialTextSizeMult - 1.0f) < 0.05f) }

    var localWidthMult by remember { mutableFloatStateOf(initialTextWidthMult) }
    var rawWidthMult by remember { mutableFloatStateOf(initialTextWidthMult) }
    var isWidthSnapped by remember { mutableStateOf(kotlin.math.abs(initialTextWidthMult - 1.0f) < 0.05f) }

    var localVerticalOffset by remember { mutableFloatStateOf(initialVerticalOffset) }
    var rawVerticalOffset by remember { mutableFloatStateOf(initialVerticalOffset) }
    var isPositionSnapped by remember { mutableStateOf(kotlin.math.abs(initialVerticalOffset) < 0.04f) }

    // Konštanta pre rýchlosť vertikálneho posunu
    val moveFactor = 1f / (screenHeight.value * 0.35f)

    val updateWidth = { delta: Float ->
        rawWidthMult = (rawWidthMult + delta).coerceIn(0.5f, 1.5f)
        val snapZone = 0.05f
        val newSnapped = if (kotlin.math.abs(rawWidthMult - 1.0f) < snapZone) 1.0f else rawWidthMult
        if (newSnapped == 1.0f && !isWidthSnapped) {
            performHaptic(HapticFeedbackType.LongPress)
            isWidthSnapped = true
        } else if (newSnapped != 1.0f && isWidthSnapped) {
            isWidthSnapped = false
        }
        localWidthMult = newSnapped
    }

    val updateSize = { delta: Float ->
        rawSizeMult = (rawSizeMult + delta).coerceIn(0.5f, 3.0f)
        val snapZone = 0.08f
        val newSnapped = if (kotlin.math.abs(rawSizeMult - 1.0f) < snapZone) 1.0f else rawSizeMult
        if (newSnapped == 1.0f && !isSizeSnapped) {
            performHaptic(HapticFeedbackType.LongPress)
            isSizeSnapped = true
        } else if (newSnapped != 1.0f && isSizeSnapped) {
            isSizeSnapped = false
        }
        localSizeMult = newSnapped
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        val screenWidthPx = with(density) { maxWidth.toPx() }
        val baseWidthPx = screenWidthPx * 0.80f

        // Výpočet veľkosti písma
        var baseSize = maxWidth.value * 0.055f
        if (verseText.length > 150) baseSize = maxWidth.value * 0.045f
        val fontScale = density.fontScale
        val fontSize = (baseSize * localSizeMult / fontScale).sp

        val composeFontFamily = getComposeFontFamily(fontFamilyStr)
        val composeFontWeight = getComposeFontWeight(fontFamilyStr, isBold)

        // Výpočet Y pozície
        val defaultOffset = maxHeight * 0.05f
        val variableOffset = (maxHeight * 0.35f) * localVerticalOffset
        val totalOffset = defaultOffset + variableOffset

        // 1. Pozadie
        if (uri != null) {

            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(uri)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .crossfade(false)
                        .build()
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (bgBlur > 0f) Modifier.blur((bgBlur * 0.7f).dp) else Modifier
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = bgDarkness))
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
            )
        }

        // 2. FAKE STATUS BAR & CLOCK
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "09:41",
                color = Color(0xFFEEEEEE),
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            val date = SimpleDateFormat("EEE, d. MMM", Locale.getDefault()).format(Date())
            Text(
                text = date,
                color = Color(0xFFEEEEEE),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 3. EDITOVATEĽNÝ TEXT BOX
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val panYDp = dragAmount.y / density.density
                        val panOffsetChange = panYDp * moveFactor

                        rawVerticalOffset =
                            (rawVerticalOffset + panOffsetChange).coerceIn(-1.0f, 1.0f)

                        val snapZone = 0.04f
                        val newSnappedOffset = if (kotlin.math.abs(rawVerticalOffset) < snapZone) {
                            0.0f
                        } else {
                            rawVerticalOffset
                        }

                        if (newSnappedOffset == 0.0f && !isPositionSnapped) {
                            performHaptic(HapticFeedbackType.LongPress)
                            isPositionSnapped = true
                        } else if (newSnappedOffset != 0.0f && isPositionSnapped) {
                            isPositionSnapped = false
                        }

                        localVerticalOffset = newSnappedOffset
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Widget kontajner
            Box(
                modifier = Modifier
                    .offset(y = totalOffset)
                    .width(maxWidth * 0.80f * localWidthMult)
            ) {
                // Vnútorne ohraničený box s textom a pozadím
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isPositionSnapped) 3.dp else 2.dp, // Indikácia snapu stredovej pozície
                            color = if (isPositionSnapped) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = maxWidth * 0.025f, vertical = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = verseText,
                            color = Color(textColor).copy(alpha = textAlpha),
                            fontSize = fontSize,
                            fontFamily = composeFontFamily,
                            lineHeight = fontSize * 1.25f,
                            fontWeight = composeFontWeight,
                            textAlign = TextAlign.Center,
                            style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                        )

                        Spacer(modifier = Modifier.height((fontSize.value * 0.5).dp))

                        Text(
                            text = verseReference,
                            color = Color(textColor).copy(alpha = textAlpha * 0.8f),
                            fontSize = fontSize * 0.75f,
                            fontFamily = composeFontFamily,
                            fontWeight = getComposeFontWeight(fontFamilyStr, false),
                            textAlign = TextAlign.Center,
                            style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                        )
                    }
                }

                // DRAG HANDLES
                val handleSize = 52.dp
                val dotSize = 12.dp

                // Pravá strana (Šírka)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 24.dp)
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val widthChangeMult = dragAmount.x / baseWidthPx
                                updateWidth(widthChangeMult * 2)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(dotSize).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).background(Color.White, CircleShape)) }

                // Ľavá strana (Šírka)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = -24.dp)
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val widthChangeMult = -dragAmount.x / baseWidthPx
                                updateWidth(widthChangeMult * 2)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(dotSize).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).background(Color.White, CircleShape)) }

                // Dolná strana (Veľkosť)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 24.dp)
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                updateSize(dragAmount.y / (200f * density.density))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(dotSize).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).background(Color.White, CircleShape)) }

                // Horná strana (Veľkosť)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = -24.dp)
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                updateSize(-dragAmount.y / (200f * density.density))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(dotSize).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).background(Color.White, CircleShape)) }
            }
        }

        // 4. BOTTOM BAR WITH ACTIONS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        startY = 0f
                    )
                )
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = strings.dragHint,
                    style = MaterialTheme.typography.labelMedium.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 16f)
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = strings.cancel, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.cancel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onSave(localSizeMult, localWidthMult, localVerticalOffset)
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = strings.done, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.done, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

    }
}



@Composable
fun EditHintBubble(text: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f, // Bounces up by 15dp
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceOffset"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.offset(y = bounceOffset.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.TouchApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }
        // Triangle pointing down at the verse
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .size(40.dp)
                .offset(y = (-14).dp) // Overlap to seamlessly connect to the bubble
        )

    }
}



@Composable
fun Pixel6LockScreenPreview(
    uri: Uri?,
    verseText: String,
    verseReference: String,
    textSizeMult: Float,
    textWidthMult: Float,
    verticalOffset: Float,
    textColor: Int,
    textAlpha: Float,
    bgBlur: Float,
    bgDarkness: Float,
    isBold: Boolean,
    useShadow: Boolean,
    fontFamilyStr: String,
    showEditHint: Boolean = false,
    showBubbleHint: Boolean = false,
    strings: AppStrings,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val previewHeight = (screenHeight * 0.75f)

    Card(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .height(previewHeight)
            .aspectRatio(9f / 20f)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidth = maxWidth
            val maxHeight = maxHeight

            // Výpočet veľkosti písma
            var baseSize = maxWidth.value * 0.055f
            if (verseText.length > 150) baseSize = maxWidth.value * 0.045f
            val fontSize = (baseSize * textSizeMult / density.fontScale).sp
            val composeFontFamily = getComposeFontFamily(fontFamilyStr)
            val composeFontWeight = getComposeFontWeight(fontFamilyStr, isBold)

            // Výpočet Y pozície
            val defaultOffset = maxHeight * 0.05f
            val variableOffset = (maxHeight * 0.35f) * verticalOffset
            val totalOffset = defaultOffset + variableOffset

            // Pozadie
            if (uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(uri)
                            .precision(Precision.INEXACT)
                            .crossfade(false)
                            .build()
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (bgBlur > 0f) Modifier.blur(bgBlur.dp) else Modifier
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = bgDarkness))
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
                )
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF212122), Color(0xFF212121))
                        )
                    ), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
                        }
                        Text(strings.clickToSelect, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (uri != null) {
                // FAKE STATUS BAR & CLOCK (Pixel Style)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Clock
                    Text(
                        text = "09:41",
                        color = Color(0xFFEEEEEE),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    )
                    // Date
                    val date = SimpleDateFormat("EEE, d. MMM", Locale.getDefault()).format(Date())
                    Text(
                        text = date,
                        color = Color(0xFFEEEEEE),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // --- NÁHĽAD VERŠA ---
                // CenterBox pre náhľad a obal
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Vonkajší Box definuje presné rozmery celého orámovaného bloku textu aj na výšku vďaka intrinsic Content
                    Box(
                        modifier = Modifier
                            .offset(y = totalOffset)
                            .width(maxWidth * 0.80f * textWidthMult)
                            .clickable { onEditClick() }
                            .then(
                                if (showEditHint) Modifier.drawBehind {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(
                                                    15f,
                                                    15f
                                                ), 0f
                                            )
                                        ),
                                        cornerRadius = CornerRadius(16.dp.toPx())
                                    )
                                } else Modifier
                            )
                    ) {

                        // Padded vnútorný obsah s textom - určuje výšku predchádzajúceho boxu
                        Box(modifier = Modifier.padding(horizontal = maxWidth * 0.025f, vertical = 16.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = verseText,
                                    color = Color(textColor).copy(alpha = textAlpha),
                                    fontSize = fontSize,
                                    fontFamily = composeFontFamily,
                                    lineHeight = fontSize * 1.25f,
                                    fontWeight = composeFontWeight,
                                    textAlign = TextAlign.Center,
                                    style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                                )

                                Spacer(modifier = Modifier.height((fontSize.value * 0.5).dp))

                                Text(
                                    text = verseReference,
                                    color = Color(textColor).copy(alpha = textAlpha * 0.8f),
                                    fontSize = fontSize * 0.75f,
                                    fontFamily = composeFontFamily,
                                    fontWeight = getComposeFontWeight(fontFamilyStr, false),
                                    textAlign = TextAlign.Center,
                                    style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                                )
                            }
                        }

                        // --- PRIDANÁ BUBLINA ---
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBubbleHint,
                            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                            exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                // Posunieme bublinu presne nad rámček s textom
                                .offset(y = (-70).dp)
                        ) {
                            EditHintBubble(text = strings.tapToEdit)
                        }

                        // Ikonka ceruzky
                        if (showEditHint) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    // Box je veľký 32.dp. Posunom o 16.dp ho vycentrujeme presne na roh.
                                    .offset(x = 16.dp, y = (-16).dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editovať",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // --- FINGERPRINT & BOTTOM ICONS ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Senzor odtlačku
                    Box(
                        modifier = Modifier
                            .padding(bottom = 100.dp)
                            .size(64.dp)
                            .background(Color(0x33FFFFFF), CircleShape)
                            .border(1.dp, Color(0x66FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Fingerprint, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Ikony na krajoch
                    Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp))
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp))
                }
            }
        }

    }
}



@Composable
fun FontPickerRow(selectedFont: String, strings: AppStrings, onFontSelected: (String) -> Unit) {
    val fonts = listOf(
        "sans-serif" to strings.fontModern,
        "sans-serif-light" to strings.fontLight,
        "sans-serif-condensed" to strings.fontCondensed,
        "serif" to strings.fontBook,
        "monospace" to strings.fontMono,
        "cursive" to strings.fontCursive
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fonts.forEach { (fontId, fontName) ->
            FilterChip(
                selected = selectedFont == fontId,
                onClick = { onFontSelected(fontId) },
                label = { Text(fontName, fontFamily = getComposeFontFamily(fontId), fontWeight = getComposeFontWeight(fontId, false)) },
                leadingIcon = if (selectedFont == fontId) {
                    { Icon(Icons.Default.Check, null) }
                } else null
            )
        }

    }
}



val TextStyleWithShadow = TextStyle(
    shadow = androidx.compose.ui.graphics.Shadow(
        color = Color.Black,
        blurRadius = 12f
    )
)

fun getComposeFontFamily(fontFamilyStr: String): FontFamily {
    return when (fontFamilyStr) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
}



fun getComposeFontWeight(fontFamilyStr: String, isBold: Boolean): FontWeight {
    if (isBold) return FontWeight.Bold
    return when (fontFamilyStr) {
        "sans-serif-light" -> FontWeight.Light
        else -> FontWeight.Normal
    }
}



// ── Color picker helpers ──────────────────────────────────────────────────────

private fun hsvToComposeColor(hue: Float, sat: Float, value: Float): Color {
    val f = { n: Float ->
        val k = (n + hue / 60f) % 6f
        value - value * sat * maxOf(0f, minOf(k, 4f - k, 1f))
    }
    return Color(f(5f), f(3f), f(1f))
}

private fun composeColorToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val mn = minOf(r, g, b); val d = max - mn
    val h = when {
        d == 0f  -> 0f
        max == r -> 60f * (((g - b) / d + 6f) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else     -> 60f * ((r - g) / d + 4f)
    }
    return Triple(h, if (max == 0f) 0f else d / max, max)
}

private fun Color.toHexStr(): String = String.format("%06X", toArgb() and 0xFFFFFF)

private fun hexStrToColor(hex: String): Color? {
    if (hex.length != 6) return null
    return try {
        val v = hex.toLong(16)
        Color(((v shr 16) and 0xFF) / 255f, ((v shr 8) and 0xFF) / 255f, (v and 0xFF) / 255f)
    } catch (_: NumberFormatException) { null }
}

private fun buildWheelBitmap(sizePx: Int, value: Float): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val cx = sizePx / 2f; val r = cx
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            val dx = x - cx; val dy = y - cx
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > r) { pixels[y * sizePx + x] = 0; continue }
            val hue = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
            val sat = (dist / r).coerceIn(0f, 1f)
            val c = hsvToComposeColor(hue, sat, value)
            pixels[y * sizePx + x] = android.graphics.Color.argb(
                255,
                (c.red * 255).toInt(),
                (c.green * 255).toInt(),
                (c.blue * 255).toInt()
            )
        }
    }
    bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    return bmp
}

@Composable
private fun rememberWheelBitmap(sizePx: Int, value: Float): ImageBitmap? {
    // Quantise to 2% steps so we don't re-render on every slider pixel
    val brightnessKey = (value * 50).toInt()
    val state = produceState<ImageBitmap?>(initialValue = null, sizePx, brightnessKey) {
        this.value = withContext(Dispatchers.Default) {
            buildWheelBitmap(sizePx, brightnessKey / 50f).asImageBitmap()
        }
    }
    return state.value
}

// ── ColorPickerRow ────────────────────────────────────────────────────────────

@Composable
fun ColorPickerRow(selectedColor: Int, strings: AppStrings, onColorSelected: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    val presetColors = listOf(
        AndroidColor.WHITE,
        AndroidColor.BLACK,
        AndroidColor.parseColor("#CFDEF3"),
        AndroidColor.parseColor("#FFF8E7"),
        AndroidColor.parseColor("#AAAAAA"),
        AndroidColor.parseColor("#FFB347"),
    )

    if (showDialog) {
        val initColor = Color(selectedColor)
        val (initH, initS, initV) = composeColorToHsv(initColor)

        var hueState   by remember { mutableStateOf(initH) }
        var satState   by remember { mutableStateOf(initS) }
        var valueState by remember { mutableStateOf(initV) }
        val pickedColor by remember(hueState, satState, valueState) {
            derivedStateOf { hsvToComposeColor(hueState, satState, valueState) }
        }
        var hexText by remember { mutableStateOf(initColor.toHexStr()) }
        var rText   by remember { mutableStateOf((initColor.red   * 255).toInt().toString()) }
        var gText   by remember { mutableStateOf((initColor.green * 255).toInt().toString()) }
        var bText   by remember { mutableStateOf((initColor.blue  * 255).toInt().toString()) }

        fun setHsv(h: Float, s: Float, v: Float) {
            hueState = h; satState = s; valueState = v
            val c = hsvToComposeColor(h, s, v)
            hexText = c.toHexStr()
            rText = (c.red   * 255).toInt().toString()
            gText = (c.green * 255).toInt().toString()
            bText = (c.blue  * 255).toInt().toString()
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(strings.colorPickerTitle) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Wheel + swatch
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val density = LocalDensity.current
                        val wheelSizePx = with(density) { 150.dp.roundToPx() }
                        val wheelBitmap = rememberWheelBitmap(wheelSizePx, valueState)

                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .pointerInput(valueState) {
                                    val sz = size.width.toFloat(); val cx = sz / 2f; val r = sz / 2f
                                    fun handle(pos: androidx.compose.ui.geometry.Offset) {
                                        val dx = pos.x - cx; val dy = pos.y - cx
                                        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(r)
                                        val h = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                                        setHsv(h, dist / r, valueState)
                                    }
                                    detectTapGestures { handle(it) }
                                }
                                .pointerInput(valueState) {
                                    val sz = size.width.toFloat(); val cx = sz / 2f; val r = sz / 2f
                                    detectDragGestures { change, _ ->
                                        val pos = change.position
                                        val dx = pos.x - cx; val dy = pos.y - cx
                                        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(r)
                                        val h = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                                        setHsv(h, dist / r, valueState)
                                    }
                                }
                        ) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                if (wheelBitmap != null) drawImage(wheelBitmap)
                                val cx2  = size.width  / 2f; val cy2 = size.height / 2f
                                val r2   = min(cx2, cy2)
                                val ang  = hueState * Math.PI.toFloat() / 180f
                                val dotX = cx2 + satState * r2 * cos(ang)
                                val dotY = cy2 + satState * r2 * sin(ang)
                                drawCircle(Color.White,   radius = 9f, center = androidx.compose.ui.geometry.Offset(dotX, dotY))
                                drawCircle(pickedColor,   radius = 6f, center = androidx.compose.ui.geometry.Offset(dotX, dotY))
                            }
                        }

                        // Right: swatch + preview text
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.colorPickerVerseColor,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(pickedColor)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            )
                            Text(
                                text = "\"For God so loved...\"",
                                fontSize = 11.sp,
                                color = pickedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Brightness slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.colorPickerBrightness,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(70.dp)
                        )
                        Slider(
                            value = valueState,
                            onValueChange = { setHsv(hueState, satState, it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(valueState * 100).toInt()}%",
                            fontSize = 12.sp,
                            modifier = Modifier.width(36.dp)
                        )
                    }

                    // Hex input
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.uppercase().take(6)
                            hexText = clean
                            if (clean.length == 6) {
                                hexStrToColor(clean)?.let { c ->
                                    val (h, s, v) = composeColorToHsv(c)
                                    hueState = h; satState = s; valueState = v
                                    rText = (c.red   * 255).toInt().toString()
                                    gText = (c.green * 255).toInt().toString()
                                    bText = (c.blue  * 255).toInt().toString()
                                }
                            }
                        },
                        label = { Text("Hex") },
                        prefix = { Text("#") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // RGB inputs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        fun onRgbChange() {
                            val r2 = rText.toIntOrNull()?.coerceIn(0, 255) ?: return
                            val g2 = gText.toIntOrNull()?.coerceIn(0, 255) ?: return
                            val b2 = bText.toIntOrNull()?.coerceIn(0, 255) ?: return
                            val c  = Color(r2 / 255f, g2 / 255f, b2 / 255f)
                            val (h, s, v) = composeColorToHsv(c)
                            hueState = h; satState = s; valueState = v
                            hexText = c.toHexStr()
                        }
                        OutlinedTextField(
                            value = rText,
                            onValueChange = { rText = it.filter { c -> c.isDigit() }.take(3); onRgbChange() },
                            label = { Text("R") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gText,
                            onValueChange = { gText = it.filter { c -> c.isDigit() }.take(3); onRgbChange() },
                            label = { Text("G") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bText,
                            onValueChange = { bText = it.filter { c -> c.isDigit() }.take(3); onRgbChange() },
                            label = { Text("B") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }

                    // Preset dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            AndroidColor.WHITE,
                            AndroidColor.BLACK,
                            AndroidColor.parseColor("#CFDEF3"),
                            AndroidColor.parseColor("#FFF8E7"),
                            AndroidColor.parseColor("#AAAAAA"),
                            AndroidColor.parseColor("#FFB347"),
                            AndroidColor.parseColor("#87CEEB"),
                            AndroidColor.parseColor("#DDA0DD"),
                        ).forEach { preset ->
                            val pc = Color(preset)
                            val sel = pickedColor.toHexStr() == pc.toHexStr()
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(pc)
                                    .border(
                                        if (sel) 2.dp else 0.5.dp,
                                        if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                                    .clickable {
                                        val (h, s, v) = composeColorToHsv(pc)
                                        setHsv(h, s, v)
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onColorSelected(pickedColor.toArgb())
                    showDialog = false
                }) { Text(strings.done) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Always-visible quick preset row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        presetColors.forEach { color ->
            val isSelected = selectedColor == color
            val ringSize by animateDpAsState(
                targetValue = if (isSelected) 3.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "ring"
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(ringSize, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (color == AndroidColor.WHITE) Color.Black else Color.White
                    )
                }
            }
        }
        // Open full picker
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.sweepGradient(
                        listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
                    )
                )
                .clickable { showDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ColorLens, "Custom Color", tint = Color.White, modifier = Modifier.size(20.dp))
        }

    }
}



@Composable
fun EnhancedSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    defaultVal: Float,
    steps: Int = 0,
    icon: ImageVector,
    performHaptic: (HapticFeedbackType) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(
                    visible = (value != defaultVal),
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    IconButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onValueChange(defaultVal)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Slider(
            value = value,
            onValueChange = {
                val rounded = (it * 10).roundToInt() / 10f
                if (rounded != value) {
                    onValueChange(rounded)
                    performHaptic(HapticFeedbackType.TextHandleMove)
                }
            },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(top = 0.dp)
        )

    }
}

/**
 * The small "book" icon shown next to the app name in the top bar. Holding
 * it down for 3 full seconds opens the Developer Logs sheet — a quick M3
 * radial progress ring fills in around the icon while held, so it's clear
 * something is happening and roughly how much longer to hold.
 */
@Composable
fun DevMenuTriggerIcon(
    onTriggered: () -> Unit,
    onHoldStart: () -> Unit,
    holdDurationMs: Int = 3000
) {
    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }
    val holdProgress = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .size(34.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        onHoldStart()
                        var triggered = false
                        val animJob = scope.launch {
                            holdProgress.snapTo(0f)
                            holdProgress.animateTo(1f, tween(holdDurationMs, easing = LinearEasing))
                            triggered = true
                            onTriggered()
                        }
                        tryAwaitRelease()
                        isHolding = false
                        animJob.cancel()
                        if (!triggered) {
                            scope.launch { holdProgress.animateTo(0f, tween(150)) }
                        } else {
                            scope.launch { holdProgress.snapTo(0f) }
                        }
                    }
                )
            }
            .background(
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
        )
        if (isHolding) {
            CircularProgressIndicator(
                progress = holdProgress.value,
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }
    }
}

private fun levelAccentColor(level: LogLevel, scheme: ColorScheme): Color = when (level) {
    LogLevel.ERROR -> scheme.error
    LogLevel.WARN -> scheme.tertiary
    LogLevel.INFO -> scheme.primary
    LogLevel.DEBUG -> scheme.onSurfaceVariant
}

private fun levelContainerColor(level: LogLevel, scheme: ColorScheme): Color = when (level) {
    LogLevel.ERROR -> scheme.errorContainer.copy(alpha = 0.55f)
    LogLevel.WARN -> scheme.tertiaryContainer.copy(alpha = 0.55f)
    LogLevel.INFO -> scheme.surfaceVariant.copy(alpha = 0.45f)
    LogLevel.DEBUG -> scheme.surfaceVariant.copy(alpha = 0.25f)
}

private fun levelIcon(level: LogLevel): ImageVector = when (level) {
    LogLevel.ERROR -> Icons.Filled.ErrorOutline
    LogLevel.WARN -> Icons.Filled.WarningAmber
    LogLevel.INFO -> Icons.Filled.Info
    LogLevel.DEBUG -> Icons.Filled.Circle
}

/**
 * A simplified, on-device "logcat" — shows what AppLogger has recorded,
 * newest first, with severity color-coding and level filtering. Because
 * AppLogger writes straight to disk, this still shows the full history
 * even after the app has been force-closed or killed by the system; a
 * warning entry logged on the *next* launch will explain when that happened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperLogSheet(onDismiss: () -> Unit, onClear: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(AppLogger.getLogEntries(context)) }
    var activeFilter by remember { mutableStateOf<LogLevel?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    // Logs are written to disk from background threads (receivers, workers)
    // that don't know this sheet is open, so we can't rely on state changes
    // to trigger a refresh. Poll instead, while the sheet is visible, so
    // new entries (e.g. "Wallpaper set successfully") show up live instead
    // of only appearing the next time the sheet is reopened.
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val fresh = AppLogger.getLogEntries(context)
            if (fresh != entries) entries = fresh
        }
    }

    val filteredEntries = remember(entries, activeFilter) {
        if (activeFilter == null) entries else entries.filter { it.level == activeFilter }
    }
    val errorCount = remember(entries) { entries.count { it.level == LogLevel.ERROR } }
    val warnCount = remember(entries) { entries.count { it.level == LogLevel.WARN } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BottomSheetDefaults.DragHandle()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Developer Logs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Live",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (entries.isEmpty()) {
                                "Nothing logged yet"
                            } else {
                                "${entries.size} entries · $warnCount warnings · $errorCount errors"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        IconButton(onClick = {
                            val text = (if (activeFilter == null) entries else filteredEntries)
                                .joinToString("\n") { it.raw }
                            clipboard.setText(AnnotatedString(text))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs")
                        }
                        IconButton(onClick = {
                            onClear()
                            entries = emptyList()
                        }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == null,
                        onClick = { activeFilter = null },
                        label = { Text("All") }
                    )
                    listOf(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG).forEach { level ->
                        FilterChip(
                            selected = activeFilter == level,
                            onClick = { activeFilter = if (activeFilter == level) null else level },
                            label = { Text(level.label) },
                            leadingIcon = {
                                Icon(
                                    levelIcon(level),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = levelAccentColor(level, MaterialTheme.colorScheme)
                                )
                            }
                        )
                    }
                }
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No logs yet — use the app for a bit and check back here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else if (filteredEntries.isEmpty()) {
            Text(
                "No entries match this filter.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredEntries.size) { index ->
                    val entry = filteredEntries[index]
                    val accent = levelAccentColor(entry.level, MaterialTheme.colorScheme)
                    val icon = if (entry.tag == "Wallpaper") Icons.Filled.Wallpaper else levelIcon(entry.level)
                    Surface(
                        color = levelContainerColor(entry.level, MaterialTheme.colorScheme),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = accent,
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                icon,
                                contentDescription = entry.level.label,
                                tint = accent,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = entry.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = entry.timestampRaw,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = entry.message,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



// --- WORKER LOGIC ---

/**
 * Computes the delay (ms) until the next exact hour-of-day boundary for a
 * 24h cycle, e.g. always lands on hh:00:00 instead of drifting later and
 * later. If `hour` is already passed today, targets tomorrow at `hour`.
 */
fun computeDailyInitialDelayMs(hour: Int): Long {
    val currentDate = Calendar.getInstance()
    val dueDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (!dueDate.after(currentDate)) {
        dueDate.add(Calendar.HOUR_OF_DAY, 24)
    }

    return dueDate.timeInMillis - currentDate.timeInMillis
}

/**
 * Computes the delay (ms) until the next UTC slot boundary for a given
 * interval, e.g. every 6h → 00:00, 06:00, 12:00, 18:00 UTC exactly, never
 * drifting to e.g. 20:43. All devices on the same interval land on the same
 * slot because epoch_hours / intervalHours is identical worldwide at a given
 * UTC instant.
 */
fun computeSlotInitialDelayMs(intervalHours: Int): Long {
    val now = System.currentTimeMillis()
    val nowEpochHours = now / (1000L * 60 * 60)
    val nextSlot = (nowEpochHours / intervalHours + 1) * intervalHours
    val nextSlotMs = nextSlot * 60L * 60L * 1000L
    return nextSlotMs - now
}

/**
 * Like computeDailyInitialDelayMs, but for wallpaper-cycling intervals of
 * 12h/24h: targets the next occurrence of `hour` (today or, if passed,
 * `intervalHours` later) on an exact hh:00:00 boundary.
 */
fun computeDailyCycleInitialDelayMs(hour: Int, intervalHours: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) {
            add(Calendar.HOUR_OF_DAY, intervalHours)
        }
    }
    return target.timeInMillis - now.timeInMillis
}

fun scheduleDailyWallpaper(context: Context, hour: Int) {
    val workManager = WorkManager.getInstance(context)
    val initialDelay = computeDailyInitialDelayMs(hour)

    val dailyWorkRequest = OneTimeWorkRequestBuilder<DailyVerseWorker>()
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build())
        .build()

    workManager.enqueueUniqueWork(
        "DailyBibleWallpaper",
        ExistingWorkPolicy.REPLACE,
        dailyWorkRequest
    )
}

/**
 * Schedules the wallpaper worker based on interval.
 * For 24h, aligns to the user-chosen time-of-day (cross-device sync via UTC epoch slot).
 * For shorter intervals, uses the interval directly — all devices on same slot because
 * epoch_hours / intervalHours gives the same slot number worldwide at the same UTC time.
 *
 * Implemented as a self-rescheduling chain of OneTimeWorkRequests (instead of a
 * PeriodicWorkRequest) so every run recomputes the exact delay to the next hour
 * boundary — this is what keeps the fire time pinned to hh:00:00 instead of
 * drifting later with every execution (see DailyVerseWorker.rescheduleNext).
 */
fun scheduleAutoWallpaper(context: Context, intervalHours: Int, dailyHour: Int) {
    val workManager = WorkManager.getInstance(context)

    if (intervalHours == 24) {
        scheduleDailyWallpaper(context, dailyHour)
        return
    }

    // For sub-day intervals: align initial delay to the next slot boundary
    // so all devices with the same interval are in sync (e.g. every 6h → 0,6,12,18 UTC)
    val initialDelayMs = computeSlotInitialDelayMs(intervalHours)

    val workRequest = OneTimeWorkRequestBuilder<DailyVerseWorker>()
        .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build())
        .build()

    workManager.enqueueUniqueWork(
        "DailyBibleWallpaper",
        ExistingWorkPolicy.REPLACE,
        workRequest
    )
}

// Helper to keep boot receiver / legacy code working
fun scheduleWorker(context: Context) {
    val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
    val savedHour = prefs.getInt("daily_hour", 6)
    val intervalHours = prefs.getInt("auto_interval_hours", 24)
    scheduleAutoWallpaper(context, intervalHours, savedHour)
}

fun runOneTimeWorker(context: Context) {
    val req = OneTimeWorkRequestBuilder<DailyVerseWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()
    WorkManager.getInstance(context).enqueue(req)
}

/**
 * Schedules or cancels the wallpaper cycling worker.
 *
 * If wallpaper cycling is enabled and the mode requires a periodic worker
 * (ON_VERSE_CHANGE or CUSTOM_INTERVAL), schedules a periodic worker under a
 * unique name "WallpaperCycling". If disabled or in ON_SCREEN_OFF / DAY_NIGHT
 * mode, cancels any existing cycling worker.
 *
 * This is separate from the verse cycling worker ("DailyBibleWallpaper") so
 * both can run independently.
 */
fun scheduleWallpaperCycling(context: Context) {
    val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
    val settings = WallpaperSettings.load(prefs)
    val workManager = WorkManager.getInstance(context)

    if (!settings.cycleEnabled) {
        workManager.cancelUniqueWork("WallpaperCycling")
        return
    }

    when (settings.cycleMode) {
        com.daklok.biblelockscreen.WallpaperManager.CYCLE_ON_SCREEN_OFF -> {
            // No periodic worker needed — ScreenOffReceiver handles it
            workManager.cancelUniqueWork("WallpaperCycling")
        }
        else -> {
            // CUSTOM_INTERVAL — schedule a self-rescheduling one-time worker
            // (independent of verse cycling). Using OneTimeWorkRequest chained
            // via rescheduleNext() (instead of PeriodicWorkRequest) keeps every
            // run pinned to the exact hour boundary instead of drifting later
            // with each execution.
            val intervalHours = settings.cycleIntervalHours
            val wallpaperData = androidx.work.workDataOf("source" to "wallpaper")
            val initialDelayMs = if (intervalHours == 12 || intervalHours == 24) {
                computeDailyCycleInitialDelayMs(settings.cycleDailyHour, intervalHours)
            } else {
                computeSlotInitialDelayMs(intervalHours)
            }
            val req = OneTimeWorkRequestBuilder<DailyVerseWorker>()
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setInputData(wallpaperData)
                .build()
            workManager.enqueueUniqueWork("WallpaperCycling", ExistingWorkPolicy.REPLACE, req)
        }
    }
}