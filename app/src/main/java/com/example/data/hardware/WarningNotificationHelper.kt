package com.example.data.hardware

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ActiveWarning
import com.example.data.model.WarningSeverity

class WarningNotificationHelper(private val context: Context) {

  private val notificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  private var lastNotifiedAtMs: Long = 0L

  init {
    ensureChannel()
  }

  fun ensureChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
          )
          .apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
          }
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun notifyWarning(warning: ActiveWarning, vibrate: Boolean, force: Boolean = false) {
    val now = System.currentTimeMillis()
    if (!force && now - lastNotifiedAtMs < COOLDOWN_MS) return

    if (hasNotificationPermission()) {
      lastNotifiedAtMs = now
      val launchIntent =
        Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
      val pendingIntent =
        PendingIntent.getActivity(
          context,
          0,
          launchIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

      val priority =
        if (warning.severity == WarningSeverity.CRITICAL) NotificationCompat.PRIORITY_HIGH
        else NotificationCompat.PRIORITY_DEFAULT

      val notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_launcher_foreground)
          .setContentTitle(warning.title)
          .setContentText("${warning.message} (${warning.metricLabel})")
          .setStyle(
            NotificationCompat.BigTextStyle()
              .bigText("${warning.message}\nMeasured: ${warning.metricLabel}")
          )
          .setPriority(priority)
          .setAutoCancel(true)
          .setContentIntent(pendingIntent)
          .build()

      notificationManager.notify(NOTIFICATION_ID, notification)
    }

    if (vibrate && warning.severity == WarningSeverity.CRITICAL) {
      triggerHapticPulse()
    }
  }

  fun triggerHapticPulse() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator?.vibrate(
          VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE)
        )
      } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createOneShot(180L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(180L)
        }
      }
    } catch (_: Throwable) {
      // Ignore if hardware vibrator unavailable
    }
  }

  companion object {
    const val CHANNEL_ID = "volt_pulse_warnings"
    private const val NOTIFICATION_ID = 4102
    private const val COOLDOWN_MS = 45_000L
  }
}
