package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.WarningSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.voltPulseDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "voltpulse_settings")

class SettingsPreferencesRepository(private val context: Context) {

  private object Keys {
    val HIGH_TEMP_ENABLED = booleanPreferencesKey("high_temp_enabled")
    val HIGH_TEMP_THRESHOLD_C = floatPreferencesKey("high_temp_threshold_c")
    val OVER_VOLTAGE_ENABLED = booleanPreferencesKey("over_voltage_enabled")
    val OVER_VOLTAGE_THRESHOLD_MV = intPreferencesKey("over_voltage_threshold_mv")
    val LOW_BATTERY_ENABLED = booleanPreferencesKey("low_battery_enabled")
    val LOW_BATTERY_THRESHOLD_PCT = intPreferencesKey("low_battery_threshold_pct")
    val CHARGE_LIMIT_ENABLED = booleanPreferencesKey("charge_limit_enabled")
    val CHARGE_LIMIT_PCT = intPreferencesKey("charge_limit_pct")
    val SHOW_IN_APP_BANNER = booleanPreferencesKey("show_in_app_banner")
    val SHOW_GAUGE_HEALTH_BADGE = booleanPreferencesKey("show_gauge_health_badge")
    val SYSTEM_NOTIFICATIONS_ENABLED = booleanPreferencesKey("system_notifications_enabled")
    val HAPTIC_ON_CRITICAL = booleanPreferencesKey("haptic_on_critical")
    val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
    val POLLING_INTERVAL_SEC = intPreferencesKey("polling_interval_sec")
    val CUSTOM_DESIGN_CAPACITY_MAH = intPreferencesKey("custom_design_capacity_mah")
    val DARK_THEME_OVERRIDE = stringPreferencesKey("dark_theme_override")
    val SELECTED_REFRESH_RATE_HZ = intPreferencesKey("selected_refresh_rate_hz")
  }

  val settingsFlow: Flow<WarningSettings> =
    context.voltPulseDataStore.data.map { prefs ->
      val themeOverride =
        when (prefs[Keys.DARK_THEME_OVERRIDE]) {
          "DARK" -> true
          "LIGHT" -> false
          else -> null
        }
      WarningSettings(
        highTempEnabled = prefs[Keys.HIGH_TEMP_ENABLED] ?: true,
        highTempThresholdC = prefs[Keys.HIGH_TEMP_THRESHOLD_C] ?: 42f,
        overVoltageEnabled = prefs[Keys.OVER_VOLTAGE_ENABLED] ?: true,
        overVoltageThresholdMv = prefs[Keys.OVER_VOLTAGE_THRESHOLD_MV] ?: 4450,
        lowBatteryEnabled = prefs[Keys.LOW_BATTERY_ENABLED] ?: true,
        lowBatteryThresholdPct = prefs[Keys.LOW_BATTERY_THRESHOLD_PCT] ?: 15,
        chargeLimitEnabled = prefs[Keys.CHARGE_LIMIT_ENABLED] ?: true,
        chargeLimitPct = prefs[Keys.CHARGE_LIMIT_PCT] ?: 85,
        showInAppBanner = prefs[Keys.SHOW_IN_APP_BANNER] ?: true,
        showGaugeHealthBadge = prefs[Keys.SHOW_GAUGE_HEALTH_BADGE] ?: true,
        systemNotificationsEnabled = prefs[Keys.SYSTEM_NOTIFICATIONS_ENABLED] ?: false,
        hapticOnCritical = prefs[Keys.HAPTIC_ON_CRITICAL] ?: true,
        useFahrenheit = prefs[Keys.USE_FAHRENHEIT] ?: false,
        pollingIntervalSec = prefs[Keys.POLLING_INTERVAL_SEC] ?: 2,
        customDesignCapacityMah = prefs[Keys.CUSTOM_DESIGN_CAPACITY_MAH] ?: 0,
        darkThemeOverride = themeOverride,
        selectedRefreshRateHz = prefs[Keys.SELECTED_REFRESH_RATE_HZ]?.takeIf { it > 0 },
      )
    }

