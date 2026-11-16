package com.inkflow.app

import com.inkflow.app.data.local.dao.FolderDao
import com.inkflow.app.data.local.dao.NotePageDao
import com.inkflow.app.data.local.dao.NotebookDao
import com.inkflow.app.data.local.entity.FolderEntity
import com.inkflow.app.data.local.entity.NotePageEntity
import com.inkflow.app.data.local.entity.NotebookEntity
import com.inkflow.app.data.model.ChecklistItem
import com.inkflow.app.data.model.InkPoint
import com.inkflow.app.data.model.InkStroke
import com.inkflow.app.data.model.InkToolType
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import com.inkflow.app.data.repository.InkFlowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InkFlowRepositoryTest {

    private lateinit var repository: InkFlowRepository

    @Before
    fun setup() {
        repository = InkFlowRepository(
            folderDao = FakeFolderDao(),
            notebookDao = FakeNotebookDao(),
            notePageDao = FakeNotePageDao()
        )
    }

    @Test
    fun createNotebook_createsStarterPageAndSupportsPinAndDuplicate() = runTest {
        val created = repository.createNotebook(
            title = "Linear Algebra",
            description = "Eigenvalues and SVD",
            defaultTemplate = PaperTemplate.GRID,
            defaultPaperColor = PaperColor.PURE_WHITE,
            createStarterPage = true
        )
        assertEquals("Linear Algebra", created.title)

        val pages = repository.observePagesForNotebook(created.id).first()
        assertEquals(1, pages.size)
        assertEquals(PaperTemplate.GRID.name, pages.first().paperTemplate)

        repository.toggleNotebookPin(created.id)
        val pinned = repository.getNotebook(created.id)
        assertNotNull(pinned)
        assertTrue(pinned!!.isPinned)

        val duplicate = repository.duplicateNotebook(created.id)
        assertNotNull(duplicate)
        assertEquals("Linear Algebra (Copy)", duplicate!!.title)
    }

    @Test
    fun savePageContent_serializesChecklistsStrokesAndTagsAndSupportsTrashRecovery() = runTest {
        val nb = repository.createNotebook(title = "Physics", createStarterPage = false)
        val page = repository.createPage(notebookId = nb.id, title = "Kinematics")

        val checklist = listOf(
            ChecklistItem("c1", "Derive projectile range", true),
            ChecklistItem("c2", "Check units", false)
        )
        val strokes = listOf(
            InkStroke(
                id = "s1",
                tool = InkToolType.PEN,
                colorArgb = 0xFF2563EB,
                strokeWidth = 4.5f,
                points = listOf(InkPoint(10f, 20f, 0.7f), InkPoint(30f, 40f, 0.8f))
            )
        )

        repository.savePageContent(
            pageId = page.id,
            title = "Kinematics & Vectors",
            bodyText = "v = v0 + at",
            checklist = checklist,
            strokes = strokes,
            tags = listOf("Mechanics", "Exam"),
            paperTemplate = PaperTemplate.CORNELL,
            paperColor = PaperColor.SOFT_SAGE
        )

        val saved = repository.getPage(page.id)!!
        assertEquals("Kinematics & Vectors", saved.title)
        assertEquals(2, repository.parseChecklist(saved.checklistJson).size)
        assertEquals(1, repository.parseStrokes(saved.strokesJson).size)
        assertEquals(listOf("mechanics", "exam"), repository.parseTags(saved.tagsCsv))

        // Move to trash and restore
        repository.movePageToTrash(page.id)
        assertTrue(repository.getPage(page.id)!!.isTrashed)

        repository.restorePageFromTrash(page.id)
        assertFalse(repository.getPage(page.id)!!.isTrashed)
    }
}

