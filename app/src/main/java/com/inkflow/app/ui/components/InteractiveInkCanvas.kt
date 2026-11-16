package com.inkflow.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inkflow.app.data.model.InkPoint
import com.inkflow.app.data.model.InkStroke
import com.inkflow.app.data.model.InkToolType
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Editor interaction mode so the user can seamlessly switch between drawing with finger/stylus,
 * lasso-selecting strokes to cut/copy/move/recolor them, and typing text.
 */
enum class EditorInputMode(val label: String) {
    DRAW("Draw"),
    LASSO("Lasso"),
    TYPE("Type")
}

/**
 * In-app custom vector brand badge for InkFlow — Warm Espresso & Amber Gold Nib aesthetic (Zero AI blue).
 */
@Composable
fun InkFlowBrandIcon(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF292524), Color(0xFFD97706))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val wavePath = Path().apply {
                moveTo(w * 0.18f, h * 0.68f)
                cubicTo(w * 0.36f, h * 0.26f, w * 0.56f, h * 0.82f, w * 0.80f, h * 0.32f)
            }
            drawPath(
                path = wavePath,
                color = Color(0xFFFAF7F2),
                style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = w * 0.12f,
                center = Offset(w * 0.80f, h * 0.30f)
            )
        }
    }
}

/**
 * OneNote-Style Infinite Expandable Grid & Vector Ink Canvas.
 * - Supports unbounded zooming out (down to 12% macro overview) and zooming in (up to 600% precision detail).
 * - Dynamically computes world-space viewport bounds `(worldLeft, worldTop, worldRight, worldBottom)`
 *   so the Grid, Ruled, Dotted, or Cornell paper expands infinitely in ALL directions as you zoom out or pan!
 * - Uses adaptive LOD (Level-of-Detail) grid subdivisions so zooming way out never lags or turns into solid lines.
 */
