package com.daklok.biblelockscreen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
    onDbChanged: () -> Unit = {}
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
            onClick = { showSheet = true },
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

    if (showSheet) {
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
    onDbChanged: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nav by remember { mutableStateOf<DbNav>(DbNav.Home) }
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
        dragHandle = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BottomSheetDefaults.DragHandle()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
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
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.92f)) {
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
            .padding(top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        // ── Action buttons ────────────────────────────────────────────────
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        code,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Hint
        if (customDbs.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
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

    fun addVerse() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        drafts = drafts + DraftVerse()
        scope.launch {
            delay(80)
            listState.animateScrollToItem(maxOf(0, drafts.size))
        }
    }

    fun save() {
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

    Column(modifier = Modifier.fillMaxSize()) {

        //Sticky header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        supportingText = {
                            Text(
                                strings.vdbCodeHint,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    // Animated save button
                    val saveBg by animateColorAsState(
                        targetValue = if (saveSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        animationSpec = tween(350),
                        label = "save_bg"
                    )
                    Button(
                        onClick = { save() },
                        enabled = !isSaving,
                        modifier = Modifier.height(56.dp).padding(bottom = 22.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = saveBg)
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

                // Verse counter + clear all
                Row(
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
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        //Verse list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (drafts.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
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
                OutlinedButton(
                    onClick = { addVerse() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.vdbAddVerse, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(40.dp))
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
                Text("${strings.vdbVerseCard} $index", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
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
    var showCodeDialog by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    // Export confirmation popup state — mirrors the "generating" popup in MainActivity
    var exportStatus by remember { mutableStateOf("idle") } // "idle" | "exporting" | "done"

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { pendingUri = uri; showCodeDialog = true }
    }

    // Code dialog for import
    if (showCodeDialog) {
        var codeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = { Text(strings.vdbImportCodeTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        strings.vdbImportCodeDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.filter { c -> c.isLetterOrDigit() }.uppercase().take(8) },
                        label = { Text(strings.vdbCodeLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (codeInput.isBlank()) return@Button
                        showCodeDialog = false
                        isImporting = true
                        scope.launch {
                            pendingUri?.let { uri ->
                                VerseJsonManager.importFromUri(context, uri, codeInput).fold(
                                    onSuccess = { count ->
                                        isImporting = false
                                        onDbChanged()
                                        showNotification("${strings.vdbImportSuccess.format(count)} \"$codeInput\"", NotificationType.SUCCESS)
                                    },
                                    onFailure = { e ->
                                        isImporting = false
                                        showNotification("${strings.vdbImportFailed}: ${e.message}", NotificationType.ERROR)
                                    }
                                )
                            }
                        }
                    },
                    enabled = codeInput.isNotBlank()
                ) { Text(strings.vdbImport) }
            },
            dismissButton = {
                TextButton(onClick = { showCodeDialog = false }) { Text(strings.cancel) }
            }
        )
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

    val customDbs = remember { VerseJsonManager.listCustomDatabases(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            //Format hint
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

            //Import
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
            }

            //Export custom databases
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

            //Export built-in databases
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