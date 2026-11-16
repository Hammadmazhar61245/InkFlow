package com.inkflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.inkflow.app.data.local.entity.FolderEntity
import com.inkflow.app.data.local.entity.NotePageEntity
import com.inkflow.app.data.local.entity.NotebookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId IS :parentId ORDER BY name ASC")
    fun observeFoldersByParent(parentId: String?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun countFolders(): Int
}

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks WHERE isTrashed = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActiveNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE isTrashed = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashedNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE isTrashed = 0 AND isArchived = 0 AND isPinned = 1 ORDER BY updatedAt DESC")
    fun observePinnedNotebooks(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE id = :id LIMIT 1")
    fun observeNotebookById(id: String): Flow<NotebookEntity?>

    @Query("SELECT * FROM notebooks WHERE id = :id LIMIT 1")
    suspend fun getNotebookById(id: String): NotebookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotebook(notebook: NotebookEntity)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteNotebookPermanently(id: String)

    @Query("SELECT COUNT(*) FROM notebooks")
    suspend fun countNotebooks(): Int
}

@Dao
interface NotePageDao {
    @Query("SELECT * FROM note_pages WHERE isTrashed = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAllActivePages(): Flow<List<NotePageEntity>>

    @Query("SELECT * FROM note_pages WHERE notebookId = :notebookId AND isTrashed = 0 ORDER BY pageOrder ASC, updatedAt DESC")
    fun observePagesForNotebook(notebookId: String): Flow<List<NotePageEntity>>

    @Query("SELECT * FROM note_pages WHERE notebookId = :notebookId AND isTrashed = 0 ORDER BY pageOrder ASC")
    suspend fun getPagesForNotebookOnce(notebookId: String): List<NotePageEntity>

    @Query("SELECT * FROM note_pages WHERE isTrashed = 0 AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavoritePages(): Flow<List<NotePageEntity>>

    @Query("SELECT * FROM note_pages WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashedPages(): Flow<List<NotePageEntity>>

    @Query("SELECT * FROM note_pages WHERE id = :pageId LIMIT 1")
    fun observePageById(pageId: String): Flow<NotePageEntity?>

    @Query("SELECT * FROM note_pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: String): NotePageEntity?

    @Query("SELECT COALESCE(MAX(pageOrder), -1) + 1 FROM note_pages WHERE notebookId = :notebookId AND isTrashed = 0")
    suspend fun getNextPageOrder(notebookId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPage(page: NotePageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPages(pages: List<NotePageEntity>)

    @Query("DELETE FROM note_pages WHERE id = :pageId")
    suspend fun deletePagePermanently(pageId: String)

    @Query("DELETE FROM note_pages WHERE isTrashed = 1")
    suspend fun emptyTrashPages()

    @Query("UPDATE note_pages SET aiSummaryCache = NULL")
    suspend fun clearAllAiCaches()

    @Query(
        """
        SELECT * FROM note_pages
        WHERE isTrashed = 0
          AND (
            title LIKE '%' || :query || '%'
            OR bodyText LIKE '%' || :query || '%'
            OR recognizedInkText LIKE '%' || :query || '%'
            OR tagsCsv LIKE '%' || :query || '%'
            OR checklistJson LIKE '%' || :query || '%'
          )
        ORDER BY isPinned DESC, updatedAt DESC
        """
    )
    fun searchActivePages(query: String): Flow<List<NotePageEntity>>
}
