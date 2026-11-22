package com.example.data.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

data class DisplayWidthState(
  val currentSmallestWidthDp: Int = 360,
  val currentDensityDpi: Int = 420,
  val nativeDensityDpi: Int = 420,
  val nativeSmallestWidthDp: Int = 360,
  val screenWidthPx: Int = 1080,
  val screenHeightPx: Int = 2400,
  val minDimensionPx: Int = 1080,
  val forcedDensitySetting: String? = null,
  val hasSecureSettingsPermission: Boolean = false,
  val isRootAvailable: Boolean = false,
)

sealed interface DisplayApplyResult {
  data class Success(val message: String, val appliedDpi: Int, val appliedSwDp: Int) : DisplayApplyResult
  data class PermissionRequired(val targetSwDp: Int, val targetDpi: Int) : DisplayApplyResult
  data class Error(val message: String) : DisplayApplyResult
}

class DisplayWidthController(private val context: Context) {

  fun readDisplayState(): DisplayWidthState {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val (widthPx, heightPx) =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.currentWindowMetrics.bounds
        bounds.width() to bounds.height()
      } else {
        @Suppress("DEPRECATION")
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        metrics.widthPixels to metrics.heightPixels
      }

    val currentDensityDpi = context.resources.displayMetrics.densityDpi
    val nativeDensityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
    val minPx = min(widthPx, heightPx).coerceAtLeast(320)
    val currentSwDp = context.resources.configuration.smallestScreenWidthDp
    val nativeSwDp = calculateSmallestWidthForDpi(minPx, nativeDensityDpi)

    val forcedSetting =
      try {
        Settings.Secure.getString(context.contentResolver, "display_density_forced")
      } catch (_: Throwable) {
        null
      }

