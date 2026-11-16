package com.inkflow.app

import android.app.Application
import com.inkflow.app.data.local.InkFlowDatabase
import com.inkflow.app.data.preferences.UserPreferencesRepository
import com.inkflow.app.data.repository.InkFlowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class InkFlowApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: InkFlowDatabase by lazy { InkFlowDatabase.getInstance(this) }
    val repository: InkFlowRepository by lazy {
        InkFlowRepository(
            folderDao = database.folderDao(),
            notebookDao = database.notebookDao(),
            notePageDao = database.notePageDao()
        )
    }
    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            InkFlowDatabase.ensureSeedData(database)
        }
    }
}
