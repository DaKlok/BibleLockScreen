package com.daklok.biblelockscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import coil.compose.rememberAsyncImagePainter
import com.daklok.biblelockscreen.ui.theme.BibleLockScreenTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            BibleLockScreenTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Scroll state pre animáciu náhľadu
    val scrollState = rememberScrollState()

    // --- STATES ---
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Nastavenia
    var textSizeMult by remember { mutableFloatStateOf(1.0f) }
    var verticalOffset by remember { mutableFloatStateOf(0.0f) }
    var textColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var textAlpha by remember { mutableFloatStateOf(1.0f) }
    var isBold by remember { mutableStateOf(true) }
    var useShadow by remember { mutableStateOf(true) }

    // Stav Workera
    var isDailyActive by remember { mutableStateOf(false) }

    // --- LOAD SAVED ---
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
        val savedUri = prefs.getString("bg_uri", null)
        if (savedUri != null) imageUri = Uri.parse(savedUri)

        textSizeMult = prefs.getFloat("text_size_mult", 1.0f)
        verticalOffset = prefs.getFloat("vertical_offset", 0.0f)
        textColor = prefs.getInt("text_color", AndroidColor.WHITE)
        textAlpha = prefs.getFloat("text_alpha", 1.0f)
        isBold = prefs.getBoolean("is_bold", true)
        useShadow = prefs.getBoolean("use_shadow", true)

        val workInfos = WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData("DailyBibleWallpaper")
        workInfos.observeForever { infos ->
            isDailyActive = infos?.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING } ?: false
        }
    }

    // --- SAVE & ACTIONS ---
    fun saveSettings() {
        val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("text_size_mult", textSizeMult)
            .putFloat("vertical_offset", verticalOffset)
            .putInt("text_color", textColor)
            .putFloat("text_alpha", textAlpha)
            .putBoolean("is_bold", isBold)
            .putBoolean("use_shadow", useShadow)
            .apply()
    }

    fun toggleDailyWorker(enable: Boolean) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        saveSettings()
        isDailyActive = enable

        if (enable) {
            scheduleWorker(context)
            scope.launch { snackbarHostState.showSnackbar("Zapnuté! Tapeta sa zmení zajtra o 6:00.") }
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("DailyBibleWallpaper")
            scope.launch { snackbarHostState.showSnackbar("Denná zmena bola vypnutá.") }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
                    .edit().putString("bg_uri", it.toString()).apply()
            } catch (e: Exception) { /* Ignorovať */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. DYNAMICKÝ PREVIEW CARD (Parallax efekt)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer {
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
                    textSizeMult = textSizeMult,
                    verticalOffset = verticalOffset,
                    textColor = textColor,
                    textAlpha = textAlpha,
                    isBold = isBold,
                    useShadow = useShadow,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        launcher.launch("image/*")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. OBLASŤ NASTAVENÍ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // HLAVNÝ PREPÍNAČ (SWITCH)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Denná zmena tapety",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isDailyActive) "Aktívne (každé ráno 6:00)" else "Vypnuté",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDailyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isDailyActive,
                        onCheckedChange = { toggleDailyWorker(it) }
                    )
                }

                HorizontalDivider()

                // NASTAVENIA
                if (imageUri != null) {
                    Text("Prispôsobenie textu", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    // Farba textu
                    ColorPickerRow(selectedColor = textColor) {
                        textColor = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); saveSettings()
                    }

                    // Štýly
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = isBold,
                            onClick = { isBold = !isBold; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); saveSettings() },
                            label = { Text("Tučné") },
                            leadingIcon = { Icon(Icons.Outlined.FormatBold, null) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = useShadow,
                            onClick = { useShadow = !useShadow; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); saveSettings() },
                            label = { Text("Tieň") },
                            leadingIcon = { Icon(Icons.Default.Hd, null) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Slidery
                    EnhancedSlider(
                        label = "Veľkosť",
                        value = textSizeMult,
                        range = 0.5f..1.5f,
                        defaultVal = 1.0f,
                        icon = Icons.Default.TextFormat,
                        onValueChange = { textSizeMult = it; saveSettings() }
                    )

                    EnhancedSlider(
                        label = "Priehľadnosť",
                        value = textAlpha,
                        range = 0.2f..1.0f,
                        defaultVal = 1.0f,
                        icon = Icons.Default.Opacity,
                        onValueChange = { textAlpha = it; saveSettings() }
                    )

                    EnhancedSlider(
                        label = "Pozícia (Hore / Dole)",
                        value = verticalOffset,
                        range = -1.0f..1.0f,
                        defaultVal = 0.0f,
                        icon = Icons.Default.ImportExport,
                        onValueChange = { verticalOffset = it; saveSettings() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Akčné tlačidlá
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iná fotka")
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                runOneTimeWorker(context)
                                scope.launch { snackbarHostState.showSnackbar("Tapeta sa generuje...") }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testovať")
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Najskôr vyber fotku", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// --- KOMPONENTY ---

@Composable
fun Pixel6LockScreenPreview(
    uri: Uri?,
    textSizeMult: Float,
    verticalOffset: Float,
    textColor: Int,
    textAlpha: Float,
    isBold: Boolean,
    useShadow: Boolean,
    onClick: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val previewHeight = (screenHeight * 0.75f)

    Card(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            .height(previewHeight)
            .aspectRatio(9f / 20f)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Pozadie
            if (uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF212121)), contentAlignment = Alignment.Center) {
                    Text("Klikni pre výber fotky", color = Color.White)
                }
            }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                // Výpočet posunu
                val baseOffset = 80.dp
                val variableOffset = (verticalOffset * 180).dp

                Column(
                    modifier = Modifier.offset(y = baseOffset + variableOffset),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Lebo tak Boh miloval svet, že svojho jednorodeného Syna dal...",
                        color = Color(textColor).copy(alpha = textAlpha),
                        fontSize = (18 * textSizeMult).sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ján 3, 16",
                        color = Color(textColor).copy(alpha = textAlpha * 0.8f),
                        fontSize = (14 * textSizeMult).sp,
                        textAlign = TextAlign.Center,
                        style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                    )
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
                Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom=16.dp))
                Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(bottom=16.dp))
            }
        }
    }
}