    return DisplayWidthState(
      currentSmallestWidthDp = currentSwDp,
      currentDensityDpi = currentDensityDpi,
      nativeDensityDpi = nativeDensityDpi,
      nativeSmallestWidthDp = nativeSwDp,
      screenWidthPx = widthPx,
      screenHeightPx = heightPx,
      minDimensionPx = minPx,
      forcedDensitySetting = forcedSetting,
      hasSecureSettingsPermission = hasWriteSecureSettingsPermission(),
      isRootAvailable = checkRootBinary(),
    )
  }

  fun hasWriteSecureSettingsPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.WRITE_SECURE_SETTINGS,
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun calculateDpiForSmallestWidth(minDimensionPx: Int, targetSmallestWidthDp: Int): Int {
    val clampedSw = targetSmallestWidthDp.coerceIn(240, 1000)
    return ((minDimensionPx * 160f) / clampedSw).roundToInt().coerceIn(120, 960)
  }

  fun calculateSmallestWidthForDpi(minDimensionPx: Int, densityDpi: Int): Int {
    val safeDpi = densityDpi.coerceAtLeast(120)
    return ((minDimensionPx * 160f) / safeDpi).roundToInt()
  }

  fun applySmallestWidth(targetSwDp: Int): DisplayApplyResult {
    val state = readDisplayState()
    val targetDpi = calculateDpiForSmallestWidth(state.minDimensionPx, targetSwDp)

    if (hasWriteSecureSettingsPermission()) {
      return applyDensityWithSecurePermission(targetDpi, targetSwDp)
    }

    return DisplayApplyResult.PermissionRequired(targetSwDp = targetSwDp, targetDpi = targetDpi)
  }

  fun resetToNativeSmallestWidth(): DisplayApplyResult {
    val state = readDisplayState()
    if (hasWriteSecureSettingsPermission()) {
      return clearDensityWithSecurePermission(state.nativeDensityDpi, state.nativeSmallestWidthDp)
    }
    return DisplayApplyResult.PermissionRequired(
      targetSwDp = state.nativeSmallestWidthDp,
      targetDpi = state.nativeDensityDpi,
    )
  }

  fun applyViaRoot(targetSwDp: Int?): DisplayApplyResult {
    val state = readDisplayState()
    return try {
      val cmd =
        if (targetSwDp == null) {
          "wm density reset"
        } else {
          val dpi = calculateDpiForSmallestWidth(state.minDimensionPx, targetSwDp)
          "wm density $dpi"
        }
      val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
      val exitCode = process.waitFor()
      if (exitCode == 0) {
        val appliedSw = targetSwDp ?: state.nativeSmallestWidthDp
        val appliedDpi =
          if (targetSwDp == null) state.nativeDensityDpi
          else calculateDpiForSmallestWidth(state.minDimensionPx, targetSwDp)
        DisplayApplyResult.Success(
          message =
            if (targetSwDp == null) "Reset smallest width to native (${appliedSw}dp)"
            else "Applied ${appliedSw}dp (${appliedDpi} dpi) via root",
          appliedDpi = appliedDpi,
          appliedSwDp = appliedSw,
        )
      } else {
        DisplayApplyResult.Error("Root command denied or exited with code $exitCode")
      }
    } catch (t: Throwable) {
      DisplayApplyResult.Error("Root access unavailable: ${t.message ?: "su not found"}")
    }
  }

  @SuppressLint("PrivateApi")
  private fun applyDensityWithSecurePermission(targetDpi: Int, targetSwDp: Int): DisplayApplyResult {
    var wmApplied = false
    try {
      val wmGlobalClass = Class.forName("android.view.WindowManagerGlobal")
      val getServiceMethod = wmGlobalClass.getMethod("getWindowManagerService")
      val wmService = getServiceMethod.invoke(null)
      if (wmService != null) {
        val userId = android.os.Process.myUserHandle().hashCode()
        val methods = wmService.javaClass.methods
        val setDensityMethod =
          methods.firstOrNull {
            it.name == "setForcedDisplayDensityForUser" && it.parameterTypes.size == 3
          }
        if (setDensityMethod != null) {
          setDensityMethod.invoke(wmService, Display.DEFAULT_DISPLAY, targetDpi, userId)
          wmApplied = true
        }
      }
    } catch (_: Throwable) {
      // Fallback to Settings.Secure below
    }

    val secureSaved =
      try {
        Settings.Secure.putString(
          context.contentResolver,
          "display_density_forced",
          targetDpi.toString(),
        )
      } catch (_: Throwable) {
        false
      }

    return if (wmApplied || secureSaved) {
      DisplayApplyResult.Success(
        message = "Applied ${targetSwDp}dp (${targetDpi} dpi) directly",
        appliedDpi = targetDpi,
        appliedSwDp = targetSwDp,
      )
    } else {
      DisplayApplyResult.Error("System rejected display_density_forced update on this ROM")
    }
  }

  @SuppressLint("PrivateApi")
  private fun clearDensityWithSecurePermission(nativeDpi: Int, nativeSwDp: Int): DisplayApplyResult {
    var wmCleared = false
    try {
      val wmGlobalClass = Class.forName("android.view.WindowManagerGlobal")
      val getServiceMethod = wmGlobalClass.getMethod("getWindowManagerService")
      val wmService = getServiceMethod.invoke(null)
      if (wmService != null) {
        val userId = android.os.Process.myUserHandle().hashCode()
        val clearMethod =
          wmService.javaClass.methods.firstOrNull {
            it.name == "clearForcedDisplayDensityForUser" && it.parameterTypes.size == 2
          }
        if (clearMethod != null) {
          clearMethod.invoke(wmService, Display.DEFAULT_DISPLAY, userId)
          wmCleared = true
        }
      }
    } catch (_: Throwable) {
      // Fallback to Settings.Secure below
    }

    val secureCleared =
      try {
        Settings.Secure.putString(context.contentResolver, "display_density_forced", null)
      } catch (_: Throwable) {
        false
      }

    return if (wmCleared || secureCleared) {
      DisplayApplyResult.Success(
        message = "Reset display to native ${nativeSwDp}dp (${nativeDpi} dpi)",
        appliedDpi = nativeDpi,
        appliedSwDp = nativeSwDp,
      )
    } else {
      DisplayApplyResult.Error("Could not reset display_density_forced on this ROM")
    }
  }

  fun openDeveloperOptions(): Boolean {
    val devIntent =
      Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    return try {
      context.startActivity(devIntent)
      true
    } catch (_: Throwable) {
      try {
        val fallback =
          Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(fallback)
        false
      } catch (_: Throwable) {
        false
      }
    }
  }

  fun adbGrantCommand(): String =
    "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

  fun adbDensityCommand(targetSwDp: Int): String {
    val state = readDisplayState()
    val dpi = calculateDpiForSmallestWidth(state.minDimensionPx, targetSwDp)
    return "adb shell wm density $dpi"
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
}
