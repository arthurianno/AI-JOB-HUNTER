package com.multi.aijobhunter.feature.shared_ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MatchScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    strokeWidth: Dp = 5.dp
) {
    val color = when {
        score >= 85 -> NeonGreen
        score >= 70 -> NeonAmber
        else -> NeonRed
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = this.size.width
            val radius = (canvasSize - strokePx) / 2f
            
            // Draw background track
            drawCircle(
                color = TerminalMediumGray,
                radius = radius,
                center = Offset(canvasSize / 2f, canvasSize / 2f),
                style = Stroke(width = strokePx)
            )
            
            // Draw progress arc
            val sweepAngle = (score / 100f) * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                size = Size(radius * 2f, radius * 2f),
                topLeft = Offset(strokePx / 2f, strokePx / 2f)
            )
        }
        Text(
            text = "$score%",
            color = PureWhite,
            fontSize = (size.value * 0.26f).sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
