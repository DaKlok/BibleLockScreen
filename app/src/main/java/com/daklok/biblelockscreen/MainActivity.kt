package com.daklok.biblelockscreen

import com.daklok.biblelockscreen.strings.*
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
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
    // wallpaper alarms to the Developer Logs. If auto-wallpaper is supposed
    // to be running but no exact alarm is currently armed, that's a strong
    // sign the system cleared it (e.g. the exact-alarm permission was
    // revoked, or — before this used AlarmManager — a reboot happened).
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                if (isDailyActive) {
                    if (!isWallpaperAlarmScheduled(context, "DailyBibleWallpaper")) {
                        AppLogger.w(context, "Alarm", "Auto-wallpaper is enabled but no exact alarm is armed — the system may have cleared it.")
                    } else {
                        AppLogger.i(context, "Alarm", "DailyBibleWallpaper: exact alarm armed")
                    }
                }
                val cyclingArmed = isWallpaperAlarmScheduled(context, "WallpaperCycling")
                AppLogger.i(context, "Alarm", "WallpaperCycling: exact alarm ${if (cyclingArmed) "armed" else "not armed"}")
            } catch (e: Exception) {
                AppLogger.e(context, "Alarm", "Failed to read scheduled alarm status: ${e.message}")
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
            cancelExactWallpaperAlarm(context, "DailyBibleWallpaper")
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
                        // `surface` defaults to the exact same tone as
                        // `background` in the static (non-dynamic) color
                        // scheme, and sits very close to it under Material
                        // You too — making this container indistinguishable
                        // from the screen behind it. `surfaceContainerLow`
                        // is deliberately one tonal step above `background`.
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                                                    cancelExactWallpaperAlarm(context, "DailyBibleWallpaper")
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
            // Material 3's `surfaceVariant` role can render almost
            // identically to `background`/`surface` — especially under
            // Material You dynamic color, where all three are derived from
            // very close tonal steps of the same neutral palette, but also
            // in the static (non-dynamic) light scheme, where `background`
            // and `surface` default to literally the same tone. That made
            // this card blend into both the settings-area background and
            // the area behind the Pixel 6 preview.
            //
            // `surfaceContainerHigh` is one of M3's newer "surface
            // container" roles, specifically designed to give each nesting
            // level a deliberately distinct, guaranteed-different tone —
            // regardless of whether the theme is static or dynamic.
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
        // Card defaults to `colorScheme.surface` — the same role used for
        // the Scaffold background directly behind this preview — so if this
        // color were ever visible (e.g. through the rounded corners), it
        // would blend right into the screen. Pick a deliberately distinct
        // tone instead.
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
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

// Vivid Material 3 Expressive green — used to make the actual moment the
// wallpaper changes visually pop out from the rest of the (mostly grey/blue)
// log stream, since that's the single most important event in this log.
private val WallpaperAppliedAccent = Color(0xFF6DFFA8)
private val WallpaperAppliedContainer = Color(0xFF0F9D58).copy(alpha = 0.50f)

/**
 * True for the specific log lines that mark the wallpaper actually being
 * changed on screen (as opposed to scheduling/diagnostic/lifecycle noise).
 */
private fun isWallpaperAppliedEntry(entry: LogEntry): Boolean =
    (entry.tag == "Worker" && entry.message.startsWith("Success: Wallpaper applied")) ||
            (entry.tag == "Wallpaper" && entry.message.startsWith("✓ Wallpaper set successfully"))

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
                    val wallpaperApplied = isWallpaperAppliedEntry(entry)
                    val accent = if (wallpaperApplied) WallpaperAppliedAccent else levelAccentColor(entry.level, MaterialTheme.colorScheme)
                    val containerColor = if (wallpaperApplied) WallpaperAppliedContainer else levelContainerColor(entry.level, MaterialTheme.colorScheme)
                    val icon = if (entry.tag == "Wallpaper" || wallpaperApplied) Icons.Filled.Wallpaper else levelIcon(entry.level)
                    Surface(
                        color = containerColor,
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
 * Delay (ms) until the next LOCAL wall-clock slot boundary for an interval
 * that evenly divides 24 (1, 2, 3, 4, 6, 8, 12h) — anchored to local
 * midnight. So "every 3 hours" lands on 00:00, 03:00, 06:00, 09:00, 12:00,
 * 15:00, 18:00, 21:00 *local* time.
 *
 * Previously this aligned to UTC epoch-hour boundaries instead, which drifts
 * by the device's UTC offset — e.g. on a UTC+2 (CEST) device, "every 3
 * hours" landed on 02:00, 05:00, ..., 17:00, 20:00, 23:00 local time instead
 * of the expected 00:00/03:00/.../18:00/21:00.
 */
fun computeSlotInitialDelayMs(intervalHours: Int): Long {
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val nextSlotHour = ((currentHour / intervalHours) + 1) * intervalHours

    val target = Calendar.getInstance().apply {
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (nextSlotHour >= 24) {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, nextSlotHour - 24)
        } else {
            set(Calendar.HOUR_OF_DAY, nextSlotHour)
        }
    }
    return target.timeInMillis - now.timeInMillis
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
    }
    // A single addition of intervalHours isn't always enough: e.g. anchor
    // hour=7 with intervalHours=12 has only one fixed candidate per day
    // (07:00), and if "now" is already past 19:00 (07:00 + 12h), that one
    // addition still lands in the past — producing a negative delay, which
    // made AlarmManager fire immediately and re-trigger in a tight loop.
    // Looping guarantees the result is always in the future regardless of
    // how many intervals have to be skipped.
    while (!target.after(now)) {
        target.add(Calendar.HOUR_OF_DAY, intervalHours)
    }
    return target.timeInMillis - now.timeInMillis
}

