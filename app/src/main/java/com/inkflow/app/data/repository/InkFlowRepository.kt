package com.inkflow.app.data.repository

import com.inkflow.app.data.local.dao.FolderDao
import com.inkflow.app.data.local.dao.NotePageDao
import com.inkflow.app.data.local.dao.NotebookDao
import com.inkflow.app.data.local.entity.FolderEntity
import com.inkflow.app.data.local.entity.NotePageEntity
import com.inkflow.app.data.local.entity.NotebookEntity
import com.inkflow.app.data.model.ChecklistItem
import com.inkflow.app.data.model.InkStroke
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Offline-first repository coordinating all Notebook, Folder, and NotePage operations.
 * Supports creating, renaming, duplicating, archiving, reordering, pinning, favoriting,
 * searching, and trash recovery.
 */
class InkFlowRepository(
    private val folderDao: FolderDao,
    private val notebookDao: NotebookDao,
    private val notePageDao: NotePageDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    // --- Folders ---
    val allFolders: Flow<List<FolderEntity>> = folderDao.observeAllFolders()

    suspend fun createFolder(
        name: String,
        parentFolderId: String? = null,
        colorHex: Long = 0xFF2563EB
    ): FolderEntity {
        val now = System.currentTimeMillis()
        val folder = FolderEntity(
            id = "folder-${UUID.randomUUID()}",
            name = name.trim().ifEmpty { "Untitled Folder" },
            parentFolderId = parentFolderId,
            colorHex = colorHex,
            createdAt = now,
            updatedAt = now
        )
        folderDao.upsertFolder(folder)
        return folder
    }

    suspend fun renameFolder(folderId: String, newName: String) {
        val existing = folderDao.getFolderById(folderId) ?: return
        folderDao.upsertFolder(
            existing.copy(
                name = newName.trim().ifEmpty { existing.name },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteFolder(folderId: String) {
        folderDao.deleteFolderById(folderId)
    }

    // --- Notebooks ---
    val activeNotebooks: Flow<List<NotebookEntity>> = notebookDao.observeActiveNotebooks()
    val pinnedNotebooks: Flow<List<NotebookEntity>> = notebookDao.observePinnedNotebooks()
    val archivedNotebooks: Flow<List<NotebookEntity>> = notebookDao.observeArchivedNotebooks()
    val trashedNotebooks: Flow<List<NotebookEntity>> = notebookDao.observeTrashedNotebooks()

    fun observeNotebook(notebookId: String): Flow<NotebookEntity?> =
        notebookDao.observeNotebookById(notebookId)

    suspend fun getNotebook(notebookId: String): NotebookEntity? =
        notebookDao.getNotebookById(notebookId)

    suspend fun createNotebook(
        title: String,
        description: String = "",
        folderId: String? = null,
        coverColorHex: Long = 0xFF2563EB,
        defaultTemplate: PaperTemplate = PaperTemplate.RULED,
        defaultPaperColor: PaperColor = PaperColor.WARM_CREAM,
        createStarterPage: Boolean = true
    ): NotebookEntity {
        val now = System.currentTimeMillis()
        val notebook = NotebookEntity(
            id = "nb-${UUID.randomUUID()}",
            title = title.trim().ifEmpty { "Untitled Notebook" },
            description = description.trim(),
            folderId = folderId,
            coverColorHex = coverColorHex,
            defaultTemplate = defaultTemplate.name,
            defaultPaperColor = defaultPaperColor.name,
            createdAt = now,
            updatedAt = now
        )
        notebookDao.upsertNotebook(notebook)
        if (createStarterPage) {
            createPage(
                notebookId = notebook.id,
                title = "Page 1",
                paperTemplate = defaultTemplate,
                paperColor = defaultPaperColor
            )
        }
        return notebook
    }

    suspend fun renameNotebook(notebookId: String, newTitle: String, newDescription: String? = null) {
        val existing = notebookDao.getNotebookById(notebookId) ?: return
        notebookDao.upsertNotebook(
            existing.copy(
                title = newTitle.trim().ifEmpty { existing.title },
                description = newDescription?.trim() ?: existing.description,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun moveNotebookToFolder(notebookId: String, folderId: String?) {
        val existing = notebookDao.getNotebookById(notebookId) ?: return
        notebookDao.upsertNotebook(
            existing.copy(folderId = folderId, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun toggleNotebookPin(notebookId: String) {
        val existing = notebookDao.getNotebookById(notebookId) ?: return
        notebookDao.upsertNotebook(
            existing.copy(isPinned = !existing.isPinned, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun toggleNotebookArchive(notebookId: String) {
        val existing = notebookDao.getNotebookById(notebookId) ?: return
        notebookDao.upsertNotebook(
            existing.copy(isArchived = !existing.isArchived, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun duplicateNotebook(notebookId: String): NotebookEntity? {
        val source = notebookDao.getNotebookById(notebookId) ?: return null
        val now = System.currentTimeMillis()
        val copyNotebook = source.copy(
            id = "nb-${UUID.randomUUID()}",
            title = "${source.title} (Copy)",
            createdAt = now,
            updatedAt = now
        )
        notebookDao.upsertNotebook(copyNotebook)
        val sourcePages = notePageDao.getPagesForNotebookOnce(notebookId)
        val copiedPages = sourcePages.map { page ->
            page.copy(
                id = "page-${UUID.randomUUID()}",
                notebookId = copyNotebook.id,
                createdAt = now,
                updatedAt = now
            )
        }
        notePageDao.upsertPages(copiedPages)
        return copyNotebook
    }

    suspend fun moveNotebookToTrash(notebookId: String) {
        val existing = notebookDao.getNotebookById(notebookId) ?: return
        val now = System.currentTimeMillis()
        notebookDao.upsertNotebook(
            existing.copy(isTrashed = true, trashedAt = now, updatedAt = now)
        )
    }

    suspend fun restoreNotebookFromTrash(notebookId: String) {
        val existing = notebookDao.getNotebookById(notebookId) ?: return
        notebookDao.upsertNotebook(
            existing.copy(isTrashed = false, trashedAt = null, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteNotebookPermanently(notebookId: String) {
        notebookDao.deleteNotebookPermanently(notebookId)
    }

    // --- Note Pages ---
    val allActivePages: Flow<List<NotePageEntity>> = notePageDao.observeAllActivePages()
    val favoritePages: Flow<List<NotePageEntity>> = notePageDao.observeFavoritePages()
    val trashedPages: Flow<List<NotePageEntity>> = notePageDao.observeTrashedPages()

    fun observePagesForNotebook(notebookId: String): Flow<List<NotePageEntity>> =
        notePageDao.observePagesForNotebook(notebookId)

    fun observePage(pageId: String): Flow<NotePageEntity?> =
        notePageDao.observePageById(pageId)

    suspend fun getPage(pageId: String): NotePageEntity? =
        notePageDao.getPageById(pageId)

    fun searchPages(query: String): Flow<List<NotePageEntity>> =
        notePageDao.searchActivePages(query.trim())

    suspend fun createPage(
        notebookId: String,
        title: String = "Untitled Note",
        bodyText: String = "",
        paperTemplate: PaperTemplate? = null,
        paperColor: PaperColor? = null
    ): NotePageEntity {
        val parentNotebook = notebookDao.getNotebookById(notebookId)
        val resolvedTemplate = paperTemplate
            ?: runCatching { PaperTemplate.valueOf(parentNotebook?.defaultTemplate ?: "") }
                .getOrDefault(PaperTemplate.RULED)
        val resolvedColor = paperColor
            ?: runCatching { PaperColor.valueOf(parentNotebook?.defaultPaperColor ?: "") }
                .getOrDefault(PaperColor.WARM_CREAM)

        val nextOrder = notePageDao.getNextPageOrder(notebookId)
        val now = System.currentTimeMillis()
        val page = NotePageEntity(
            id = "page-${UUID.randomUUID()}",
            notebookId = notebookId,
            title = title.trim().ifEmpty { "Untitled Note" },
            bodyText = bodyText,
            paperTemplate = resolvedTemplate.name,
            paperColor = resolvedColor.name,
            pageOrder = nextOrder,
            createdAt = now,
            updatedAt = now
        )
        notePageDao.upsertPage(page)
        parentNotebook?.let {
            notebookDao.upsertNotebook(it.copy(updatedAt = now))
        }
        return page
    }

    suspend fun savePageContent(
        pageId: String,
        title: String,
        bodyText: String,
        checklist: List<ChecklistItem>,
        strokes: List<InkStroke>,
        tags: List<String>,
        paperTemplate: PaperTemplate,
        paperColor: PaperColor,
        recognizedInkText: String? = null
    ) {
        val existing = notePageDao.getPageById(pageId) ?: return
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            title = title.trim().ifEmpty { "Untitled Note" },
            bodyText = bodyText,
            checklistJson = json.encodeToString(checklist),
            strokesJson = json.encodeToString(strokes),
            tagsCsv = tags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct().joinToString(","),
            paperTemplate = paperTemplate.name,
            paperColor = paperColor.name,
            recognizedInkText = recognizedInkText ?: existing.recognizedInkText,
            updatedAt = now
        )
        notePageDao.upsertPage(updated)
        notebookDao.getNotebookById(existing.notebookId)?.let { nb ->
            notebookDao.upsertNotebook(nb.copy(updatedAt = now))
        }
    }

    suspend fun togglePagePin(pageId: String) {
        val existing = notePageDao.getPageById(pageId) ?: return
        notePageDao.upsertPage(
            existing.copy(isPinned = !existing.isPinned, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun togglePageFavorite(pageId: String) {
        val existing = notePageDao.getPageById(pageId) ?: return
        notePageDao.upsertPage(
            existing.copy(isFavorite = !existing.isFavorite, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun duplicatePage(pageId: String): NotePageEntity? {
        val existing = notePageDao.getPageById(pageId) ?: return null
        val now = System.currentTimeMillis()
        val nextOrder = notePageDao.getNextPageOrder(existing.notebookId)
        val duplicated = existing.copy(
            id = "page-${UUID.randomUUID()}",
            title = "${existing.title} (Copy)",
            pageOrder = nextOrder,
            createdAt = now,
            updatedAt = now
        )
        notePageDao.upsertPage(duplicated)
        return duplicated
    }

    /**
     * Reorders a page up (-1) or down (+1) within its notebook's page list.
     */
    suspend fun movePageOrder(pageId: String, direction: Int) {
        val page = notePageDao.getPageById(pageId) ?: return
        val siblings = notePageDao.getPagesForNotebookOnce(page.notebookId).sortedBy { it.pageOrder }
        val index = siblings.indexOfFirst { it.id == pageId }
        if (index == -1) return
        val targetIndex = index + direction
        if (targetIndex !in siblings.indices) return

        val reordered = siblings.toMutableList()
        val item = reordered.removeAt(index)
        reordered.add(targetIndex, item)
        val normalized = reordered.mapIndexed { idx, entity ->
            entity.copy(pageOrder = idx)
        }
        notePageDao.upsertPages(normalized)
    }

    suspend fun movePageToTrash(pageId: String) {
        val existing = notePageDao.getPageById(pageId) ?: return
        val now = System.currentTimeMillis()
        notePageDao.upsertPage(
            existing.copy(isTrashed = true, trashedAt = now, updatedAt = now)
        )
    }

    suspend fun restorePageFromTrash(pageId: String) {
        val existing = notePageDao.getPageById(pageId) ?: return
        notePageDao.upsertPage(
            existing.copy(isTrashed = false, trashedAt = null, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deletePagePermanently(pageId: String) {
        notePageDao.deletePagePermanently(pageId)
    }

    suspend fun emptyTrash() {
        notePageDao.emptyTrashPages()
    }

    suspend fun clearCachedAiResults() {
        notePageDao.clearAllAiCaches()
    }

    // --- JSON helpers ---
    fun parseChecklist(checklistJson: String): List<ChecklistItem> =
        runCatching { json.decodeFromString<List<ChecklistItem>>(checklistJson) }
            .getOrDefault(emptyList())

    fun parseStrokes(strokesJson: String): List<InkStroke> =
        runCatching { json.decodeFromString<List<InkStroke>>(strokesJson) }
            .getOrDefault(emptyList())

    fun parseTags(tagsCsv: String): List<String> =
        tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