@Composable
fun InteractiveInkCanvas(
    paperTemplate: PaperTemplate,
    paperColor: PaperColor,
    strokes: List<InkStroke>,
    inputMode: EditorInputMode,
    activeTool: InkToolType,
    activeColorArgb: Long,
    activeStrokeWidth: Float,
    activeOpacity: Float,
    stylusOnlyMode: Boolean,
    selectedStrokeIds: Set<String>,
    zoomScale: Float,
    panOffset: Offset,
    onZoomPanChange: (Float, Offset) -> Unit,
    onCommitStroke: (InkStroke) -> Unit,
    onEraseStrokesByIds: (Set<String>) -> Unit,
    onUpdateSelectedStrokes: (Set<String>) -> Unit,
    onMoveSelectedStrokes: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStrokes by rememberUpdatedState(strokes)
    val currentTool by rememberUpdatedState(activeTool)
    val currentColor by rememberUpdatedState(activeColorArgb)
    val currentWidth by rememberUpdatedState(activeStrokeWidth)
    val currentOpacity by rememberUpdatedState(activeOpacity)
    val currentInputMode by rememberUpdatedState(inputMode)
    val currentStylusOnly by rememberUpdatedState(stylusOnlyMode)
    val currentSelectedIds by rememberUpdatedState(selectedStrokeIds)
    val currentScale by rememberUpdatedState(zoomScale)
    val currentPan by rememberUpdatedState(panOffset)

    val activePoints = remember { mutableStateListOf<InkPoint>() }
    val lassoPoints = remember { mutableStateListOf<Offset>() }
    var eraserCursor by remember { mutableStateOf<Offset?>(null) }

    fun screenToCanvas(screenPt: Offset): Offset = Offset(
        x = (screenPt.x - currentPan.x) / currentScale,
        y = (screenPt.y - currentPan.y) / currentScale
    )

    val drawingModifier = if (inputMode != EditorInputMode.TYPE) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                firstDown.consume()

                val isStylus = firstDown.type == PointerType.Stylus || firstDown.type == PointerType.Eraser
                val isHardwareEraser = firstDown.type == PointerType.Eraser

                // If Stylus-Only mode is enabled and user touches with a finger, pan with 1 finger
                val oneFingerPanMode = currentStylusOnly && !isStylus

                var isMultiTouch = false
                var isDraggingLassoSelection = false
                var lastDragCanvasPt = screenToCanvas(firstDown.position)
                val startTime = firstDown.uptimeMillis

                if (!oneFingerPanMode) {
                    val startCanvasPt = lastDragCanvasPt
                    when {
                        currentInputMode == EditorInputMode.LASSO -> {
                            val bounds = computeSelectionBounds(currentStrokes, currentSelectedIds)
                            if (bounds != null && bounds.inflate(34f / currentScale).contains(startCanvasPt)) {
                                isDraggingLassoSelection = true
                            } else {
                                isDraggingLassoSelection = false
                                onUpdateSelectedStrokes(emptySet())
                                lassoPoints.clear()
                                lassoPoints.add(startCanvasPt)
                            }
                        }

                        currentTool == InkToolType.ERASER || isHardwareEraser -> {
                            eraserCursor = startCanvasPt
                            val hitIds = findStrokesNearPoint(
                                strokes = currentStrokes,
                                point = startCanvasPt,
                                radius = max(24f / currentScale, currentWidth * 2.4f)
                            )
                            if (hitIds.isNotEmpty()) {
                                onEraseStrokesByIds(hitIds)
                            }
                        }

                        else -> {
                            activePoints.clear()
                            activePoints.add(
                                InkPoint(
                                    x = startCanvasPt.x,
                                    y = startCanvasPt.y,
                                    pressure = firstDown.pressure.coerceIn(0.2f, 1.4f),
                                    tilt = 0f,
                                    timestampDeltaMs = 0L
                                )
                            )
                        }
                    }
                }

                // Continuous pointer tracking loop until all fingers/stylus lift
                do {
                    val event = awaitPointerEvent()
                    val pressedChanges = event.changes.filter { it.pressed }

                    if (pressedChanges.size >= 2) {
                        // Switch cleanly to 2-finger pinch-to-zoom & pan around the pinch centroid!
                        if (!isMultiTouch) {
                            isMultiTouch = true
                            activePoints.clear()
                            lassoPoints.clear()
                            eraserCursor = null
                        }
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)
                        if (zoomChange != 1f || panChange != Offset.Zero) {
                            // Allow zooming out down to 0.12f (12% OneNote Infinite Grid) and zooming in to 6.0f (600%)
                            val newScale = (currentScale * zoomChange).coerceIn(0.12f, 6.0f)
                            val effectiveZoom = newScale / currentScale
                            val newPan = if (centroid != Offset.Unspecified) {
                                centroid - (centroid - currentPan) * effectiveZoom + panChange
                            } else {
                                currentPan + panChange
                            }
                            onZoomPanChange(newScale, newPan)
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } else if (pressedChanges.size == 1 && !isMultiTouch) {
                        val change = pressedChanges.first()
                        if (oneFingerPanMode) {
                            val delta = change.position - change.previousPosition
                            if (delta != Offset.Zero) {
                                onZoomPanChange(currentScale, currentPan + delta)
                            }
                            change.consume()
                        } else {
                            val canvasPt = screenToCanvas(change.position)
                            val minMoveThreshold = 1.0f / currentScale.coerceAtLeast(0.2f)
                            when {
                                currentInputMode == EditorInputMode.LASSO -> {
                                    if (isDraggingLassoSelection) {
                                        val dx = canvasPt.x - lastDragCanvasPt.x
                                        val dy = canvasPt.y - lastDragCanvasPt.y
                                        if (hypot(dx, dy) >= minMoveThreshold) {
                                            onMoveSelectedStrokes(dx, dy)
                                            lastDragCanvasPt = canvasPt
                                        }
                                    } else {
                                        lassoPoints.add(canvasPt)
                                    }
                                }

                                currentTool == InkToolType.ERASER || isHardwareEraser -> {
                                    eraserCursor = canvasPt
                                    val hitIds = findStrokesNearPoint(
                                        strokes = currentStrokes,
                                        point = canvasPt,
                                        radius = max(24f / currentScale, currentWidth * 2.4f)
                                    )
                                    if (hitIds.isNotEmpty()) {
                                        onEraseStrokesByIds(hitIds)
                                    }
                                }

                                else -> {
                                    // Read historical sub-frame points for silky smooth handwriting curves
                                    change.historical.forEach { hist ->
                                        val hCanvas = screenToCanvas(hist.position)
                                        val last = activePoints.lastOrNull()
                                        if (last == null || hypot(hCanvas.x - last.x, hCanvas.y - last.y) >= minMoveThreshold) {
                                            activePoints.add(
                                                InkPoint(
                                                    x = hCanvas.x,
                                                    y = hCanvas.y,
                                                    pressure = change.pressure.coerceIn(0.2f, 1.4f),
                                                    tilt = 0f,
                                                    timestampDeltaMs = (hist.uptimeMillis - startTime).coerceAtLeast(0L)
                                                )
                                            )
                                        }
                                    }

                                    val last = activePoints.lastOrNull()
                                    if (last == null || hypot(canvasPt.x - last.x, canvasPt.y - last.y) >= minMoveThreshold) {
                                        activePoints.add(
                                            InkPoint(
                                                x = canvasPt.x,
                                                y = canvasPt.y,
                                                pressure = change.pressure.coerceIn(0.2f, 1.4f),
                                                tilt = 0f,
                                                timestampDeltaMs = (change.uptimeMillis - startTime).coerceAtLeast(0L)
                                            )
                                        )
                                    }
                                }
                            }
                            change.consume()
                        }
                    }
                } while (event.changes.any { it.pressed })

                // Pointer Up: finalize stroke or lasso selection
                eraserCursor = null
                if (!isMultiTouch && !oneFingerPanMode) {
                    if (currentInputMode == EditorInputMode.LASSO) {
                        if (!isDraggingLassoSelection && lassoPoints.size >= 3) {
                            val selected = findStrokesInsideLasso(currentStrokes, lassoPoints)
                            onUpdateSelectedStrokes(selected)
                        }
                        lassoPoints.clear()
                    } else if (currentTool != InkToolType.ERASER && !isHardwareEraser && activePoints.isNotEmpty()) {
                        val resolvedOpacity = when (currentTool) {
                            InkToolType.HIGHLIGHTER -> min(currentOpacity, 0.38f)
                            InkToolType.MARKER -> min(currentOpacity, 0.82f)
                            InkToolType.PENCIL -> min(currentOpacity, 0.78f)
                            else -> currentOpacity
                        }
                        val committed = InkStroke(
                            id = "stroke-${UUID.randomUUID()}",
                            tool = currentTool,
                            colorArgb = currentColor,
                            strokeWidth = currentWidth,
                            opacity = resolvedOpacity,
                            points = activePoints.toList()
                        )
                        activePoints.clear()
                        onCommitStroke(committed)
                    }
                } else {
                    activePoints.clear()
                    lassoPoints.clear()
                }
            }
        }
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .then(drawingModifier)
        ) {
            // Fill the entire screen viewport with the paper color first so zooming out NEVER leaves blank edges
            val bgColor = Color(paperColor.hexColor)
            drawRect(color = bgColor)

            // Compute exact visible world-space bounds from current zoomScale & panOffset
            val safeScale = zoomScale.coerceAtLeast(0.08f)
            val worldLeft = -panOffset.x / safeScale
            val worldTop = -panOffset.y / safeScale
            val worldRight = (size.width - panOffset.x) / safeScale
            val worldBottom = (size.height - panOffset.y) / safeScale

            withTransform({
                translate(left = panOffset.x, top = panOffset.y)
                scale(scaleX = safeScale, scaleY = safeScale, pivot = Offset.Zero)
            }) {
                // 1. Infinite Expandable OneNote-Style Paper Grid / Lines across the entire visible world rect
                drawInfinitePaperGrid(
                    paperTemplate = paperTemplate,
                    paperColor = paperColor,
                    worldLeft = worldLeft,
                    worldTop = worldTop,
                    worldRight = worldRight,
                    worldBottom = worldBottom,
                    zoomScale = safeScale
                )

                // 2. Saved Vector Ink Strokes
                val highlighters = strokes.filter { it.tool == InkToolType.HIGHLIGHTER }
                val foregroundStrokes = strokes.filter { it.tool != InkToolType.HIGHLIGHTER }

                highlighters.forEach { stroke ->
                    drawRichInkStroke(stroke, isSelected = selectedStrokeIds.contains(stroke.id))
                }
                foregroundStrokes.forEach { stroke ->
                    drawRichInkStroke(stroke, isSelected = selectedStrokeIds.contains(stroke.id))
                }

                // 3. Active In-Progress Stroke (Real-time zero-lag feedback)
                if (activePoints.isNotEmpty()) {
                    val previewOpacity = when (activeTool) {
                        InkToolType.HIGHLIGHTER -> min(activeOpacity, 0.38f)
                        InkToolType.MARKER -> min(activeOpacity, 0.82f)
                        InkToolType.PENCIL -> min(activeOpacity, 0.78f)
                        else -> activeOpacity
                    }
                    drawRichInkStroke(
                        stroke = InkStroke(
                            id = "preview",
                            tool = activeTool,
                            colorArgb = activeColorArgb,
                            strokeWidth = activeStrokeWidth,
                            opacity = previewOpacity,
                            points = activePoints
                        ),
                        isSelected = false
                    )
                }

                // 4. Lasso Selection Loop & Bounding Box
                if (lassoPoints.size >= 2) {
                    val lassoPath = Path().apply {
                        moveTo(lassoPoints.first().x, lassoPoints.first().y)
                        for (i in 1 until lassoPoints.size) {
                            lineTo(lassoPoints[i].x, lassoPoints[i].y)
                        }
                    }
                    drawPath(
                        path = lassoPath,
                        color = Color(0xFFD97706),
                        style = Stroke(
                            width = (2.2.dp.toPx() / safeScale),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(14f / safeScale, 10f / safeScale),
                                0f
                            )
                        )
                    )
                }

                val selectionBounds = computeSelectionBounds(strokes, selectedStrokeIds)
                if (selectionBounds != null) {
                    val padded = selectionBounds.inflate(14f / safeScale)
                    drawRect(
                        color = Color(0xFFD97706).copy(alpha = 0.12f),
                        topLeft = padded.topLeft,
                        size = padded.size
                    )
                    drawRect(
                        color = Color(0xFFD97706),
                        topLeft = padded.topLeft,
                        size = padded.size,
                        style = Stroke(
                            width = (2.dp.toPx() / safeScale),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(12f / safeScale, 8f / safeScale),
                                0f
                            )
                        )
                    )
                }

                // 5. Eraser Ring
                eraserCursor?.let { center ->
                    val radius = max(24f / safeScale, activeStrokeWidth * 2.4f)
                    drawCircle(
                        color = Color(0xFFE11D48).copy(alpha = 0.20f),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFFE11D48),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx() / safeScale)
                    )
                }
            }
        }
    }
}

