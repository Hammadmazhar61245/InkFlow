package com.inkflow.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inkflow.app.data.model.AccentPalette
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import com.inkflow.app.data.model.ThemeMode
import com.inkflow.app.data.preferences.UserPreferences

/**
 * Settings & Customization Screen for theme mode, adjustable accent palettes,
 * default notebook paper templates/colors, stylus input preferences, and offline privacy controls.
 */
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectAccentPalette: (AccentPalette) -> Unit,
    onSelectDefaultTemplate: (PaperTemplate) -> Unit,
    onSelectDefaultPaperColor: (PaperColor) -> Unit,
    onToggleStylusOnly: (Boolean) -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onClearAiCache: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Settings & Customization", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Personalize your notebook appearance, default paper, and offline privacy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Theme & Accent Palette Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ThemeMode.entries) { mode ->
                            FilterChip(
                                selected = preferences.themeMode == mode,
                                onClick = { onSelectThemeMode(mode) },
                                label = { Text(mode.displayName) }
                            )
                        }
                    }

                    Text("Accent Color Palette", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(AccentPalette.entries) { palette ->
                            val isSelected = preferences.accentPalette == palette
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onSelectAccentPalette(palette) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(palette.lightPrimary))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = palette.displayName.substringBefore(" "),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Default Paper Style Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Default New Note Paper", style = MaterialTheme.typography.titleMedium)
                    Text("Template", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PaperTemplate.entries) { template ->
                            FilterChip(
                                selected = preferences.defaultPaperTemplate == template,
                                onClick = { onSelectDefaultTemplate(template) },
                                label = { Text(template.displayName) }
                            )
                        }
                    }

                    Text("Paper Tone", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PaperColor.entries) { color ->
                            FilterChip(
                                selected = preferences.defaultPaperColor == color,
                                onClick = { onSelectDefaultPaperColor(color) },
                                label = { Text(color.displayName) }
                            )
                        }
                    }
                }
            }
        }

        // Stylus & Autosave Behavior Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Writing & Storage", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Stylus-Only Drawing Mode", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Use stylus for ink and single-finger touch for panning",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.stylusOnlyMode,
                            onCheckedChange = onToggleStylusOnly
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Real-Time Background Autosave", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Persist edits to local Room SQLite database automatically",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.autoSaveEnabled,
                            onCheckedChange = onToggleAutoSave
                        )
                    }
                }
            }
        }

        // Offline Privacy & Data Control Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Privacy & Local Data", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "InkFlow is offline-first. Your notebooks, handwritten strokes, and checklists are stored strictly on-device in your local Room database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onClearAiCache) {
                        Text("Clear Locally Cached AI Summaries")
                    }
                }
            }
        }
    }
}
