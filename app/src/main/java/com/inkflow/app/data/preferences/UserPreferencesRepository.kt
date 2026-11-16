package com.inkflow.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.inkflow.app.data.model.AccentPalette
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import com.inkflow.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "inkflow_settings")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.AMBER_QUILL,
    val defaultPaperTemplate: PaperTemplate = PaperTemplate.GRID,
    val defaultPaperColor: PaperColor = PaperColor.WARM_CREAM,
    val stylusOnlyMode: Boolean = false,
    val showLineNumbersInCornell: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val aiEndpointUrl: String = "",
    val oneNoteConnectedEmail: String = "",
    val oneNoteClientId: String = "",
    val oneNoteLastSyncedAt: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val DEFAULT_TEMPLATE = stringPreferencesKey("default_template")
        val DEFAULT_PAPER_COLOR = stringPreferencesKey("default_paper_color")
        val STYLUS_ONLY = booleanPreferencesKey("stylus_only_mode")
        val CORNELL_GUIDES = booleanPreferencesKey("cornell_guides")
        val AUTOSAVE_ENABLED = booleanPreferencesKey("autosave_enabled")
        val AI_ENDPOINT_URL = stringPreferencesKey("ai_endpoint_url")
        val ONENOTE_EMAIL = stringPreferencesKey("onenote_email")
        val ONENOTE_CLIENT_ID = stringPreferencesKey("onenote_client_id")
        val ONENOTE_LAST_SYNC = androidx.datastore.preferences.core.longPreferencesKey("onenote_last_sync")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            UserPreferences(
                themeMode = runCatching {
                    ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
                }.getOrDefault(ThemeMode.SYSTEM),
                accentPalette = runCatching {
                    AccentPalette.valueOf(prefs[Keys.ACCENT_PALETTE] ?: AccentPalette.AMBER_QUILL.name)
                }.getOrDefault(AccentPalette.AMBER_QUILL),
                defaultPaperTemplate = runCatching {
                    PaperTemplate.valueOf(prefs[Keys.DEFAULT_TEMPLATE] ?: PaperTemplate.GRID.name)
                }.getOrDefault(PaperTemplate.GRID),
                defaultPaperColor = runCatching {
                    PaperColor.valueOf(prefs[Keys.DEFAULT_PAPER_COLOR] ?: PaperColor.WARM_CREAM.name)
                }.getOrDefault(PaperColor.WARM_CREAM),
                stylusOnlyMode = prefs[Keys.STYLUS_ONLY] ?: false,
                showLineNumbersInCornell = prefs[Keys.CORNELL_GUIDES] ?: true,
                autoSaveEnabled = prefs[Keys.AUTOSAVE_ENABLED] ?: true,
                aiEndpointUrl = prefs[Keys.AI_ENDPOINT_URL] ?: "",
                oneNoteConnectedEmail = prefs[Keys.ONENOTE_EMAIL] ?: "",
                oneNoteClientId = prefs[Keys.ONENOTE_CLIENT_ID] ?: "",
                oneNoteLastSyncedAt = prefs[Keys.ONENOTE_LAST_SYNC] ?: 0L
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentPalette(palette: AccentPalette) {
        context.dataStore.edit { it[Keys.ACCENT_PALETTE] = palette.name }
    }

    suspend fun setDefaultPaperTemplate(template: PaperTemplate) {
        context.dataStore.edit { it[Keys.DEFAULT_TEMPLATE] = template.name }
    }

    suspend fun setDefaultPaperColor(color: PaperColor) {
        context.dataStore.edit { it[Keys.DEFAULT_PAPER_COLOR] = color.name }
    }

    suspend fun setStylusOnlyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STYLUS_ONLY] = enabled }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOSAVE_ENABLED] = enabled }
    }

    suspend fun setAiEndpointUrl(url: String) {
        context.dataStore.edit { it[Keys.AI_ENDPOINT_URL] = url.trim() }
    }

    suspend fun setOneNoteConnection(email: String, clientId: String = "", syncedAt: Long = System.currentTimeMillis()) {
        context.dataStore.edit {
            it[Keys.ONENOTE_EMAIL] = email.trim()
            if (clientId.isNotBlank()) it[Keys.ONENOTE_CLIENT_ID] = clientId.trim()
            it[Keys.ONENOTE_LAST_SYNC] = syncedAt
        }
    }

    suspend fun disconnectOneNote() {
        context.dataStore.edit {
            it[Keys.ONENOTE_EMAIL] = ""
            it[Keys.ONENOTE_LAST_SYNC] = 0L
        }
    }
}
