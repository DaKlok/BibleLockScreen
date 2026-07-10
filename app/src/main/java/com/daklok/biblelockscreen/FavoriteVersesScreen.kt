package com.daklok.biblelockscreen

import com.daklok.biblelockscreen.strings.AppStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Favorite-verses page — the third page of the main [HorizontalPager]
 * (page 0 = lock-screen preview, page 1 = [WallpaperScreen], page 2 = this).
 *
 * The whole page is pinned to a **fixed height** equal to the preview
 * page's height (`screenHeightDp * 0.75f`, same constant `Pixel6LockScreenPreview`
 * uses). That's the fix for "the page turning gets weird once the list is
 * long": if this page's height instead grew with the number of favorites,
 * the pager (which auto-sizes to the current page via `animateContentSize`)
 * would resize — sometimes drastically — every time the user swiped onto
 * or off of it. Pinning the height means favorites just scroll *inside*
 * a stable frame, so page 0 ↔ page 2 transitions never jump, no matter
 * how many verses are saved. The empty state is vertically centered in
 * that same frame so it never looks like a mostly-blank page either.
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
    performHaptic: (HapticFeedbackType) -> Unit
) {
    var pendingRemove by remember { mutableStateOf<FavoriteVerse?>(null) }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val pageHeight = screenHeight * 0.75f

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

    // Swiping left reveals a delete background and opens the confirmation
    // dialog — it never dismisses the row directly (confirmValueChange
    // always returns false), so the card springs back while the dialog is
    // shown, and only actually disappears once the user confirms removal
    // (which drops it from the `favorites` list passed in from above).
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
        }
    ) {
        SettingsCard(
            modifier = Modifier.clickable(onClick = onSetAsWallpaper)
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