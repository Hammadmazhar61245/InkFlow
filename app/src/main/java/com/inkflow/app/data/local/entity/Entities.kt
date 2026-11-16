package com.inkflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate

/**
 * Represents a Folder that can be nested inside another parent Folder (`parentFolderId`).
 */
@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentFolderId"), Index("name")]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentFolderId: String? = null,
    val colorHex: Long = 0xFF2563EB,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents a Notebook optionally organized inside a Folder.
 * Supports pinning, archiving, soft-deletion (trash), and cover customization.
 */
@Entity(
    tableName = "notebooks",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId"), Index("isPinned"), Index("isArchived"), Index("isTrashed")]
)
data class NotebookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val folderId: String? = null,
    val coverColorHex: Long = 0xFF2563EB,
    val iconName: String = "book",
    val defaultTemplate: String = PaperTemplate.RULED.name,
    val defaultPaperColor: String = PaperColor.WARM_CREAM.name,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Represents an individual Page/Note inside a Notebook.
 * Supports rich text, interactive checklists, structured ink strokes, recognized handwriting text,
 * page ordering, pinning, favorites, tags, and trash recovery.
 */
@Entity(
    tableName = "note_pages",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("notebookId"),
        Index("isPinned"),
        Index("isFavorite"),
        Index("isTrashed"),
        Index("updatedAt"),
        Index("pageOrder")
    ]
)
data class NotePageEntity(
    @PrimaryKey val id: String,
    val notebookId: String,
    val title: String,
    val bodyText: String = "",
    val checklistJson: String = "[]",
    val strokesJson: String = "[]",
    val recognizedInkText: String = "",
    val tagsCsv: String = "",
    val paperTemplate: String = PaperTemplate.RULED.name,
    val paperColor: String = PaperColor.WARM_CREAM.name,
    val pageOrder: Int = 0,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val aiSummaryCache: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