val TextStyleWithShadow = TextStyle(
    shadow = androidx.compose.ui.graphics.Shadow(
        color = Color.Black,
        blurRadius = 12f
    )
)

@Composable
fun ColorPickerRow(selectedColor: Int, onColorSelected: (Int) -> Unit) {
    val colors = listOf(
        AndroidColor.WHITE,
        AndroidColor.BLACK,
        AndroidColor.parseColor("#FFFFE0"), // Light Yellow
        AndroidColor.parseColor("#87CEEB"), // Sky Blue
        AndroidColor.parseColor("#FFB6C1"), // Light Pink
        AndroidColor.parseColor("#98FB98")  // Pale Green
    )

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (selectedColor == color) 3.dp else 1.dp,
                        color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (selectedColor == color) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color == AndroidColor.WHITE || color == AndroidColor.parseColor("#FFFFE0")) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    defaultVal: Float,
    icon: ImageVector,
    onValueChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }

            // Value + Reset
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (value != defaultVal) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onValueChange(defaultVal)
                    }) {
                        Icon(Icons.Outlined.RestartAlt, "Reset", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Slider(
            value = value,
            onValueChange = {
                onValueChange(it)
                if ((it * 10).toInt() % 2 == 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        )
    }
}

// --- WORKER LOGIC ---

fun scheduleWorker(context: Context) {
    val currentDate = Calendar.getInstance()
    val dueDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 6)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    if (dueDate.before(currentDate)) {
        dueDate.add(Calendar.HOUR_OF_DAY, 24)
    }

    val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

    val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyVerseWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "DailyBibleWallpaper",
        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
        dailyWorkRequest
    )
}

fun runOneTimeWorker(context: Context) {
    val req = OneTimeWorkRequestBuilder<DailyVerseWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()
    WorkManager.getInstance(context).enqueue(req)
}