  suspend fun updateSettings(transform: (WarningSettings) -> WarningSettings) {
    context.voltPulseDataStore.edit { prefs ->
      val current =
        WarningSettings(
          highTempEnabled = prefs[Keys.HIGH_TEMP_ENABLED] ?: true,
          highTempThresholdC = prefs[Keys.HIGH_TEMP_THRESHOLD_C] ?: 42f,
          overVoltageEnabled = prefs[Keys.OVER_VOLTAGE_ENABLED] ?: true,
          overVoltageThresholdMv = prefs[Keys.OVER_VOLTAGE_THRESHOLD_MV] ?: 4450,
          lowBatteryEnabled = prefs[Keys.LOW_BATTERY_ENABLED] ?: true,
          lowBatteryThresholdPct = prefs[Keys.LOW_BATTERY_THRESHOLD_PCT] ?: 15,
          chargeLimitEnabled = prefs[Keys.CHARGE_LIMIT_ENABLED] ?: true,
          chargeLimitPct = prefs[Keys.CHARGE_LIMIT_PCT] ?: 85,
          showInAppBanner = prefs[Keys.SHOW_IN_APP_BANNER] ?: true,
          showGaugeHealthBadge = prefs[Keys.SHOW_GAUGE_HEALTH_BADGE] ?: true,
          systemNotificationsEnabled = prefs[Keys.SYSTEM_NOTIFICATIONS_ENABLED] ?: false,
          hapticOnCritical = prefs[Keys.HAPTIC_ON_CRITICAL] ?: true,
          useFahrenheit = prefs[Keys.USE_FAHRENHEIT] ?: false,
          pollingIntervalSec = prefs[Keys.POLLING_INTERVAL_SEC] ?: 2,
          customDesignCapacityMah = prefs[Keys.CUSTOM_DESIGN_CAPACITY_MAH] ?: 0,
          darkThemeOverride =
            when (prefs[Keys.DARK_THEME_OVERRIDE]) {
              "DARK" -> true
              "LIGHT" -> false
              else -> null
            },
          selectedRefreshRateHz = prefs[Keys.SELECTED_REFRESH_RATE_HZ]?.takeIf { it > 0 },
        )
      val updated = transform(current)
      prefs[Keys.HIGH_TEMP_ENABLED] = updated.highTempEnabled
      prefs[Keys.HIGH_TEMP_THRESHOLD_C] = updated.highTempThresholdC
      prefs[Keys.OVER_VOLTAGE_ENABLED] = updated.overVoltageEnabled
      prefs[Keys.OVER_VOLTAGE_THRESHOLD_MV] = updated.overVoltageThresholdMv
      prefs[Keys.LOW_BATTERY_ENABLED] = updated.lowBatteryEnabled
      prefs[Keys.LOW_BATTERY_THRESHOLD_PCT] = updated.lowBatteryThresholdPct
      prefs[Keys.CHARGE_LIMIT_ENABLED] = updated.chargeLimitEnabled
      prefs[Keys.CHARGE_LIMIT_PCT] = updated.chargeLimitPct
      prefs[Keys.SHOW_IN_APP_BANNER] = updated.showInAppBanner
      prefs[Keys.SHOW_GAUGE_HEALTH_BADGE] = updated.showGaugeHealthBadge
      prefs[Keys.SYSTEM_NOTIFICATIONS_ENABLED] = updated.systemNotificationsEnabled
      prefs[Keys.HAPTIC_ON_CRITICAL] = updated.hapticOnCritical
      prefs[Keys.USE_FAHRENHEIT] = updated.useFahrenheit
      prefs[Keys.POLLING_INTERVAL_SEC] = updated.pollingIntervalSec
      prefs[Keys.CUSTOM_DESIGN_CAPACITY_MAH] = updated.customDesignCapacityMah
      prefs[Keys.DARK_THEME_OVERRIDE] =
        when (updated.darkThemeOverride) {
          true -> "DARK"
          false -> "LIGHT"
          null -> "SYSTEM"
        }
      if (updated.selectedRefreshRateHz != null && updated.selectedRefreshRateHz > 0) {
        prefs[Keys.SELECTED_REFRESH_RATE_HZ] = updated.selectedRefreshRateHz
      } else {
        prefs.remove(Keys.SELECTED_REFRESH_RATE_HZ)
      }
    }
  }
}
