package com.example.ui.screens

import android.view.Choreographer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.hardware.RefreshRateApplyResult
import com.example.data.hardware.RefreshRateState
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltCrimson
import com.example.ui.theme.VoltCyan
import com.example.ui.theme.VoltEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RefreshRateScreen(
  refreshRateState: RefreshRateState,
  selectedRefreshRateHz: Int?,
  batteryTempC: Float,
  highTempThresholdC: Float,
  feedback: RefreshRateApplyResult?,
  adbGrantCommand: String,
  adbLockCommand: String,
  onSelectMode: (Int?) -> Unit,
  onLockSystemWide: (Int) -> Unit,
  onResetSystemWide: () -> Unit,
  onLockViaRoot: (Int?) -> Unit,
  onOpenDisplaySettings: () -> Unit,
  onOpenDeveloperSettings: () -> Unit,
  onRefreshState: () -> Unit,
  onShowMessage: (String) -> Unit,
) {
  val clipboardManager = LocalClipboardManager.current
  var isBenchmarkRunning by remember { mutableStateOf(true) }
  var measuredFps by remember { mutableIntStateOf(0) }
  var frameTimeMs by remember { mutableFloatStateOf(0f) }
  var pulseProgress by remember { mutableFloatStateOf(0f) }

  // Real Choreographer frame callback — measures actual hardware frame pacing (never faked)
  DisposableEffect(isBenchmarkRunning) {
    if (!isBenchmarkRunning) {
      measuredFps = 0
      frameTimeMs = 0f
      return@DisposableEffect onDispose {}
    }

    val choreographer = Choreographer.getInstance()
    var windowStartNs = 0L
    var lastFrameNs = 0L
    var frameCount = 0
    var active = true

    val callback =
      object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
          if (!active) return
          if (windowStartNs == 0L) {
            windowStartNs = frameTimeNanos
            lastFrameNs = frameTimeNanos
          } else {
            val deltaNs = frameTimeNanos - lastFrameNs
            if (deltaNs > 0L) {
              val instantMs = deltaNs / 1_000_000f
              frameTimeMs = if (frameTimeMs == 0f) instantMs else frameTimeMs * 0.8f + instantMs * 0.2f
              // Advance visual verification bar cleanly based on real elapsed frame time
              pulseProgress = (pulseProgress + (deltaNs / 1_600_000_000f)) % 1f
            }
            lastFrameNs = frameTimeNanos
            frameCount++
            val windowElapsedNs = frameTimeNanos - windowStartNs
            if (windowElapsedNs >= 500_000_000L) {
              measuredFps = ((frameCount * 1_000_000_000.0) / windowElapsedNs).roundToInt()
              windowStartNs = frameTimeNanos
              frameCount = 0
            }
          }
          choreographer.postFrameCallback(this)
        }
      }

    choreographer.postFrameCallback(callback)
    onDispose {
      active = false
      choreographer.removeFrameCallback(callback)
    }
  }

  val targetHzForLock = selectedRefreshRateHz ?: refreshRateState.maxSupportedHz
  val activePanelHzRounded = refreshRateState.activeDisplayHz.roundToInt()
  val isPanelMatchingTarget =
    selectedRefreshRateHz == null || kotlin.math.abs(activePanelHzRounded - selectedRefreshRateHz) <= 2

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
            text = "Refresh Rate & FPS Lock",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Hardware max ${refreshRateState.maxSupportedHz} Hz • Real Choreographer telemetry",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onRefreshState,
          modifier = Modifier.size(48.dp).testTag("refresh_fps_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "Refresh display modes",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    // Real-Time Hardware FPS Telemetry Card
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
                imageVector = Icons.Rounded.Speed,
                contentDescription = "Real FPS meter",
                tint = VoltEmerald,
              )
              Text(
                text = "Real Working FPS Monitor",
                style = MaterialTheme.typography.titleMedium,
              )
            }

            val lockColor =
              when {
                refreshRateState.isSystemLocked -> VoltEmerald
                selectedRefreshRateHz != null -> VoltCyan
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              }
            Surface(
              shape = MaterialTheme.shapes.small,
              color = lockColor.copy(alpha = 0.16f),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector =
                    if (refreshRateState.isSystemLocked) Icons.Rounded.Lock
                    else Icons.Rounded.LockOpen,
                  contentDescription = null,
                  tint = lockColor,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text =
                    when {
                      refreshRateState.isSystemLocked ->
                        "System Locked @ ${refreshRateState.systemLockedHz} Hz"
                      selectedRefreshRateHz != null -> "In-App ${selectedRefreshRateHz} Hz"
                      else -> "Dynamic Auto"
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = lockColor,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FpsStatTile(
              label = "Live Measured FPS",
              value = if (isBenchmarkRunning && measuredFps > 0) "$measuredFps FPS" else if (isBenchmarkRunning) "Measuring..." else "Paused",
              sub =
                if (isBenchmarkRunning && frameTimeMs > 0f)
                  String.format("%.1f ms frame time", frameTimeMs)
                else "Choreographer VSync",
              accent =
                when {
                  measuredFps >= 110 -> VoltEmerald
                  measuredFps >= 80 -> VoltCyan
                  measuredFps > 0 -> VoltAmber
                  else -> MaterialTheme.colorScheme.onSurface
                },
              modifier = Modifier.weight(1f),
            )

            FpsStatTile(
              label = "Panel Active Mode",
              value = String.format("%.1f Hz", refreshRateState.activeDisplayHz),
              sub = "Max ${refreshRateState.maxSupportedHz} Hz supported",
              accent = if (isPanelMatchingTarget) VoltCyan else VoltAmber,
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Real VSync motion rail so user can visually verify 60 vs 90 vs 120 vs 144 Hz smoothness
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Live Frame Pacing Motion Test",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(
                  onClick = { isBenchmarkRunning = !isBenchmarkRunning },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("toggle_fps_benchmark_button"),
                ) {
                  Icon(
                    imageVector =
                      if (isBenchmarkRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isBenchmarkRunning) "Pause meter" else "Start meter",
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(if (isBenchmarkRunning) "Pause" else "Start")
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Box(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                val trackColor = MaterialTheme.colorScheme.outlineVariant
                val indicatorPos =
                  if (pulseProgress <= 0.5f) pulseProgress * 2f else (1f - pulseProgress) * 2f
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val cy = size.height / 2f
                  drawLine(
                    color = trackColor,
                    start = Offset(10.dp.toPx(), cy),
                    end = Offset(size.width - 10.dp.toPx(), cy),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                  )
                  val travel = (size.width - 20.dp.toPx()).coerceAtLeast(1f)
                  val x = 10.dp.toPx() + travel * indicatorPos
                  drawCircle(
                    brush = Brush.radialGradient(listOf(VoltEmerald, VoltCyan)),
                    radius = 9.dp.toPx(),
                    center = Offset(x, cy),
                  )
                }
              }
            }
          }
        }
      }
    }

    // Hardware-Detected FPS Mode Selector
    item {
      Card(
        modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
          Text(
            text = "Supported Display Modes",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text =
              if (refreshRateState.supportedHzOptions.size > 1)
                "Detected ${refreshRateState.supportedHzOptions.joinToString(" / ") { "${it}Hz" }} on this panel. Select a target FPS:"
              else
                "This display panel reports ${refreshRateState.maxSupportedHz} Hz maximum hardware refresh rate.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(12.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = selectedRefreshRateHz == null,
              onClick = { onSelectMode(null) },
              label = { Text("Auto (Dynamic)") },
              modifier = Modifier.testTag("fps_mode_auto"),
            )

            refreshRateState.supportedHzOptions.forEach { hz ->
              FilterChip(
                selected = selectedRefreshRateHz == hz,
                onClick = { onSelectMode(hz) },
                label = {
                  Text(
                    if (hz == refreshRateState.maxSupportedHz) "$hz FPS (Max)"
                    else "$hz FPS"
                  )
                },
                modifier = Modifier.testTag("fps_mode_$hz"),
              )
            }
          }

          if (batteryTempC >= highTempThresholdC && (selectedRefreshRateHz ?: 0) >= 90) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = VoltAmber.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, VoltAmber.copy(alpha = 0.45f)),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.WarningAmber,
                  contentDescription = null,
                  tint = VoltAmber,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text =
                    "Battery is warm (${String.format("%.1f°C", batteryTempC)}). Locking high FPS during gaming may increase heat.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }
    }

    // System-Wide Game Lock Card
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.SportsEsports,
              contentDescription = null,
              tint = VoltCyan,
            )
            Text(
              text = "System-Wide & Gaming FPS Lock",
              style = MaterialTheme.typography.titleMedium,
            )
          }

          Text(
            text =
              "Locks Android's min & peak display refresh rate to ${targetHzForLock} Hz so games and apps don't drop back to 60 Hz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FpsStatTile(
              label = "System Min Rate",
              value =
                refreshRateState.systemMinRefreshRate?.let { "${it.roundToInt()} Hz" } ?: "Default",
              sub = "min_refresh_rate",
              accent = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            FpsStatTile(
              label = "System Peak Rate",
              value =
                refreshRateState.systemPeakRefreshRate?.let { "${it.roundToInt()} Hz" } ?: "Default",
              sub = "peak_refresh_rate",
              accent = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
          }

          Button(
            onClick = { onLockSystemWide(targetHzForLock) },
            modifier = Modifier.fillMaxWidth().testTag("lock_fps_system_button"),
          ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lock $targetHzForLock FPS System-Wide")
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onResetSystemWide,
              modifier = Modifier.weight(1f).testTag("reset_fps_system_button"),
            ) {
              Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Reset Auto")
            }

            OutlinedButton(
              onClick = onOpenDisplaySettings,
              modifier = Modifier.weight(1f).testTag("open_display_settings_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Display Settings")
            }
          }

          if (refreshRateState.isRootAvailable) {
            OutlinedButton(
              onClick = { onLockViaRoot(targetHzForLock) },
              modifier = Modifier.fillMaxWidth().testTag("lock_fps_root_button"),
            ) {
              Icon(
                Icons.Rounded.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                if (refreshRateState.isAndroid15OrHigher)
                  "Lock $targetHzForLock FPS + Bypass 60Hz Game Cap (Root)"
                else "Lock $targetHzForLock FPS via Root (su)"
              )
            }
          }

          if (refreshRateState.isAndroid15OrHigher) {
            OutlinedButton(
              onClick = {
                onShowMessage("In Developer options, enable 'Disable default frame rate for games'")
                onOpenDeveloperSettings()
              },
              modifier = Modifier.fillMaxWidth().testTag("open_dev_game_fps_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Developer Options: Disable 60Hz Game Cap")
            }
          }
        }
      }
    }

    // Feedback Card
    if (feedback != null) {
      item {
        val (title, body, accent) =
          when (feedback) {
            is RefreshRateApplyResult.Success ->
              Triple("System Refresh Rate Updated", feedback.message, VoltEmerald)
            is RefreshRateApplyResult.PermissionRequired ->
              Triple(
                "One-Time ADB Grant Needed for System-Wide Lock",
                "In-app window mode is active, but locking ${feedback.targetHz} Hz across other games requires WRITE_SECURE_SETTINGS (one-time ADB grant below).",
                VoltAmber,
              )
            is RefreshRateApplyResult.Error ->
              Triple("Could Not Lock System Rate", feedback.message, VoltCrimson)
          }
        Card(
          modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(),
          shape = MaterialTheme.shapes.medium,
          colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
          border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (feedback is RefreshRateApplyResult.Success) {
                Icon(
                  Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = accent,
                  modifier = Modifier.size(18.dp),
                )
              }
              Text(text = title, style = MaterialTheme.typography.titleMedium, color = accent)
            }
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

    // ADB Unlock Card when WRITE_SECURE_SETTINGS isn't granted yet
    item {
      AnimatedVisibility(visible = !refreshRateState.hasSecureSettingsPermission) {
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
                text = "Unlock System-Wide Game FPS Lock (No Root)",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            Text(
              text =
                "Run this one-time ADB command once (also unlocks Developer Smallest Width) so VoltPulse can lock min & peak refresh rates for all games:",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FpsCommandCopyRow(
              label = "Grant Direct System Control",
              command = adbGrantCommand,
              tag = "copy_fps_adb_grant",
              onCopy = {
                clipboardManager.setText(AnnotatedString(adbGrantCommand))
                onShowMessage("Copied ADB grant command")
              },
            )

            FpsCommandCopyRow(
              label = "Or Lock $targetHzForLock FPS Immediately via ADB",
              command = adbLockCommand,
              tag = "copy_fps_adb_lock",
              onCopy = {
                clipboardManager.setText(AnnotatedString(adbLockCommand))
                onShowMessage("Copied ADB FPS lock command")
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FpsStatTile(
  label: String,
  value: String,
  sub: String,
  accent: androidx.compose.ui.graphics.Color,
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
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = accent,
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
private fun FpsCommandCopyRow(
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
