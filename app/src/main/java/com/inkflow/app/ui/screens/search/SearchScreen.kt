package com.inkflow.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inkflow.app.data.local.entity.NotePageEntity
import com.inkflow.app.data.local.entity.NotebookEntity
import com.inkflow.app.ui.screens.home.RecentNoteCard

/**
 * Search and Multi-Filter Screen across note titles, typed text, recognized handwriting,
 * checklists, notebooks, and tags.
 */
@Composable
fun SearchScreen(
    query: String,
    results: List<NotePageEntity>,
    notebooks: List<NotebookEntity>,
    selectedNotebookId: String?,
    selectedTag: String?,
    onlyFavorites: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectNotebookFilter: (String?) -> Unit,
    onSelectTagFilter: (String?) -> Unit,
    onToggleFavoritesFilter: (Boolean) -> Unit,
    onOpenPage: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val allTags = results
        .flatMap { it.tagsCsv.split(",") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Search & Filter",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Search titles, body text, handwritten ink, or #tags...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        // Filter Chips Row (Favorites + Notebooks + Tags)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = { onToggleFavoritesFilter(!onlyFavorites) },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Favorites") }
                )
            }
            items(notebooks, key = { it.id }) { nb ->
                FilterChip(
                    selected = selectedNotebookId == nb.id,
                    onClick = {
                        onSelectNotebookFilter(if (selectedNotebookId == nb.id) null else nb.id)
                    },
                    label = { Text(nb.title) }
                )
            }
            items(allTags, key = { it }) { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = {
                        onSelectTagFilter(if (selectedTag == tag) null else tag)
                    },
                    label = { Text("#$tag") }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${results.size} matching notes",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (results.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("No matching notes found", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Try clearing a filter chip or searching for a different keyword.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(results, key = { it.id }) { page ->
                    val nb = notebooks.find { it.id == page.notebookId }
                    RecentNoteCard(
                        page = page,
                        notebookTitle = nb?.title ?: "Notebook",
                        notebookColorHex = nb?.coverColorHex ?: 0xFF2563EB,
                        onClick = { onOpenPage(page.id) },
                        onTogglePin = { onTogglePin(page.id) },
                        onToggleFavorite = { onToggleFavorite(page.id) }
                    )
                }
            }
        }
    }
}
