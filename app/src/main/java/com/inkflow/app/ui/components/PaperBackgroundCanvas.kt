package com.inkflow.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.inkflow.app.data.model.InkStroke
import com.inkflow.app.data.model.PaperColor
import com.inkflow.app.data.model.PaperTemplate

/**
 * Renders the notebook paper background (Blank, Ruled, Grid, Dotted, Cornell) along with any
 * structured vector ink strokes stored on the page.
 */
@Composable
fun PaperBackgroundCanvas(
    paperTemplate: PaperTemplate,
    paperColor: PaperColor,
    strokes: List<InkStroke> = emptyList(),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bgColor = Color(paperColor.hexColor)
        val lineColor = Color(paperColor.darkLineHex)
        drawRect(color = bgColor)

        val spacingPx = 28.dp.toPx()
        val strokeWidthPx = 1.dp.toPx()

        when (paperTemplate) {
            PaperTemplate.BLANK -> Unit

            PaperTemplate.RULED -> {
                val marginX = 42.dp.toPx()
                var y = spacingPx * 1.5f
                while (y < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidthPx
                    )
                    y += spacingPx
                }
                // Subtle red/coral vertical margin rule
                drawLine(
                    color = Color(0xFFE11D48).copy(alpha = 0.25f),
                    start = Offset(marginX, 0f),
                    end = Offset(marginX, size.height),
                    strokeWidth = strokeWidthPx * 1.2f
                )
            }

            PaperTemplate.GRID -> {
                var x = spacingPx
                while (x < size.width) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.75f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidthPx
                    )
                    x += spacingPx
                }
                var y = spacingPx
                while (y < size.height) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.75f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidthPx
                    )
                    y += spacingPx
                }
            }

            PaperTemplate.DOTTED -> {
                val dotRadius = 1.6.dp.toPx()
                var x = spacingPx
                while (x < size.width) {
                    var y = spacingPx
                    while (y < size.height) {
                        drawCircle(
                            color = lineColor,
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += spacingPx
                    }
                    x += spacingPx
                }
            }

            PaperTemplate.CORNELL -> {
                val cueColumnX = (size.width * 0.28f).coerceAtLeast(88.dp.toPx())
                val summaryRowY = (size.height - 96.dp.toPx()).coerceAtLeast(size.height * 0.78f)
                var y = spacingPx * 1.5f
                while (y < summaryRowY) {
                    drawLine(
                        color = lineColor,
                        start = Offset(cueColumnX, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidthPx
                    )
                    y += spacingPx
                }
                // Cue column divider
                drawLine(
                    color = Color(0xFF2563EB).copy(alpha = 0.35f),
                    start = Offset(cueColumnX, 0f),
                    end = Offset(cueColumnX, summaryRowY),
                    strokeWidth = strokeWidthPx * 1.6f
                )
                // Summary bottom divider
                drawLine(
                    color = Color(0xFF2563EB).copy(alpha = 0.35f),
                    start = Offset(0f, summaryRowY),
                    end = Offset(size.width, summaryRowY),
                    strokeWidth = strokeWidthPx * 1.6f
                )
            }
        }

        // Render structured vector strokes
        strokes.forEach { stroke ->
            drawStructuredStroke(stroke)
        }
    }
}

private fun DrawScope.drawStructuredStroke(stroke: InkStroke) {
    val pts = stroke.points
    if (pts.isEmpty()) return
    val color = Color(stroke.colorArgb).copy(alpha = stroke.opacity.coerceIn(0.05f, 1f))
    if (pts.size == 1) {
        drawCircle(
            color = color,
            radius = stroke.strokeWidth / 2f,
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
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = stroke.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
