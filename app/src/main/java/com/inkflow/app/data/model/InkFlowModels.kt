package com.inkflow.app.data.model

import kotlinx.serialization.Serializable

/**
 * Configurable paper templates supported by InkFlow pages.
 */
@Serializable
enum class PaperTemplate(val displayName: String) {
    BLANK("Blank"),
    RULED("Ruled Lines"),
    GRID("Square Grid"),
    DOTTED("Dot Matrix"),
    CORNELL("Cornell Study")
}

/**
 * Customizable paper background colors with carefully tuned hex values for comfortable reading and writing.
 */
@Serializable
enum class PaperColor(val displayName: String, val hexColor: Long, val darkLineHex: Long) {
    WARM_CREAM("Warm Cream", 0xFFFDFBF7, 0xFFE2DDD2),
    PURE_WHITE("Pure White", 0xFFFFFFFF, 0xFFE2E8F0),
    SOFT_SAGE("Soft Sage", 0xFFF4F9F5, 0xFFD4E4D8),
    PASTEL_SKY("Pastel Sky", 0xFFF2F8FD, 0xFFD2E3F3),
    VINTAGE_SEPIA("Vintage Sepia", 0xFFF8F1E5, 0xFFDFD2BC),
    MIDNIGHT_SLATE("Obsidian Charcoal", 0xFF181615, 0xFF2E2A27)
}

/**
 * App-wide theme mode preference.
 */
@Serializable
enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Paper"),
    DARK("Dark Ink")
}

/**
 * Adjustable accent color palettes for visual customization.
 */
@Serializable
enum class AccentPalette(
    val displayName: String,
    val lightPrimary: Long,
    val lightSecondary: Long,
    val darkPrimary: Long,
    val darkSecondary: Long
) {
    AMBER_QUILL("Amber Espresso", 0xFFD97706, 0xFFF59E0B, 0xFFF59E0B, 0xFFFBBF24),
    EMERALD_STUDY("Sage Matcha", 0xFF059669, 0xFF10B981, 0xFF10B981, 0xFF34D399),
    CRIMSON_SEAL("Terracotta Rose", 0xFFE11D48, 0xFFF43F5E, 0xFFF43F5E, 0xFFFB7185),
    GRAPHITE_MONO("Warm Graphite", 0xFF44403C, 0xFF78716C, 0xFFD6D3D1, 0xFFA8A29E),
    AMETHYST_FLOW("Plum Velvet", 0xFF9333EA, 0xFFA855F7, 0xFFC084FC, 0xFFD8B4FE),
    COBALT_INK("Classic Ink", 0xFF2563EB, 0xFF0EA5E9, 0xFF60A5FA, 0xFF38BDF8)
}

/**
 * Interactive checklist item stored inside a note page.
 */
@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false
)

/**
 * Structured Ink Stroke model (ready for Phase 2 stylus/finger vector engine & Phase 1 persistence).
 */
@Serializable
data class InkPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val tilt: Float = 0f,
    val timestampDeltaMs: Long = 0L
)

@Serializable
enum class InkToolType(val displayName: String) {
    PEN("Fountain Pen"),
    BALLPOINT("Ballpoint"),
    PENCIL("Pencil"),
    BRUSH("Brush Pen"),
    MARKER("Marker"),
    HIGHLIGHTER("Highlighter"),
    ERASER("Eraser")
}

@Serializable
data class InkStroke(
    val id: String,
    val tool: InkToolType = InkToolType.PEN,
    val colorArgb: Long = 0xFF1E293B,
    val strokeWidth: Float = 4f,
    val opacity: Float = 1f,
    val points: List<InkPoint> = emptyList()
)

/**
 * Immutable snapshot of a note's editable content used by the Undo/Redo stack in the Note Editor.
 */
data class NoteEditSnapshot(
    val title: String,
    val bodyText: String,
    val checklist: List<ChecklistItem>,
    val tags: List<String>,
    val paperTemplate: PaperTemplate,
    val paperColor: PaperColor,
    val strokes: List<InkStroke>
)
