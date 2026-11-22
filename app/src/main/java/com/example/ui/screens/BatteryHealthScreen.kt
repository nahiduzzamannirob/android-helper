package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ActiveWarning
import com.example.data.model.BatteryTelemetry
import com.example.data.model.HealthGrade
import com.example.data.model.WarningSettings
import com.example.ui.components.BatteryTelemetryGauge
import com.example.ui.components.WarningBannerCard
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltCrimson
import com.example.ui.theme.VoltCyan
import com.example.ui.theme.VoltEmerald
import com.example.ui.viewmodel.formatTemp

@Composable
fun BatteryHealthScreen(
  telemetry: BatteryTelemetry,
  settings: WarningSettings,
  activeWarnings: List<ActiveWarning>,
  onRefresh: () -> Unit,
  onSetCustomCapacity: (Int) -> Unit,
  onDismissTestWarning: () -> Unit,
  onNavigateToCharging: () -> Unit,
  onNavigateToDisplay: () -> Unit,
  onNavigateToWarnings: () -> Unit,
) {
  var showCalibrateDialog by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 680.dp

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      item {
        Column(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
          // Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Battery Health Check",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = "${telemetry.manufacturer} ${telemetry.deviceModel} • ${telemetry.androidVersion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = onRefresh,
              modifier = Modifier.size(48.dp).testTag("refresh_battery_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Refresh battery telemetry",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      if (settings.showInAppBanner && activeWarnings.isNotEmpty()) {
        item {
          Column(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            activeWarnings.forEach { warning ->
              WarningBannerCard(
                warning = warning,
                onOpenSettings = onNavigateToWarnings,
                onDismissTest =
                  if (warning.id == "test_preview") onDismissTestWarning else null,
              )
            }
          }
        }
      }

      item {
        if (isWide) {
          Row(
            modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
          ) {
            HeroGaugeCard(
              telemetry = telemetry,
              settings = settings,
              hasWarning = activeWarnings.isNotEmpty(),
              modifier = Modifier.weight(1f),
            )
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              CapacityAnalysisCard(
                telemetry = telemetry,
                onCalibrateClick = { showCalibrateDialog = true },
              )
              QuickActionsCard(
                onNavigateToCharging = onNavigateToCharging,
                onNavigateToDisplay = onNavigateToDisplay,
                onNavigateToWarnings = onNavigateToWarnings,
              )
            }
          }
        } else {
          Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            HeroGaugeCard(
              telemetry = telemetry,
              settings = settings,
              hasWarning = activeWarnings.isNotEmpty(),
              modifier = Modifier.fillMaxWidth(),
            )
            CapacityAnalysisCard(
              telemetry = telemetry,
              onCalibrateClick = { showCalibrateDialog = true },
            )
          }
        }
      }

      item {
        Column(
          modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Hardware Cell Telemetry",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            DiagnosticTile(
              title = "System Health",
              value = telemetry.hardwareHealth.displayName,
              subtitle = if (telemetry.isPresent) "Cell Present" else "Cell Missing",
              icon = Icons.Rounded.Biotech,
              accent = if (telemetry.hardwareHealth.isHealthy) VoltEmerald else VoltCrimson,
              modifier = Modifier.weight(1f),
            )
            DiagnosticTile(
              title = "Chemistry",
              value = telemetry.technology,
              subtitle = "Rated ${telemetry.designCapacityMah} mAh",
              icon = Icons.Rounded.Memory,
              accent = VoltCyan,
              modifier = Modifier.weight(1f),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            DiagnosticTile(
              title = "Cycle Count",
              value = telemetry.cycleCount?.let { "$it cycles" } ?: "Not Exposed",
              subtitle = if (telemetry.cycleCount != null) "OS / Kernel Counter" else "Android < 14 / OEM locked",
              icon = Icons.Rounded.Replay,
              accent = VoltCyan,
              modifier = Modifier.weight(1f),
            )
            DiagnosticTile(
              title = "Charge Stored",
              value = telemetry.chargeCounterMah?.let { "$it mAh" } ?: "Unsupported",
              subtitle =
                telemetry.energyCounterMwh?.let { "$it mWh energy" } ?: "Real-time coulomb counter",
              icon = Icons.Rounded.BatteryChargingFull,
              accent = VoltEmerald,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      if (!isWide) {
        item {
          QuickActionsCard(
            onNavigateToCharging = onNavigateToCharging,
            onNavigateToDisplay = onNavigateToDisplay,
            onNavigateToWarnings = onNavigateToWarnings,
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
          )
        }
      }
    }
  }

  if (showCalibrateDialog) {
    CalibrateCapacityDialog(
      currentCapacityMah = telemetry.designCapacityMah,
      isCustom = settings.customDesignCapacityMah > 0,
      onDismiss = { showCalibrateDialog = false },
      onConfirm = { value ->
        onSetCustomCapacity(value)
        showCalibrateDialog = false
      },
    )
  }
}

@Composable
private fun HeroGaugeCard(
  telemetry: BatteryTelemetry,
  settings: WarningSettings,
  hasWarning: Boolean,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      BatteryTelemetryGauge(
        levelPct = telemetry.levelPct,
        healthPct = telemetry.estimatedHealthPct,
        healthGrade = telemetry.healthGrade,
        chargingState = telemetry.chargingState,
        showHealthBadge = settings.showGaugeHealthBadge,
        hasActiveWarning = hasWarning,
        modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val tempText = formatTemp(telemetry.temperatureC, settings.useFahrenheit)
        val isHot = settings.highTempEnabled && telemetry.temperatureC >= settings.highTempThresholdC
        MetricChip(
          icon = Icons.Rounded.DeviceThermostat,
          label = "Temp",
          value = tempText,
          accent = if (isHot) VoltCrimson else VoltEmerald,
          modifier = Modifier.weight(1f),
        )
        MetricChip(
          icon = Icons.Rounded.ElectricBolt,
          label = "Voltage",
          value = String.format("%.2f V", telemetry.voltageMv / 1000f),
          accent = VoltCyan,
          modifier = Modifier.weight(1f),
        )
        MetricChip(
          icon = Icons.Rounded.BatteryChargingFull,
          label = "Source",
          value = telemetry.pluggedSource.displayName,
          accent = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun CapacityAnalysisCard(
  telemetry: BatteryTelemetry,
  onCalibrateClick: () -> Unit,
) {
  val healthPct = telemetry.estimatedHealthPct
  val progressColor =
    when (telemetry.healthGrade) {
      HealthGrade.EXCELLENT -> VoltEmerald
      HealthGrade.GOOD -> VoltCyan
      HealthGrade.FAIR -> VoltAmber
      HealthGrade.DEGRADED -> VoltCrimson
      HealthGrade.CALIBRATING -> MaterialTheme.colorScheme.primary
    }

  Card(
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Universal Capacity Analysis",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Source: ${telemetry.designCapacitySource}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedButton(
          onClick = onCalibrateClick,
          modifier = Modifier.testTag("calibrate_capacity_button"),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = "Calibrate rated capacity",
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Calibrate")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = "Estimated Full",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = telemetry.estimatedFullCapacityMah?.let { "$it mAh" } ?: "Calibrating...",
            style = MaterialTheme.typography.titleLarge,
            color = progressColor,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "Rated Design",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${telemetry.designCapacityMah} mAh",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      LinearProgressIndicator(
        progress = { ((healthPct ?: 100) / 100f).coerceIn(0.05f, 1f) },
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = progressColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )

      Spacer(modifier = Modifier.height(8.dp))

      AnimatedVisibility(visible = telemetry.estimatedFullCapacityMah == null) {
        Text(
          text =
            "Charge above 15% or connect a charger so the fuel-gauge coulomb counter can estimate full cell wear on this device.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      AnimatedVisibility(visible = healthPct != null) {
        Text(
          text =
            "Estimated cell retention is ${healthPct ?: 0}% (${telemetry.healthGrade.label}). Tap Calibrate if your phone's rated mAh differs.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun QuickActionsCard(
  onNavigateToCharging: () -> Unit,
  onNavigateToDisplay: () -> Unit,
  onNavigateToWarnings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Direct Controls & Tools",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onNavigateToCharging,
          modifier = Modifier.weight(1f).testTag("quick_charging_button"),
        ) {
          Icon(Icons.Rounded.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Charging Stats")
        }
        OutlinedButton(
          onClick = onNavigateToWarnings,
          modifier = Modifier.weight(1f).testTag("quick_warnings_button"),
        ) {
          Icon(
            Icons.Rounded.NotificationsActive,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Alert Display")
        }
      }
      Button(
        onClick = onNavigateToDisplay,
        modifier = Modifier.fillMaxWidth().testTag("quick_display_sw_button"),
      ) {
        Icon(Icons.Rounded.DisplaySettings, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Change Developer Smallest Width (dp)")
      }
    }
  }
}

@Composable
private fun DiagnosticTile(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accent,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun MetricChip(
  icon: ImageVector,
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accent,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CalibrateCapacityDialog(
  currentCapacityMah: Int,
  isCustom: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit,
) {
  var input by remember { mutableStateOf(currentCapacityMah.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Calibrate Design Capacity") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text =
            "Enter your phone's factory rated battery capacity (mAh) for accurate health percentage calculation across any device.",
          style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
          value = input,
          onValueChange = { raw -> input = raw.filter { it.isDigit() }.take(5) },
          label = { Text("Rated Capacity (mAh)") },
          placeholder = { Text("e.g. 5000") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth().testTag("calibrate_capacity_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = input.toIntOrNull() ?: 0
          if (parsed in 500..25000) onConfirm(parsed)
        },
        modifier = Modifier.testTag("calibrate_confirm_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      Row {
        if (isCustom) {
          TextButton(onClick = { onConfirm(0) }) { Text("Auto-Detect") }
        }
        TextButton(onClick = onDismiss) { Text("Cancel") }
      }
    },
  )
}
