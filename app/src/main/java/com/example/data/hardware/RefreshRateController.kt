package com.example.data.hardware

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

data class RefreshRateModeInfo(
  val modeId: Int,
  val roundedHz: Int,
  val exactHz: Float,
  val widthPx: Int,
  val heightPx: Int,
)

data class RefreshRateState(
  val supportedModes: List<RefreshRateModeInfo> = emptyList(),
  val supportedHzOptions: List<Int> = emptyList(),
  val maxSupportedHz: Int = 60,
  val activeDisplayHz: Float = 60f,
  val activeModeId: Int = 0,
  val systemMinRefreshRate: Float? = null,
  val systemPeakRefreshRate: Float? = null,
  val hasSecureSettingsPermission: Boolean = false,
  val isRootAvailable: Boolean = false,
  val isAndroid15OrHigher: Boolean = Build.VERSION.SDK_INT >= 35,
) {
  val isSystemLocked: Boolean
    get() {
      val minR = systemMinRefreshRate ?: return false
      val peakR = systemPeakRefreshRate ?: return false
      return minR > 0f && peakR > 0f && abs(minR - peakR) < 2f
    }

  val systemLockedHz: Int?
    get() = if (isSystemLocked) systemPeakRefreshRate?.roundToInt() else null
}

sealed interface RefreshRateApplyResult {
  data class Success(val message: String, val lockedHz: Int?) : RefreshRateApplyResult
  data class PermissionRequired(val targetHz: Int) : RefreshRateApplyResult
  data class Error(val message: String) : RefreshRateApplyResult
}

class RefreshRateController(private val context: Context) {

  private val displayManager: DisplayManager? =
    context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

  fun getDefaultDisplay(): Display? {
    val fromManager = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
    if (fromManager != null) return fromManager
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    @Suppress("DEPRECATION")
    return wm?.defaultDisplay
  }

  fun readRefreshRateState(): RefreshRateState {
    val display = getDefaultDisplay()
    val currentMode = display?.mode
    val activeW = currentMode?.physicalWidth ?: 0
    val activeH = currentMode?.physicalHeight ?: 0

    val rawModes = display?.supportedModes?.toList().orEmpty()
    // Filter to modes matching active panel resolution so we don't duplicate 60Hz/120Hz across scaled resolutions
    val matchingResModes =
      if (activeW > 0 && activeH > 0) {
        val filtered = rawModes.filter { it.physicalWidth == activeW && it.physicalHeight == activeH }
        filtered.ifEmpty { rawModes }
      } else {
        rawModes
      }

    // Group by rounded Hz (e.g., 59.94 -> 60, 120.0 -> 120, 144.0 -> 144)
    val distinctByHz =
      matchingResModes
        .groupBy { normalizeHz(it.refreshRate) }
        .mapNotNull { (hz, modes) ->
          val best = modes.maxByOrNull { it.refreshRate } ?: return@mapNotNull null
          RefreshRateModeInfo(
            modeId = best.modeId,
            roundedHz = hz,
            exactHz = best.refreshRate,
            widthPx = best.physicalWidth,
            heightPx = best.physicalHeight,
          )
        }
        .sortedBy { it.roundedHz }

    val hzOptions = distinctByHz.map { it.roundedHz }
    val maxHz = hzOptions.maxOrNull() ?: normalizeHz(currentMode?.refreshRate ?: 60f)

    val minRate = readSystemFloatSetting("min_refresh_rate")
    val peakRate = readSystemFloatSetting("peak_refresh_rate")

    return RefreshRateState(
      supportedModes = distinctByHz,
      supportedHzOptions = hzOptions,
      maxSupportedHz = maxHz,
      activeDisplayHz = currentMode?.refreshRate ?: 60f,
      activeModeId = currentMode?.modeId ?: 0,
      systemMinRefreshRate = minRate,
      systemPeakRefreshRate = peakRate,
      hasSecureSettingsPermission = hasWriteSecureSettings(),
      isRootAvailable = checkRootBinary(),
      isAndroid15OrHigher = Build.VERSION.SDK_INT >= 35,
    )
  }

  fun findModeForHz(targetHz: Int): RefreshRateModeInfo? {
    val state = readRefreshRateState()
    return state.supportedModes.firstOrNull { it.roundedHz == targetHz }
      ?: state.supportedModes.minByOrNull { abs(it.roundedHz - targetHz) }
  }

