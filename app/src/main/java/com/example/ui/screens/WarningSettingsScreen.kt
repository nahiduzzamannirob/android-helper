package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.WarningSettings
import com.example.ui.viewmodel.formatTemp
import kotlin.math.roundToInt

@Composable
fun WarningSettingsScreen(
  settings: WarningSettings,
  onUpdateSettings: ((WarningSettings) -> WarningSettings) -> Unit,
  onTestWarning: () -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val context = LocalContext.current

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        onUpdateSettings { it.copy(systemNotificationsEnabled = true) }
        onShowMessage("Battery warning notifications enabled")
      } else {
        onUpdateSettings { it.copy(systemNotificationsEnabled = false) }
        onShowMessage("Notification permission denied — in-app banners remain active")
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      Column(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth()) {
        Text(
          text = "Health Warnings & Display",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Configure protection thresholds & how alerts are displayed",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Warning Display Settings Card
    item {
      Card(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Warning Display Settings",
            style = MaterialTheme.typography.titleMedium,
          )

          SettingSwitchRow(
            icon = Icons.Rounded.Visibility,
            title = "In-App Alert Banner",
            subtitle = "Show prominent live warning banners on the Health dashboard",
            checked = settings.showInAppBanner,
            tag = "switch_in_app_banner",
            onCheckedChange = { checked ->
              onUpdateSettings { it.copy(showInAppBanner = checked) }
            },
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          SettingSwitchRow(
            icon = Icons.Rounded.Shield,
            title = "Gauge Health Status Badge",
            subtitle = "Display cell health grade and warning pill inside the main gauge",
            checked = settings.showGaugeHealthBadge,
            tag = "switch_gauge_badge",
            onCheckedChange = { checked ->
              onUpdateSettings { it.copy(showGaugeHealthBadge = checked) }
            },
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          SettingSwitchRow(
            icon = Icons.Rounded.NotificationsActive,
            title = "System Notification Alerts",
            subtitle = "Push status-bar alerts when temperature or voltage spikes",
            checked = settings.systemNotificationsEnabled,
            tag = "switch_system_notifications",
            onCheckedChange = { checked ->
              if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted =
                  ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                  ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                  onUpdateSettings { it.copy(systemNotificationsEnabled = true) }
                } else {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              } else {
                onUpdateSettings { it.copy(systemNotificationsEnabled = checked) }
              }
            },
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          SettingSwitchRow(
            icon = Icons.Rounded.Vibration,
            title = "Haptic Vibration on Critical Alert",
            subtitle = "Vibrate briefly when overheat or over-voltage triggers",
            checked = settings.hapticOnCritical,
            tag = "switch_haptic_critical",
            onCheckedChange = { checked ->
              onUpdateSettings { it.copy(hapticOnCritical = checked) }
            },
          )

          Button(
            onClick = onTestWarning,
            modifier = Modifier.fillMaxWidth().testTag("test_warning_button"),
          ) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Preview Health Warning Alert")
          }
        }
      }
    }

    // Threshold Triggers Card
    item {
      Card(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "Health Warning Thresholds",
            style = MaterialTheme.typography.titleMedium,
          )

          ThresholdSliderBlock(
            icon = Icons.Rounded.DeviceThermostat,
            title = "High Temperature Warning",
            valueLabel = formatTemp(settings.highTempThresholdC, settings.useFahrenheit),
            enabled = settings.highTempEnabled,
            value = settings.highTempThresholdC,
            range = 35f..55f,
            switchTag = "switch_high_temp",
            sliderTag = "slider_high_temp",
            onEnabledChange = { on -> onUpdateSettings { it.copy(highTempEnabled = on) } },
            onValueChange = { v ->
              onUpdateSettings { it.copy(highTempThresholdC = v.roundToInt().toFloat()) }
            },
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          ThresholdSliderBlock(
            icon = Icons.Rounded.ElectricBolt,
            title = "Over-Voltage Threshold",
            valueLabel = "${settings.overVoltageThresholdMv} mV",
            enabled = settings.overVoltageEnabled,
            value = settings.overVoltageThresholdMv.toFloat(),
            range = 4300f..4650f,
            switchTag = "switch_over_voltage",
            sliderTag = "slider_over_voltage",
            onEnabledChange = { on -> onUpdateSettings { it.copy(overVoltageEnabled = on) } },
            onValueChange = { v ->
              val snapped = ((v / 25f).roundToInt() * 25).coerceIn(4300, 4650)
              onUpdateSettings { it.copy(overVoltageThresholdMv = snapped) }
            },
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          ThresholdSliderBlock(
            icon = Icons.Rounded.BatteryAlert,
            title = "Low Battery Warning",
            valueLabel = "${settings.lowBatteryThresholdPct}%",
            enabled = settings.lowBatteryEnabled,
            value = settings.lowBatteryThresholdPct.toFloat(),
            range = 5f..30f,
            switchTag = "switch_low_battery",
            sliderTag = "slider_low_battery",
            onEnabledChange = { on -> onUpdateSettings { it.copy(lowBatteryEnabled = on) } },
            onValueChange = { v ->
              onUpdateSettings { it.copy(lowBatteryThresholdPct = v.roundToInt()) }
            },
          )

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          ThresholdSliderBlock(
            icon = Icons.Rounded.BatteryChargingFull,
            title = "Charge Limit Reminder",
            valueLabel = "${settings.chargeLimitPct}%",
            enabled = settings.chargeLimitEnabled,
            value = settings.chargeLimitPct.toFloat(),
            range = 70f..100f,
            switchTag = "switch_charge_limit",
            sliderTag = "slider_charge_limit",
            onEnabledChange = { on -> onUpdateSettings { it.copy(chargeLimitEnabled = on) } },
            onValueChange = { v ->
              val snapped = ((v / 5f).roundToInt() * 5).coerceIn(70, 100)
              onUpdateSettings { it.copy(chargeLimitPct = snapped) }
            },
          )
        }
      }
    }

    // Telemetry Display Units & Theme Card
    item {
      Card(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Telemetry Display Preferences",
            style = MaterialTheme.typography.titleMedium,
          )

          Text(
            text = "Temperature Unit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = !settings.useFahrenheit,
              onClick = { onUpdateSettings { it.copy(useFahrenheit = false) } },
              label = { Text("Celsius (°C)") },
              modifier = Modifier.testTag("unit_celsius_chip"),
            )
            FilterChip(
              selected = settings.useFahrenheit,
              onClick = { onUpdateSettings { it.copy(useFahrenheit = true) } },
              label = { Text("Fahrenheit (°F)") },
              modifier = Modifier.testTag("unit_fahrenheit_chip"),
            )
          }

          Text(
            text = "Live Telemetry Refresh Rate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 5).forEach { sec ->
              FilterChip(
                selected = settings.pollingIntervalSec == sec,
                onClick = { onUpdateSettings { it.copy(pollingIntervalSec = sec) } },
                label = { Text("${sec}s") },
                modifier = Modifier.testTag("polling_${sec}s_chip"),
              )
            }
          }

          Text(
            text = "Appearance",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = settings.darkThemeOverride == null,
              onClick = { onUpdateSettings { it.copy(darkThemeOverride = null) } },
              label = { Text("System") },
            )
            FilterChip(
              selected = settings.darkThemeOverride == true,
              onClick = { onUpdateSettings { it.copy(darkThemeOverride = true) } },
              label = { Text("Dark Lab") },
            )
            FilterChip(
              selected = settings.darkThemeOverride == false,
              onClick = { onUpdateSettings { it.copy(darkThemeOverride = false) } },
              label = { Text("Light Lab") },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SettingSwitchRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  tag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(22.dp),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(tag),
    )
  }
}

@Composable
private fun ThresholdSliderBlock(
  icon: ImageVector,
  title: String,
  valueLabel: String,
  enabled: Boolean,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  switchTag: String,
  sliderTag: String,
  onEnabledChange: (Boolean) -> Unit,
  onValueChange: (Float) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
          text = "Trigger at $valueLabel",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Switch(
        checked = enabled,
        onCheckedChange = onEnabledChange,
        modifier = Modifier.testTag(switchTag),
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Slider(
      value = value.coerceIn(range.start, range.endInclusive),
      onValueChange = onValueChange,
      valueRange = range,
      enabled = enabled,
      modifier = Modifier.fillMaxWidth().testTag(sliderTag),
    )
  }
}
