package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ElectricMeter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ChargingSessionEntity
import com.example.data.model.BatteryTelemetry
import com.example.data.model.ChargingStateLabel
import com.example.data.model.TelemetrySample
import com.example.data.model.WarningSettings
import com.example.ui.components.ChartMetricMode
import com.example.ui.components.TelemetryWaveformChart
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltCyan
import com.example.ui.theme.VoltEmerald
import com.example.ui.viewmodel.ChargingAggregateStats
import com.example.ui.viewmodel.formatTemp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChargingStatsScreen(
  telemetry: BatteryTelemetry,
  settings: WarningSettings,
  samples: List<TelemetrySample>,
  sessions: List<ChargingSessionEntity>,
  aggregateStats: ChargingAggregateStats,
  onRefresh: () -> Unit,
  onSaveSnapshot: () -> Unit,
  onClearHistory: () -> Unit,
) {
  var chartMode by remember { mutableStateOf(ChartMetricMode.CURRENT_MA) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      Row(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Direct Charging Statistics",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Real-time wattage, amperage flow & logged sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onRefresh,
          modifier = Modifier.size(48.dp).testTag("refresh_charging_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "Refresh charging stats",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    // Live Charging Power Telemetry Card
    item {
      Card(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = "Power flow",
                tint =
                  if (telemetry.chargingState == ChargingStateLabel.CHARGING) VoltCyan
                  else VoltEmerald,
              )
              Text(
                text =
                  if (telemetry.chargingState == ChargingStateLabel.CHARGING)
                    "Charging via ${telemetry.pluggedSource.displayName}"
                  else "${telemetry.chargingState.displayName} • ${telemetry.pluggedSource.displayName}",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            Surface(
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = String.format("%.1f W", telemetry.powerWatts),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            val currentLabel =
              telemetry.currentNowMa?.let { if (it > 0) "+$it mA" else "$it mA" } ?: "N/A"
            val avgLabel =
              telemetry.currentAvgMa?.let { if (it > 0) "+$it mA" else "$it mA" } ?: "N/A"
            LiveStatBox(
              label = "Live Current",
              value = currentLabel,
              sub = "Avg: $avgLabel",
              modifier = Modifier.weight(1f),
            )
            LiveStatBox(
              label = "Cell Voltage",
              value = "${telemetry.voltageMv} mV",
              sub = String.format("%.3f V", telemetry.voltageMv / 1000f),
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            LiveStatBox(
              label = "Temperature",
              value = formatTemp(telemetry.temperatureC, settings.useFahrenheit),
              sub = telemetry.hardwareHealth.displayName,
              modifier = Modifier.weight(1f),
            )
            LiveStatBox(
              label = "Est. Time to Full",
              value = formatEta(telemetry.chargeTimeRemainingMs),
              sub = "Level ${telemetry.levelPct}%",
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = onSaveSnapshot,
            modifier = Modifier.fillMaxWidth().testTag("save_snapshot_button"),
          ) {
            Icon(Icons.Rounded.AddChart, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Current Telemetry Snapshot")
          }
        }
      }
    }

    // Live Rolling Waveform Chart
    item {
      Card(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Live Telemetry Waveform",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "${settings.pollingIntervalSec}s rate",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartMetricMode.entries.forEach { mode ->
              FilterChip(
                selected = chartMode == mode,
                onClick = { chartMode = mode },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("chart_mode_${mode.name.lowercase()}"),
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          val chartValues =
            when (chartMode) {
              ChartMetricMode.CURRENT_MA -> samples.map { it.currentMa.toFloat() }
              ChartMetricMode.POWER_W -> samples.map { it.powerWatts }
              ChartMetricMode.TEMP_C ->
                samples.map {
                  if (settings.useFahrenheit) it.temperatureC * 9f / 5f + 32f else it.temperatureC
                }
            }

          val chartAccent =
            when (chartMode) {
              ChartMetricMode.CURRENT_MA -> VoltCyan
              ChartMetricMode.POWER_W -> VoltEmerald
              ChartMetricMode.TEMP_C -> VoltAmber
            }

          val suffix =
            if (chartMode == ChartMetricMode.TEMP_C) {
              if (settings.useFahrenheit) "°F" else "°C"
            } else {
              chartMode.unit
            }

          TelemetryWaveformChart(
            values = chartValues,
            accentColor = chartAccent,
            unitSuffix = suffix,
          )
        }
      }
    }

    // Aggregate Stats Row
    item {
      Card(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          AggregateColumn("Sessions", "${aggregateStats.sessionCount}")
          AggregateColumn("Peak Power", String.format("%.1f W", aggregateStats.maxPeakWatts))
          AggregateColumn("Avg Peak", "${aggregateStats.avgPeakCurrentMa} mA")
          AggregateColumn(
            "Max Temp",
            if (aggregateStats.maxTempRecordedC > 0f)
              formatTemp(aggregateStats.maxTempRecordedC, settings.useFahrenheit)
            else "--",
          )
        }
      }
    }

    // Session History Header
    item {
      Row(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = "Session history",
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Charging Session Log",
            style = MaterialTheme.typography.titleMedium,
          )
        }
        if (sessions.isNotEmpty()) {
          TextButton(
            onClick = onClearHistory,
            modifier = Modifier.testTag("clear_sessions_button"),
          ) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear")
          }
        }
      }
    }

    if (sessions.isEmpty()) {
      item {
        Card(
          modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.ElectricMeter,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(34.dp),
            )
            Text(
              text = "No Charging Sessions Recorded Yet",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text =
                "Connect a charger to record live charging statistics automatically, or tap Log Current Telemetry Snapshot above.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(sessions, key = { it.id }) { session ->
        ChargingSessionRow(
          session = session,
          useFahrenheit = settings.useFahrenheit,
          modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun LiveStatBox(
  label: String,
  value: String,
  sub: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = sub,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun AggregateColumn(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ChargingSessionRow(
  session: ChargingSessionEntity,
  useFahrenheit: Boolean,
  modifier: Modifier = Modifier,
) {
  val dateStr = remember(session.startTimeMs) {
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(session.startTimeMs))
  }
  val durationMin = ((session.endTimeMs - session.startTimeMs) / 60_000L).coerceAtLeast(0L)

  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "$dateStr • ${session.powerSource}",
            style = MaterialTheme.typography.titleMedium,
          )
        }
        Text(
          text = "${session.startLevelPct}% → ${session.endLevelPct}%",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = VoltEmerald,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text =
          "Peak ${String.format("%.1f W", session.peakPowerWatts)} • ${session.peakCurrentMa} mA • Max ${formatTemp(session.maxTempC, useFahrenheit)} • Avg ${session.avgVoltageMv} mV • ${durationMin}m",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private fun formatEta(ms: Long?): String {
  if (ms == null || ms <= 0L) return "N/A"
  val totalMin = (ms / 60_000L).toInt()
  val hrs = totalMin / 60
  val mins = totalMin % 60
  return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
