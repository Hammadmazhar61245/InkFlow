package com.inkflow.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.inkflow.app.ui.components.EditorInputMode
import com.inkflow.app.ui.components.InkFlowEntrySplash
import com.inkflow.app.ui.screens.browser.NotebookBrowserScreen
import com.inkflow.app.ui.screens.editor.NoteEditorScreen
import com.inkflow.app.ui.screens.home.HomeDashboardScreen
import com.inkflow.app.ui.screens.search.SearchScreen
import com.inkflow.app.ui.screens.settings.SettingsScreen
import com.inkflow.app.ui.theme.InkFlowTheme

enum class TopLevelDestination(val label: String) {
    HOME("Studio"),
    NOTEBOOKS("Notebooks"),
    SEARCH("Search"),
    SETTINGS("Settings")
}

@Composable
fun InkFlowApp(viewModel: InkFlowViewModel) {
    val context = LocalContext.current
    val prefs by viewModel.userPreferences.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val activeNotebooks by viewModel.activeNotebooks.collectAsState()
    val pinnedNotebooks by viewModel.pinnedNotebooks.collectAsState()
    val archivedNotebooks by viewModel.archivedNotebooks.collectAsState()
    val trashedNotebooks by viewModel.trashedNotebooks.collectAsState()
    val allActivePages by viewModel.allActivePages.collectAsState()
    val trashedPages by viewModel.trashedPages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedNotebookFilter by viewModel.selectedNotebookFilter.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val onlyFavoritesFilter by viewModel.onlyFavoritesFilter.collectAsState()
    val editorState by viewModel.editorState.collectAsState()
    val oneNoteStatusMessage by viewModel.oneNoteStatusMessage.collectAsState()
    val isOneNoteSyncing by viewModel.isOneNoteSyncing.collectAsState()
    val oneNoteDeviceCode by viewModel.oneNoteDeviceCode.collectAsState()

    var showEntrySplash by rememberSaveable { mutableStateOf(true) }
    var currentTab by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    var activeEditorPageId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedNotebookId by rememberSaveable { mutableStateOf<String?>("nb-default") }

    InkFlowTheme(
        themeMode = prefs.themeMode,
        accentPalette = prefs.accentPalette
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (activeEditorPageId != null) {
                NoteEditorScreen(
                    state = editorState,
                    stylusOnlyMode = prefs.stylusOnlyMode,
                    onBack = { activeEditorPageId = null },
                    onTitleChange = viewModel::updateEditorTitle,
                    onBodyChange = viewModel::updateEditorBody,
                    onAddChecklistItem = viewModel::addChecklistItem,
                    onToggleChecklistItem = viewModel::toggleChecklistItem,
                    onDeleteChecklistItem = viewModel::deleteChecklistItem,
                    onAddTag = viewModel::addTagToActiveNote,
                    onRemoveTag = viewModel::removeTagFromActiveNote,
                    onSelectPaperTemplate = viewModel::updatePaperTemplate,
                    onSelectPaperColor = viewModel::updatePaperColor,
                    onToggleDistractionFree = viewModel::toggleDistractionFreeMode,
                    onToggleFullScreenCanvas = viewModel::toggleFullScreenCanvas,
                    onSetInputMode = viewModel::setEditorInputMode,
                    onSelectInkTool = viewModel::selectInkTool,
                    onSelectInkColor = viewModel::selectInkColor,
                    onUpdateStrokeWidth = viewModel::updateStrokeWidth,
                    onUpdateStrokeOpacity = viewModel::updateStrokeOpacity,
                    onCommitStroke = viewModel::addCommittedStroke,
                    onEraseStrokesByIds = viewModel::eraseStrokesByIds,
                    onUpdateSelectedStrokes = viewModel::updateSelectedStrokes,
                    onSelectAllStrokes = viewModel::selectAllStrokes,
                    onCutStrokes = viewModel::cutSelectedStrokes,
                    onCopyStrokes = viewModel::copySelectedStrokes,
                    onPasteStrokes = viewModel::pasteCopiedStrokes,
                    onDuplicateStrokes = viewModel::duplicateSelectedStrokes,
                    onMoveSelectedStrokes = viewModel::moveSelectedStrokes,
                    onDeleteSelectedStrokes = viewModel::deleteSelectedStrokes,
                    onClearAllStrokes = viewModel::clearAllStrokesOnPage,
                    onTogglePin = viewModel::toggleActiveNotePin,
                    onToggleFavorite = viewModel::toggleActiveNoteFavorite,
                    onUndo = viewModel::undoEditorAction,
                    onRedo = viewModel::redoEditorAction
                )
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == TopLevelDestination.HOME,
                                onClick = { currentTab = TopLevelDestination.HOME },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Studio") },
                                label = { Text("Studio") }
                            )
                            NavigationBarItem(
                                selected = currentTab == TopLevelDestination.NOTEBOOKS,
                                onClick = { currentTab = TopLevelDestination.NOTEBOOKS },
                                icon = { Icon(Icons.Default.AutoStories, contentDescription = "Notebooks") },
                                label = { Text("Notebooks") }
                            )
                            NavigationBarItem(
                                selected = currentTab == TopLevelDestination.SEARCH,
                                onClick = { currentTab = TopLevelDestination.SEARCH },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                label = { Text("Search") }
                            )
                            NavigationBarItem(
                                selected = currentTab == TopLevelDestination.SETTINGS,
                                onClick = { currentTab = TopLevelDestination.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            TopLevelDestination.HOME -> HomeDashboardScreen(
                                notebooks = activeNotebooks,
                                pinnedNotebooks = pinnedNotebooks,
                                folders = folders,
                                recentPages = allActivePages,
                                oneNoteConnectedEmail = prefs.oneNoteConnectedEmail,
                                isOneNoteSyncing = isOneNoteSyncing,
                                oneNoteStatusMessage = oneNoteStatusMessage,
                                onDismissOneNoteStatus = viewModel::dismissOneNoteStatus,
                                oneNoteClientId = prefs.oneNoteClientId,
                                oneNoteDeviceCode = oneNoteDeviceCode,
                                onStartMicrosoftSignIn = { clientId ->
                                    viewModel.signInAndSyncMicrosoftOneNote(clientId)
                                },
                                onSyncWithToken = { token ->
                                    viewModel.syncMicrosoftOneNoteWithToken(token)
                                },
                                onCancelMicrosoftSignIn = viewModel::cancelMicrosoftSignIn,
                                onImportOneNoteUri = { uri ->
                                    viewModel.importOneNoteFileFromUri(context, uri) { newPageId ->
                                        viewModel.openNoteForEditing(newPageId, EditorInputMode.DRAW)
                                        activeEditorPageId = newPageId
                                    }
                                },
                                onDisconnectOneNote = viewModel::disconnectOneNoteAccount,
                                onOpenPage = { pageId ->
                                    viewModel.openNoteForEditing(pageId, EditorInputMode.DRAW)
                                    activeEditorPageId = pageId
                                },
                                onOpenNotebook = { notebookId ->
                                    expandedNotebookId = notebookId
                                    currentTab = TopLevelDestination.NOTEBOOKS
                                },
                                onQuickSketchCanvas = {
                                    viewModel.createQuickNote(
                                        initialTitle = "Infinite Grid Canvas",
                                        templateOverride = com.inkflow.app.data.model.PaperTemplate.GRID
                                    ) { newPageId ->
                                        viewModel.openNoteForEditing(newPageId, EditorInputMode.DRAW)
                                        activeEditorPageId = newPageId
                                    }
                                },
                                onOpenSearch = { currentTab = TopLevelDestination.SEARCH },
                                onTogglePagePin = viewModel::togglePagePin,
                                onTogglePageFavorite = viewModel::togglePageFavorite,
                                onCreateNotebook = viewModel::createNotebook
                            )

                            TopLevelDestination.NOTEBOOKS -> NotebookBrowserScreen(
                                folders = folders,
                                activeNotebooks = activeNotebooks,
                                archivedNotebooks = archivedNotebooks,
                                trashedNotebooks = trashedNotebooks,
                                allActivePages = allActivePages,
                                trashedPages = trashedPages,
                                selectedNotebookId = expandedNotebookId,
                                onSelectNotebook = { expandedNotebookId = it },
                                onOpenPage = { pageId ->
                                    viewModel.openNoteForEditing(pageId, EditorInputMode.DRAW)
                                    activeEditorPageId = pageId
                                },
                                onCreatePageInNotebook = { nbId, title ->
                                    viewModel.createPageInNotebook(nbId, title) { newPageId ->
                                        viewModel.openNoteForEditing(newPageId, EditorInputMode.DRAW)
                                        activeEditorPageId = newPageId
                                    }
                                },
                                onCreateNotebook = viewModel::createNotebook,
                                onRenameNotebook = viewModel::renameNotebook,
                                onDuplicateNotebook = viewModel::duplicateNotebook,
                                onToggleNotebookPin = viewModel::toggleNotebookPin,
                                onToggleNotebookArchive = viewModel::toggleNotebookArchive,
                                onTrashNotebook = viewModel::trashNotebook,
                                onRestoreNotebook = viewModel::restoreNotebook,
                                onDeleteNotebookForever = viewModel::deleteNotebookForever,
                                onCreateFolder = viewModel::createFolder,
                                onDeleteFolder = viewModel::deleteFolder,
                                onDuplicatePage = viewModel::duplicatePage,
                                onMovePageOrder = viewModel::movePageOrder,
                                onTrashPage = viewModel::trashPage,
                                onRestorePage = viewModel::restorePage,
                                onDeletePageForever = viewModel::deletePageForever,
                                onEmptyTrash = viewModel::emptyTrash
                            )

                            TopLevelDestination.SEARCH -> SearchScreen(
                                query = searchQuery,
                                results = searchResults,
                                notebooks = activeNotebooks,
                                selectedNotebookId = selectedNotebookFilter,
                                selectedTag = selectedTagFilter,
                                onlyFavorites = onlyFavoritesFilter,
                                onQueryChange = viewModel::setSearchQuery,
                                onSelectNotebookFilter = viewModel::setNotebookFilter,
                                onSelectTagFilter = viewModel::setTagFilter,
                                onToggleFavoritesFilter = viewModel::setOnlyFavoritesFilter,
                                onOpenPage = { pageId ->
                                    viewModel.openNoteForEditing(pageId, EditorInputMode.DRAW)
                                    activeEditorPageId = pageId
                                },
                                onTogglePin = viewModel::togglePagePin,
                                onToggleFavorite = viewModel::togglePageFavorite
                            )

                            TopLevelDestination.SETTINGS -> SettingsScreen(
                                preferences = prefs,
                                onSelectThemeMode = viewModel::setThemeMode,
                                onSelectAccentPalette = viewModel::setAccentPalette,
                                onSelectDefaultTemplate = viewModel::setDefaultPaperTemplate,
                                onSelectDefaultPaperColor = viewModel::setDefaultPaperColor,
                                onToggleStylusOnly = viewModel::setStylusOnlyMode,
                                onToggleAutoSave = viewModel::setAutoSaveEnabled,
                                onClearAiCache = viewModel::clearCachedAiResults
                            )
                        }
                    }
                }
            }

            // Animated Entry Splash Overlay on app launch
            if (showEntrySplash) {
                InkFlowEntrySplash(
                    onSplashFinished = { showEntrySplash = false }
                )
            }
        }
    }
}