/**
 * Renders an infinitely expanding OneNote-style grid, ruled lines, dot matrix, or Cornell frame
 * across the entire visible world bounding box `(worldLeft, worldTop, worldRight, worldBottom)`.
 * As the user zooms out or pans in any direction, the grid seamlessly expands and scales with the canvas.
 */
private fun DrawScope.drawInfinitePaperGrid(
    paperTemplate: PaperTemplate,
    paperColor: PaperColor,
    worldLeft: Float,
    worldTop: Float,
    worldRight: Float,
    worldBottom: Float,
    zoomScale: Float
) {
    val lineColor = Color(paperColor.darkLineHex)
    val baseSpacing = 30.dp.toPx()

    // Adaptive Level-of-Detail (LOD) so zooming way out (e.g. 15%) stays 60fps and visually crisp
    val lodMultiplier = when {
        zoomScale < 0.22f -> 4f
        zoomScale < 0.42f -> 2f
        else -> 1f
    }
    val minorStep = baseSpacing * lodMultiplier
    val majorStep = minorStep * 5f

    // Keep grid stroke widths crisp regardless of zoom factor
    val minorStrokeWidth = (1.dp.toPx() / zoomScale.coerceIn(0.35f, 2.2f))
    val majorStrokeWidth = (1.6.dp.toPx() / zoomScale.coerceIn(0.35f, 2.2f))

    val startX = floor(worldLeft / minorStep) * minorStep
    val endX = ceil(worldRight / minorStep) * minorStep
    val startY = floor(worldTop / minorStep) * minorStep
    val endY = ceil(worldBottom / minorStep) * minorStep

    when (paperTemplate) {
        PaperTemplate.BLANK -> Unit

        PaperTemplate.GRID -> {
            // Minor vertical grid lines
            var x = startX
            while (x <= endX) {
                val isMajor = kotlin.math.abs(x % majorStep) < 0.5f
                drawLine(
                    color = if (isMajor) lineColor.copy(alpha = 0.95f) else lineColor.copy(alpha = 0.55f),
                    start = Offset(x, worldTop),
                    end = Offset(x, worldBottom),
                    strokeWidth = if (isMajor) majorStrokeWidth else minorStrokeWidth
                )
                x += minorStep
            }

            // Minor & major horizontal grid lines
            var y = startY
            while (y <= endY) {
                val isMajor = kotlin.math.abs(y % majorStep) < 0.5f
                drawLine(
                    color = if (isMajor) lineColor.copy(alpha = 0.95f) else lineColor.copy(alpha = 0.55f),
                    start = Offset(worldLeft, y),
                    end = Offset(worldRight, y),
                    strokeWidth = if (isMajor) majorStrokeWidth else minorStrokeWidth
                )
                y += minorStep
            }

            // Subtle origin axis crosshairs at (0,0) so you always have a spatial anchor when zooming out
            if (worldLeft <= 0f && worldRight >= 0f) {
                drawLine(
                    color = Color(0xFFD97706).copy(alpha = 0.25f),
                    start = Offset(0f, worldTop),
                    end = Offset(0f, worldBottom),
                    strokeWidth = majorStrokeWidth * 1.2f
                )
            }
            if (worldTop <= 0f && worldBottom >= 0f) {
                drawLine(
                    color = Color(0xFFD97706).copy(alpha = 0.25f),
                    start = Offset(worldLeft, 0f),
                    end = Offset(worldRight, 0f),
                    strokeWidth = majorStrokeWidth * 1.2f
                )
            }
        }

        PaperTemplate.RULED -> {
            var y = startY
            while (y <= endY) {
                drawLine(
                    color = lineColor.copy(alpha = 0.85f),
                    start = Offset(worldLeft, y),
                    end = Offset(worldRight, y),
                    strokeWidth = minorStrokeWidth
                )
                y += minorStep
            }
            val marginX = 48.dp.toPx()
            if (marginX in worldLeft..worldRight) {
                drawLine(
                    color = Color(0xFFE11D48).copy(alpha = 0.30f),
                    start = Offset(marginX, worldTop),
                    end = Offset(marginX, worldBottom),
                    strokeWidth = majorStrokeWidth
                )
            }
        }

        PaperTemplate.DOTTED -> {
            val dotRadius = (1.7.dp.toPx() / zoomScale.coerceIn(0.45f, 1.8f))
            var x = startX
            while (x <= endX) {
                var y = startY
                while (y <= endY) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.85f),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += minorStep
                }
                x += minorStep
            }
        }

        PaperTemplate.CORNELL -> {
            var y = startY
            while (y <= endY) {
                drawLine(
                    color = lineColor.copy(alpha = 0.78f),
                    start = Offset(worldLeft, y),
                    end = Offset(worldRight, y),
                    strokeWidth = minorStrokeWidth
                )
                y += minorStep
            }
            val cueColumnX = 110.dp.toPx()
            if (cueColumnX in worldLeft..worldRight) {
                drawLine(
                    color = Color(0xFFD97706).copy(alpha = 0.45f),
                    start = Offset(cueColumnX, worldTop),
                    end = Offset(cueColumnX, worldBottom),
                    strokeWidth = majorStrokeWidth * 1.3f
                )
            }
        }
    }
}

