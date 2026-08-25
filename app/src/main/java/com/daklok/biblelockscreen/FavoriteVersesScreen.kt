package com.daklok.biblelockscreen

import com.daklok.biblelockscreen.strings.AppStrings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
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
 * Design language is pulled from what the app *already* does elsewhere,
 * rather than inventing a new one — so the favorites page reads as part
 * of the same app as the wallpaper and settings pages:
 *  - Circular icon container for the page header, exactly like
 *    [WallpaperScreen]'s header.
 *  - A small filled `Surface` pill for the favorite count, the same
 *    pattern as the active-wallpaper badge on the wallpaper thumbnails.
 *  - Cards use [SettingsCard]'s `surfaceContainerHigh` so they sit at the
 *    same elevation/tone as the settings cards.
 *  - Row actions are plain tinted [IconButton]s (primary for Share, error
 *    for Remove) with no extra background box — exactly how
 *    `CustomDbRow` in `VerseDatabaseScreen` does its Edit/Delete pair.
 *  - The reference pill recolors by source (custom vs. built-in), the
 *    same "tinted code badge" idea `CustomDbRow`/`BuiltInDbRow` use.
 *
 * The user can switch between three card layouts in Settings → Appearance
 * (see [FavoriteCardStyle]); all three render the same [FavoriteVerse] and
 * expose the same actions, only the presentation differs.
 *
 * The one genuinely new piece of motion is the swipe-to-delete reveal:
 * the backdrop color and delete-icon size scale continuously with how
 * far the row has been dragged, instead of flipping on/off. A haptic
 * fires exactly once when the drag crosses the trigger threshold, so the
 * "this will be removed" outcome is felt before it happens. That gesture
 * is tied directly to the active drag on that one row, so it never fires
 * just from scrolling.
 *
 * Deliberately NOT here: per-row entrance animations that replay on scroll.
 * An earlier version staggered each row's fade-in with a `LaunchedEffect`
 * inside the row composable — but `LazyColumn` recomposes rows from scratch
 * every time they re-enter the composed window, so that "entrance" replayed
 * on every scroll and made the list feel janky. `animateItem()` is used
 * instead: it only animates when a row's *position* in the list changes
 * (add/remove/reorder), never on ordinary scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    strings: AppStrings,
    appLang: String,
    favorites: List<FavoriteVerse>,
    onSetAsWallpaper: (FavoriteVerse) -> Unit,
    onShare: (FavoriteVerse) -> Unit,
    onRemove: (FavoriteVerse) -> Unit,
    performHaptic: (HapticFeedbackType) -> Unit,
    cardStyle: FavoriteCardStyle = FavoriteCardStyle.QUOTE,
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    strings.favoritesTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                // Subtitle line: a live count badge when there are verses,
                // otherwise a hint line — same compact header pattern as
                // WallpaperScreen's (title + bodySmall subtitle).
                if (favorites.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Same small filled-pill pattern as the active-wallpaper
                        // badge in WallpaperScreen, just rounder and lower-emphasis
                        // since this is a count, not a status flag.
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = strings.favoritesCount.format(favorites.size),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = strings.favoritesSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = strings.favoritesSubtitle,
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
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(
                    favorites,
                    // Stable key = identity tuple, so animateItem() can
                    // correctly track a verse across add/remove/reorder
                    // instead of treating every change as a brand-new row.
                    key = { "${it.lang}|${it.ref}|${it.text.hashCode()}" }
                ) { fav ->
                    when (cardStyle) {
                        FavoriteCardStyle.QUOTE -> FavoriteVerseRowQuote(
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
                            },
                            // animateItem() = smooth repositioning when the
                            // list changes (add/remove). Only fires on
                            // structural change, NOT on every scroll, so it
                            // avoids the "replays entrance on scroll" jank.
                            modifier = Modifier.animateItem()
                        )
                        FavoriteCardStyle.COMPACT -> FavoriteVerseRowCompact(
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
                            },
                            modifier = Modifier.animateItem()
                        )
                        FavoriteCardStyle.HERO -> FavoriteVerseRowHero(
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
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
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
 *  pages read as one visual language. Fades/scales in so it doesn't pop. */
@Composable
private fun FavoritesEmptyCard(strings: AppStrings) {
    // mount-flag so the entrance plays once on first appearance, not on
    // every recomposition. Keeps it from replaying if e.g. the theme flips.
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }
    AnimatedVisibility(
        visible = mounted,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
            scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow)),
        exit = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
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
}

