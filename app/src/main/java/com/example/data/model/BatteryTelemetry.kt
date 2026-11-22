package com.example.data.model

enum class ChargingStateLabel(val displayName: String) {
  CHARGING("Charging"),
  DISCHARGING("Discharging"),
  FULL("Full"),
  NOT_CHARGING("Not Charging"),
  UNKNOWN("Unknown"),
}

enum class PluggedSourceLabel(val displayName: String) {
  AC("AC Adapter"),
  USB("USB Port"),
  WIRELESS("Wireless"),
  DOCK("Dock"),
  BATTERY("On Battery"),
}

enum class HardwareHealthLabel(val displayName: String, val isHealthy: Boolean) {
  GOOD("Good", true),
  OVERHEAT("Overheat", false),
  DEAD("Dead Cell", false),
  OVER_VOLTAGE("Over Voltage", false),
  UNSPECIFIED_FAILURE("Hardware Failure", false),
  COLD("Too Cold", false),
  UNKNOWN("Unknown", true),
}

enum class HealthGrade(val label: String) {
  EXCELLENT("Excellent"),
  GOOD("Good"),
  FAIR("Fair"),
  DEGRADED("Degraded"),
  CALIBRATING("Calibrating"),
}

enum class WarningSeverity {
  CRITICAL,
  WARNING,
  INFO,
}

data class ActiveWarning(
  val id: String,
  val title: String,
  val message: String,
  val severity: WarningSeverity,
  val metricLabel: String,
)

data class TelemetrySample(
  val timestampMs: Long,
  val currentMa: Int,
  val powerWatts: Float,
  val voltageMv: Int,
  val temperatureC: Float,
  val levelPct: Int,
  val isCharging: Boolean,
)

data class BatteryTelemetry(
  val levelPct: Int = 0,
  val scale: Int = 100,
  val chargingState: ChargingStateLabel = ChargingStateLabel.UNKNOWN,
  val pluggedSource: PluggedSourceLabel = PluggedSourceLabel.BATTERY,
  val hardwareHealth: HardwareHealthLabel = HardwareHealthLabel.UNKNOWN,
  val temperatureC: Float = 0f,
  val voltageMv: Int = 0,
  val technology: String = "Li-ion",
  val isPresent: Boolean = true,
  val cycleCount: Int? = null,
  val currentNowMa: Int? = null,
  val currentAvgMa: Int? = null,
  val chargeCounterMah: Int? = null,
  val energyCounterMwh: Long? = null,
  val kernelChargeFullMah: Int? = null,
  val designCapacityMah: Int = 0,
  val designCapacitySource: String = "Auto",
  val estimatedFullCapacityMah: Int? = null,
  val estimatedHealthPct: Int? = null,
  val healthGrade: HealthGrade = HealthGrade.CALIBRATING,
  val powerWatts: Float = 0f,
  val chargeTimeRemainingMs: Long? = null,
  val deviceModel: String = "",
  val manufacturer: String = "",
  val androidVersion: String = "",
  val timestampMs: Long = 0L,
)
