package com.daklok.biblelockscreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Wallpaper Screen — the second page of the main HorizontalPager.
//
// Shows a gallery of saved wallpapers (add / set active / delete) plus
// auto-cycling settings (interval, time, on-lock) and night-mode options.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    strings: AppStrings,
    showNotification: (String, NotificationType) -> Job,
    onWallpaperChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("bible_app_prefs", android.content.Context.MODE_PRIVATE) }

    var wallpapers by remember { mutableStateOf(WallpaperManager.listWallpapers(context)) }
    var activeId by remember { mutableStateOf(prefs.getString("active_wallpaper_id", null) ?: "") }
    var settings by remember { mutableStateOf(WallpaperSettings.load(prefs)) }
    var deleteTarget by remember { mutableStateOf<WallpaperManager.Wallpaper?>(null) }

    // Refresh the gallery every time this composable enters composition
    // (i.e. when the user swipes to the wallpaper page). This catches
    // wallpapers added via the main screen's photo picker.
    LaunchedEffect(Unit) {
        wallpapers = WallpaperManager.listWallpapers(context)
        activeId = prefs.getString("active_wallpaper_id", null) ?: ""
    }

    fun refresh() {
        wallpapers = WallpaperManager.listWallpapers(context)
        activeId = prefs.getString("active_wallpaper_id", null) ?: ""
    }

    fun setActive(wallpaper: WallpaperManager.Wallpaper) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (WallpaperManager.setActiveWallpaper(context, wallpaper.id)) {
            prefs.edit().putString("active_wallpaper_id", wallpaper.id).apply()
            activeId = wallpaper.id
            onWallpaperChanged()
            showNotification("${strings.wpSetActive}: ${wallpaper.id}", NotificationType.SUCCESS)
        }
    }

    fun saveSettings(newSettings: WallpaperSettings) {
        settings = newSettings
        WallpaperSettings.save(prefs.edit(), newSettings)
        scheduleWallpaperCycling(context)
    }

    // Multi-photo picker for adding wallpapers (only in the wallpaper screen)
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                var added = 0
                uris.forEach { uri ->
                    val newId = WallpaperManager.addWallpaper(context, uri)
                    if (newId != null) added++
                }
                refresh()
                // If no active wallpaper yet, auto-activate the first one added
                if (activeId.isBlank() && wallpapers.isNotEmpty()) {
                    val first = wallpapers.first()
                    setActive(first)
                }
                if (added > 0) {
                    showNotification(
                        if (added == 1) strings.wpAdd
                        else "$added ${strings.wpAdd}",
                        NotificationType.SUCCESS
                    )
                } else {
                    showNotification(strings.wpAdd, NotificationType.ERROR)
                }
            }
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { wp ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(strings.wpDeleteConfirm) },
            text = { Text(strings.wpDeleteConfirmDesc, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        WallpaperManager.deleteWallpaper(context, wp.id)
                        if (wp.id == activeId) {
                            // If we deleted the active one, activate the next available
                            val remaining = wallpapers.filter { it.id != wp.id }
                            if (remaining.isNotEmpty()) {
                                setActive(remaining.first())
                            } else {
                                // No wallpapers left — clear the active ID and
                                // delete the legacy user_wallpaper.jpg so the
                                // home screen shows the "no wallpaper" state.
                                prefs.edit().remove("active_wallpaper_id").apply()
                                prefs.edit().remove("bg_uri").apply()
                                activeId = ""
                                val legacyFile = java.io.File(context.filesDir, "user_wallpaper.jpg")
                                if (legacyFile.exists()) legacyFile.delete()
                                onWallpaperChanged()
                            }
                        }
                        refresh()
                        deleteTarget = null
                        showNotification(strings.wpDelete, NotificationType.INFO)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(strings.wpDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.cancel) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Wallpaper, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    strings.wpScreenTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    strings.wpScreenSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Gallery ───────────────────────────────────────────────────
        if (wallpapers.isEmpty()) {
            // Empty state — M3 Expressive
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        strings.wpGalleryEmpty,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        strings.wpGalleryEmptyDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            photoPicker.launch("image/*")
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.wpAdd, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            // Gallery — horizontal scrollable row of wallpaper thumbnails
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    wallpapers.forEach { wp ->
                        WallpaperThumbnail(
                            strings = strings,
                            context = context,
                            wallpaper = wp,
                            isActive = wp.id == activeId,
                            onSetActive = { setActive(wp) },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                deleteTarget = wp
                            },
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
                // Add button
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        photoPicker.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.wpAdd, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }

        // ── Auto-cycling settings ─────────────────────────────────────
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggle header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Schedule, null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            strings.wpCycleTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            strings.wpCycleDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.cycleEnabled,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            saveSettings(settings.copy(cycleEnabled = it))
                        }
                    )
                }

                // Sub-settings — only visible when enabled
                AnimatedVisibility(
                    visible = settings.cycleEnabled,
                    enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(280)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        // ── Mode picker (segmented buttons) ──────────────
                        val modeOptions = listOf(
                            strings.wpCycleModeInterval,
                            strings.wpCycleModeScreenOff
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            modeOptions.forEachIndexed { index, label ->
                                val mode = if (index == 0)
                                    WallpaperManager.CYCLE_CUSTOM_INTERVAL
                                else
                                    WallpaperManager.CYCLE_ON_SCREEN_OFF
                                SegmentedButton(
                                    selected = settings.cycleMode == mode,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        saveSettings(settings.copy(cycleMode = mode))
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modeOptions.size)
                                ) {
                                    Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // ── Mode-specific sub-settings ────────────────────
                        AnimatedVisibility(
                            visible = settings.cycleMode == WallpaperManager.CYCLE_CUSTOM_INTERVAL,
                            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(160)) + fadeOut(tween(160))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        strings.wpCycleInterval,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val intervalLabel = when (settings.cycleIntervalHours) {
                                        1 -> strings.autoWallpaperEvery1h
                                        2 -> strings.autoWallpaperEvery2h
                                        3 -> strings.autoWallpaperEvery3h
                                        6 -> strings.autoWallpaperEvery6h
                                        12 -> strings.autoWallpaperEvery12h
                                        else -> strings.autoWallpaperEvery24h
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            intervalLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                val intervalSteps = listOf(1, 2, 3, 6, 12, 24)
                                val sliderIndex = intervalSteps.indexOf(settings.cycleIntervalHours).coerceAtLeast(0).toFloat()
                                Slider(
                                    value = sliderIndex,
                                    onValueChange = { v ->
                                        val hours = intervalSteps[v.toInt()]
                                        saveSettings(settings.copy(cycleIntervalHours = hours))
                                    },
                                    valueRange = 0f..(intervalSteps.size - 1).toFloat(),
                                    steps = intervalSteps.size - 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Daily hour — for 12h and 24h intervals
                                AnimatedVisibility(visible = settings.cycleIntervalHours == 12 || settings.cycleIntervalHours == 24) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                strings.wpCycleDailyHour,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    String.format("%02d:00", settings.cycleDailyHour),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Slider(
                                            value = settings.cycleDailyHour.toFloat(),
                                            onValueChange = { v ->
                                                saveSettings(settings.copy(cycleDailyHour = v.toInt()))
                                            },
                                            valueRange = 0f..23f,
                                            steps = 22,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // On-screen-off info
                        AnimatedVisibility(
                            visible = settings.cycleMode == WallpaperManager.CYCLE_ON_SCREEN_OFF,
                            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(160)) + fadeOut(tween(160))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        strings.wpCycleModeScreenOffDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Warning about potential slowness
                                AnimatedVisibility(
                                    visible = true,
                                    enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                                    exit = shrinkVertically(tween(160)) + fadeOut(tween(160))
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
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wallpaper thumbnail card — shows the image with active/delete overlays
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WallpaperThumbnail(
    strings: AppStrings,
    context: android.content.Context,
    wallpaper: WallpaperManager.Wallpaper,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .aspectRatio(9f / 16f) // phone-screen aspect ratio
            .clickable { onSetActive() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Wallpaper image
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(wallpaper.file)
                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlay for badge visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            // Active badge (top-left)
            if (isActive) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            strings.wpActiveBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Delete button (top-right) — M3 Expressive squircle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(percent = 35))
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close, null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
