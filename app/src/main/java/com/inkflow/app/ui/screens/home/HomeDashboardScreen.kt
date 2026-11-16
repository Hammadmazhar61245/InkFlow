package com.inkflow.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inkflow.app.data.local.entity.FolderEntity
import com.inkflow.app.data.local.entity.NotePageEntity
import com.inkflow.app.data.local.entity.NotebookEntity
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import com.inkflow.app.ui.components.InkFlowBrandIcon
import com.inkflow.app.ui.components.PaperBackgroundCanvas
import com.inkflow.app.ui.components.formatRelativeTime

@Composable
fun HomeDashboardScreen(
    notebooks: List<NotebookEntity>,
    pinnedNotebooks: List<NotebookEntity>,
    folders: List<FolderEntity>,
    recentPages: List<NotePageEntity>,
    oneNoteConnectedEmail: String,
    isOneNoteSyncing: Boolean,
    oneNoteStatusMessage: String?,
    onDismissOneNoteStatus: () -> Unit,
    oneNoteClientId: String,
    oneNoteDeviceCode: com.inkflow.app.data.onenote.OneNoteSyncService.DeviceCodeInfo?,
    onStartMicrosoftSignIn: (String) -> Unit,
    onSyncWithToken: (String) -> Unit,
    onCancelMicrosoftSignIn: () -> Unit,
    onImportOneNoteUri: (Uri) -> Unit,
    onDisconnectOneNote: () -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenNotebook: (String) -> Unit,
    onQuickSketchCanvas: () -> Unit,
    onOpenSearch: () -> Unit,
    onTogglePagePin: (String) -> Unit,
    onTogglePageFavorite: (String) -> Unit,
    onCreateNotebook: (String, String, String?, Long, PaperTemplate, PaperColor) -> Unit
) {
    var showNewNotebookDialog by remember { mutableStateOf(false) }
    var showOneNoteConnectDialog by remember { mutableStateOf(false) }
    var showIntroGuide by rememberSaveable { mutableStateOf(true) }

    val oneNoteFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportOneNoteUri(uri)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onQuickSketchCanvas,
                icon = { Icon(Icons.Default.Draw, contentDescription = "New Infinite Canvas") },
                text = { Text("New Infinite Canvas", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Warm Studio Brand Header + Search Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InkFlowBrandIcon(size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "InkFlow Studio",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Infinite Grid & OneNote Studio",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(onClick = { showNewNotebookDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Notebook")
                        }
                    }

                    // Sleek Search Pill
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenSearch),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search notes",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Search sketches, OneNote pages, or #tags...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Warm Espresso & Amber Studio Welcome Banner (Zero AI blue!)
            if (showIntroGuide || recentPages.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF1C1917),
                                            Color(0xFF451A03),
                                            Color(0xFFB45309)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "INFINITE CANVAS STUDIO",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFFDE68A)
                                        )
                                    }
                                    if (recentPages.isNotEmpty()) {
                                        TextButton(onClick = { showIntroGuide = false }) {
                                            Text("Hide", color = Color.White.copy(alpha = 0.8f))
                                        }
                                    }
                                }

                                Text(
                                    text = "Expandable Infinite Grid & Floating Palette",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFFAF7F2)
                                )
                                Text(
                                    text = "Ã¢â‚¬Â¢ OneNote-Style Infinite Grid: Pinch with 2 fingers or tap Zoom Ã¢Ë†â€™ to zoom out as far as you wantÃ¢â‚¬â€the grid expands infinitely!\nÃ¢â‚¬Â¢ Animated Floating Palette Pod: Tap the Brush, Pigment Ring, or Nib Orb at the bottom of the canvas to pop open animated tool drawers.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFF5F1EB).copy(alpha = 0.92f)
                                )

                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = onQuickSketchCanvas,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color(0xFF1C1917)
                                    )
                                ) {
                                    Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open Infinite Grid Canvas", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Microsoft OneNote Cloud Sync & Import Hub Card (replaces removed Quick Shortcuts!)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(Color(0xFF7719AA)), // Official Microsoft OneNote Purple
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "N",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Microsoft OneNote Sync & Import",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (oneNoteConnectedEmail.isNotBlank()) {
                                            "Connected: $oneNoteConnectedEmail"
                                        } else {
                                            "Sync cloud notebooks or import OneNote pages & InkML"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (oneNoteConnectedEmail.isNotBlank()) {
                                            Color(0xFF059669)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }

                            if (isOneNoteSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp
                                )
                            } else if (oneNoteConnectedEmail.isNotBlank()) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = "Connected",
                                    tint = Color(0xFF059669)
                                )
                            }
                        }

                        AnimatedVisibility(visible = !oneNoteStatusMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDismissOneNoteStatus() }
                            ) {
                                Text(
                                    text = oneNoteStatusMessage.orEmpty(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showOneNoteConnectDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7719AA),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (oneNoteConnectedEmail.isNotBlank()) "Sync OneNote" else "Connect OneNote",
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    oneNoteFileLauncher.launch(
                                        arrayOf("text/html", "text/xml", "application/xml", "text/plain", "*/*")
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.FileOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Import File", maxLines = 1)
                            }
                        }

                        if (oneNoteConnectedEmail.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onDisconnectOneNote) {
                                    Text("Switch / Disconnect Microsoft Account")
                                }
                            }
                        }
                    }
                }
            }

            // 4. Notebooks Carousel
            if (pinnedNotebooks.isNotEmpty() || notebooks.isNotEmpty()) {
                val listToShow = if (pinnedNotebooks.isNotEmpty()) pinnedNotebooks else notebooks
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader(title = "Your Notebooks", subtitle = "${notebooks.size} total")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(listToShow, key = { it.id }) { notebook ->
                                PinnedNotebookCard(
                                    notebook = notebook,
                                    pageCount = recentPages.count { it.notebookId == notebook.id },
                                    onClick = { onOpenNotebook(notebook.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. User's Pages Section
            item {
                SectionHeader(
                    title = "Your Pages",
                    subtitle = if (recentPages.isEmpty()) "Ready for your first note" else "${recentPages.size} saved locally"
                )
            }

            if (recentPages.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Your studio canvas is clean and ready",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap 'New Infinite Canvas' below or connect your Microsoft OneNote account above to import your pages.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentPages, key = { it.id }) { page ->
                    val parentNotebook = notebooks.find { it.id == page.notebookId }
                    RecentNoteCard(
                        page = page,
                        notebookTitle = parentNotebook?.title ?: "Notebook",
                        notebookColorHex = parentNotebook?.coverColorHex ?: 0xFFD97706,
                        onClick = { onOpenPage(page.id) },
                        onTogglePin = { onTogglePagePin(page.id) },
                        onToggleFavorite = { onTogglePageFavorite(page.id) }
                    )
                }
            }
        }
    }

    if (showOneNoteConnectDialog) {
        MicrosoftOneNoteAuthModal(
            initialClientId = oneNoteClientId,
            isSyncing = isOneNoteSyncing,
            statusMessage = oneNoteStatusMessage,
            deviceCode = oneNoteDeviceCode,
            onStartSignIn = onStartMicrosoftSignIn,
            onSyncWithToken = onSyncWithToken,
            onCancel = onCancelMicrosoftSignIn,
            onDismiss = { showOneNoteConnectDialog = false }
        )
    }

    if (showNewNotebookDialog) {
        CreateNotebookDialog(
            folders = folders,
            onDismiss = { showNewNotebookDialog = false },
            onConfirm = { title, desc, folderId, colorHex, template, paperColor ->
                onCreateNotebook(title, desc, folderId, colorHex, template, paperColor)
                showNewNotebookDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PinnedNotebookCard(
    notebook: NotebookEntity,
    pageCount: Int,
    onClick: () -> Unit
) {
    val coverColor = Color(notebook.coverColorHex)
    Card(
        modifier = Modifier
            .width(210.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(coverColor)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "Pinned",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = notebook.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notebook.description.ifBlank { "Digital study notebook" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "$pageCount pages Ã¢â‚¬Â¢ ${formatRelativeTime(notebook.updatedAt)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RecentNoteCard(
    page: NotePageEntity,
    notebookTitle: String,
    notebookColorHex: Long,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val template = runCatching { PaperTemplate.valueOf(page.paperTemplate) }.getOrDefault(PaperTemplate.GRID)
    val paperColor = runCatching { PaperColor.valueOf(page.paperColor) }.getOrDefault(PaperColor.WARM_CREAM)
    val tags = page.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 84.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                PaperBackgroundCanvas(
                    paperTemplate = template,
                    paperColor = paperColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(notebookColorHex))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = notebookTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row {
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (page.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin note",
                                tint = if (page.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (page.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorite note",
                                tint = if (page.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = page.bodyText.ifBlank { "Tap to draw or write on infinite ${template.displayName.lowercase()} paper..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.take(2).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = formatRelativeTime(page.updatedAt),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CreateNotebookDialog(
    folders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, Long, PaperTemplate, PaperColor) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(folders.firstOrNull()?.id) }
    var selectedColorHex by remember { mutableLongStateOf(0xFFD97706) }
    var selectedTemplate by remember { mutableStateOf(PaperTemplate.GRID) }
    var selectedPaperColor by remember { mutableStateOf(PaperColor.WARM_CREAM) }

    val coverColors = listOf(0xFFD97706, 0xFF059669, 0xFFE11D48, 0xFF7719AA, 0xFF44403C, 0xFF2563EB)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Notebook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notebook Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Subject") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Cover Color", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    coverColors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColorHex == hex) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
                Text("Default Paper Template", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PaperTemplate.entries) { tmpl ->
                        AssistChip(
                            onClick = { selectedTemplate = tmpl },
                            label = { Text(tmpl.displayName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        title.ifBlank { "Studio Notebook" },
                        description,
                        selectedFolderId,
                        selectedColorHex,
                        selectedTemplate,
                        selectedPaperColor
                    )
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