private fun DrawScope.drawRichInkStroke(stroke: InkStroke, isSelected: Boolean) {
    val pts = stroke.points
    if (pts.isEmpty()) return

    val baseColor = Color(stroke.colorArgb).copy(alpha = stroke.opacity.coerceIn(0.08f, 1f))
    val avgPressure = pts.map { it.pressure }.average().toFloat().coerceIn(0.25f, 1.35f)

    val effectiveWidth = when (stroke.tool) {
        InkToolType.HIGHLIGHTER -> stroke.strokeWidth * 2.6f
        InkToolType.MARKER -> stroke.strokeWidth * 1.6f
        InkToolType.BRUSH -> stroke.strokeWidth * (0.65f + avgPressure * 0.85f)
        InkToolType.PEN -> stroke.strokeWidth * (0.8f + avgPressure * 0.45f)
        InkToolType.PENCIL -> stroke.strokeWidth * 0.95f
        InkToolType.BALLPOINT -> stroke.strokeWidth
        InkToolType.ERASER -> stroke.strokeWidth
    }

    val cap = when (stroke.tool) {
        InkToolType.HIGHLIGHTER, InkToolType.MARKER -> StrokeCap.Square
        else -> StrokeCap.Round
    }

    if (pts.size == 1) {
        drawCircle(
            color = baseColor,
            radius = effectiveWidth / 2f,
            center = Offset(pts[0].x, pts[0].y)
        )
        return
    }

    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) {
            val prev = pts[i - 1]
            val cur = pts[i]
            val midX = (prev.x + cur.x) / 2f
            val midY = (prev.y + cur.y) / 2f
            quadraticTo(prev.x, prev.y, midX, midY)
        }
        lineTo(pts.last().x, pts.last().y)
    }

    if (isSelected) {
        drawPath(
            path = path,
            color = Color(0xFFD97706).copy(alpha = 0.45f),
            style = Stroke(
                width = effectiveWidth + 10f,
                cap = cap,
                join = StrokeJoin.Round
            )
        )
    }

    val pathEffect = if (stroke.tool == InkToolType.PENCIL) {
        PathEffect.dashPathEffect(floatArrayOf(14f, 1.5f), 0f)
    } else {
        null
    }

    drawPath(
        path = path,
        color = baseColor,
        style = Stroke(
            width = effectiveWidth,
            cap = cap,
            join = StrokeJoin.Round,
            pathEffect = pathEffect
        ),
        blendMode = BlendMode.SrcOver
    )
}

