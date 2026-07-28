package com.daklok.biblelockscreen

import com.daklok.biblelockscreen.strings.AppStrings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Favorite-verses page — the third page of the main [HorizontalPager]
 * (page 0 = lock-screen preview, page 1 = [WallpaperScreen], page 2 = this).
 *
 * The whole page is pinned to a **fixed height**. That's the fix for "the
 * page turning gets weird once the list is long": if this page's height
 * instead grew with the number of favorites, the pager (which auto-sizes
 * to the current page via `animateContentSize`) would resize — sometimes
 * drastically — every time the user swiped onto or off of it. Pinning
 * the height means favorites just scroll *inside* a stable frame, so
 * page 0 ↔ page 2 transitions never jump, no matter how many verses are
 * saved. The empty state is vertically centered in that same frame so it
 * never looks like a mostly-blank page either.
 *
 * Unlike the preview page (pinned to `screenHeightDp * 0.75f` to match
 * `Pixel6LockScreenPreview`'s phone-mockup proportions), this page has no
 * verse settings or swipe-down arrow underneath it anymore, so its
 * [pageHeight] is computed by the caller from the actual space available
 * rather than a fixed fraction of the screen.
 *
 * Each row is intentionally minimal: tapping the whole card applies that
 * verse as the current wallpaper (mirrors the "tap a thumbnail to select
 * it" pattern already used in `WallpaperScreen`), a small overflow menu
 * holds the secondary actions (share / remove), and swiping the row away
 * is a shortcut for the same removal the menu offers.
 */
@Composable
fun FavoritesScreen(
    strings: AppStrings,
    appLang: String,
    favorites: List<FavoriteVerse>,
    onSetAsWallpaper: (FavoriteVerse) -> Unit,
    onShare: (FavoriteVerse) -> Unit,
    onRemove: (FavoriteVerse) -> Unit,
    performHaptic: (HapticFeedbackType) -> Unit,
    pageHeight: Dp = LocalConfiguration.current.screenHeightDp.dp * 0.88f
) {
    var pendingRemove by remember { mutableStateOf<FavoriteVerse?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(pageHeight)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────
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
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    strings.favoritesTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (favorites.isNotEmpty()) {
                    Text(
                        text = strings.favoritesCount.format(favorites.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── List / empty state — fills the rest of the fixed frame ──────
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FavoritesEmptyCard(strings = strings)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(favorites, key = { "${it.lang}|${it.ref}|${it.text.hashCode()}" }) { fav ->
                    FavoriteVerseRow(
                        favorite = fav,
                        strings = strings,
                        appLang = appLang,
                        onSetAsWallpaper = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onSetAsWallpaper(fav)
                        },
                        onShare = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onShare(fav)
                        },
                        onDeleteRequest = {
                            performHaptic(HapticFeedbackType.LongPress)
                            pendingRemove = fav
                        }
                    )
                }
            }
        }
    }

    // Confirmation dialog before removing — same style as the existing
    // backup-restore confirmation dialog (see showRestoreConfirm in MainActivity).
    pendingRemove?.let { fav ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            icon = {
                Icon(
                    Icons.Filled.Delete, null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(strings.confirmRemoveFavoriteTitle) },
            text = {
                Text(
                    strings.confirmRemoveFavoriteDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove(fav)
                        pendingRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(strings.removeFromFavorites) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text(strings.cancel) }
            }
        )
    }
}

/** Empty state — mirrors WallpaperScreen's empty-gallery card so the two
 *  pages read as one visual language. */
@Composable
private fun FavoritesEmptyCard(strings: AppStrings) {
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
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                strings.favoritesEmpty,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                strings.favoritesEmptyDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A single favorite verse.
 *
 * Minimalist by design: the reference is a small tinted pill (not another
 * full-size text line), the whole card is a tap target for "use this as
 * my wallpaper" (with a small leading wallpaper glyph as a discoverability
 * hint), and Share / Remove live in a `⋮` overflow menu instead of a row
 * of always-visible icon buttons. Swiping the card away is a shortcut for
 * the same removal the menu offers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteVerseRow(
    favorite: FavoriteVerse,
    strings: AppStrings,
    appLang: String,
    onSetAsWallpaper: () -> Unit,
    onShare: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // ─────────────────────────────────────────────────────────────────────
    // Custom one-directional swipe-to-delete.
    //
    // Material3's SwipeToDismissBox consumes ALL horizontal drag gestures
    // once it detects one — even when `enableDismissFromStartToEnd = false`.
    // That means a rightward swipe (which should navigate the inner
    // HorizontalPager back to the wallpaper page) gets eaten by the
    // SwipeToDismissBox instead: the card bounces a few pixels and snaps
    // back, and the pager never sees the gesture.
    //
    // This custom implementation only claims the gesture when the drag is
    // LEFTWARD (negative X). Rightward drags are never consumed, so they
    // propagate cleanly to the parent HorizontalPager for page navigation.
    // ─────────────────────────────────────────────────────────────────────
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val triggerDistance = with(density) { 120.dp.toPx() } // swipe this far left → trigger delete
    val maxDrag = triggerDistance * 1.5f                  // clamp so card doesn't fly off-screen
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var dragging = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        // Pointer lifted — gesture ended
                        if (!change.pressed) {
                            if (dragging) {
                                scope.launch {
                                    if (offsetX.value <= -triggerDistance) {
                                        onDeleteRequest()
                                    }
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                            break
                        }

                        val dx = change.positionChange().x
                        val dy = change.positionChange().y
                        totalX += dx
                        totalY += dy

                        if (!dragging) {
                            // Haven't committed to a direction yet — wait
                            // until movement exceeds touch slop, then decide.
                            val slop = viewConfiguration.touchSlop
                            if (abs(totalX) > slop || abs(totalY) > slop) {
                                if (abs(totalX) > abs(totalY) && totalX < 0) {
                                    // Leftward horizontal drag — claim it
                                    dragging = true
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(
                                            (offsetX.value + totalX).coerceIn(-maxDrag, 0f)
                                        )
                                    }
                                } else {
                                    // Rightward or vertical — bail out
                                    // WITHOUT consuming. The parent
                                    // HorizontalPager can then claim the
                                    // gesture for page navigation.
                                    break
                                }
                            }
                        } else {
                            // Already tracking a leftward drag — follow finger
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(
                                    (offsetX.value + dx).coerceIn(-maxDrag, 0f)
                                )
                            }
                        }
                    }
                }
            }
    ) {
        // Background — delete icon revealed as card slides left
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        // Foreground — the card itself, offset left by the drag amount
        SettingsCard(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .clickable(onClick = onSetAsWallpaper)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wallpaper,
                            contentDescription = strings.setAsWallpaper,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = favorite.ref,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = formatAddedDate(favorite.addedAt, appLang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = favorite.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(4.dp))

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.share) },
                            leadingIcon = {
                                Icon(Icons.Filled.Share, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    strings.removeFromFavorites,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteRequest()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun localeForLangCode(code: String): Locale = when (code) {
    "SK" -> Locale("sk")
    "CZ" -> Locale("cs")
    "EN" -> Locale.ENGLISH
    "ES" -> Locale("es")
    "IT" -> Locale.ITALIAN
    "FR" -> Locale.FRENCH
    "DE" -> Locale.GERMAN
    "HU" -> Locale("hu")
    "PL" -> Locale("pl")
    else -> Locale.getDefault()
}

private fun formatAddedDate(millis: Long, appLang: String): String {
    if (millis <= 0L) return ""
    val fmt = SimpleDateFormat("d. MMM yyyy", localeForLangCode(appLang))
    return fmt.format(Date(millis))
}