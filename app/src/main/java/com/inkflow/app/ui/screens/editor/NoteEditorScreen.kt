package com.inkflow.app.ui.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomInMap
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkflow.app.data.model.InkStroke
import com.inkflow.app.data.model.InkToolType
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import com.inkflow.app.ui.ActiveEditorState
import com.inkflow.app.ui.SaveStatus
import com.inkflow.app.ui.components.AnimatedFloatingToolPalette
import com.inkflow.app.ui.components.EditorInputMode
import com.inkflow.app.ui.components.InteractiveInkCanvas
import kotlin.math.roundToInt

/**
 * Modernized NoteEditorScreen with:
 * - Guaranteed Finger & Stylus Drawing: When in DRAW or LASSO mode, the InteractiveInkCanvas sits ON TOP
 *   of the passive text display so text fields never steal finger/stylus touch events.
 * - Quick Cut / Copy / Paste / Duplicate / Select All shortcut bar for both Ink Strokes and Text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    state: ActiveEditorState,
    stylusOnlyMode: Boolean,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onDeleteChecklistItem: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSelectPaperTemplate: (PaperTemplate) -> Unit,
    onSelectPaperColor: (PaperColor) -> Unit,
    onToggleDistractionFree: () -> Unit,
    onToggleFullScreenCanvas: () -> Unit,
    onSetInputMode: (EditorInputMode) -> Unit,
    onSelectInkTool: (InkToolType) -> Unit,
    onSelectInkColor: (Long) -> Unit,
    onUpdateStrokeWidth: (Float) -> Unit,
    onUpdateStrokeOpacity: (Float) -> Unit,
    onCommitStroke: (InkStroke) -> Unit,
    onEraseStrokesByIds: (Set<String>) -> Unit,
    onUpdateSelectedStrokes: (Set<String>) -> Unit,
    onSelectAllStrokes: () -> Unit,
    onCutStrokes: () -> Unit,
    onCopyStrokes: () -> Unit,
    onPasteStrokes: () -> Unit,
    onDuplicateStrokes: () -> Unit,
    onMoveSelectedStrokes: (Float, Float) -> Unit,
    onDeleteSelectedStrokes: () -> Unit,
    onClearAllStrokes: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    var showPaperCustomizer by remember { mutableStateOf(false) }
    var showStrokeTuner by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }
    var newTagText by remember { mutableStateOf("") }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val isDarkPaper = state.paperColor == PaperColor.MIDNIGHT_SLATE
    val inkTextColor = if (isDarkPaper) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val mutedPaperTextColor = if (isDarkPaper) Color(0xFF94A3B8) else Color(0xFF475569)

    val inkPalette = listOf(
        0xFF4F46E5, // Electric Indigo
        0xFF1E293B, // Deep Ink Slate
        0xFF0EA5E9, // Sky Cyan
        0xFF059669, // Emerald Green
        0xFF7C3AED, // Royal Violet
        0xFFF43F5E, // Crimson Rose
        0xFFD97706, // Amber Gold
        0xFFFACC15, // Highlighter Yellow
        0xFFEC4899, // Neon Magenta
        0xFFFFFFFF  // Pure Chalk White
    )

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // Row 1: Navigation, Status, Undo/Redo, Paper & Fullscreen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            Column {
                                Text(
                                    text = state.title.ifBlank { "Untitled Canvas" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (state.saveStatus == SaveStatus.SAVED) {
                                            Icons.Default.CloudDone
                                        } else {
                                            Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${state.strokes.size} strokes • ${if (state.saveStatus == SaveStatus.SAVING) "Saving..." else "Saved"}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onUndo, enabled = state.canUndo) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                            }
                            IconButton(onClick = onRedo, enabled = state.canRedo) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                            }
                            IconButton(onClick = { showPaperCustomizer = !showPaperCustomizer }) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Paper & Grid Style",
                                    tint = if (showPaperCustomizer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onTogglePin) {
                                Icon(
                                    imageVector = if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "Pin",
                                    tint = if (state.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onToggleFavorite) {
                                Icon(
                                    imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (state.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onToggleFullScreenCanvas) {
                                Icon(
                                    imageVector = if (state.isFullScreenCanvas) {
                                        Icons.Default.FullscreenExit
                                    } else {
                                        Icons.Default.Fullscreen
                                    },
                                    contentDescription = "Full-Screen Canvas",
                                    tint = if (state.isFullScreenCanvas) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Row 2: Mode Switcher + Cut / Copy / Paste / Duplicate Quick Action Bar
                    AnimatedVisibility(visible = !state.isDistractionFree) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(EditorInputMode.entries) { mode ->
                                FilterChip(
                                    selected = state.inputMode == mode,
                                    onClick = { onSetInputMode(mode) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = when (mode) {
                                                EditorInputMode.DRAW -> Icons.Default.Draw
                                                EditorInputMode.LASSO -> Icons.Default.SelectAll
                                                EditorInputMode.TYPE -> Icons.Default.Keyboard
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(mode.label) }
                                )
                            }

                            // Cut / Copy / Paste / Duplicate Shortcuts
                            item {
                                AssistChip(
                                    onClick = {
                                        if (state.inputMode == EditorInputMode.TYPE) {
                                            clipboardManager.setText(AnnotatedString(state.bodyText))
                                            onBodyChange("")
                                        } else {
                                            onCutStrokes()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCut, contentDescription = "Cut", modifier = Modifier.size(15.dp))
                                    },
                                    label = { Text("Cut") }
                                )
                            }

                            item {
                                AssistChip(
                                    onClick = {
                                        if (state.inputMode == EditorInputMode.TYPE) {
                                            clipboardManager.setText(AnnotatedString(state.bodyText))
                                        } else {
                                            onCopyStrokes()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(15.dp))
                                    },
                                    label = { Text("Copy") }
                                )
                            }

                            item {
                                AssistChip(
                                    onClick = {
                                        if (state.inputMode == EditorInputMode.TYPE) {
                                            val clip = clipboardManager.getText()?.text ?: ""
                                            if (clip.isNotEmpty()) {
                                                onBodyChange(
                                                    if (state.bodyText.isEmpty()) clip else "${state.bodyText}\n$clip"
                                                )
                                            }
                                        } else {
                                            onPasteStrokes()
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(15.dp))
                                    },
                                    label = {
                                        Text(
                                            if (state.inputMode != EditorInputMode.TYPE && state.clipboardStrokesCount > 0) {
                                                "Paste (${state.clipboardStrokesCount})"
                                            } else {
                                                "Paste"
                                            }
                                        )
                                    }
                                )
                            }

                            if (state.inputMode != EditorInputMode.TYPE && state.strokes.isNotEmpty()) {
                                item {
                                    AssistChip(
                                        onClick = onDuplicateStrokes,
                                        leadingIcon = {
                                            Icon(Icons.Default.FileCopy, contentDescription = "Duplicate", modifier = Modifier.size(15.dp))
                                        },
                                        label = { Text("Duplicate") }
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = onSelectAllStrokes,
                                        leadingIcon = {
                                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", modifier = Modifier.size(15.dp))
                                        },
                                        label = { Text("Select All") }
                                    )
                                }
                            }

                            // Infinite Grid Zoom Controls (- / % / +) so user can zoom out with 1 tap or 2-finger pinch!
                            item {
                                AssistChip(
                                    onClick = {
                                        zoomScale = (zoomScale * 0.72f).coerceIn(0.12f, 6.0f)
                                    },
                                    label = { Text("Zoom −") }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.ZoomInMap,
                                            contentDescription = "Reset Zoom",
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    label = { Text("${(zoomScale * 100).roundToInt()}%") }
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        zoomScale = (zoomScale * 1.35f).coerceIn(0.12f, 6.0f)
                                    },
                                    label = { Text("Zoom +") }
                                )
                            }
                        }
                    }

                    // Collapsible Paper & Background Selector Bar
                    AnimatedVisibility(visible = showPaperCustomizer && !state.isDistractionFree) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Infinite Paper Grid & Template", style = MaterialTheme.typography.labelLarge)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PaperTemplate.entries) { template ->
                                    FilterChip(
                                        selected = state.paperTemplate == template,
                                        onClick = { onSelectPaperTemplate(template) },
                                        label = { Text(template.displayName) }
                                    )
                                }
                            }
                            Text("Paper Color", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(PaperColor.entries) { pColor ->
                                    val selected = state.paperColor == pColor
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(pColor.hexColor))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clickable { onSelectPaperColor(pColor) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = pColor.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (pColor == PaperColor.MIDNIGHT_SLATE) Color.White else Color(0xFF1C1917)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.inputMode != EditorInputMode.TYPE) {
                // =========================================================================
                // DRAW & LASSO MODE: InteractiveInkCanvas is the primary touch receptor!
                // Any existing typed text is rendered passively underneath the ink strokes
                // so BasicTextField NEVER steals finger or stylus touch events.
                // =========================================================================
                InteractiveInkCanvas(
                    paperTemplate = state.paperTemplate,
                    paperColor = state.paperColor,
                    strokes = state.strokes,
                    inputMode = state.inputMode,
                    activeTool = state.activeTool,
                    activeColorArgb = state.activeColorArgb,
                    activeStrokeWidth = state.activeStrokeWidth,
                    activeOpacity = state.activeOpacity,
                    stylusOnlyMode = stylusOnlyMode,
                    selectedStrokeIds = state.selectedStrokeIds,
                    zoomScale = zoomScale,
                    panOffset = panOffset,
                    onZoomPanChange = { newScale, newPan ->
                        zoomScale = newScale
                        panOffset = newPan
                    },
                    onCommitStroke = onCommitStroke,
                    onEraseStrokesByIds = onEraseStrokesByIds,
                    onUpdateSelectedStrokes = onUpdateSelectedStrokes,
                    onMoveSelectedStrokes = onMoveSelectedStrokes,
                    modifier = Modifier.fillMaxSize()
                )

                // Non-interactive text preview overlay (does not consume pointer input)
                if (!state.isFullScreenCanvas && (state.bodyText.isNotBlank() || state.checklist.isNotEmpty())) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (state.paperTemplate == PaperTemplate.RULED) 52.dp else 22.dp,
                                end = 22.dp,
                                top = 16.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.bodyText.isNotBlank()) {
                            Text(
                                text = state.bodyText,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    color = inkTextColor.copy(alpha = 0.78f)
                                )
                            )
                        }
                    }
                }
            } else {
                // =========================================================================
                // TYPE MODE: InteractiveInkCanvas renders paper + strokes underneath,
                // while editable text fields & checklists sit on top for keyboard input.
                // =========================================================================
                InteractiveInkCanvas(
                    paperTemplate = state.paperTemplate,
                    paperColor = state.paperColor,
                    strokes = state.strokes,
                    inputMode = EditorInputMode.TYPE,
                    activeTool = state.activeTool,
                    activeColorArgb = state.activeColorArgb,
                    activeStrokeWidth = state.activeStrokeWidth,
                    activeOpacity = state.activeOpacity,
                    stylusOnlyMode = stylusOnlyMode,
                    selectedStrokeIds = emptySet(),
                    zoomScale = zoomScale,
                    panOffset = panOffset,
                    onZoomPanChange = { _, _ -> },
                    onCommitStroke = {},
                    onEraseStrokesByIds = {},
                    onUpdateSelectedStrokes = {},
                    onMoveSelectedStrokes = { _, _ -> },
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(
                            start = if (state.paperTemplate == PaperTemplate.RULED) 52.dp else 22.dp,
                            end = 22.dp,
                            top = 16.dp,
                            bottom = 48.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BasicTextField(
                        value = state.title,
                        onValueChange = onTitleChange,
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkTextColor
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            if (state.title.isEmpty()) {
                                Text(
                                    text = "Page Title...",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mutedPaperTextColor.copy(alpha = 0.55f)
                                )
                            }
                            innerTextField()
                        }
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.tags.forEach { tag ->
                            AssistChip(
                                onClick = { onRemoveTag(tag) },
                                label = { Text("#$tag") },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove tag",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            placeholder = { Text("Add #tag...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                onAddTag(newTagText)
                                newTagText = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Tag")
                        }
                    }

                    HorizontalDivider(color = mutedPaperTextColor.copy(alpha = 0.2f))

                    BasicTextField(
                        value = state.bodyText,
                        onValueChange = onBodyChange,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = inkTextColor
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        decorationBox = { innerTextField ->
                            if (state.bodyText.isEmpty()) {
                                Text(
                                    text = "Type notes here, or tap 'Draw' above to sketch with your finger or stylus...",
                                    fontSize = 16.sp,
                                    lineHeight = 26.sp,
                                    color = mutedPaperTextColor.copy(alpha = 0.55f)
                                )
                            }
                            innerTextField()
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Checklist (${state.checklist.count { it.isChecked }}/${state.checklist.size})",
                                style = MaterialTheme.typography.titleMedium
                            )

                            state.checklist.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { onToggleChecklistItem(item.id) }
                                    )
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textDecoration = if (item.isChecked) {
                                                TextDecoration.LineThrough
                                            } else {
                                                TextDecoration.None
                                            }
                                        ),
                                        color = if (item.isChecked) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onDeleteChecklistItem(item.id) }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete checklist item",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newChecklistText,
                                    onValueChange = { newChecklistText = it },
                                    placeholder = { Text("Add checklist task...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        onAddChecklistItem(newChecklistText)
                                        newChecklistText = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Checklist Item")
                                }
                            }
                        }
                    }
                }
            }

            // Animated Floating Studio Palette Pod (opens Brushes, Colors, or Nib Tuner on specific clicks)
            if (state.inputMode != EditorInputMode.TYPE) {
                AnimatedFloatingToolPalette(
                    activeTool = state.activeTool,
                    activeColorArgb = state.activeColorArgb,
                    activeStrokeWidth = state.activeStrokeWidth,
                    activeOpacity = state.activeOpacity,
                    selectedStrokesCount = state.selectedStrokeIds.size,
                    hasStrokesOnPage = state.strokes.isNotEmpty(),
                    onSelectTool = onSelectInkTool,
                    onSelectColor = onSelectInkColor,
                    onUpdateStrokeWidth = onUpdateStrokeWidth,
                    onUpdateStrokeOpacity = onUpdateStrokeOpacity,
                    onDeleteSelectedStrokes = onDeleteSelectedStrokes,
                    onRequestClearAll = { showClearConfirmDialog = true },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Ink on Page?") },
            text = {
                Text("This will remove all ${state.strokes.size} handwritten strokes from the current page. You can still Undo this action immediately.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllStrokes()
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("Clear Ink", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
