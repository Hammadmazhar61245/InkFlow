package com.inkflow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.inkflow.app.data.local.dao.FolderDao
import com.inkflow.app.data.local.dao.NotePageDao
import com.inkflow.app.data.local.dao.NotebookDao
import com.inkflow.app.data.local.entity.FolderEntity
import com.inkflow.app.data.local.entity.NotePageEntity
import com.inkflow.app.data.local.entity.NotebookEntity
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate

@Database(
    entities = [
        FolderEntity::class,
        NotebookEntity::class,
        NotePageEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class InkFlowDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun notebookDao(): NotebookDao
    abstract fun notePageDao(): NotePageDao

    companion object {
        @Volatile
        private var INSTANCE: InkFlowDatabase? = null

        fun getInstance(context: Context): InkFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    InkFlowDatabase::class.java,
                    "inkflow_notebook.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /**
         * Ensures a single clean default notebook exists without any pre-made extra notes.
         */
        suspend fun ensureSeedData(db: InkFlowDatabase) {
            if (db.notebookDao().countNotebooks() > 0) return

            val now = System.currentTimeMillis()
            val starterNotebook = NotebookEntity(
                id = "nb-default",
                title = "My Studio Notebook",
                description = "Infinite grid canvas for handwriting, sketches & OneNote sync",
                folderId = null,
                coverColorHex = 0xFFD97706,
                defaultTemplate = PaperTemplate.GRID.name,
                defaultPaperColor = PaperColor.WARM_CREAM.name,
                isPinned = true,
                createdAt = now,
                updatedAt = now
            )
            db.notebookDao().upsertNotebook(starterNotebook)
        }
    }
}
