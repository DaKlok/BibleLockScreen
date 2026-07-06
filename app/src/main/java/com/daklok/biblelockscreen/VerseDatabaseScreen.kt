package com.daklok.biblelockscreen

import com.daklok.biblelockscreen.strings.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Entry — drop inside SettingsCard in the settings sheet:
//
//   Spacer(Modifier.height(24.dp))
//   SettingsSectionHeader(icon = Icons.AutoMirrored.Outlined.LibraryBooks, title = "Verse databases")
//   SettingsCard {
//       VerseDatabaseSection(strings = strings, showNotification = showNotification)
//   }
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun VerseDatabaseSection(
    strings: AppStrings,
    showNotification: (String, NotificationType) -> Job,
    onDbChanged: () -> Unit = {},
    // If provided, the "Manage" button delegates to the caller (who is then
    // responsible for showing VerseDatabaseSheet). If null, the section
    // manages its own sheet internally — preserves backward compatibility.
    onManage: (() -> Unit)? = null
) {
    var showSheet by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.LibraryBooks,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                strings.vdbTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                strings.vdbSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalButton(
            onClick = {
                if (onManage != null) onManage()
                else showSheet = true
            },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        ) {
            Text(strings.vdbManage, style = MaterialTheme.typography.labelMedium)
        }
    }

    if (onManage == null && showSheet) {
        VerseDatabaseSheet(
            strings = strings,
            showNotification = showNotification,
            onDismiss = { showSheet = false },
            onDbChanged = onDbChanged
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nav
// ─────────────────────────────────────────────────────────────────────────────

private sealed class DbNav {
    data object Home : DbNav()
    data object Create : DbNav()
    data class Edit(val db: CustomVerseDb) : DbNav()
    data object ImportExport : DbNav()
}

// ─────────────────────────────────────────────────────────────────────────────
// Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseDatabaseSheet(
    strings: AppStrings,
    showNotification: (String, NotificationType) -> Job,
    onDismiss: () -> Unit,
    onDbChanged: () -> Unit = {},
    openCreate: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nav by remember { mutableStateOf<DbNav>(if (openCreate) DbNav.Create else DbNav.Home) }
    val isHome = nav is DbNav.Home

    val sheetTitle = when (val n = nav) {
        is DbNav.Home -> strings.vdbTitle
        is DbNav.Create -> strings.vdbNewDatabase
        is DbNav.Edit -> "${strings.vdbEditPrefix} ${n.db.lang}"
        is DbNav.ImportExport -> strings.vdbImportExport
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight(0.92f)) {
            // Header row — back/close + animated title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 0.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (isHome) onDismiss() else nav = DbNav.Home }) {
                    Icon(
                        imageVector = if (isHome) Icons.Default.Close
                        else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedContent(
                    targetState = sheetTitle,
                    transitionSpec = {
                        (slideInVertically { -it / 2 } + fadeIn(tween(200))) togetherWith
                                (slideOutVertically { it / 2 } + fadeOut(tween(150)))
                    },
                    label = "sheet_title"
                ) { title ->
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Animated nav content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = nav,
                    transitionSpec = {
                        val toHome = targetState is DbNav.Home
                        if (toHome)
                            (slideInHorizontally { -it / 4 } + fadeIn(tween(220))) togetherWith
                                    (slideOutHorizontally { it / 4 } + fadeOut(tween(160)))
                        else
                            (slideInHorizontally { it / 4 } + fadeIn(tween(220))) togetherWith
                                    (slideOutHorizontally { -it / 4 } + fadeOut(tween(160)))
                    },
                    label = "db_nav"
                ) { current ->
                    when (current) {
                        is DbNav.Home -> HomeScreen(
                            strings = strings,
                            showNotification = showNotification,
                            onCreate = { nav = DbNav.Create },
                            onEdit = { nav = DbNav.Edit(it) },
                            onImportExport = { nav = DbNav.ImportExport },
                            onDbChanged = onDbChanged
                        )
                        is DbNav.Create -> CreateEditScreen(
                            strings = strings,
                            showNotification = showNotification,
                            existingDb = null,
                            onDone = { nav = DbNav.Home },
                            onDbChanged = onDbChanged
                        )
                        is DbNav.Edit -> CreateEditScreen(
                            strings = strings,
                            showNotification = showNotification,
                            existingDb = current.db,
                            onDone = { nav = DbNav.Home },
                            onDbChanged = onDbChanged
                        )
                        is DbNav.ImportExport -> ImportExportScreen(
                            strings = strings,
                            showNotification = showNotification,
                            onDbChanged = onDbChanged
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Home screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeScreen(
    strings: AppStrings,
    showNotification: (String, NotificationType) -> Job,
    onCreate: () -> Unit,
    onEdit: (CustomVerseDb) -> Unit,
    onImportExport: () -> Unit,
    onDbChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var customDbs by remember { mutableStateOf(VerseJsonManager.listCustomDatabases(context)) }
    var deleteTarget by remember { mutableStateOf<CustomVerseDb?>(null) }

    fun refresh() {
        customDbs = VerseJsonManager.listCustomDatabases(context)
        onDbChanged()
    }

    // Delete confirmation dialog
    deleteTarget?.let { db ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = {
                Icon(
                    Icons.Outlined.DeleteForever, null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("${strings.vdbDeleteTitle} \"${db.lang}\"?") },
            text = {
                Text(
                    "${db.verseCount} ${strings.vdbDeleteText}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        VerseJsonManager.deleteCustomDatabase(context, db.lang)
                        refresh()
                        deleteTarget = null
                        showNotification("${strings.vdbDeleted} ${db.lang}", NotificationType.INFO)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(strings.vdbDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(strings.cancel) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        // ── Action buttons ────────────────────────────────────────────────
        // Primary "Create new" gets full emphasis; "Import / Export" reads as
        // a clear secondary action with a softer outline.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCreate()
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.vdbCreateNew, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onImportExport()
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.SwapVert, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(strings.vdbImportExport)
            }
        }

        // ── Custom databases ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = customDbs.isNotEmpty(),
            enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(280)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            DbSection(label = strings.vdbSectionCustom) {
                customDbs.forEachIndexed { idx, db ->
                    if (idx > 0) HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    CustomDbRow(
                        strings = strings,
                        db = db,
                        onEdit = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEdit(db)
                        },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            deleteTarget = db
                        }
                    )
                }
            }
        }

        // ── Built-in databases ────────────────────────────────────────────
        DbSection(label = strings.vdbSectionBuiltIn) {
            availableLanguages.forEachIndexed { idx, (code, name) ->
                if (idx > 0) HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
                BuiltInDbRow(name = name, code = code)
            }
        }

        // Hint
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        ) {
            Icon(
                Icons.Default.Info, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Text(
                strings.vdbHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Section container — label + rounded card
@Composable
private fun DbSection(
    label: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun CustomDbRow(
    strings: AppStrings,
    db: CustomVerseDb,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Code badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                db.lang,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontFamily = FontFamily.Monospace
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${db.verseCount} ${strings.vdbVerses}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                strings.vdbCustomLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Edit
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Edit, "Edit",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        // Delete
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete, contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Built-in DB row — mirrors CustomDbRow styling for visual consistency, but
// uses a muted badge (since these can't be edited/deleted).
@Composable
private fun BuiltInDbRow(name: String, code: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                code,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Create / Edit (shared)
// ─────────────────────────────────────────────────────────────────────────────

data class DraftVerse(
    val id: Long = System.nanoTime(),
    val text: String = "",
    val ref: String = ""
)

@Composable
private fun CreateEditScreen(
    strings: AppStrings,
    showNotification: (String, NotificationType) -> Job,
    existingDb: CustomVerseDb?,
    onDone: () -> Unit,
    onDbChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val isEditing = existingDb != null

    var langCode by remember { mutableStateOf(existingDb?.lang ?: "") }
    var drafts by remember {
        mutableStateOf<List<DraftVerse>>(
            if (existingDb != null)
                VerseJsonManager.loadCustomVerses(context, existingDb.lang)
                    ?.map { DraftVerse(text = it.text, ref = it.ref) } ?: emptyList()
            else emptyList()
        )
    }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    // Shown when the user taps Save with a code that matches an existing
    // custom DB. The dialog is the final gate before a destructive
    // overwrite — the inline warning card below the text field is the
    // first gate.
    var showOverwriteConfirm by remember { mutableStateOf(false) }

    // Conflict detection — computed each recomposition so the inline
    // warning card stays in sync as the user types.
    val builtinCodes = remember { availableLanguages.map { it.first }.toSet() }
    val existingCustomDbs = remember { VerseJsonManager.listCustomDatabases(context) }
    val existingCustomCodes = remember(existingCustomDbs) { existingCustomDbs.map { it.lang }.toSet() }
    val trimmedCode = langCode.trim().uppercase()
    val conflictsWithCustom = !isEditing && trimmedCode.isNotBlank() && trimmedCode in existingCustomCodes
    val conflictsWithBuiltin = !isEditing && trimmedCode.isNotBlank() && trimmedCode in builtinCodes && !conflictsWithCustom
    val conflictingDbVerseCount = if (conflictsWithCustom) existingCustomDbs.find { it.lang == trimmedCode }?.verseCount ?: 0 else 0

    fun addVerse() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        drafts = drafts + DraftVerse()
        scope.launch {
            delay(80)
            listState.animateScrollToItem(maxOf(0, drafts.size))
        }
    }

    fun performSave() {
        focusManager.clearFocus()
        val code = langCode.trim().uppercase()
        if (code.isBlank()) { showNotification(strings.vdbErrorCode, NotificationType.ERROR); return }
        val valid = drafts.filter { it.text.isNotBlank() }
        if (valid.isEmpty()) { showNotification(strings.vdbErrorVerse, NotificationType.ERROR); return }
        isSaving = true
        scope.launch {
            val verses = valid.map { Verse(text = it.text.trim(), ref = it.ref.trim(), lang = code) }
            VerseJsonManager.saveCustomDatabase(context, code, verses).fold(
                onSuccess = {
                    isSaving = false
                    saveSuccess = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDbChanged()
                    showNotification(
                        if (isEditing) "${strings.vdbUpdated} \"$code\" · ${verses.size} ${strings.vdbVerses}"
                        else "${strings.vdbCreated} \"$code\" · ${verses.size} ${strings.vdbVerses}",
                        NotificationType.SUCCESS
                    )
                    delay(600)
                    onDone()
                },
                onFailure = {
                    isSaving = false
                    showNotification("${strings.vdbErrorFailed}: ${it.message}", NotificationType.ERROR)
                }
            )
        }
    }

    fun save() {
        focusManager.clearFocus()
        val code = langCode.trim().uppercase()
        if (code.isBlank()) { showNotification(strings.vdbErrorCode, NotificationType.ERROR); return }
        val valid = drafts.filter { it.text.isNotBlank() }
        if (valid.isEmpty()) { showNotification(strings.vdbErrorVerse, NotificationType.ERROR); return }
        // If the code matches an existing custom DB and we're creating new
        // (not editing the same one), gate behind a confirmation dialog so
        // the user can't accidentally overwrite their data.
        if (conflictsWithCustom) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showOverwriteConfirm = true
            return
        }
        performSave()
    }

    // Animated save-button color: primary → success green
    val saveBg by animateColorAsState(
        targetValue = if (saveSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
        animationSpec = tween(350),
        label = "save_bg"
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Top: Language code section ────────────────────────────────────
        // Full-width field — no longer crammed beside the save button. The
        // supporting text now has room to breathe and the field reads as
        // the primary identifier of the database.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = langCode,
                onValueChange = {
                    if (!isEditing)
                        langCode = it.filter { c -> c.isLetterOrDigit() }.uppercase().take(8)
                },
                label = { Text(strings.vdbCodeLabel) },
                placeholder = { Text(strings.vdbCodePlaceholder) },
                singleLine = true,
                enabled = !isEditing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                isError = conflictsWithCustom,
                supportingText = {
                    Text(
                        strings.vdbCodeHint,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )

            // ── Prominent warning cards ───────────────────────────────────
            // Replaces the old tiny supportingText warning. These cards are
            // full-width, icon-led, and use error/tertiary container colors
            // so they're impossible to miss while typing.
            AnimatedVisibility(
                visible = conflictsWithCustom,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(160)) + fadeOut(tween(160))
            ) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                strings.vdbWarningCodeCustom.format(trimmedCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            if (conflictingDbVerseCount > 0) {
                                Text(
                                    "$conflictingDbVerseCount ${strings.vdbVerses}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = conflictsWithBuiltin,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(160)) + fadeOut(tween(160))
            ) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            strings.vdbWarningCodeBuiltin.format(trimmedCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // ── Overwrite confirmation dialog ────────────────────────────────
        // Final gate before a destructive overwrite. Even if the user missed
        // the inline warning card above, this dialog forces an explicit
        // "Overwrite" / "Cancel" decision.
        if (showOverwriteConfirm) {
            AlertDialog(
                onDismissRequest = { showOverwriteConfirm = false },
                icon = {
                    Icon(
                        Icons.Default.Warning, null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text(strings.vdbOverwriteTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            strings.vdbOverwriteDesc.format(trimmedCode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (conflictingDbVerseCount > 0) {
                            Text(
                                "$conflictingDbVerseCount ${strings.vdbVerses}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOverwriteConfirm = false
                            performSave()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) { Text(strings.vdbOverwriteConfirm) }
                },
                dismissButton = {
                    TextButton(onClick = { showOverwriteConfirm = false }) { Text(strings.cancel) }
                }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        // ── Verse counter + clear all ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filledCount = drafts.count { it.text.isNotBlank() }
            AnimatedContent(
                targetState = drafts.size,
                transitionSpec = {
                    slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                },
                label = "counter",
                modifier = Modifier.weight(1f)
            ) { count ->
                Text(
                    if (count == 0) strings.vdbNoVerses
                    else "$filledCount / $count ${if (count != 1) strings.vdbVersePlural else strings.vdbVerse} ${strings.vdbVersesFilled}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(visible = drafts.isNotEmpty()) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        drafts = emptyList()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(strings.vdbClearAll, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        // ── Verse list ────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (drafts.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Expressive circular icon container — M3 Expressive
                            // promotes bold, contained iconography for empty states.
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(strings.vdbNoVersesYet, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                strings.vdbAddFirstVerse,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            itemsIndexed(drafts, key = { _, d -> d.id }) { idx, draft ->
                VerseEditorCard(
                    strings = strings,
                    index = idx + 1,
                    draft = draft,
                    autoFocus = idx == drafts.lastIndex && draft.text.isEmpty(),
                    onTextChange = { t -> drafts = drafts.map { if (it.id == draft.id) it.copy(text = t) else it } },
                    onRefChange = { r -> drafts = drafts.map { if (it.id == draft.id) it.copy(ref = r) else it } },
                    onDelete = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        drafts = drafts.filter { it.id != draft.id }
                    }
                )
            }

            item(key = "add_btn") {
                // Softer "add" tile — feels more inviting than a hard outline.
                // Uses primaryContainer tint + subtle border for a calm CTA.
                OutlinedButton(
                    onClick = { addVerse() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.vdbAddVerse, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // ── Sticky bottom save bar ────────────────────────────────────────
        // Replaces the cramped header save button. Full-width primary action
        // with shadow elevation so it reads as a pinned action bar — more
        // M3-idiomatic than a tiny button stuck next to a text field.
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Button(
                onClick = { save() },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = saveBg,
                    disabledContainerColor = saveBg.copy(alpha = 0.6f)
                )
            ) {
                AnimatedContent(
                    targetState = when { isSaving -> 0; saveSuccess -> 1; else -> 2 },
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                    label = "save_icon"
                ) { state ->
                    when (state) {
                        0 -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        1 -> Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = Color.White)
                        else -> Text(if (isEditing) strings.vdbUpdate else strings.vdbSave, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseEditorCard(
    strings: AppStrings,
    index: Int,
    draft: DraftVerse,
    autoFocus: Boolean,
    onTextChange: (String) -> Unit,
    onRefChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val textFocus = remember { FocusRequester() }
    val isFilled = draft.text.isNotBlank()

    LaunchedEffect(draft.id) {
        if (autoFocus) {
            delay(120)
            try { textFocus.requestFocus() } catch (_: Exception) {}
        }
    }

    val badgeBg by animateColorAsState(
        targetValue = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        animationSpec = tween(280), label = "badge_bg"
    )
    val badgeFg by animateColorAsState(
        targetValue = if (isFilled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        animationSpec = tween(280), label = "badge_fg"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isFilled) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null,
        modifier = Modifier.fillMaxWidth().animateContentSize(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status badge — index when empty, check when filled.
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isFilled,
                        transitionSpec = { scaleIn(tween(200)) + fadeIn() togetherWith scaleOut(tween(150)) + fadeOut() },
                        label = "badge_icon"
                    ) { filled ->
                        if (filled) {
                            Icon(Icons.Default.Check, null, tint = badgeFg, modifier = Modifier.size(14.dp))
                        } else {
                            Text("$index", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = badgeFg)
                        }
                    }
                }
                Text(
                    "${strings.vdbVerseCard} $index",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            OutlinedTextField(
                value = draft.text,
                onValueChange = onTextChange,
                label = { Text(strings.vdbVerseText) },
                modifier = Modifier.fillMaxWidth().focusRequester(textFocus),
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = draft.ref,
                onValueChange = onRefChange,
                label = { Text(strings.vdbReference) },
                placeholder = { Text(strings.vdbReferencePlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                leadingIcon = {
                    Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Import / Export
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ImportExportScreen(
    strings: AppStrings,
    showNotification: (String, NotificationType) -> Job,
    onDbChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    // Inline import panel state — replaces the old modal code dialog +
    // conflict dialog. After picking a file, the import panel slides in
    // below the Import section with a code field + live warnings (same
    // pattern as CreateEditScreen).
    var showImportPanel by remember { mutableStateOf(false) }
    var importCode by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    // Overwrite confirmation dialog — shown when the user taps Import with
    // a code that already exists as a custom DB. Same pattern as the create
    // screen so the UX is consistent.
    var showOverwriteConfirm by remember { mutableStateOf(false) }

    // Export confirmation popup state — mirrors the "generating" popup in MainActivity
    var exportStatus by remember { mutableStateOf("idle") } // "idle" | "exporting" | "done"

    // Custom databases — declared early so the import panel can refresh it
    // immediately after a successful import.
    var customDbs by remember { mutableStateOf(VerseJsonManager.listCustomDatabases(context)) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingUri = uri
            importCode = ""
            showImportPanel = true
        }
    }

    // Conflict detection for the import code field — mirrors CreateEditScreen.
    val builtinCodes = remember { availableLanguages.map { it.first }.toSet() }
    val existingCustomCodes = remember(customDbs) { customDbs.map { it.lang }.toSet() }
    val trimmedImportCode = importCode.trim().uppercase()
    val importConflictsCustom = trimmedImportCode.isNotBlank() && trimmedImportCode in existingCustomCodes
    val importConflictsBuiltin = trimmedImportCode.isNotBlank() && trimmedImportCode in builtinCodes && !importConflictsCustom
    val importConflictVerseCount = if (importConflictsCustom) customDbs.find { it.lang == trimmedImportCode }?.verseCount ?: 0 else 0

    // Shared import helper — performs the actual import, refreshes the
    // local customDbs list, and closes the inline panel.
    fun performImport(code: String) {
        isImporting = true
        scope.launch {
            pendingUri?.let { uri ->
                VerseJsonManager.importFromUri(context, uri, code).fold(
                    onSuccess = { count ->
                        isImporting = false
                        customDbs = VerseJsonManager.listCustomDatabases(context)
                        onDbChanged()
                        showImportPanel = false
                        pendingUri = null
                        importCode = ""
                        showNotification("${strings.vdbImportSuccess.format(count)} \"$code\"", NotificationType.SUCCESS)
                    },
                    onFailure = { e ->
                        isImporting = false
                        showNotification("${strings.vdbImportFailed}: ${e.message}", NotificationType.ERROR)
                    }
                )
            }
        }
    }

    fun startImport() {
        val code = importCode.trim().uppercase()
        if (code.isBlank()) return
        // If the code matches an existing custom DB, gate behind the
        // overwrite confirmation dialog (same UX as CreateEditScreen).
        if (importConflictsCustom) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showOverwriteConfirm = true
            return
        }
        performImport(code)
    }

    // Helper that exports a code and shows the animated popup
    fun doExport(code: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            exportStatus = "exporting"
            val path = VerseJsonManager.exportToDownloads(context, code)
            delay(800) // slight delay so the popup is visible
            if (path != null) {
                exportStatus = "done"
                delay(2000)
                exportStatus = "idle"
            } else {
                exportStatus = "idle"
                showNotification(strings.vdbExportFailed, NotificationType.ERROR)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Format hint
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(strings.vdbJsonFormat, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "[{\"text\":\"...\",\"ref\":\"Gen 1:1\",\"lang\":\"MY\"}]",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Import
            DbSection(label = strings.vdbImportTitle) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.vdbImportJson, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(strings.vdbImportDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        FilledTonalButton(
                            onClick = { filePicker.launch("application/json") },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ) { Text(strings.vdbBrowse, style = MaterialTheme.typography.labelMedium) }
                    }
                }

                // ── Inline import panel ──────────────────────────────────
                // Replaces the old modal code dialog. Slides in below the
                // browse row after a file is picked. Same live-warning UX
                // as CreateEditScreen — no surprise overwrites.
                AnimatedVisibility(
                    visible = showImportPanel,
                    enter = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(280)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Selected file chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Description, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                pendingUri?.lastPathSegment?.substringAfterLast('/') ?: "verses.json",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    showImportPanel = false
                                    pendingUri = null
                                    importCode = ""
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(strings.cancel, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Code field — mirrors CreateEditScreen (isError +
                        // supportingText + live warning cards below).
                        OutlinedTextField(
                            value = importCode,
                            onValueChange = { importCode = it.filter { c -> c.isLetterOrDigit() }.uppercase().take(8) },
                            label = { Text(strings.vdbCodeLabel) },
                            placeholder = { Text(strings.vdbCodePlaceholder) },
                            singleLine = true,
                            isError = importConflictsCustom,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            supportingText = {
                                Text(
                                    strings.vdbImportCodeDesc,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )

                        // Custom-conflict warning card (red)
                        AnimatedVisibility(
                            visible = importConflictsCustom,
                            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(160)) + fadeOut(tween(160))
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Warning, null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            strings.vdbWarningCodeCustom.format(trimmedImportCode),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        if (importConflictVerseCount > 0) {
                                            Text(
                                                "$importConflictVerseCount ${strings.vdbVerses}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Builtin-conflict info card (softer)
                        AnimatedVisibility(
                            visible = importConflictsBuiltin,
                            enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(160)) + fadeOut(tween(160))
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Info, null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        strings.vdbWarningCodeBuiltin.format(trimmedImportCode),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        // Import button — always enabled when code is non-blank.
                        // If the code conflicts with a custom DB, the overwrite
                        // confirmation dialog handles the gate.
                        Button(
                            onClick = { startImport() },
                            enabled = importCode.isNotBlank() && !isImporting,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(strings.vdbImport, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── Overwrite confirmation dialog ────────────────────────────
            // Same dialog as CreateEditScreen — shown when the user taps
            // Import with a code that matches an existing custom DB.
            if (showOverwriteConfirm) {
                AlertDialog(
                    onDismissRequest = { showOverwriteConfirm = false },
                    icon = {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = { Text(strings.vdbOverwriteTitle) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                strings.vdbOverwriteDesc.format(trimmedImportCode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (importConflictVerseCount > 0) {
                                Text(
                                    "$importConflictVerseCount ${strings.vdbVerses}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showOverwriteConfirm = false
                                performImport(trimmedImportCode)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) { Text(strings.vdbOverwriteConfirm) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showOverwriteConfirm = false }) { Text(strings.cancel) }
                    }
                )
            }

            // Export custom databases
            if (customDbs.isNotEmpty()) {
                DbSection(label = strings.vdbExportCustom) {
                    customDbs.forEachIndexed { idx, db ->
                        if (idx > 0) HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                        ExportRow(
                            strings = strings,
                            name = db.lang,
                            subtitle = "${db.verseCount} ${strings.vdbVerses} · ${strings.vdbCustomLabel}",
                            code = db.lang,
                            onExport = { doExport(db.lang) }
                        )
                    }
                }
            }

            // Export built-in databases
            DbSection(label = strings.vdbExportBuiltIn) {
                availableLanguages.forEachIndexed { idx, (code, name) ->
                    if (idx > 0) HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    ExportRow(
                        strings = strings,
                        name = name,
                        subtitle = "verses_$code.json",
                        code = code,
                        onExport = { doExport(code) }
                    )
                }
            }
        }

        // Export status popup
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = exportStatus != "idle",
                enter = fadeIn() + slideInVertically { it / 2 } + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + slideOutVertically { it / 2 } + scaleOut(targetScale = 0.9f)
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
                        if (exportStatus == "exporting") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(strings.vdbExporting, style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(
                                Icons.Default.Check, null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(strings.vdbExportDone, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportRow(
    strings: AppStrings,
    name: String,
    subtitle: String,
    code: String,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(
            onClick = onExport,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.vdbExport, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Verse Language Picker
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseLanguagePicker(
    strings: AppStrings,
    selectedCode: String,
    selectedSource: String,
    customDbs: List<CustomVerseDb>,
    onSelect: (code: String, source: String) -> Unit,
    onCreateCustom: () -> Unit,
    // Fired when the user taps the Default or Custom segmented button.
    // The parent uses this to auto-apply the last-selected code for that
    // segment (so switching back to Default restores e.g. Slovak).
    onSegmentChange: (source: String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    // The active segment is driven by selectedSource — this is the key fix:
    // a custom DB with code "EN" no longer pretends to be the built-in "EN".
    val isCustomSelected = selectedSource == LocalBibleProvider.SOURCE_CUSTOM
    var segment by remember(isCustomSelected) { mutableStateOf(if (isCustomSelected) 1 else 0) }
    // Which picker dialog is open: 0 = none, 1 = default, 2 = custom
    var dialogOpen by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            strings.verseLanguage,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        // ── Default / Custom segmented toggle ────────────────────────────
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = segment == 0,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    segment = 0
                    dialogOpen = 0
                    // Notify parent so it can auto-apply the last-selected
                    // built-in code. Only fires when actually switching.
                    if (isCustomSelected) onSegmentChange(LocalBibleProvider.SOURCE_BUILTIN)
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(strings.vdbSourceDefault, maxLines = 1) }

            SegmentedButton(
                selected = segment == 1,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    segment = 1
                    dialogOpen = 0
                    // Notify parent so it can auto-apply the last-selected
                    // custom code (or the first custom DB if none yet).
                    if (!isCustomSelected) onSegmentChange(LocalBibleProvider.SOURCE_CUSTOM)
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(strings.vdbSourceCustom, maxLines = 1) }
        }

        // ── Compact dropdown button / CTA per segment ────────────────────
        when (segment) {
            0 -> {
                val selectedLabel = if (isCustomSelected) {
                    availableLanguages.find { it.first == selectedCode }?.second
                        ?: strings.vdbSourceDefault
                } else {
                    availableLanguages.find { it.first == selectedCode }?.second
                        ?: selectedCode
                }
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        dialogOpen = 1
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(selectedLabel)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
            1 -> {
                if (customDbs.isEmpty()) {
                    OutlinedButton(
                        onClick = onCreateCustom,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.vdbEmptyCustomCta, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                } else {
                    val buttonLabel = if (isCustomSelected) {
                        customDbs.find { it.lang == selectedCode }?.lang ?: strings.vdbSourceCustom
                    } else {
                        strings.vdbSourceCustom
                    }
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            dialogOpen = 2
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(buttonLabel)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────
    if (dialogOpen == 1) {
        LanguagePickerDialog(
            onDismiss = { dialogOpen = 0 },
            title = strings.verseLanguage,
            dismissLabel = strings.cancel,
            icon = Icons.AutoMirrored.Outlined.LibraryBooks,
            items = availableLanguages.map { (code, name) ->
                LanguagePickerItem(code = code, title = name)
            },
            // Only mark as selected if source is BUILTIN — this is the core
            // fix for the "EN looks selected in both lists" bug.
            selectedCode = if (isCustomSelected) "" else selectedCode,
            onSelect = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(it, LocalBibleProvider.SOURCE_BUILTIN)
                dialogOpen = 0
            }
        )
    }
    if (dialogOpen == 2) {
        LanguagePickerDialog(
            onDismiss = { dialogOpen = 0 },
            title = strings.verseLanguage,
            dismissLabel = strings.cancel,
            icon = Icons.AutoMirrored.Outlined.LibraryBooks,
            items = customDbs.map { db ->
                LanguagePickerItem(
                    code = db.lang,
                    title = db.lang,
                    subtitle = "${db.verseCount} ${strings.vdbVerses} · ${strings.vdbCustomLabel}"
                )
            },
            // Only mark as selected if source is CUSTOM.
            selectedCode = if (isCustomSelected) selectedCode else "",
            onSelect = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(it, LocalBibleProvider.SOURCE_CUSTOM)
                dialogOpen = 0
            },
            footer = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            dialogOpen = 0
                            onCreateCustom()
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        strings.vdbCreateNew,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Language Picker Dialog
// ─────────────────────────────────────────────────────────────────────────────

data class LanguagePickerItem(
    val code: String,
    val title: String,
    val subtitle: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerDialog(
    onDismiss: () -> Unit,
    title: String,
    dismissLabel: String,
    icon: ImageVector? = null,
    items: List<LanguagePickerItem>,
    selectedCode: String,
    onSelect: (String) -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                // ── Title row ──────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Scrollable list ────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    items.forEach { item ->
                        val isSelected = item.code == selectedCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item.code) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Code badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    item.code,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            // Title + optional subtitle
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (item.subtitle != null) {
                                    Text(
                                        item.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Check icon when selected
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (footer != null) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        footer()
                    }
                }

                // ── Cancel button ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(dismissLabel) }
                }
            }
        }
    }
}