// Request codes for the two independent alarm "channels" so they never
// overwrite each other's PendingIntent.
private const val ALARM_REQUEST_CODE_DAILY = 1001
private const val ALARM_REQUEST_CODE_CYCLING = 1002

private fun alarmRequestCode(uniqueWorkName: String) =
    if (uniqueWorkName == "WallpaperCycling") ALARM_REQUEST_CODE_CYCLING else ALARM_REQUEST_CODE_DAILY

private fun buildAlarmPendingIntent(context: Context, uniqueWorkName: String, source: String, create: Boolean): PendingIntent? {
    val intent = Intent(context, WallpaperAlarmReceiver::class.java).apply {
        putExtra(WallpaperAlarmReceiver.EXTRA_UNIQUE_WORK_NAME, uniqueWorkName)
        putExtra(WallpaperAlarmReceiver.EXTRA_SOURCE, source)
    }
    val flags = (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE) or
            PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(context, alarmRequestCode(uniqueWorkName), intent, flags)
}

/**
 * Schedules an exact wallpaper-change alarm for [triggerAtMillis].
 *
 * This is the actual fix for the "wallpaper changes later and later every
 * time" drift: a WorkManager PeriodicWorkRequest (or even a OneTimeWorkRequest
 * with setInitialDelay) only guarantees "no earlier than" — Android's Doze
 * mode and per-app battery/standby throttling are free to push the real
 * execution back by anywhere from a few minutes to, on some phones,
 * 15–20+ minutes, and that slack has no reason to shrink back down, so each
 * run tends to land later than the one before. AlarmManager.
 * setExactAndAllowWhileIdle is the one API Android grants an explicit
 * exception to Doze deferral for, which is why it's used here instead.
 *
 * If the exact-alarm permission isn't granted (Android 12+, revocable in
 * Settings → Apps → special access → Alarms & reminders), this falls back to
 * a WorkManager OneTimeWorkRequest with the same delay so auto-wallpaper still
 * works — just without the precise-timing guarantee — instead of doing
 * nothing at all.
 */
fun scheduleExactWallpaperAlarm(context: Context, uniqueWorkName: String, triggerAtMillis: Long, source: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Safety net: a trigger time that's already in the past (or barely in
    // the future) would make AlarmManager fire immediately, and if whatever
    // reschedules the *next* alarm has the same bug, that becomes a tight
    // refire loop (this happened once from a delay-computation bug — see
    // computeDailyCycleInitialDelayMs). Clamp to at least 60s out and log
    // loudly, so a future bug is visibly caught here instead of silently
    // spinning.
    val minTriggerAtMillis = System.currentTimeMillis() + 60_000L
    val safeTriggerAtMillis = if (triggerAtMillis < minTriggerAtMillis) {
        AppLogger.e(context, "Alarm", "scheduleExactWallpaperAlarm got a non-future trigger time for '$uniqueWorkName' (${triggerAtMillis - System.currentTimeMillis()}ms from now) — clamping to 60s out to prevent a refire loop. This indicates a delay-calculation bug.")
        minTriggerAtMillis
    } else {
        triggerAtMillis
    }

    val pendingIntent = buildAlarmPendingIntent(context, uniqueWorkName, source, create = true)!!

    val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    if (canScheduleExact) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTriggerAtMillis, pendingIntent)
            val whenStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(safeTriggerAtMillis))
            AppLogger.i(context, "Alarm", "Exact alarm set for '$uniqueWorkName' at $whenStr")
            return
        } catch (e: SecurityException) {
            AppLogger.e(context, "Alarm", "SecurityException scheduling exact alarm: ${e.message}")
        }
    } else {
        AppLogger.w(context, "Alarm", "Exact-alarm permission not granted — falling back to WorkManager (timing may drift). Enable it in Settings → Apps → Alarms & reminders.")
    }

    // Fallback: WorkManager one-time request with the same delay.
    val delay = (safeTriggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    val data = androidx.work.workDataOf("source" to source)
    val request = OneTimeWorkRequestBuilder<DailyVerseWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(data)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request)
}

