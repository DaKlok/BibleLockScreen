package com.daklok.biblelockscreen

import com.daklok.biblelockscreen.strings.AppStrings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen (modal) list of the user's favorite verses.
 *
 * Presented on top of the rest of the UI via `AnimatedVisibility` +
 * `slideInVertically` from [MainActivity]'s `MainScreen` — it is not part
 * of the `HorizontalPager`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    strings: AppStrings,
    appLang: String,
    favorites: List<FavoriteVerse>,
    onBack: () -> Unit,
    onSetAsWallpaper: (FavoriteVerse) -> Unit,
    onShare: (FavoriteVerse) -> Unit,
    onRemove: (FavoriteVerse) -> Unit,
    performHaptic: (HapticFeedbackType) -> Unit
) {
    var pendingRemove by remember { mutableStateOf<FavoriteVerse?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(strings.favoritesTitle, fontWeight = FontWeight.Bold)
                            if (favorites.isNotEmpty()) {
                                Text(
                                    text = strings.favoritesCount.format(favorites.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            if (favorites.isEmpty()) {
                FavoritesEmptyState(
                    strings = strings,
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
private fun FavoritesEmptyState(strings: AppStrings, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = strings.favoritesEmpty,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = strings.favoritesEmptyDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

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
        SettingsCard {
            Text(
                text = favorite.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = favorite.ref,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = strings.addedOn.format(formatAddedDate(favorite.addedAt, appLang)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onSetAsWallpaper) {
                    Icon(
                        imageVector = Icons.Filled.Wallpaper,
                        contentDescription = strings.setAsWallpaper,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = strings.share,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = strings.removeFromFavorites,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** Small animated badge shown on the favorites icon in the TopAppBar. */
@Composable
fun FavoritesCountBadge(count: Int) {
    Badge(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(200)) { it } + fadeIn(tween(200))) togetherWith
                        (slideOutVertically(animationSpec = tween(200)) { -it } + fadeOut(tween(150)))
            },
            label = "favorites_count_badge"
        ) { animatedCount ->
            Text(animatedCount.toString())
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
    val fmt = SimpleDateFormat("d. MMMM yyyy", localeForLangCode(appLang))
    return fmt.format(Date(millis))
}