// ═══════════════════════════════════════════════════════════════════════════
// Swipe-to-delete shell
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Wraps a card in a one-directional (left-only) swipe-to-delete reveal.
 *
 * Why custom and not Material3's [androidx.compose.material3.SwipeToDismissBox]:
 * SwipeToDismissBox consumes ALL horizontal drag gestures once it detects one
 * — even with `enableDismissFromStartToEnd = false`. That means a rightward
 * swipe (which should navigate the inner [HorizontalPager] back to the
 * wallpaper page) gets eaten by SwipeToDismissBox instead: the card bounces a
 * few pixels and snaps back, and the pager never sees the gesture.
 *
 * This implementation only claims the gesture when the drag is LEFTWARD
 * (negative X). Rightward and vertical drags are never consumed, so they
 * propagate cleanly to the parent pager for page navigation.
 *
 * `onDeleteTrigger` fires the moment the drag first crosses the threshold
 * (not on every pixel past it), so the confirmation dialog opens exactly
 * once per swipe. A haptic is expected to be wired in by the caller.
 *
 * @param content the card itself, rendered on top of the delete backdrop.
 */
@Composable
private fun SwipeToDeleteBox(
    onDeleteTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val triggerDistance = with(density) { 120.dp.toPx() } // swipe this far left → trigger delete
    val maxDrag = triggerDistance * 1.5f                  // clamp so card doesn't fly off-screen
    val offsetX = remember { Animatable(0f) }

    // Track whether we've already fired for *this* drag, so the trigger
    // fires exactly once per gesture instead of every frame past the line.
    var triggeredThisDrag by remember { mutableStateOf(false) }

    // Continuous 0f→1f progress toward the delete trigger. Only changes
    // while THIS row is actively being dragged, so reading it here only
    // recomposes this one row's backdrop/icon — it has no bearing on
    // scrolling the list.
    val swipeProgress = (-offsetX.value / triggerDistance).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var dragging = false
                    triggeredThisDrag = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        // Pointer lifted — gesture ended
                        if (!change.pressed) {
                            if (dragging) {
                                scope.launch {
                                    // If we crossed the trigger during the drag,
                                    // open the confirmation dialog (once).
                                    if (triggeredThisDrag) {
                                        onDeleteTrigger()
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
                            // Fire the trigger haptic the instant we first
                            // cross the threshold — once per drag. This is
                            // the "you've gone far enough" confirmation; the
                            // confirmation dialog itself opens on release.
                            if (!triggeredThisDrag && -offsetX.value >= triggerDistance) {
                                triggeredThisDrag = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    }
                }
            }
    ) {
        // Background — delete icon revealed as card slides left. Color
        // deepens from errorContainer toward error, and the icon grows
        // slightly, the closer the drag gets to the trigger distance.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    lerp(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.error,
                        swipeProgress
                    )
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = lerp(
                    MaterialTheme.colorScheme.onErrorContainer,
                    MaterialTheme.colorScheme.onError,
                    swipeProgress
                ),
                modifier = Modifier.size(20.dp + 6.dp * swipeProgress)
            )
        }

        // Foreground — the card itself, offset left by the drag amount so
        // it visually slides off to reveal the delete backdrop beneath it.
        Box(
            modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Shared card building blocks
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The small "source" badge — recolors by source, same idea as
 * CustomDbRow's (tinted, primaryContainer) vs. BuiltInDbRow's
 * (muted, surfaceVariant) code badge: a custom favorite looks visibly
 * different from a built-in one at a glance.
 */
@Composable
private fun SourceBadge(favorite: FavoriteVerse) {
    val isCustom = favorite.source.equals("custom", ignoreCase = true)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCustom) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.primaryContainer
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = favorite.ref,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isCustom) MaterialTheme.colorScheme.onTertiaryContainer
            else MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Card container shared by all three styles: same [SettingsCard] tone
 * (surfaceContainerHigh) so favorites match the settings cards, with an
 * added press-to-shrink scale for tactile feedback. The whole card is the
 * tap target for "use this as my wallpaper" (mirrors the "tap a thumbnail
 * to select it" pattern in WallpaperScreen).
 *
 * @param onTap if non-null, the card is tap-to-act and gets a press scale.
 *              Pass null for non-interactive previews (used by the settings
 *              style picker).
 */
@Composable
private fun FavoriteCardContainer(
    onTap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Spring-driven press scale — feels like the card "yields" under the
    // finger, the same expressive micro-interaction M3 recommends for
    // tappable cards. `collectIsPressedAsState()` mirrors the press state
    // that `clickable` itself tracks, so the scale animates in lockstep
    // with the actual tap gesture.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "cardPressScale"
    )

    SettingsCard(
        modifier = modifier
            .scale(scale)
            .then(
                if (onTap != null) {
                    // clickable (not a custom pointerInput) deliberately:
                    // it internally cancels itself once the gesture becomes
                    // a drag that the parent SwipeToDeleteBox has claimed,
                    // so a swipe-to-delete never also fires set-as-wallpaper.
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onTap
                    )
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Style 1 — QUOTE: verse as headline + explicit action row
// ═══════════════════════════════════════════════════════════════════════════

/**
 * "Detailed" card: the verse is the hero of the card, with a clear,
 * labelled action bar beneath it (set-as-wallpaper + share + delete).
 *
 * This is the default — it's the most legible and the action bar makes
 * each verb self-explanatory rather than relying on icon recognition.
 */
@Composable
private fun FavoriteVerseRowQuote(
    favorite: FavoriteVerse,
    strings: AppStrings,
    appLang: String,
    onSetAsWallpaper: () -> Unit,
    onShare: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeToDeleteBox(onDeleteTrigger = onDeleteRequest, modifier = modifier) {
        FavoriteCardContainer(onTap = onSetAsWallpaper) {
            // ── meta row: ref pill + added date ──
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
                SourceBadge(favorite = favorite)
                Text(
                    text = formatAddedDate(favorite.addedAt, appLang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(8.dp))
            // ── the verse itself ──
            Text(
                text = favorite.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            // ── action bar ──
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary action is a labelled text button — the verb makes
                // the tap target's meaning unambiguous.
                TextButton(
                    onClick = onSetAsWallpaper,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Wallpaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.setAsWallpaper,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = strings.share,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteRequest, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = strings.removeFromFavorites,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Style 2 — COMPACT: ref pill + text + trailing icons, single-row-ish
// ═══════════════════════════════════════════════════════════════════════════

/**
 * "Compact" card: a tight one-or-two-line row — meta on top, verse below
 * — with share/delete as always-visible trailing icon buttons. Closest to
 * the previous (pre-redesign) layout, so users who liked the old density
 * can get it back.
 */
@Composable
private fun FavoriteVerseRowCompact(
    favorite: FavoriteVerse,
    strings: AppStrings,
    appLang: String,
    onSetAsWallpaper: () -> Unit,
    onShare: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeToDeleteBox(onDeleteTrigger = onDeleteRequest, modifier = modifier) {
        FavoriteCardContainer(onTap = onSetAsWallpaper) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SourceBadge(favorite = favorite)
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
                // Always-visible action pair — same plain tinted IconButton
                // pattern (36dp target, 18dp glyph) as the Edit/Delete pair
                // in VerseDatabaseScreen's CustomDbRow.
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = strings.share,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteRequest, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = strings.removeFromFavorites,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Style 3 — HERO: large, poster-like quote with a small attribution line
// ═══════════════════════════════════════════════════════════════════════════

/**
 * "Quote" card: the verse set large with a decorative quotation glyph, and
 * the reference as an attribution line beneath it — almost like a verse-of-
 * the-day poster. Emotion-forward, less info-dense; the tallest of the three.
 */
@Composable
private fun FavoriteVerseRowHero(
    favorite: FavoriteVerse,
    strings: AppStrings,
    appLang: String,
    onSetAsWallpaper: () -> Unit,
    onShare: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeToDeleteBox(onDeleteTrigger = onDeleteRequest, modifier = modifier) {
        FavoriteCardContainer(onTap = onSetAsWallpaper) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Decorative oversized opening-quote glyph, faint and pinned
                // top-start — gives the card its "poster quote" character
                // without competing with the actual text for legibility.
                Icon(
                    imageVector = Icons.Filled.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(40.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = favorite.text,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            // Attribution line: em-dash + ref, centered, like a citation.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "— ${favorite.ref}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatAddedDate(favorite.addedAt, appLang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = strings.share,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteRequest, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = strings.removeFromFavorites,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Settings: card-style picker with live mini previews
// ═══════════════════════════════════════════════════════════════════════════

/**
 * A three-option picker for [FavoriteCardStyle], shown in Settings →
 * Appearance. Each option is a small live preview of the actual card style
 * (non-interactive) plus its label + description, so the user picks by look
 * rather than guessing from a name.
 *
 * Reads as one visual language with the rest of the appearance section
 * (same [SettingsCard] tone, same icon+label header pattern, same rounded
 * selection ring as the wallpaper "active" border).
 */
@Composable
fun FavoriteCardStylePicker(
    selected: FavoriteCardStyle,
    strings: AppStrings,
    onSelect: (FavoriteCardStyle) -> Unit
) {
    val options = listOf(
        Triple(FavoriteCardStyle.QUOTE, strings.favCardStyleQuote, strings.favCardStyleQuoteDesc),
        Triple(FavoriteCardStyle.COMPACT, strings.favCardStyleCompact, strings.favCardStyleCompactDesc),
        Triple(FavoriteCardStyle.HERO, strings.favCardStyleHero, strings.favCardStyleHeroDesc)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (style, label, desc) ->
            val isSelected = selected == style
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelect(style) }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Live mini-preview of the card style (non-interactive).
                CardStyleMiniPreview(style = style)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    minLines = 2
                )
            }
        }
    }
}

/**
 * A tiny, static stand-in for each card style — just enough to convey the
 * shape/layout at a glance inside the picker. No real data, no actions.
 */
@Composable
private fun CardStyleMiniPreview(style: FavoriteCardStyle) {
    val cardShape = RoundedCornerShape(10.dp)
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val lineBase = MaterialTheme.colorScheme.onSurface
    val pillBg = MaterialTheme.colorScheme.primaryContainer
    when (style) {
        FavoriteCardStyle.QUOTE -> {
            // meta line + 2 text lines + action row divider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(cardBg)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        Modifier
                            .size(width = 16.dp, height = 7.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(pillBg)
                    )
                    Box(
                        Modifier
                            .size(width = 18.dp, height = 5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(lineBase.copy(alpha = 0.3f))
                    )
                }
                PreviewLines(lineBase, count = 2)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(width = 22.dp, height = 6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        )
                        Box(
                            Modifier.size(7.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }
        FavoriteCardStyle.COMPACT -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(cardBg)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(width = 16.dp, height = 7.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(pillBg)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        )
                        Box(
                            Modifier.size(7.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        )
                    }
                }
                PreviewLines(lineBase, count = 2)
            }
        }
        FavoriteCardStyle.HERO -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(cardBg)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PreviewLines(lineBase, count = 2, centered = true)
                Box(
                    Modifier
                        .size(width = 26.dp, height = 5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                )
            }
        }
    }
}

/** Draws `count` grey "skeleton text" lines used by the mini previews. */
@Composable
private fun ColumnScope.PreviewLines(
    color: androidx.compose.ui.graphics.Color,
    count: Int,
    centered: Boolean = false
) {
    val weights = listOf(1f, 0.7f)
    repeat(count) { i ->
        val w = weights.getOrElse(i) { 0.6f }
        val rowMod = if (centered) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxWidth(w)
        }
        Box(
            modifier = rowMod
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.25f))
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Date formatting helpers
// ═══════════════════════════════════════════════════════════════════════════

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