/** Cancels both the exact alarm and any WorkManager fallback job for [uniqueWorkName]. */
fun cancelExactWallpaperAlarm(context: Context, uniqueWorkName: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildAlarmPendingIntent(context, uniqueWorkName, "verse", create = false)
    if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
    WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
}

/** True if an exact alarm is currently armed for [uniqueWorkName] (used for the Developer Logs diagnostic). */
fun isWallpaperAlarmScheduled(context: Context, uniqueWorkName: String): Boolean =
    buildAlarmPendingIntent(context, uniqueWorkName, "verse", create = false) != null

fun scheduleDailyWallpaper(context: Context, hour: Int) {
    val initialDelay = computeDailyInitialDelayMs(hour)
    scheduleExactWallpaperAlarm(context, "DailyBibleWallpaper", System.currentTimeMillis() + initialDelay, "verse")
}

/**
 * Schedules the wallpaper worker based on interval.
 * For 12h/24h, aligns to the user-chosen time-of-day (dailyHour) in LOCAL time.
 * For shorter intervals (1/2/3/6h), aligns to local-midnight-based slot
 * boundaries — see computeSlotInitialDelayMs.
 *
 * Implemented as a self-rescheduling chain of exact AlarmManager alarms
 * (see scheduleExactWallpaperAlarm) so every run recomputes the exact delay
 * to the next hour boundary — this is what keeps the fire time pinned to
 * hh:00:00 instead of drifting later with every execution (see
 * DailyVerseWorker.rescheduleNext).
 */
fun scheduleAutoWallpaper(context: Context, intervalHours: Int, dailyHour: Int) {
    val initialDelayMs = when (intervalHours) {
        24 -> computeDailyInitialDelayMs(dailyHour)
        12 -> computeDailyCycleInitialDelayMs(dailyHour, 12)
        else -> computeSlotInitialDelayMs(intervalHours)
    }
    scheduleExactWallpaperAlarm(context, "DailyBibleWallpaper", System.currentTimeMillis() + initialDelayMs, "verse")
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
 * Schedules or cancels the wallpaper cycling alarm.
 *
 * If wallpaper cycling is enabled and the mode requires a periodic trigger
 * (CUSTOM_INTERVAL), arms an exact alarm under a unique name "WallpaperCycling".
 * If disabled or in ON_SCREEN_OFF / DAY_NIGHT mode, cancels any existing
 * cycling alarm.
 *
 * This is separate from the verse cycling alarm ("DailyBibleWallpaper") so
 * both can run independently.
 */
fun scheduleWallpaperCycling(context: Context) {
    val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
    val settings = WallpaperSettings.load(prefs)

    if (!settings.cycleEnabled) {
        cancelExactWallpaperAlarm(context, "WallpaperCycling")
        return
    }

    when (settings.cycleMode) {
        com.daklok.biblelockscreen.WallpaperManager.CYCLE_ON_SCREEN_OFF -> {
            // No periodic alarm needed — ScreenOffReceiver handles it
            cancelExactWallpaperAlarm(context, "WallpaperCycling")
        }
        else -> {
            // CUSTOM_INTERVAL — schedule a self-rescheduling exact alarm
            // (independent of verse cycling). Chained via rescheduleNext()
            // (instead of a PeriodicWorkRequest) keeps every run pinned to
            // the exact hour boundary instead of drifting later with each
            // execution.
            val intervalHours = settings.cycleIntervalHours
            val initialDelayMs = if (intervalHours == 12 || intervalHours == 24) {
                computeDailyCycleInitialDelayMs(settings.cycleDailyHour, intervalHours)
            } else {
                computeSlotInitialDelayMs(intervalHours)
            }
            scheduleExactWallpaperAlarm(context, "WallpaperCycling", System.currentTimeMillis() + initialDelayMs, "wallpaper")
        }
    }
}
