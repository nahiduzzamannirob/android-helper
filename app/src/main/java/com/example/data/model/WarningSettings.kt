package com.example.data.model

data class WarningSettings(
  val highTempEnabled: Boolean = true,
  val highTempThresholdC: Float = 42f,
  val overVoltageEnabled: Boolean = true,
  val overVoltageThresholdMv: Int = 4450,
  val lowBatteryEnabled: Boolean = true,
  val lowBatteryThresholdPct: Int = 15,
  val chargeLimitEnabled: Boolean = true,
  val chargeLimitPct: Int = 85,
  val showInAppBanner: Boolean = true,
  val showGaugeHealthBadge: Boolean = true,
  val systemNotificationsEnabled: Boolean = false,
  val hapticOnCritical: Boolean = true,
  val useFahrenheit: Boolean = false,
  val pollingIntervalSec: Int = 2,
  val customDesignCapacityMah: Int = 0,
  val darkThemeOverride: Boolean? = null,
  val selectedRefreshRateHz: Int? = null,
)
