package org.continuumcalendar.ui.illustrations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Infinity Open mark — matches apps/desktop Marks.tsx OpenMark. */
@Composable
fun OpenMark(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(16.dp)) {
        val stroke = Stroke(width = 1.4.dp.toPx())
        val r = 3.2.dp.toPx()
        val left = Offset(5.2.dp.toPx(), size.height / 2f)
        val right = Offset(10.8.dp.toPx(), size.height / 2f)
        drawCircle(color = color, radius = r, center = left, style = stroke)
        drawCircle(color = color, radius = r, center = right, style = stroke)
        drawCircle(color = color, radius = 1.dp.toPx(), center = left)
        drawCircle(color = color, radius = 1.dp.toPx(), center = right)
    }
}
