package com.inkflow.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inkflow.app.data.model.InkToolType
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class ActivePaletteDrawer {
    NONE,
    BRUSHES,
    COLORS,
    STROKE_TUNER
}

/**
 * Animated Floating Studio Palette Pod.
 * Replaces static bottom rows of buttons/colors with a sleek, floating animated pill
 * that springs open into dedicated Brush, Color Swatch, or Stroke Width drawers only when clicked!
 */
@Composable
fun AnimatedFloatingToolPalette(
    activeTool: InkToolType,
    activeColorArgb: Long,
    activeStrokeWidth: Float,
    activeOpacity: Float,
    selectedStrokesCount: Int,
    hasStrokesOnPage: Boolean,
    onSelectTool: (InkToolType) -> Unit,
    onSelectColor: (Long) -> Unit,
    onUpdateStrokeWidth: (Float) -> Unit,
    onUpdateStrokeOpacity: (Float) -> Unit,
    onDeleteSelectedStrokes: () -> Unit,
    onRequestClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var openDrawer by remember { mutableStateOf(ActivePaletteDrawer.NONE) }

    val studioInkColors = remember {
        listOf(
            0xFF1C1917L, // Deep Charcoal Ink
            0xFFD97706L, // Warm Amber Gold
            0xFF059669L, // Forest Sage Green
            0xFFE11D48L, // Terracotta Crimson
            0xFF7C3AEDL, // Royal Plum
            0xFFEA580CL, // Burnt Sienna Orange
            0xFF0D9488L, // Deep Teal
            0xFFFACC15L, // Warm Sun Highlighter
            0xFFEC4899L, // Coral Rose
            0xFF2563EBL, // Classic Fountain Blue
            0xFF78716CL, // Warm Graphite
            0xFFFAF7F2L  // Chalk Ivory (for dark paper)
        )
    }

    val drawerScale by animateFloatAsState(
        targetValue = if (openDrawer != ActivePaletteDrawer.NONE) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "drawerScale"
    )

    val triggerRotation by animateFloatAsState(
        targetValue = if (openDrawer != ActivePaletteDrawer.NONE) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "triggerRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Animated Popup Widget Card (Opens on specific clicks with spring physics)
        AnimatedVisibility(
            visible = openDrawer != ActivePaletteDrawer.NONE,
            enter = fadeIn() + scaleIn(
                initialScale = 0.82f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + scaleOut(targetScale = 0.85f) + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(drawerScale)
                    .shadow(16.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Drawer Header with title & close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (openDrawer) {
                                ActivePaletteDrawer.BRUSHES -> "Select Studio Instrument"
                                ActivePaletteDrawer.COLORS -> if (selectedStrokesCount > 0) {
                                    "Recolor $selectedStrokesCount Selected Strokes"
                                } else {
                                    "Ink & Pigment Wheel"
                                }
                                ActivePaletteDrawer.STROKE_TUNER -> "Nib Size & Ink Flow"
                                ActivePaletteDrawer.NONE -> ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { openDrawer = ActivePaletteDrawer.NONE },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Palette",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    when (openDrawer) {
                        ActivePaletteDrawer.BRUSHES -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(InkToolType.entries) { tool ->
                                    val isSelected = activeTool == tool
                                    val cardScale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.06f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "toolScale"
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .scale(cardScale)
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                onSelectTool(tool)
                                                openDrawer = ActivePaletteDrawer.NONE
                                            },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = toolIconFor(tool),
                                                contentDescription = tool.displayName,
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = tool.displayName,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ActivePaletteDrawer.COLORS -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                items(studioInkColors) { colorHex ->
                                    val isCurrent = activeColorArgb == colorHex
                                    val swatchScale by animateFloatAsState(
                                        targetValue = if (isCurrent) 1.22f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "swatchScale"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .scale(swatchScale)
                                            .clip(CircleShape)
                                            .background(Color(colorHex))
                                            .border(
                                                width = if (isCurrent) 3.dp else 1.dp,
                                                color = if (isCurrent) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.Gray.copy(alpha = 0.45f)
                                                },
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                onSelectColor(colorHex)
                                                openDrawer = ActivePaletteDrawer.NONE
                                            }
                                    )
                                }
                            }
                        }

                        ActivePaletteDrawer.STROKE_TUNER -> {
                            // Live Animated Wave Preview showing current nib width, opacity & color
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val wave = Path().apply {
                                        moveTo(0f, h / 2f)
                                        val steps = 30
                                        for (i in 1..steps) {
                                            val t = i.toFloat() / steps
                                            val x = t * w
                                            val y = (h / 2f) + sin(t * Math.PI * 2.0).toFloat() * (h * 0.25f)
                                            lineTo(x, y)
                                        }
                                    }
                                    drawPath(
                                        path = wave,
                                        color = Color(activeColorArgb).copy(alpha = activeOpacity),
                                        style = Stroke(width = activeStrokeWidth * 1.8f, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Nib Size: ${activeStrokeWidth.roundToInt()} pt",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Ink Opacity: ${(activeOpacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Slider(
                                value = activeStrokeWidth,
                                onValueChange = onUpdateStrokeWidth,
                                valueRange = 1.5f..32f
                            )
                            Slider(
                                value = activeOpacity,
                                onValueChange = onUpdateStrokeOpacity,
                                valueRange = 0.15f..1.0f
                            )
                        }

                        ActivePaletteDrawer.NONE -> Unit
                    }
                }
            }
        }

        // 2. Compact Floating Studio Pill Dock (Always accessible, never clutters the screen)
        Surface(
            modifier = Modifier
                .shadow(14.dp, RoundedCornerShape(36.dp)),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(36.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Active Brush Orb (Click to spring open the Brush Selector Fan)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (openDrawer == ActivePaletteDrawer.BRUSHES) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            openDrawer = if (openDrawer == ActivePaletteDrawer.BRUSHES) {
                                ActivePaletteDrawer.NONE
                            } else {
                                ActivePaletteDrawer.BRUSHES
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = toolIconFor(activeTool),
                            contentDescription = activeTool.displayName,
                            tint = if (openDrawer == ActivePaletteDrawer.BRUSHES) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (openDrawer == ActivePaletteDrawer.BRUSHES) triggerRotation * 0.1f else 0f)
                        )
                        Text(
                            text = activeTool.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (openDrawer == ActivePaletteDrawer.BRUSHES) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                // Button 2: Active Color Ring Orb (Click to spring open the Pigment Swatch Wheel)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(activeColorArgb))
                        .border(
                            width = if (openDrawer == ActivePaletteDrawer.COLORS) 3.dp else 2.dp,
                            color = if (openDrawer == ActivePaletteDrawer.COLORS) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape
                        )
                        .clickable {
                            openDrawer = if (openDrawer == ActivePaletteDrawer.COLORS) {
                                ActivePaletteDrawer.NONE
                            } else {
                                ActivePaletteDrawer.COLORS
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (openDrawer == ActivePaletteDrawer.COLORS) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Colors",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Button 3: Nib Size & Opacity Orb (Click to spring open the Live Wave Tuner)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (openDrawer == ActivePaletteDrawer.STROKE_TUNER) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            openDrawer = if (openDrawer == ActivePaletteDrawer.STROKE_TUNER) {
                                ActivePaletteDrawer.NONE
                            } else {
                                ActivePaletteDrawer.STROKE_TUNER
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Stroke Size",
                            modifier = Modifier.size(16.dp),
                            tint = if (openDrawer == ActivePaletteDrawer.STROKE_TUNER) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = "${activeStrokeWidth.roundToInt()}pt",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (openDrawer == ActivePaletteDrawer.STROKE_TUNER) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                // Quick Eraser Toggle Orb
                Surface(
                    shape = CircleShape,
                    color = if (activeTool == InkToolType.ERASER) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            onSelectTool(
                                if (activeTool == InkToolType.ERASER) InkToolType.PEN else InkToolType.ERASER
                            )
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LayersClear,
                            contentDescription = "Eraser",
                            modifier = Modifier.size(18.dp),
                            tint = if (activeTool == InkToolType.ERASER) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                // Delete Selection or Clear Canvas Action
                if (selectedStrokesCount > 0) {
                    TextButton(onClick = onDeleteSelectedStrokes) {
                        Text("Delete ($selectedStrokesCount)", color = MaterialTheme.colorScheme.error)
                    }
                } else if (hasStrokesOnPage) {
                    IconButton(
                        onClick = onRequestClearAll,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear Canvas",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun toolIconFor(tool: InkToolType) = when (tool) {
    InkToolType.PEN -> Icons.Default.Create
    InkToolType.BALLPOINT -> Icons.Default.Edit
    InkToolType.PENCIL -> Icons.Default.Gesture
    InkToolType.BRUSH -> Icons.Default.Brush
    InkToolType.MARKER -> Icons.Default.FormatColorFill
    InkToolType.HIGHLIGHTER -> Icons.Default.AutoFixNormal
    InkToolType.ERASER -> Icons.Default.LayersClear
}