private class FakeFolderDao : FolderDao {
    private val items = MutableStateFlow<List<FolderEntity>>(emptyList())
    override fun observeAllFolders(): Flow<List<FolderEntity>> = items
    override fun observeFoldersByParent(parentId: String?): Flow<List<FolderEntity>> =
        items.map { list -> list.filter { it.parentFolderId == parentId } }
    override suspend fun getFolderById(id: String): FolderEntity? = items.value.find { it.id == id }
    override suspend fun upsertFolder(folder: FolderEntity) {
        items.value = items.value.filterNot { it.id == folder.id } + folder
    }
    override suspend fun updateFolder(folder: FolderEntity) = upsertFolder(folder)
    override suspend fun deleteFolderById(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
    override suspend fun countFolders(): Int = items.value.size
}

private class FakeNotebookDao : NotebookDao {
    private val items = MutableStateFlow<List<NotebookEntity>>(emptyList())
    override fun observeActiveNotebooks(): Flow<List<NotebookEntity>> =
        items.map { list -> list.filter { !it.isTrashed && !it.isArchived } }
    override fun observeArchivedNotebooks(): Flow<List<NotebookEntity>> =
        items.map { list -> list.filter { !it.isTrashed && it.isArchived } }
    override fun observeTrashedNotebooks(): Flow<List<NotebookEntity>> =
        items.map { list -> list.filter { it.isTrashed } }
    override fun observePinnedNotebooks(): Flow<List<NotebookEntity>> =
        items.map { list -> list.filter { !it.isTrashed && !it.isArchived && it.isPinned } }
    override fun observeNotebookById(id: String): Flow<NotebookEntity?> =
        items.map { list -> list.find { it.id == id } }
    override suspend fun getNotebookById(id: String): NotebookEntity? = items.value.find { it.id == id }
    override suspend fun upsertNotebook(notebook: NotebookEntity) {
        items.value = items.value.filterNot { it.id == notebook.id } + notebook
    }
    override suspend fun deleteNotebookPermanently(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
    override suspend fun countNotebooks(): Int = items.value.size
}

private class FakeNotePageDao : NotePageDao {
    private val items = MutableStateFlow<List<NotePageEntity>>(emptyList())
    override fun observeAllActivePages(): Flow<List<NotePageEntity>> =
        items.map { list -> list.filter { !it.isTrashed } }
    override fun observePagesForNotebook(notebookId: String): Flow<List<NotePageEntity>> =
        items.map { list -> list.filter { it.notebookId == notebookId && !it.isTrashed } }
    override suspend fun getPagesForNotebookOnce(notebookId: String): List<NotePageEntity> =
        items.value.filter { it.notebookId == notebookId && !it.isTrashed }
    override fun observeFavoritePages(): Flow<List<NotePageEntity>> =
        items.map { list -> list.filter { !it.isTrashed && it.isFavorite } }
    override fun observeTrashedPages(): Flow<List<NotePageEntity>> =
        items.map { list -> list.filter { it.isTrashed } }
    override fun observePageById(pageId: String): Flow<NotePageEntity?> =
        items.map { list -> list.find { it.id == pageId } }
    override suspend fun getPageById(pageId: String): NotePageEntity? = items.value.find { it.id == pageId }
    override suspend fun getNextPageOrder(notebookId: String): Int =
        (items.value.filter { it.notebookId == notebookId && !it.isTrashed }.maxOfOrNull { it.pageOrder } ?: -1) + 1
    override suspend fun upsertPage(page: NotePageEntity) {
        items.value = items.value.filterNot { it.id == page.id } + page
    }
    override suspend fun upsertPages(pages: List<NotePageEntity>) {
        pages.forEach { upsertPage(it) }
    }
    override suspend fun deletePagePermanently(pageId: String) {
        items.value = items.value.filterNot { it.id == pageId }
    }
    override suspend fun emptyTrashPages() {
        items.value = items.value.filterNot { it.isTrashed }
    }
    override suspend fun clearAllAiCaches() {
        items.value = items.value.map { it.copy(aiSummaryCache = null) }
    }
    override fun searchActivePages(query: String): Flow<List<NotePageEntity>> =
        items.map { list ->
            list.filter { !it.isTrashed && (it.title.contains(query, true) || it.bodyText.contains(query, true)) }
        }
}