  fun applySystemRefreshRateLock(targetHz: Int?): RefreshRateApplyResult {
    val state = readRefreshRateState()
    if (!hasWriteSecureSettings()) {
      return RefreshRateApplyResult.PermissionRequired(targetHz ?: state.maxSupportedHz)
    }

    return try {
      val cr = context.contentResolver
      if (targetHz == null) {
        // Restore dynamic/adaptive refresh rate (min = 0, peak = max supported)
        val minOk = Settings.System.putFloat(cr, "min_refresh_rate", 0f)
        val peakOk = Settings.System.putFloat(cr, "peak_refresh_rate", state.maxSupportedHz.toFloat())
        if (minOk || peakOk) {
          RefreshRateApplyResult.Success(
            message = "Restored dynamic adaptive refresh rate (0–${state.maxSupportedHz} Hz)",
            lockedHz = null,
          )
        } else {
          RefreshRateApplyResult.Error("ROM rejected min/peak_refresh_rate update")
        }
      } else {
        val exactRate = findModeForHz(targetHz)?.exactHz ?: targetHz.toFloat()
        val minOk = Settings.System.putFloat(cr, "min_refresh_rate", exactRate)
        val peakOk = Settings.System.putFloat(cr, "peak_refresh_rate", exactRate)
        if (minOk && peakOk) {
          RefreshRateApplyResult.Success(
            message = "System display locked at ${targetHz} Hz (${String.format("%.1f", exactRate)} Hz)",
            lockedHz = targetHz,
          )
        } else {
          RefreshRateApplyResult.Error("System rejected locking refresh rate on this ROM")
        }
      }
    } catch (t: Throwable) {
      RefreshRateApplyResult.Error("Failed to write refresh rate setting: ${t.message ?: "Restricted"}")
    }
  }

  fun applySystemRefreshRateViaRoot(targetHz: Int?): RefreshRateApplyResult {
    val state = readRefreshRateState()
    return try {
      val cmd =
        if (targetHz == null) {
          "settings put system min_refresh_rate 0 && settings put system peak_refresh_rate ${state.maxSupportedHz}"
        } else {
          val exactRate = findModeForHz(targetHz)?.exactHz ?: targetHz.toFloat()
          val gameOverride =
            if (Build.VERSION.SDK_INT >= 35)
              " && setprop debug.graphics.game_default_frame_rate.disabled true"
            else ""
          "settings put system min_refresh_rate $exactRate && settings put system peak_refresh_rate $exactRate$gameOverride"
        }
      val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
      val exit = process.waitFor()
      if (exit == 0) {
        RefreshRateApplyResult.Success(
          message =
            if (targetHz == null) "Restored dynamic refresh rate via root"
            else "Locked system & games at ${targetHz} Hz via root",
          lockedHz = targetHz,
        )
      } else {
        RefreshRateApplyResult.Error("Root command exited with code $exit")
      }
    } catch (t: Throwable) {
      RefreshRateApplyResult.Error("Root unavailable: ${t.message ?: "su not found"}")
    }
  }

  fun openSystemDisplaySettings(): Boolean {
    return try {
      val intent =
        Intent(Settings.ACTION_DISPLAY_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
      context.startActivity(intent)
      true
    } catch (_: Throwable) {
      false
    }
  }

  fun openDeveloperOptions(): Boolean {
    return try {
      val intent =
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      context.startActivity(intent)
      true
    } catch (_: Throwable) {
      false
    }
  }

  fun adbGrantCommand(): String =
    "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

  fun adbLockFpsCommand(targetHz: Int): String {
    val gameFlag =
      if (Build.VERSION.SDK_INT >= 35)
        " && adb shell setprop debug.graphics.game_default_frame_rate.disabled true"
      else ""
    return "adb shell settings put system min_refresh_rate $targetHz && adb shell settings put system peak_refresh_rate $targetHz$gameFlag"
  }

  private fun readSystemFloatSetting(key: String): Float? {
    return try {
      val value = Settings.System.getFloat(context.contentResolver, key, -1f)
      if (value >= 0f) value else null
    } catch (_: Throwable) {
      null
    }
  }

  private fun hasWriteSecureSettings(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.WRITE_SECURE_SETTINGS,
    ) == PackageManager.PERMISSION_GRANTED
  }

  private fun checkRootBinary(): Boolean {
    val paths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su")
    return paths.any {
      try {
        File(it).exists()
      } catch (_: Throwable) {
        false
      }
    }
  }

  companion object {
    fun normalizeHz(rawHz: Float): Int {
      val common = listOf(24, 30, 48, 50, 60, 72, 90, 120, 144, 165, 240)
      val closest = common.minByOrNull { abs(it - rawHz) }
      return if (closest != null && abs(closest - rawHz) <= 2.2f) closest
      else rawHz.roundToInt()
    }
  }
}
