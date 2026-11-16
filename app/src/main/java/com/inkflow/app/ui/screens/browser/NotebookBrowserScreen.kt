package com.inkflow.app.ui.screens.browser

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.inkflow.app.data.local.entity.FolderEntity
import com.inkflow.app.data.local.entity.NotePageEntity
import com.inkflow.app.data.local.entity.NotebookEntity
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import com.inkflow.app.ui.components.formatRelativeTime
import com.inkflow.app.ui.screens.home.CreateNotebookDialog

/**
 * Full Notebook, Nested Folder, Archive & Trash Recovery Browser.
 * Supports creating/renaming/duplicating/archiving/deleting notebooks, nested folders,
 * and reordering/duplicating/restoring pages.
 */
@Composable
fun NotebookBrowserScreen(
    folders: List<FolderEntity>,
    activeNotebooks: List<NotebookEntity>,
    archivedNotebooks: List<NotebookEntity>,
    trashedNotebooks: List<NotebookEntity>,
    allActivePages: List<NotePageEntity>,
    trashedPages: List<NotePageEntity>,
    selectedNotebookId: String?,
    onSelectNotebook: (String?) -> Unit,
    onOpenPage: (String) -> Unit,
    onCreatePageInNotebook: (String, String) -> Unit,
    onCreateNotebook: (String, String, String?, Long, PaperTemplate, PaperColor) -> Unit,
    onRenameNotebook: (String, String, String) -> Unit,
    onDuplicateNotebook: (String) -> Unit,
    onToggleNotebookPin: (String) -> Unit,
    onToggleNotebookArchive: (String) -> Unit,
    onTrashNotebook: (String) -> Unit,
    onRestoreNotebook: (String) -> Unit,
    onDeleteNotebookForever: (String) -> Unit,
    onCreateFolder: (String, String?, Long) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onDuplicatePage: (String) -> Unit,
    onMovePageOrder: (String, Int) -> Unit,
    onTrashPage: (String) -> Unit,
    onRestorePage: (String) -> Unit,
    onDeletePageForever: (String) -> Unit,
    onEmptyTrash: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Archived, 2 = Trash Bin
    var selectedFolderFilter by remember { mutableStateOf<String?>(null) }
    var showCreateNotebookDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var notebookToRename by remember { mutableStateOf<NotebookEntity?>(null) }
    var showCreatePageDialogForNotebook by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Notebooks & Folders",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Organize subjects, reorder pages, or restore from trash",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { showCreateFolderDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                }
                FilledTonalButton(onClick = { showCreateNotebookDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }
        }

        TabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Notebooks (${activeNotebooks.size})") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Archived (${archivedNotebooks.size})") }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Trash (${trashedPages.size + trashedNotebooks.size})") }
            )
        }

        when (activeTab) {
            0 -> {
                val filteredNotebooks = if (selectedFolderFilter == null) {
                    activeNotebooks
                } else {
                    activeNotebooks.filter { it.folderId == selectedFolderFilter }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Folder Filter Chips (supports nested folder display)
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedFolderFilter == null,
                                    onClick = { selectedFolderFilter = null },
                                    label = { Text("All Folders") }
                                )
                            }
                            items(folders, key = { it.id }) { folder ->
                                val parentName = folders.find { it.id == folder.parentFolderId }?.name
                                val label = if (parentName != null) "$parentName / ${folder.name}" else folder.name
                                FilterChip(
                                    selected = selectedFolderFilter == folder.id,
                                    onClick = {
                                        selectedFolderFilter =
                                            if (selectedFolderFilter == folder.id) null else folder.id
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = Color(folder.colorHex),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    items(filteredNotebooks, key = { it.id }) { notebook ->
                        val pagesInNotebook = allActivePages
                            .filter { it.notebookId == notebook.id }
                            .sortedBy { it.pageOrder }
                        val isExpanded = selectedNotebookId == notebook.id

                        NotebookAccordionCard(
                            notebook = notebook,
                            pages = pagesInNotebook,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                onSelectNotebook(if (isExpanded) null else notebook.id)
                            },
                            onAddPage = { showCreatePageDialogForNotebook = notebook.id },
                            onOpenPage = onOpenPage,
                            onRename = { notebookToRename = notebook },
                            onDuplicate = { onDuplicateNotebook(notebook.id) },
                            onTogglePin = { onToggleNotebookPin(notebook.id) },
                            onArchive = { onToggleNotebookArchive(notebook.id) },
                            onTrash = { onTrashNotebook(notebook.id) },
                            onDuplicatePage = onDuplicatePage,
                            onMovePageOrder = onMovePageOrder,
                            onTrashPage = onTrashPage
                        )
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (archivedNotebooks.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                title = "No Archived Notebooks",
                                subtitle = "Archive completed semester notebooks to keep your dashboard tidy."
                            )
                        }
                    } else {
                        items(archivedNotebooks, key = { it.id }) { notebook ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(notebook.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            notebook.description.ifBlank { "Archived notebook" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(onClick = { onToggleNotebookArchive(notebook.id) }) {
                                        Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Unarchive")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (trashedPages.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Deleted Pages", style = MaterialTheme.typography.titleMedium)
                                OutlinedButton(onClick = onEmptyTrash) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Empty Trash")
                                }
                            }
                        }
                        items(trashedPages, key = { it.id }) { page ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(page.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Deleted ${formatRelativeTime(page.trashedAt ?: page.updatedAt)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { onRestorePage(page.id) }) {
                                            Icon(
                                                Icons.Default.RestoreFromTrash,
                                                contentDescription = "Restore Page",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { onDeletePageForever(page.id) }) {
                                            Icon(
                                                Icons.Default.DeleteForever,
                                                contentDescription = "Delete Forever",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (trashedNotebooks.isNotEmpty()) {
                        item {
                            Text("Deleted Notebooks", style = MaterialTheme.typography.titleMedium)
                        }
                        items(trashedNotebooks, key = { it.id }) { nb ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(nb.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Notebook in Trash",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { onRestoreNotebook(nb.id) }) {
                                            Icon(
                                                Icons.Default.RestoreFromTrash,
                                                contentDescription = "Restore Notebook",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { onDeleteNotebookForever(nb.id) }) {
                                            Icon(
                                                Icons.Default.DeleteForever,
                                                contentDescription = "Delete Notebook Forever",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (trashedPages.isEmpty() && trashedNotebooks.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                title = "Trash Bin is Empty",
                                subtitle = "Deleted notes and notebooks stay here until you permanently remove them."
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateNotebookDialog) {
        CreateNotebookDialog(
            folders = folders,
            onDismiss = { showCreateNotebookDialog = false },
            onConfirm = { title, desc, folderId, colorHex, template, paperColor ->
                onCreateNotebook(title, desc, folderId, colorHex, template, paperColor)
                showCreateNotebookDialog = false
            }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            existingFolders = folders,
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name, parentId, colorHex ->
                onCreateFolder(name, parentId, colorHex)
                showCreateFolderDialog = false
            }
        )
    }

    notebookToRename?.let { nb ->
        var newTitle by remember(nb.id) { mutableStateOf(nb.title) }
        var newDesc by remember(nb.id) { mutableStateOf(nb.description) }
        AlertDialog(
            onDismissRequest = { notebookToRename = null },
            title = { Text("Rename Notebook") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameNotebook(nb.id, newTitle, newDesc)
                        notebookToRename = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { notebookToRename = null }) { Text("Cancel") }
            }
        )
    }

    showCreatePageDialogForNotebook?.let { notebookId ->
        var pageTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePageDialogForNotebook = null },
            title = { Text("New Page in Notebook") },
            text = {
                OutlinedTextField(
                    value = pageTitle,
                    onValueChange = { pageTitle = it },
                    label = { Text("Page Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreatePageInNotebook(notebookId, pageTitle.ifBlank { "Untitled Page" })
                        showCreatePageDialogForNotebook = null
                    }
                ) { Text("Create & Open") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePageDialogForNotebook = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NotebookAccordionCard(
    notebook: NotebookEntity,
    pages: List<NotePageEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddPage: () -> Unit,
    onOpenPage: (String) -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onDuplicatePage: (String) -> Unit,
    onMovePageOrder: (String, Int) -> Unit,
    onTrashPage: (String) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(notebook.coverColorHex))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notebook.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (notebook.isPinned) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = "${pages.size} pages • Updated ${formatRelativeTime(notebook.updatedAt)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(onClick = onAddPage) {
                    Icon(Icons.Default.Add, contentDescription = "Add Page", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Page")
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Notebook Menu")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (notebook.isPinned) "Unpin Notebook" else "Pin Notebook") },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Notebook") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onTrash()
                            }
                        )
                    }
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pages.isEmpty()) {
                        Text(
                            text = "No pages yet. Tap '+ Page' to start writing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        pages.forEachIndexed { idx, page ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPage(page.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = page.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${page.paperTemplate} • ${formatRelativeTime(page.updatedAt)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMovePageOrder(page.id, -1) },
                                        enabled = idx > 0,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMovePageOrder(page.id, 1) },
                                        enabled = idx < pages.lastIndex,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDuplicatePage(page.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Duplicate Page",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onTrashPage(page.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Trash Page",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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

@Composable
private fun CreateFolderDialog(
    existingFolders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
                if (existingFolders.isNotEmpty()) {
                    Text("Parent Folder (Optional nesting)", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = parentId == null,
                                onClick = { parentId = null },
                                label = { Text("Top Level") }
                            )
                        }
                        items(existingFolders, key = { it.id }) { f ->
                            FilterChip(
                                selected = parentId == f.id,
                                onClick = { parentId = f.id },
                                label = { Text(f.name) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.ifBlank { "New Folder" }, parentId, 0xFF2563EB) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EmptyStatePanel(title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
