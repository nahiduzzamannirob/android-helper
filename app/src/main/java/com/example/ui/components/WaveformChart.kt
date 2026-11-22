package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs

enum class ChartMetricMode(val label: String, val unit: String) {
  CURRENT_MA("Current", "mA"),
  POWER_W("Power", "W"),
  TEMP_C("Temp", "°"),
}

@Composable
fun TelemetryWaveformChart(
  values: List<Float>,
  accentColor: Color,
  unitSuffix: String,
  modifier: Modifier = Modifier,
) {
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

  Column(modifier = modifier) {
    Box(modifier = Modifier.fillMaxWidth().height(156.dp)) {
      if (values.size < 2) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = "Collecting live hardware samples...",
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
          )
        }
      } else {
        Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
          val minVal = values.minOrNull() ?: 0f
          val maxVal = values.maxOrNull() ?: 1f
          val rawSpan = maxVal - minVal
          val pad = if (rawSpan < 0.5f) 1f else rawSpan * 0.15f
          val low = minVal - pad
          val high = maxVal + pad
          val span = (high - low).coerceAtLeast(0.1f)

          // Horizontal reference dashed lines
          val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
          for (i in 0..3) {
            val y = size.height * (i / 3f)
            drawLine(
              color = gridColor,
              start = Offset(0f, y),
              end = Offset(size.width, y),
              strokeWidth = 1.dp.toPx(),
              pathEffect = dashEffect,
            )
          }

          val stepX = size.width / (values.size - 1).coerceAtLeast(1)
          val linePath = Path()
          val fillPath = Path()

          values.forEachIndexed { index, v ->
            val x = index * stepX
            val normalized = ((v - low) / span).coerceIn(0f, 1f)
            val y = size.height - (normalized * size.height)
            if (index == 0) {
              linePath.moveTo(x, y)
              fillPath.moveTo(x, size.height)
              fillPath.lineTo(x, y)
            } else {
              linePath.lineTo(x, y)
              fillPath.lineTo(x, y)
            }
            if (index == values.lastIndex) {
              fillPath.lineTo(x, size.height)
              fillPath.close()
            }
          }

          drawPath(
            path = fillPath,
            brush =
              Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.28f), accentColor.copy(alpha = 0.0f))
              ),
          )

          drawPath(
            path = linePath,
            color = accentColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
          )

          val lastY =
            size.height - ((((values.last() - low) / span).coerceIn(0f, 1f)) * size.height)
          val lastX = (values.size - 1) * stepX
          drawCircle(color = accentColor.copy(alpha = 0.28f), radius = 8.dp.toPx(), center = Offset(lastX, lastY))
          drawCircle(color = accentColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
        }
      }
    }

    if (values.isNotEmpty()) {
      Spacer(modifier = Modifier.height(6.dp))
      val minV = values.minOrNull() ?: 0f
      val avgV = values.average().toFloat()
      val maxV = values.maxOrNull() ?: 0f
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "MIN ${formatMetricValue(minV, unitSuffix)}",
          style = MaterialTheme.typography.labelSmall,
          color = labelColor,
        )
        Text(
          text = "AVG ${formatMetricValue(avgV, unitSuffix)}",
          style = MaterialTheme.typography.labelSmall,
          color = labelColor,
        )
        Text(
          text = "PEAK ${formatMetricValue(maxV, unitSuffix)}",
          style = MaterialTheme.typography.labelSmall,
          color = accentColor,
        )
      }
    }
  }
}

private fun formatMetricValue(v: Float, suffix: String): String {
  return if (abs(v) >= 100f) "${v.toInt()} $suffix" else String.format("%.1f %s", v, suffix)
}
