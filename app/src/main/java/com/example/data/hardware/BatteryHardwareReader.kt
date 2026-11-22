package com.example.data.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.example.data.model.BatteryTelemetry
import com.example.data.model.ChargingStateLabel
import com.example.data.model.HardwareHealthLabel
import com.example.data.model.HealthGrade
import com.example.data.model.PluggedSourceLabel
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

class BatteryHardwareReader(private val context: Context) {

  private val batteryManager: BatteryManager? =
    context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

  fun readTelemetry(customDesignCapacityMah: Int = 0): BatteryTelemetry {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
    val levelPct =
      if (level >= 0) ((level * 100f) / scale).roundToInt().coerceIn(0, 100)
      else (batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0).coerceIn(0, 100)

    val statusInt = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
      ?: BatteryManager.BATTERY_STATUS_UNKNOWN
    val chargingState =
      when (statusInt) {
        BatteryManager.BATTERY_STATUS_CHARGING -> ChargingStateLabel.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingStateLabel.DISCHARGING
        BatteryManager.BATTERY_STATUS_FULL -> ChargingStateLabel.FULL
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingStateLabel.NOT_CHARGING
        else ->
          if (batteryManager?.isCharging == true) ChargingStateLabel.CHARGING
          else ChargingStateLabel.UNKNOWN
      }

    val pluggedInt = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val pluggedSource =
      when {
        (pluggedInt and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> PluggedSourceLabel.AC
        (pluggedInt and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> PluggedSourceLabel.USB
        (pluggedInt and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 -> PluggedSourceLabel.WIRELESS
        (pluggedInt and BatteryManager.BATTERY_PLUGGED_DOCK) != 0 -> PluggedSourceLabel.DOCK
        else -> PluggedSourceLabel.BATTERY
      }

    val healthInt = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
      ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
    val hardwareHealth =
      when (healthInt) {
        BatteryManager.BATTERY_HEALTH_GOOD -> HardwareHealthLabel.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> HardwareHealthLabel.OVERHEAT
        BatteryManager.BATTERY_HEALTH_DEAD -> HardwareHealthLabel.DEAD
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> HardwareHealthLabel.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> HardwareHealthLabel.UNSPECIFIED_FAILURE
        BatteryManager.BATTERY_HEALTH_COLD -> HardwareHealthLabel.COLD
        else -> HardwareHealthLabel.UNKNOWN
      }

    val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val temperatureC = tempTenths / 10f

    val rawVoltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    val voltageMv = if (rawVoltage in 1..20) rawVoltage * 1000 else rawVoltage

    val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() } ?: "Li-ion"
    val isPresent = intent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: true

    val apiCycleCount: Int? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        intent?.getIntExtra("android.os.extra.CYCLE_COUNT", -1)?.takeIf { it >= 0 }
      } else {
        null
      }
    val cycleCount = apiCycleCount ?: readSysfsInt(CYCLE_COUNT_PATHS)?.takeIf { it >= 0 }

    val rawCurrentNowUa = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val currentNowMa = normalizeCurrentMa(rawCurrentNowUa, chargingState)

    val rawCurrentAvgUa = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
    val currentAvgMa = normalizeCurrentMa(rawCurrentAvgUa, chargingState)

    val rawChargeCounterUah = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
    val chargeCounterMah =
      rawChargeCounterUah?.takeIf { it != Int.MIN_VALUE && it > 0 }?.let { it / 1000 }

    val rawEnergyCounterNwh = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
    val energyCounterMwh =
      rawEnergyCounterNwh?.takeIf { it != Long.MIN_VALUE && it > 0L }?.let { it / 1_000_000L }

    val kernelChargeFullMah = readSysfsCapacityMah(CHARGE_FULL_PATHS)
    val (autoDesignMah, autoSource) = detectDesignCapacity()
    val designCapacityMah = if (customDesignCapacityMah > 0) customDesignCapacityMah else autoDesignMah
    val designSource = if (customDesignCapacityMah > 0) "User Calibrated" else autoSource

    val estimatedFullMah =
      when {
        kernelChargeFullMah != null && kernelChargeFullMah in 500..25000 -> kernelChargeFullMah
        chargeCounterMah != null && levelPct >= 15 && chargeCounterMah > 150 ->
          ((chargeCounterMah / (levelPct / 100f)).roundToInt()).takeIf { it in 500..25000 }
        else -> null
      }

    val estimatedHealthPct =
      if (estimatedFullMah != null && designCapacityMah > 0) {
        ((estimatedFullMah * 100f) / designCapacityMah).roundToInt().coerceIn(20, 105)
      } else {
        null
      }

    val healthGrade =
      when {
        !hardwareHealth.isHealthy -> HealthGrade.DEGRADED
        estimatedHealthPct == null -> HealthGrade.CALIBRATING
        estimatedHealthPct >= 90 -> HealthGrade.EXCELLENT
        estimatedHealthPct >= 80 -> HealthGrade.GOOD
        estimatedHealthPct >= 70 -> HealthGrade.FAIR
        else -> HealthGrade.DEGRADED
      }

    val effectiveCurrentMa = currentNowMa ?: currentAvgMa ?: 0
    val powerWatts =
      if (voltageMv > 0 && effectiveCurrentMa != 0) {
        (voltageMv / 1000f) * (abs(effectiveCurrentMa) / 1000f)
      } else {
        0f
      }

    val chargeTimeRemainingMs =
      if (chargingState == ChargingStateLabel.CHARGING) {
        val osEstimate =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            batteryManager?.computeChargeTimeRemaining()?.takeIf { it > 0L }
          } else {
            null
          }
        osEstimate
          ?: run {
            val netChargeMa = effectiveCurrentMa
            if (netChargeMa > 80 && designCapacityMah > 0 && levelPct < 100) {
              val remainingMah = designCapacityMah * ((100 - levelPct) / 100f)
              ((remainingMah / netChargeMa) * 3_600_000f).toLong().coerceAtLeast(60_000L)
            } else {
              null
            }
          }
      } else {
        null
      }

    return BatteryTelemetry(
      levelPct = levelPct,
      scale = scale,
      chargingState = chargingState,
      pluggedSource = pluggedSource,
      hardwareHealth = hardwareHealth,
      temperatureC = temperatureC,
      voltageMv = voltageMv,
      technology = technology,
      isPresent = isPresent,
      cycleCount = cycleCount,
      currentNowMa = currentNowMa,
      currentAvgMa = currentAvgMa,
      chargeCounterMah = chargeCounterMah,
      energyCounterMwh = energyCounterMwh,
      kernelChargeFullMah = kernelChargeFullMah,
      designCapacityMah = designCapacityMah,
      designCapacitySource = designSource,
      estimatedFullCapacityMah = estimatedFullMah,
      estimatedHealthPct = estimatedHealthPct,
      healthGrade = healthGrade,
      powerWatts = powerWatts,
      chargeTimeRemainingMs = chargeTimeRemainingMs,
      deviceModel = Build.MODEL.orEmpty(),
      manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() },
      androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
      timestampMs = System.currentTimeMillis(),
    )
  }

  private fun normalizeCurrentMa(rawUa: Int?, state: ChargingStateLabel): Int? {
    if (rawUa == null || rawUa == Int.MIN_VALUE || rawUa == Int.MAX_VALUE) return null
    val ma = rawUa / 1000
    if (ma == 0) return 0
    return when (state) {
      ChargingStateLabel.CHARGING -> abs(ma)
      ChargingStateLabel.DISCHARGING -> -abs(ma)
      else -> ma
    }
  }

  @SuppressLint("PrivateApi")
  private fun detectDesignCapacity(): Pair<Int, String> {
    readSysfsCapacityMah(DESIGN_CAPACITY_PATHS)?.let { sysfsMah ->
      if (sysfsMah in 500..25000) {
        return sysfsMah to "Kernel Sysfs"
      }
    }

    try {
      val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
      val constructor = powerProfileClass.getConstructor(Context::class.java)
      val profile = constructor.newInstance(context)
      val method = powerProfileClass.getMethod("getAveragePower", String::class.java)
      val capacityDouble = method.invoke(profile, "battery.capacity") as? Double
      if (capacityDouble != null && capacityDouble > 500.0) {
        return capacityDouble.roundToInt() to "OEM PowerProfile"
      }
    } catch (_: Throwable) {
      // Fallback safely when OEM restricts private API reflection
    }

    return 4500 to "Standard Estimate"
  }

  private fun readSysfsCapacityMah(paths: List<String>): Int? {
    val raw = readSysfsInt(paths) ?: return null
    return when {
      raw >= 500_000 -> raw / 1000
      raw in 500..30_000 -> raw
      else -> null
    }
  }

  private fun readSysfsInt(paths: List<String>): Int? {
    for (path in paths) {
      try {
        val file = File(path)
        if (file.exists() && file.canRead()) {
          val text = file.readText().trim()
          val parsed = text.toIntOrNull()
          if (parsed != null) return parsed
        }
      } catch (_: Throwable) {
        // SELinux or permission denial on kernel node — skip safely
      }
    }
    return null
  }

  companion object {
    private val DESIGN_CAPACITY_PATHS =
      listOf(
        "/sys/class/power_supply/battery/charge_full_design",
        "/sys/class/power_supply/bms/charge_full_design",
        "/sys/class/power_supply/battery/batt_full_capacity",
      )

    private val CHARGE_FULL_PATHS =
      listOf(
        "/sys/class/power_supply/battery/charge_full",
        "/sys/class/power_supply/bms/charge_full",
        "/sys/class/power_supply/battery/fg_fullcapnom",
      )

    private val CYCLE_COUNT_PATHS =
      listOf(
        "/sys/class/power_supply/battery/cycle_count",
        "/sys/class/power_supply/bms/cycle_count",
      )
  }
}
