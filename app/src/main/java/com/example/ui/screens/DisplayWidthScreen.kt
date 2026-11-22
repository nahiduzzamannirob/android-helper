package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.hardware.DisplayApplyResult
import com.example.data.hardware.DisplayWidthState
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltCrimson
import com.example.ui.theme.VoltCyan
import com.example.ui.theme.VoltEmerald

@Composable
fun DisplayWidthScreen(
  displayState: DisplayWidthState,
  targetSwDp: Int,
  calculatedTargetDpi: Int,
  feedback: DisplayApplyResult?,
  adbGrantCommand: String,
  adbDensityCommand: String,
  onTargetSwChanged: (Int) -> Unit,
  onApplyDirect: () -> Unit,
  onResetDirect: () -> Unit,
  onApplyRoot: (Int?) -> Unit,
  onOpenDeveloperSettings: () -> Unit,
  onRefreshState: () -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val clipboardManager = LocalClipboardManager.current
  var textInput by remember(targetSwDp) { mutableStateOf(targetSwDp.toString()) }

  LaunchedEffect(targetSwDp) {
    if (textInput != targetSwDp.toString()) {
      textInput = targetSwDp.toString()
    }
  }

  val presets = listOf(320, 360, 392, 411, 440, 480, 540)
  val scaleRatio =
    displayState.nativeSmallestWidthDp.toFloat() / targetSwDp.coerceAtLeast(240).toFloat()

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
            text = "Developer Smallest Width",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Direct DPI & sw-dp scaling controller",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onRefreshState,
          modifier = Modifier.size(48.dp).testTag("refresh_display_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "Refresh display metrics",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    // Current Display Telemetry Card
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
                imageVector = Icons.Rounded.DisplaySettings,
                contentDescription = null,
                tint = VoltCyan,
              )
              Text(
                text = "Active Screen Metrics",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            val permissionColor =
              if (displayState.hasSecureSettingsPermission) VoltEmerald else VoltAmber
            Surface(
              shape = MaterialTheme.shapes.small,
              color = permissionColor.copy(alpha = 0.16f),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector =
                    if (displayState.hasSecureSettingsPermission) Icons.Rounded.CheckCircle
                    else Icons.Rounded.LockOpen,
                  contentDescription = null,
                  tint = permissionColor,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text =
                    if (displayState.hasSecureSettingsPermission) "Direct Write Active"
                    else "Sandbox / ADB Setup",
                  style = MaterialTheme.typography.labelSmall,
                  color = permissionColor,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DisplayMetricBox(
              label = "Current SW",
              value = "${displayState.currentSmallestWidthDp} dp",
              sub = "Native ${displayState.nativeSmallestWidthDp} dp",
              modifier = Modifier.weight(1f),
            )
            DisplayMetricBox(
              label = "Current Density",
              value = "${displayState.currentDensityDpi} dpi",
              sub = "Native ${displayState.nativeDensityDpi} dpi",
              modifier = Modifier.weight(1f),
            )
            DisplayMetricBox(
              label = "Resolution",
              value = "${displayState.screenWidthPx}×${displayState.screenHeightPx}",
              sub = "Min ${displayState.minDimensionPx} px",
              modifier = Modifier.weight(1.2f),
            )
          }
        }
      }
    }

    // Smallest Width Editor Card
    item {
      Card(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
          Text(
            text = "Set Smallest Width (dp)",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Higher dp makes UI elements smaller & sharper; lower dp enlarges text & controls.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { onTargetSwChanged(targetSwDp - 10) },
              modifier = Modifier.size(48.dp).testTag("sw_minus_button"),
              contentPadding = PaddingValues(0.dp),
            ) {
              Icon(Icons.Rounded.Remove, contentDescription = "Decrease smallest width")
            }

            OutlinedTextField(
              value = textInput,
              onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(3)
                textInput = digits
                digits.toIntOrNull()?.let { parsed ->
                  if (parsed in 280..720) onTargetSwChanged(parsed)
                }
              },
              label = { Text("Smallest Width (dp)") },
              suffix = { Text("dp") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("smallest_width_input"),
            )

            OutlinedButton(
              onClick = { onTargetSwChanged(targetSwDp + 10) },
              modifier = Modifier.size(48.dp).testTag("sw_plus_button"),
              contentPadding = PaddingValues(0.dp),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = "Increase smallest width")
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Slider(
            value = targetSwDp.toFloat(),
            onValueChange = { onTargetSwChanged(it.toInt()) },
            valueRange = 280f..720f,
            modifier = Modifier.fillMaxWidth().testTag("smallest_width_slider"),
          )

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            presets.forEach { preset ->
              FilterChip(
                selected = targetSwDp == preset,
                onClick = { onTargetSwChanged(preset) },
                label = {
                  Text(
                    if (preset == displayState.nativeSmallestWidthDp) "$preset (Native)"
                    else "$preset dp"
                  )
                },
                modifier = Modifier.testTag("sw_preset_$preset"),
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Target DPI & Scale Preview
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Target DPI: $calculatedTargetDpi dpi",
                  style = MaterialTheme.typography.labelMedium,
                  color = VoltCyan,
                )
                Text(
                  text = String.format("UI Scale: %.2fx", scaleRatio),
                  style = MaterialTheme.typography.labelMedium,
                  color = VoltEmerald,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              val previewFontSize = (14f * scaleRatio).coerceIn(10f, 22f).sp
              Text(
                text = "Live Scale Preview: Battery 92% • 3.84V • ${targetSwDp}dp layout",
                fontSize = previewFontSize,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = onApplyDirect,
            modifier = Modifier.fillMaxWidth().testTag("apply_sw_button"),
          ) {
            Icon(Icons.Rounded.DisplaySettings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apply ${targetSwDp}dp Directly")
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(targetSwDp.toString()))
                onShowMessage("Copied ${targetSwDp}dp — opening Developer Settings")
                onOpenDeveloperSettings()
              },
              modifier = Modifier.weight(1f).testTag("open_dev_settings_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Developer Settings")
            }

            OutlinedButton(
              onClick = onResetDirect,
              modifier = Modifier.weight(1f).testTag("reset_sw_button"),
            ) {
              Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Reset Native")
            }
          }

          if (displayState.isRootAvailable) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = { onApplyRoot(targetSwDp) },
              modifier = Modifier.fillMaxWidth().testTag("apply_root_button"),
            ) {
              Icon(
                Icons.Rounded.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Apply ${targetSwDp}dp via Root (su)")
            }
          }
        }
      }
    }

    // Feedback Banner
    if (feedback != null) {
      item {
        val (title, body, accent) =
          when (feedback) {
            is DisplayApplyResult.Success ->
              Triple("Smallest Width Updated", feedback.message, VoltEmerald)
            is DisplayApplyResult.PermissionRequired ->
              Triple(
                "One-Time Permission or Developer Settings Needed",
                "Android restricts apps from modifying system display density without WRITE_SECURE_SETTINGS. Grant once via ADB below for 1-tap switching, or tap Developer Settings above (with ${feedback.targetSwDp} already copied).",
                VoltAmber,
              )
            is DisplayApplyResult.Error ->
              Triple("Could Not Apply Directly", feedback.message, VoltCrimson)
          }

        Card(
          modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              color = accent,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = body,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }

    // ADB Grant & Direct Shell Guide Card
    item {
      AnimatedVisibility(visible = !displayState.hasSecureSettingsPermission) {
        Card(
          modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Rounded.Terminal, contentDescription = null, tint = VoltCyan)
              Text(
                text = "Unlock Direct In-App Switching (No Root)",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            Text(
              text =
                "Run this one-time ADB command from a PC or Wireless Debugging shell to let VoltPulse change Developer Settings Smallest Width directly:",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CommandCopyBox(
              label = "Grant Direct Permission",
              command = adbGrantCommand,
              tag = "copy_adb_grant_button",
              onCopy = {
                clipboardManager.setText(AnnotatedString(adbGrantCommand))
                onShowMessage("Copied ADB grant command")
              },
            )

            CommandCopyBox(
              label = "Or Apply ${targetSwDp}dp Directly via ADB",
              command = adbDensityCommand,
              tag = "copy_adb_density_button",
              onCopy = {
                clipboardManager.setText(AnnotatedString(adbDensityCommand))
                onShowMessage("Copied wm density command")
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DisplayMetricBox(
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
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
      )
      Text(
        text = sub,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun CommandCopyBox(
  label: String,
  command: String,
  tag: String,
  onCopy: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = command,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      IconButton(onClick = onCopy, modifier = Modifier.size(48.dp).testTag(tag)) {
        Icon(
          imageVector = Icons.Rounded.ContentCopy,
          contentDescription = "Copy $label",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