private fun findStrokesNearPoint(
    strokes: List<InkStroke>,
    point: Offset,
    radius: Float
): Set<String> {
    val hit = mutableSetOf<String>()
    for (stroke in strokes) {
        val threshold = radius + stroke.strokeWidth / 2f
        if (stroke.points.any { pt -> hypot(pt.x - point.x, pt.y - point.y) <= threshold }) {
            hit.add(stroke.id)
        }
    }
    return hit
}

private fun findStrokesInsideLasso(
    strokes: List<InkStroke>,
    polygon: List<Offset>
): Set<String> {
    if (polygon.size < 3) return emptySet()
    val minX = polygon.minOf { it.x }
    val maxX = polygon.maxOf { it.x }
    val minY = polygon.minOf { it.y }
    val maxY = polygon.maxOf { it.y }
    val bounds = Rect(minX, minY, maxX, maxY)

    return strokes.filter { stroke ->
        stroke.points.any { pt ->
            val pos = Offset(pt.x, pt.y)
            bounds.contains(pos) && isPointInPolygon(pos, polygon)
        }
    }.map { it.id }.toSet()
}

private fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val xi = polygon[i].x
        val yi = polygon[i].y
        val xj = polygon[j].x
        val yj = polygon[j].y
        val intersect = ((yi > point.y) != (yj > point.y)) &&
            (point.x < (xj - xi) * (point.y - yi) / ((yj - yi).takeIf { it != 0f } ?: 0.0001f) + xi)
        if (intersect) inside = !inside
        j = i
    }
    return inside
}

private fun computeSelectionBounds(
    strokes: List<InkStroke>,
    selectedIds: Set<String>
): Rect? {
    if (selectedIds.isEmpty()) return null
    val selectedPts = strokes.filter { selectedIds.contains(it.id) }.flatMap { it.points }
    if (selectedPts.isEmpty()) return null
    return Rect(
        left = selectedPts.minOf { it.x },
        top = selectedPts.minOf { it.y },
        right = selectedPts.maxOf { it.x },
        bottom = selectedPts.maxOf { it.y }
    )
}
