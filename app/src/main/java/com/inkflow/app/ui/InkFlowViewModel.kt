package com.inkflow.app.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.inkflow.app.data.local.entity.FolderEntity
import com.inkflow.app.data.local.entity.NotePageEntity
import com.inkflow.app.data.local.entity.NotebookEntity
import com.inkflow.app.data.model.AccentPalette
import com.inkflow.app.data.model.ChecklistItem
import com.inkflow.app.data.model.InkPoint
import com.inkflow.app.data.model.InkStroke
import com.inkflow.app.data.model.InkToolType
import com.inkflow.app.data.model.NoteEditSnapshot
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import com.inkflow.app.data.model.ThemeMode
import com.inkflow.app.data.onenote.ImportedOneNotePage
import com.inkflow.app.data.onenote.OneNoteSyncService
import com.inkflow.app.data.preferences.UserPreferences
import com.inkflow.app.data.preferences.UserPreferencesRepository
import com.inkflow.app.data.repository.InkFlowRepository
import com.inkflow.app.ui.components.EditorInputMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class SaveStatus {
    SAVED,
    SAVING,
    UNSAVED
}

data class ActiveEditorState(
    val pageId: String = "",
    val notebookId: String = "",
    val notebookTitle: String = "",
    val title: String = "",
    val bodyText: String = "",
    val checklist: List<ChecklistItem> = emptyList(),
    val strokes: List<InkStroke> = emptyList(),
    val tags: List<String> = emptyList(),
    val paperTemplate: PaperTemplate = PaperTemplate.GRID,
    val paperColor: PaperColor = PaperColor.WARM_CREAM,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isDistractionFree: Boolean = false,
    val isFullScreenCanvas: Boolean = false,
    val inputMode: EditorInputMode = EditorInputMode.DRAW,
    val activeTool: InkToolType = InkToolType.PEN,
    val activeColorArgb: Long = 0xFF4F46E5,
    val activeStrokeWidth: Float = 5.0f,
    val activeOpacity: Float = 1.0f,
    val selectedStrokeIds: Set<String> = emptySet(),
    val clipboardStrokesCount: Int = 0,
    val lastSavedAt: Long = System.currentTimeMillis(),
    val saveStatus: SaveStatus = SaveStatus.SAVED,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class InkFlowViewModel(
    private val repository: InkFlowRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    val folders: StateFlow<List<FolderEntity>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeNotebooks: StateFlow<List<NotebookEntity>> = repository.activeNotebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pinnedNotebooks: StateFlow<List<NotebookEntity>> = repository.pinnedNotebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedNotebooks: StateFlow<List<NotebookEntity>> = repository.archivedNotebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trashedNotebooks: StateFlow<List<NotebookEntity>> = repository.trashedNotebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allActivePages: StateFlow<List<NotePageEntity>> = repository.allActivePages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoritePages: StateFlow<List<NotePageEntity>> = repository.favoritePages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trashedPages: StateFlow<List<NotePageEntity>> = repository.trashedPages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Search state ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedNotebookFilter = MutableStateFlow<String?>(null)
    val selectedNotebookFilter: StateFlow<String?> = _selectedNotebookFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _onlyFavoritesFilter = MutableStateFlow(false)
    val onlyFavoritesFilter: StateFlow<Boolean> = _onlyFavoritesFilter.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<NotePageEntity>> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) repository.allActivePages else repository.searchPages(query)
        },
        _selectedNotebookFilter,
        _selectedTagFilter,
        _onlyFavoritesFilter
    ) { pages, nbId, tag, favOnly ->
        pages.filter { page ->
            val matchesNb = nbId == null || page.notebookId == nbId
            val matchesFav = !favOnly || page.isFavorite
            val matchesTag = tag == null || repository.parseTags(page.tagsCsv).contains(tag)
            matchesNb && matchesFav && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Active Note Editor state with Undo/Redo, Cut/Copy/Paste & Debounced Autosave ---
    private val _editorState = MutableStateFlow(ActiveEditorState())
    val editorState: StateFlow<ActiveEditorState> = _editorState.asStateFlow()

    private val undoStack = ArrayDeque<NoteEditSnapshot>()
    private val redoStack = ArrayDeque<NoteEditSnapshot>()
    private var copiedStrokesClipboard: List<InkStroke> = emptyList()
    private var autoSaveJob: Job? = null

    fun openNoteForEditing(pageId: String, initialMode: EditorInputMode = EditorInputMode.DRAW) {
        viewModelScope.launch {
            val page = repository.getPage(pageId) ?: return@launch
            val notebook = repository.getNotebook(page.notebookId)
            val template = runCatching { PaperTemplate.valueOf(page.paperTemplate) }
                .getOrDefault(PaperTemplate.GRID)
            val color = runCatching { PaperColor.valueOf(page.paperColor) }
                .getOrDefault(PaperColor.WARM_CREAM)

            undoStack.clear()
            redoStack.clear()

            _editorState.value = ActiveEditorState(
                pageId = page.id,
                notebookId = page.notebookId,
                notebookTitle = notebook?.title ?: "My Studio Notebook",
                title = page.title,
                bodyText = page.bodyText,
                checklist = repository.parseChecklist(page.checklistJson),
                strokes = repository.parseStrokes(page.strokesJson),
                tags = repository.parseTags(page.tagsCsv),
                paperTemplate = template,
                paperColor = color,
                isPinned = page.isPinned,
                isFavorite = page.isFavorite,
                isDistractionFree = false,
                isFullScreenCanvas = false,
                inputMode = initialMode,
                activeTool = InkToolType.PEN,
                activeColorArgb = if (color == PaperColor.MIDNIGHT_SLATE) 0xFF38BDF8 else 0xFF1E293B,
                activeStrokeWidth = 5.0f,
                activeOpacity = 1.0f,
                selectedStrokeIds = emptySet(),
                clipboardStrokesCount = copiedStrokesClipboard.size,
                lastSavedAt = page.updatedAt,
                saveStatus = SaveStatus.SAVED,
                canUndo = false,
                canRedo = false
            )
        }
    }

    private fun currentSnapshot(state: ActiveEditorState): NoteEditSnapshot = NoteEditSnapshot(
        title = state.title,
        bodyText = state.bodyText,
        checklist = state.checklist,
        tags = state.tags,
        paperTemplate = state.paperTemplate,
        paperColor = state.paperColor,
        strokes = state.strokes
    )

    private fun pushUndoSnapshot() {
        val current = _editorState.value
        if (current.pageId.isEmpty()) return
        val snap = currentSnapshot(current)
        if (undoStack.lastOrNull() != snap) {
            undoStack.addLast(snap)
            if (undoStack.size > 50) undoStack.removeFirst()
            redoStack.clear()
        }
    }

    private fun scheduleAutoSave() {
        val current = _editorState.value
        if (current.pageId.isEmpty()) return
        _editorState.value = current.copy(
            saveStatus = SaveStatus.SAVING,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350L)
            persistActiveEditorState()
        }
    }

    private suspend fun persistActiveEditorState() {
        val state = _editorState.value
        if (state.pageId.isEmpty()) return
        repository.savePageContent(
            pageId = state.pageId,
            title = state.title,
            bodyText = state.bodyText,
            checklist = state.checklist,
            strokes = state.strokes,
            tags = state.tags,
            paperTemplate = state.paperTemplate,
            paperColor = state.paperColor
        )
        val now = System.currentTimeMillis()
        _editorState.value = _editorState.value.copy(
            lastSavedAt = now,
            saveStatus = SaveStatus.SAVED,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }

    // --- Drawing, Lasso & Cut/Copy/Paste Shortcuts ---
    fun setEditorInputMode(mode: EditorInputMode) {
        _editorState.value = _editorState.value.copy(
            inputMode = mode,
            selectedStrokeIds = if (mode != EditorInputMode.LASSO) emptySet() else _editorState.value.selectedStrokeIds
        )
    }

    fun selectInkTool(tool: InkToolType) {
        val defaultOpacity = when (tool) {
            InkToolType.HIGHLIGHTER -> 0.35f
            InkToolType.MARKER -> 0.80f
            InkToolType.PENCIL -> 0.75f
            else -> 1.0f
        }
        val defaultWidth = when (tool) {
            InkToolType.HIGHLIGHTER -> 14f
            InkToolType.MARKER -> 9f
            InkToolType.BRUSH -> 7f
            InkToolType.PEN -> 5.0f
            InkToolType.BALLPOINT -> 3.5f
            InkToolType.PENCIL -> 3.0f
            InkToolType.ERASER -> 18f
        }
        _editorState.value = _editorState.value.copy(
            inputMode = EditorInputMode.DRAW,
            activeTool = tool,
            activeStrokeWidth = defaultWidth,
            activeOpacity = defaultOpacity,
            selectedStrokeIds = emptySet()
        )
    }

    fun selectInkColor(colorArgb: Long) {
        val current = _editorState.value
        if (current.selectedStrokeIds.isNotEmpty()) {
            pushUndoSnapshot()
            val updatedStrokes = current.strokes.map { s ->
                if (current.selectedStrokeIds.contains(s.id)) s.copy(colorArgb = colorArgb) else s
            }
            _editorState.value = current.copy(
                activeColorArgb = colorArgb,
                strokes = updatedStrokes
            )
            scheduleAutoSave()
        } else {
            _editorState.value = current.copy(activeColorArgb = colorArgb)
        }
    }

    fun updateStrokeWidth(width: Float) {
        _editorState.value = _editorState.value.copy(activeStrokeWidth = width.coerceIn(1.5f, 32f))
    }

    fun updateStrokeOpacity(opacity: Float) {
        _editorState.value = _editorState.value.copy(activeOpacity = opacity.coerceIn(0.15f, 1.0f))
    }

    fun addCommittedStroke(stroke: InkStroke) {
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(
            strokes = _editorState.value.strokes + stroke
        )
        scheduleAutoSave()
    }

    fun eraseStrokesByIds(idsToErase: Set<String>) {
        if (idsToErase.isEmpty()) return
        val existing = _editorState.value.strokes
        if (existing.none { idsToErase.contains(it.id) }) return
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(
            strokes = existing.filterNot { idsToErase.contains(it.id) },
            selectedStrokeIds = _editorState.value.selectedStrokeIds - idsToErase
        )
        scheduleAutoSave()
    }

    fun updateSelectedStrokes(ids: Set<String>) {
        _editorState.value = _editorState.value.copy(selectedStrokeIds = ids)
    }

    fun selectAllStrokes() {
        val allIds = _editorState.value.strokes.map { it.id }.toSet()
        _editorState.value = _editorState.value.copy(
            inputMode = EditorInputMode.LASSO,
            selectedStrokeIds = allIds
        )
    }

    fun copySelectedStrokes() {
        val state = _editorState.value
        val targets = if (state.selectedStrokeIds.isNotEmpty()) {
            state.strokes.filter { state.selectedStrokeIds.contains(it.id) }
        } else {
            state.strokes
        }
        if (targets.isEmpty()) return
        copiedStrokesClipboard = targets
        _editorState.value = state.copy(clipboardStrokesCount = copiedStrokesClipboard.size)
    }

    fun cutSelectedStrokes() {
        val state = _editorState.value
        val targets = if (state.selectedStrokeIds.isNotEmpty()) {
            state.strokes.filter { state.selectedStrokeIds.contains(it.id) }
        } else {
            state.strokes
        }
        if (targets.isEmpty()) return
        copiedStrokesClipboard = targets
        val idsToRemove = targets.map { it.id }.toSet()
        pushUndoSnapshot()
        _editorState.value = state.copy(
            strokes = state.strokes.filterNot { idsToRemove.contains(it.id) },
            selectedStrokeIds = emptySet(),
            clipboardStrokesCount = copiedStrokesClipboard.size
        )
        scheduleAutoSave()
    }

    fun pasteCopiedStrokes() {
        if (copiedStrokesClipboard.isEmpty()) return
        pushUndoSnapshot()
        val pasted = copiedStrokesClipboard.map { stroke ->
            stroke.copy(
                id = "stroke-${UUID.randomUUID()}",
                points = stroke.points.map { pt ->
                    pt.copy(x = pt.x + 28f, y = pt.y + 28f)
                }
            )
        }
        val newIds = pasted.map { it.id }.toSet()
        _editorState.value = _editorState.value.copy(
            strokes = _editorState.value.strokes + pasted,
            inputMode = EditorInputMode.LASSO,
            selectedStrokeIds = newIds
        )
        scheduleAutoSave()
    }

    fun duplicateSelectedStrokes() {
        copySelectedStrokes()
        pasteCopiedStrokes()
    }

    fun moveSelectedStrokes(dx: Float, dy: Float) {
        val selected = _editorState.value.selectedStrokeIds
        if (selected.isEmpty()) return
        val updated = _editorState.value.strokes.map { stroke ->
            if (selected.contains(stroke.id)) {
                stroke.copy(
                    points = stroke.points.map { pt ->
                        InkPoint(
                            x = pt.x + dx,
                            y = pt.y + dy,
                            pressure = pt.pressure,
                            tilt = pt.tilt,
                            timestampDeltaMs = pt.timestampDeltaMs
                        )
                    }
                )
            } else {
                stroke
            }
        }
        _editorState.value = _editorState.value.copy(strokes = updated)
        scheduleAutoSave()
    }

    fun deleteSelectedStrokes() {
        val selected = _editorState.value.selectedStrokeIds
        if (selected.isEmpty()) return
        eraseStrokesByIds(selected)
    }

    fun clearAllStrokesOnPage() {
        if (_editorState.value.strokes.isEmpty()) return
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(
            strokes = emptyList(),
            selectedStrokeIds = emptySet()
        )
        scheduleAutoSave()
    }

    fun toggleFullScreenCanvas() {
        val nextFullScreen = !_editorState.value.isFullScreenCanvas
        _editorState.value = _editorState.value.copy(
            isFullScreenCanvas = nextFullScreen,
            inputMode = if (nextFullScreen) EditorInputMode.DRAW else _editorState.value.inputMode
        )
    }

    // --- Text & Checklist Actions ---
    fun updateEditorTitle(newTitle: String) {
        if (newTitle == _editorState.value.title) return
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(title = newTitle)
        scheduleAutoSave()
    }

    fun updateEditorBody(newBody: String) {
        if (newBody == _editorState.value.bodyText) return
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(bodyText = newBody)
        scheduleAutoSave()
    }

    fun addChecklistItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        pushUndoSnapshot()
        val updated = _editorState.value.checklist + ChecklistItem(
            id = "chk-${UUID.randomUUID()}",
            text = trimmed,
            isChecked = false
        )
        _editorState.value = _editorState.value.copy(checklist = updated)
        scheduleAutoSave()
    }

    fun toggleChecklistItem(itemId: String) {
        pushUndoSnapshot()
        val updated = _editorState.value.checklist.map { item ->
            if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
        }
        _editorState.value = _editorState.value.copy(checklist = updated)
        scheduleAutoSave()
    }

    fun deleteChecklistItem(itemId: String) {
        pushUndoSnapshot()
        val updated = _editorState.value.checklist.filterNot { it.id == itemId }
        _editorState.value = _editorState.value.copy(checklist = updated)
        scheduleAutoSave()
    }

    fun addTagToActiveNote(tag: String) {
        val clean = tag.trim().lowercase().removePrefix("#")
        if (clean.isEmpty() || _editorState.value.tags.contains(clean)) return
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(tags = _editorState.value.tags + clean)
        scheduleAutoSave()
    }

    fun removeTagFromActiveNote(tag: String) {
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(tags = _editorState.value.tags - tag)
        scheduleAutoSave()
    }

    fun updatePaperTemplate(template: PaperTemplate) {
        if (template == _editorState.value.paperTemplate) return
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(paperTemplate = template)
        scheduleAutoSave()
    }

    fun updatePaperColor(color: PaperColor) {
        if (color == _editorState.value.paperColor) return
        pushUndoSnapshot()
        _editorState.value = _editorState.value.copy(paperColor = color)
        scheduleAutoSave()
    }

    fun toggleDistractionFreeMode() {
        _editorState.value = _editorState.value.copy(
            isDistractionFree = !_editorState.value.isDistractionFree
        )
    }

    fun toggleActiveNotePin() {
        val pageId = _editorState.value.pageId
        if (pageId.isEmpty()) return
        viewModelScope.launch {
            repository.togglePagePin(pageId)
            _editorState.value = _editorState.value.copy(isPinned = !_editorState.value.isPinned)
        }
    }

    fun toggleActiveNoteFavorite() {
        val pageId = _editorState.value.pageId
        if (pageId.isEmpty()) return
        viewModelScope.launch {
            repository.togglePageFavorite(pageId)
            _editorState.value = _editorState.value.copy(isFavorite = !_editorState.value.isFavorite)
        }
    }

    fun undoEditorAction() {
        if (undoStack.isEmpty()) return
        val current = _editorState.value
        redoStack.addLast(currentSnapshot(current))
        val previous = undoStack.removeLast()
        _editorState.value = current.copy(
            title = previous.title,
            bodyText = previous.bodyText,
            checklist = previous.checklist,
            tags = previous.tags,
            paperTemplate = previous.paperTemplate,
            paperColor = previous.paperColor,
            strokes = previous.strokes,
            selectedStrokeIds = emptySet(),
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
        scheduleAutoSave()
    }

    fun redoEditorAction() {
        if (redoStack.isEmpty()) return
        val current = _editorState.value
        undoStack.addLast(currentSnapshot(current))
        val next = redoStack.removeLast()
        _editorState.value = current.copy(
            title = next.title,
            bodyText = next.bodyText,
            checklist = next.checklist,
            tags = next.tags,
            paperTemplate = next.paperTemplate,
            paperColor = next.paperColor,
            strokes = next.strokes,
            selectedStrokeIds = emptySet(),
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
        scheduleAutoSave()
    }

    // --- Quick creation & organization actions ---
    fun createQuickNote(
        initialTitle: String = "Untitled Canvas",
        templateOverride: PaperTemplate? = null,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val prefs = userPreferences.value
            val targetNotebook = activeNotebooks.value.firstOrNull()
                ?: repository.createNotebook(
                    title = "My Studio Notebook",
                    description = "Your personal canvas for handwriting, sketches, and notes",
                    defaultTemplate = templateOverride ?: prefs.defaultPaperTemplate,
                    defaultPaperColor = prefs.defaultPaperColor,
                    createStarterPage = false
                )
            val newPage = repository.createPage(
                notebookId = targetNotebook.id,
                title = initialTitle,
                paperTemplate = templateOverride ?: prefs.defaultPaperTemplate,
                paperColor = prefs.defaultPaperColor
            )
            onCreated(newPage.id)
        }
    }

    fun createPageInNotebook(notebookId: String, title: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newPage = repository.createPage(
                notebookId = notebookId,
                title = title.ifBlank { "Untitled Page" }
            )
            onCreated(newPage.id)
        }
    }

    fun createNotebook(
        title: String,
        description: String,
        folderId: String?,
        coverColorHex: Long,
        template: PaperTemplate,
        paperColor: PaperColor
    ) {
        viewModelScope.launch {
            repository.createNotebook(
                title = title,
                description = description,
                folderId = folderId,
                coverColorHex = coverColorHex,
                defaultTemplate = template,
                defaultPaperColor = paperColor
            )
        }
    }

    fun renameNotebook(notebookId: String, newTitle: String, newDescription: String) {
        viewModelScope.launch {
            repository.renameNotebook(notebookId, newTitle, newDescription)
        }
    }

    fun duplicateNotebook(notebookId: String) {
        viewModelScope.launch { repository.duplicateNotebook(notebookId) }
    }

    fun toggleNotebookPin(notebookId: String) {
        viewModelScope.launch { repository.toggleNotebookPin(notebookId) }
    }

    fun toggleNotebookArchive(notebookId: String) {
        viewModelScope.launch { repository.toggleNotebookArchive(notebookId) }
    }

    fun trashNotebook(notebookId: String) {
        viewModelScope.launch { repository.moveNotebookToTrash(notebookId) }
    }

    fun restoreNotebook(notebookId: String) {
        viewModelScope.launch { repository.restoreNotebookFromTrash(notebookId) }
    }

    fun deleteNotebookForever(notebookId: String) {
        viewModelScope.launch { repository.deleteNotebookPermanently(notebookId) }
    }

    fun createFolder(name: String, parentFolderId: String?, colorHex: Long) {
        viewModelScope.launch { repository.createFolder(name, parentFolderId, colorHex) }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch { repository.renameFolder(folderId, newName) }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch { repository.deleteFolder(folderId) }
    }

    fun togglePagePin(pageId: String) {
        viewModelScope.launch { repository.togglePagePin(pageId) }
    }

    fun togglePageFavorite(pageId: String) {
        viewModelScope.launch { repository.togglePageFavorite(pageId) }
    }

    fun duplicatePage(pageId: String) {
        viewModelScope.launch { repository.duplicatePage(pageId) }
    }

    fun movePageOrder(pageId: String, direction: Int) {
        viewModelScope.launch { repository.movePageOrder(pageId, direction) }
    }

    fun trashPage(pageId: String) {
        viewModelScope.launch { repository.movePageToTrash(pageId) }
    }

    fun restorePage(pageId: String) {
        viewModelScope.launch { repository.restorePageFromTrash(pageId) }
    }

    fun deletePageForever(pageId: String) {
        viewModelScope.launch { repository.deletePagePermanently(pageId) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }

    // --- Search & Filters ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setNotebookFilter(notebookId: String?) {
        _selectedNotebookFilter.value = notebookId
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = tag
    }

    fun setOnlyFavoritesFilter(enabled: Boolean) {
        _onlyFavoritesFilter.value = enabled
    }

    // --- Preferences ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAccentPalette(palette: AccentPalette) {
        viewModelScope.launch { preferencesRepository.setAccentPalette(palette) }
    }

    fun setDefaultPaperTemplate(template: PaperTemplate) {
        viewModelScope.launch { preferencesRepository.setDefaultPaperTemplate(template) }
    }

    fun setDefaultPaperColor(color: PaperColor) {
        viewModelScope.launch { preferencesRepository.setDefaultPaperColor(color) }
    }

    fun setStylusOnlyMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setStylusOnlyMode(enabled) }
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoSaveEnabled(enabled) }
    }

    fun setAiEndpointUrl(url: String) {
        viewModelScope.launch { preferencesRepository.setAiEndpointUrl(url) }
    }

    fun clearCachedAiResults() {
        viewModelScope.launch { repository.clearCachedAiResults() }
    }

    // --- Microsoft OneNote Sync & Import ---
    private val _oneNoteStatusMessage = MutableStateFlow<String?>(null)
    val oneNoteStatusMessage: StateFlow<String?> = _oneNoteStatusMessage.asStateFlow()

    private val _isOneNoteSyncing = MutableStateFlow(false)
    val isOneNoteSyncing: StateFlow<Boolean> = _isOneNoteSyncing.asStateFlow()

    fun dismissOneNoteStatus() {
        _oneNoteStatusMessage.value = null
    }

    private val _oneNoteDeviceCode = MutableStateFlow<OneNoteSyncService.DeviceCodeInfo?>(null)
    val oneNoteDeviceCode: StateFlow<OneNoteSyncService.DeviceCodeInfo?> = _oneNoteDeviceCode.asStateFlow()

    private var oneNoteSignInJob: Job? = null

    fun cancelMicrosoftSignIn() {
        oneNoteSignInJob?.cancel()
        _oneNoteDeviceCode.value = null
        _isOneNoteSyncing.value = false
    }

    /**
     * Real Microsoft sign-in (device-code flow). The user authenticates on microsoft.com,
     * so InkFlow never handles their password. Then notebooks are fetched via Microsoft Graph.
     */
    fun signInAndSyncMicrosoftOneNote(
        clientId: String,
        onFirstPageImported: ((String) -> Unit)? = null
    ) {
        if (clientId.isBlank()) {
            _oneNoteStatusMessage.value = "Enter your Azure Application (client) ID first."
            return
        }
        oneNoteSignInJob?.cancel()
        oneNoteSignInJob = viewModelScope.launch {
            _isOneNoteSyncing.value = true
            _oneNoteStatusMessage.value = "Contacting Microsoft..."
            val info = OneNoteSyncService.startDeviceCodeSignIn(clientId).getOrElse { err ->
                _isOneNoteSyncing.value = false
                _oneNoteStatusMessage.value = "Microsoft sign-in error: ${err.message}"
                return@launch
            }
            _oneNoteDeviceCode.value = info
            _oneNoteStatusMessage.value = "Waiting for you to sign in on microsoft.com..."
            val token = OneNoteSyncService.pollDeviceCodeToken(clientId, info).getOrElse { err ->
                _oneNoteDeviceCode.value = null
                _isOneNoteSyncing.value = false
                _oneNoteStatusMessage.value = "Microsoft sign-in error: ${err.message}"
                return@launch
            }
            _oneNoteDeviceCode.value = null
            syncWithToken(token, clientId, onFirstPageImported)
        }
    }

    /** Sync using an access token the user already has (advanced). */
    fun syncMicrosoftOneNoteWithToken(
        accessToken: String,
        onFirstPageImported: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isOneNoteSyncing.value = true
            syncWithToken(accessToken, "", onFirstPageImported)
        }
    }

    private suspend fun syncWithToken(
        token: String,
        clientId: String,
        onFirstPageImported: ((String) -> Unit)?
    ) {
        _oneNoteStatusMessage.value = "Signed in. Fetching your OneNote pages..."
        val pages = OneNoteSyncService.syncFromMicrosoftGraph(token).getOrElse { err ->
            _isOneNoteSyncing.value = false
            _oneNoteStatusMessage.value = "Microsoft Graph error: ${err.message}"
            return
        }
        val createdIds = persistImportedOneNotePages(pages)
        preferencesRepository.setOneNoteConnection(
            email = "Microsoft account",
            clientId = clientId,
            syncedAt = System.currentTimeMillis()
        )
        _isOneNoteSyncing.value = false
        _oneNoteStatusMessage.value = if (pages.isEmpty()) {
            "Signed in, but no OneNote pages were found on this account."
        } else {
            "Imported ${createdIds.size} new OneNote page(s) (${pages.size - createdIds.size} already up to date)."
        }
        createdIds.firstOrNull()?.let { onFirstPageImported?.invoke(it) }
    }


    fun importOneNoteFileFromUri(
        context: Context,
        uri: Uri,
        onPageImported: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isOneNoteSyncing.value = true
            _oneNoteStatusMessage.value = "Importing OneNote file & InkML strokes..."
            val result = OneNoteSyncService.importOneNoteUri(context, uri)
            result.onSuccess { imported ->
                val ids = persistImportedOneNotePages(listOf(imported))
                _isOneNoteSyncing.value = false
                _oneNoteStatusMessage.value = "Imported '${imported.pageTitle}' from OneNote!"
                ids.firstOrNull()?.let { onPageImported?.invoke(it) }
            }.onFailure { err ->
                _isOneNoteSyncing.value = false
                _oneNoteStatusMessage.value = "Failed to import OneNote file: ${err.message}"
            }
        }
    }

    fun disconnectOneNoteAccount() {
        viewModelScope.launch {
            preferencesRepository.disconnectOneNote()
            _oneNoteStatusMessage.value = "Disconnected Microsoft OneNote account."
        }
    }

    private suspend fun persistImportedOneNotePages(pages: List<ImportedOneNotePage>): List<String> {
        if (pages.isEmpty()) return emptyList()
        val createdIds = mutableListOf<String>()
        val existingNotebooks = activeNotebooks.value.toMutableList()

        for (item in pages) {
            val notebook = existingNotebooks.find {
                it.title.equals(item.notebookTitle, ignoreCase = true)
            } ?: repository.createNotebook(
                title = item.notebookTitle,
                description = "Synced with Microsoft OneNote",
                coverColorHex = 0xFF7C3AED, // OneNote Purple signature badge
                defaultTemplate = PaperTemplate.GRID,
                defaultPaperColor = PaperColor.WARM_CREAM,
                createStarterPage = false
            ).also { existingNotebooks.add(it) }

            val alreadyImported = allActivePages.value.any {
                it.notebookId == notebook.id && it.title == item.pageTitle
            }
            if (alreadyImported) continue

            val newPage = repository.createPage(
                notebookId = notebook.id,
                title = item.pageTitle,
                bodyText = item.bodyText,
                paperTemplate = PaperTemplate.GRID,
                paperColor = PaperColor.WARM_CREAM
            )
            repository.savePageContent(
                pageId = newPage.id,
                title = item.pageTitle,
                bodyText = item.bodyText,
                checklist = item.checklistItems,
                strokes = item.inkStrokes,
                tags = item.tags,
                paperTemplate = PaperTemplate.GRID,
                paperColor = PaperColor.WARM_CREAM
            )
            createdIds.add(newPage.id)
        }
        return createdIds
    }

    class Factory(
        private val repository: InkFlowRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InkFlowViewModel(repository, preferencesRepository) as T
        }
    }
}
