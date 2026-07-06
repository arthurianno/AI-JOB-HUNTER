package com.multi.aijobhunter.feature.shared_ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multi.aijobhunter.core.model.RadarMetrics
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarChart(
    metrics: RadarMetrics,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labels = listOf("Hard Skills", "Soft Skills", "Experience", "Salary", "Industry")
    val values = listOf(metrics.hardSkills, metrics.softSkills, metrics.experience, metrics.salaryMatch, metrics.industryMatch)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val center = center
        val maxRadius = size.minDimension / 2.7f

        // Draw web layers (concentric polygons)
        val numLevels = 4
        for (level in 1..numLevels) {
            val levelRadius = maxRadius * (level.toFloat() / numLevels)
            val path = Path()
            for (i in 0 until 5) {
                val angle = Math.toRadians((i * 72 - 90).toDouble())
                val x = center.x + levelRadius * cos(angle).toFloat()
                val y = center.y + levelRadius * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = MutedText.copy(alpha = 0.15f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Draw radial spokes and labels
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72 - 90).toDouble())
            val x = center.x + maxRadius * cos(angle).toFloat()
            val y = center.y + maxRadius * sin(angle).toFloat()
            drawLine(
                color = MutedText.copy(alpha = 0.25f),
                start = center,
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx()
            )
            
            // Draw Label text
            val label = labels[i]
            val textLayoutResult = textMeasurer.measure(
                text = label,
                style = TextStyle(color = MutedText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            )
            val textWidth = textLayoutResult.size.width
            val textHeight = textLayoutResult.size.height
            
            // Offset text from tip slightly
            val textDist = maxRadius + 10.dp.toPx()
            val tx = center.x + textDist * cos(angle).toFloat() - (textWidth / 2f)
            val ty = center.y + textDist * sin(angle).toFloat() - (textHeight / 2f)
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(tx, ty)
            )
        }

        // Draw metrics polygon
        val metricPath = Path()
        for (i in 0 until 5) {
            val value = values[i].coerceIn(0.0f, 1.0f)
            val angle = Math.toRadians((i * 72 - 90).toDouble())
            val r = maxRadius * value
            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat()
            if (i == 0) metricPath.moveTo(x, y) else metricPath.lineTo(x, y)
        }
        metricPath.close()

        // Fill metric area
        drawPath(
            path = metricPath,
            color = NeonGreen.copy(alpha = 0.2f)
        )
        // Outline metric area
        drawPath(
            path = metricPath,
            color = NeonGreen,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
