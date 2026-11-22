package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChargingStateLabel
import com.example.data.model.HealthGrade
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltCrimson
import com.example.ui.theme.VoltCyan
import com.example.ui.theme.VoltEmerald
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BatteryTelemetryGauge(
  levelPct: Int,
  healthPct: Int?,
  healthGrade: HealthGrade,
  chargingState: ChargingStateLabel,
  showHealthBadge: Boolean,
  hasActiveWarning: Boolean,
  modifier: Modifier = Modifier,
) {
  val animatedSweep by
    animateFloatAsState(
      targetValue = (levelPct.coerceIn(0, 100) / 100f) * 260f,
      animationSpec = spring(stiffness = Spring.StiffnessLow),
      label = "battery_gauge_sweep",
    )

  val primaryAccent =
    when {
      hasActiveWarning -> VoltAmber
      levelPct <= 15 -> VoltCrimson
      chargingState == ChargingStateLabel.CHARGING -> VoltCyan
      else -> VoltEmerald
    }

  val secondaryAccent =
    when {
      hasActiveWarning -> VoltCrimson
      chargingState == ChargingStateLabel.CHARGING -> VoltEmerald
      else -> VoltCyan
    }

  val trackColor = MaterialTheme.colorScheme.outlineVariant
  val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

  Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
      val diameter = size.minDimension
      val strokeWidth = diameter * 0.085f
      val inset = strokeWidth / 2f + 10.dp.toPx()
      val arcSize = Size(diameter - inset * 2f, diameter - inset * 2f)
      val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)

      // Outer precision tick marks
      val center = Offset(size.width / 2f, size.height / 2f)
      val outerTickRadius = diameter / 2f - 2.dp.toPx()
      val totalTicks = 26
      for (i in 0..totalTicks) {
        val angleDeg = 140f + (i.toFloat() / totalTicks) * 260f
        val rad = Math.toRadians(angleDeg.toDouble())
        val isMajor = i % 5 == 0
        val innerR = outerTickRadius - if (isMajor) 8.dp.toPx() else 4.dp.toPx()
        val p1 = Offset(center.x + (innerR * cos(rad)).toFloat(), center.y + (innerR * sin(rad)).toFloat())
        val p2 =
          Offset(
            center.x + (outerTickRadius * cos(rad)).toFloat(),
            center.y + (outerTickRadius * sin(rad)).toFloat(),
          )
        drawLine(
          color = if (i * 100 / totalTicks <= levelPct) primaryAccent.copy(alpha = 0.8f) else tickColor,
          start = p1,
          end = p2,
          strokeWidth = if (isMajor) 2.2.dp.toPx() else 1.2.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }

      // Track arc
      drawArc(
        color = trackColor,
        startAngle = 140f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
      )

      // Active gradient telemetry arc
      if (animatedSweep > 0.5f) {
        drawArc(
          brush = Brush.sweepGradient(listOf(secondaryAccent, primaryAccent, secondaryAccent)),
          startAngle = 140f,
          sweepAngle = animatedSweep,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (chargingState == ChargingStateLabel.CHARGING) {
          Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = "Charging active",
            tint = primaryAccent,
            modifier = Modifier.size(18.dp),
          )
        }
        Text(
          text = chargingState.displayName.uppercase(),
          style = MaterialTheme.typography.labelMedium,
          color = primaryAccent,
        )
      }

      Text(
        text = "$levelPct%",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(6.dp))

      if (showHealthBadge) {
        val badgeColor =
          when {
            hasActiveWarning -> VoltAmber
            healthGrade == HealthGrade.DEGRADED -> VoltCrimson
            healthGrade == HealthGrade.FAIR -> VoltAmber
            else -> VoltEmerald
          }
        Surface(
          shape = RoundedCornerShape(50),
          color = badgeColor.copy(alpha = 0.14f),
          modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.45f), CircleShape),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = if (hasActiveWarning) Icons.Rounded.WarningAmber else Icons.Rounded.Shield,
              contentDescription = "Battery health grade",
              tint = badgeColor,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text =
                if (healthPct != null) "Health $healthPct% • ${healthGrade.label}"
                else "Health: ${healthGrade.label}",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = badgeColor,
            )
          }
        }
      }
    }
  }
}
