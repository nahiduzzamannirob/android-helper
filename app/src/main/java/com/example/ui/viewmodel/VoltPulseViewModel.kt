package com.example.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.hardware.BatteryHardwareReader
import com.example.data.hardware.DisplayApplyResult
import com.example.data.hardware.DisplayWidthController
import com.example.data.hardware.DisplayWidthState
import com.example.data.hardware.RefreshRateApplyResult
import com.example.data.hardware.RefreshRateController
import com.example.data.hardware.RefreshRateModeInfo
import com.example.data.hardware.RefreshRateState
import com.example.data.hardware.WarningNotificationHelper
import com.example.data.local.ChargingSessionEntity
import com.example.data.local.SettingsPreferencesRepository
import com.example.data.local.VoltPulseDatabase
import com.example.data.model.ActiveWarning
import com.example.data.model.BatteryTelemetry
import com.example.data.model.ChargingStateLabel
import com.example.data.model.PluggedSourceLabel
import com.example.data.model.TelemetrySample
import com.example.data.model.WarningSettings
import com.example.data.model.WarningSeverity
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChargingAggregateStats(
  val sessionCount: Int = 0,
  val maxPeakWatts: Float = 0f,
  val maxTempRecordedC: Float = 0f,
  val avgPeakCurrentMa: Int = 0,
)

data class VoltPulseUiState(
  val telemetry: BatteryTelemetry = BatteryTelemetry(),
  val settings: WarningSettings = WarningSettings(),
  val samples: List<TelemetrySample> = emptyList(),
  val activeWarnings: List<ActiveWarning> = emptyList(),
  val recentSessions: List<ChargingSessionEntity> = emptyList(),
  val aggregateStats: ChargingAggregateStats = ChargingAggregateStats(),
  val displayState: DisplayWidthState = DisplayWidthState(),
  val targetSmallestWidthDp: Int = 360,
  val displayFeedback: DisplayApplyResult? = null,
  val refreshRateState: RefreshRateState = RefreshRateState(),
  val refreshRateFeedback: RefreshRateApplyResult? = null,
  val statusToastMessage: String? = null,
)

class VoltPulseViewModel(application: Application) : AndroidViewModel(application) {

  private val hardwareReader = BatteryHardwareReader(application)
  private val settingsRepo = SettingsPreferencesRepository(application)
  private val sessionDao = VoltPulseDatabase.getInstance(application).chargingSessionDao()
  private val displayController = DisplayWidthController(application)
  private val refreshRateController = RefreshRateController(application)
  private val displayManager = application.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
  private val notificationHelper = WarningNotificationHelper(application)

  private val _telemetry = MutableStateFlow(hardwareReader.readTelemetry())
  private val _samples = MutableStateFlow<List<TelemetrySample>>(emptyList())
  private val _testWarning = MutableStateFlow<ActiveWarning?>(null)
  private val _displayState = MutableStateFlow(displayController.readDisplayState())
  private val _targetSwDp = MutableStateFlow(_displayState.value.currentSmallestWidthDp)
  private val _displayFeedback = MutableStateFlow<DisplayApplyResult?>(null)
  private val _refreshRateState = MutableStateFlow(refreshRateController.readRefreshRateState())
  private val _refreshRateFeedback = MutableStateFlow<RefreshRateApplyResult?>(null)
  private val _statusToast = MutableStateFlow<String?>(null)

  private val settingsFlow: StateFlow<WarningSettings> =
    settingsRepo.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = WarningSettings(),
    )

  private val aggregateFlow =
    combine(
      sessionDao.observeSessionCount(),
      sessionDao.observeMaxPeakWatts(),
      sessionDao.observeMaxTemperature(),
      sessionDao.observeAvgPeakCurrent(),
    ) { count, maxW, maxT, avgI ->
      ChargingAggregateStats(
        sessionCount = count,
        maxPeakWatts = maxW ?: 0f,
        maxTempRecordedC = maxT ?: 0f,
        avgPeakCurrentMa = avgI?.toInt() ?: 0,
      )
    }

  private val baseTelemetryFlow =
    combine(_telemetry, settingsFlow, _samples, _testWarning) {
      telemetry,
      settings,
      samples,
      testWarning ->
      val evaluated = evaluateWarnings(telemetry, settings)
      val warnings = if (testWarning != null) listOf(testWarning) + evaluated else evaluated
      Quad(telemetry, settings, samples, warnings)
    }

  private val displayCombinedFlow =
    combine(_displayState, _targetSwDp, _displayFeedback, _refreshRateState, _refreshRateFeedback) {
      dState,
      targetSw,
      feedback,
      fpsState,
      fpsFeedback ->
      DisplayBundle(dState, targetSw, feedback, fpsState, fpsFeedback)
    }

  val uiState: StateFlow<VoltPulseUiState> =
    combine(
        baseTelemetryFlow,
        sessionDao.observeRecentSessions(),
        aggregateFlow,
        displayCombinedFlow,
        _statusToast,
      ) { base, sessions, agg, disp, toast ->
        VoltPulseUiState(
          telemetry = base.first,
          settings = base.second,
          samples = base.third,
          activeWarnings = base.fourth,
          recentSessions = sessions,
          aggregateStats = agg,
          displayState = disp.displayState,
          targetSmallestWidthDp = disp.targetSwDp,
          displayFeedback = disp.displayFeedback,
          refreshRateState = disp.refreshRateState,
          refreshRateFeedback = disp.refreshRateFeedback,
          statusToastMessage = toast,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue =
          VoltPulseUiState(
            telemetry = _telemetry.value,
            displayState = _displayState.value,
            targetSmallestWidthDp = _displayState.value.currentSmallestWidthDp,
            refreshRateState = _refreshRateState.value,
          ),
      )

  // Active charging session tracker
  private var activeSessionEntity: ChargingSessionEntity? = null
  private var voltageSum: Long = 0L
  private var voltageCount: Int = 0
  private var pollingJob: Job? = null

  private val batteryReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        refreshTelemetryNow()
      }
    }

  private val displayListener =
    object : DisplayManager.DisplayListener {
      override fun onDisplayAdded(displayId: Int) = refreshRefreshRateState()
      override fun onDisplayRemoved(displayId: Int) = refreshRefreshRateState()
      override fun onDisplayChanged(displayId: Int) {
        if (displayId == android.view.Display.DEFAULT_DISPLAY) {
          refreshRefreshRateState()
        }
      }
    }

  init {
    getApplication<Application>()
      .registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    displayManager?.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))

    viewModelScope.launch {
      settingsFlow
        .map { it.pollingIntervalSec }
        .distinctUntilChanged()
        .collect { intervalSec -> startPolling(intervalSec) }
    }

    viewModelScope.launch {
      settingsFlow
        .map { it.customDesignCapacityMah }
        .distinctUntilChanged()
        .collect { refreshTelemetryNow() }
    }
  }

  private fun startPolling(intervalSec: Int) {
    pollingJob?.cancel()
    pollingJob =
      viewModelScope.launch(Dispatchers.IO) {
        while (isActive) {
          refreshTelemetryNow()
          delay(intervalSec.coerceIn(1, 10) * 1000L)
        }
      }
  }

  fun refreshTelemetryNow() {
    val settings = settingsFlow.value
    val latest = hardwareReader.readTelemetry(settings.customDesignCapacityMah)
    _telemetry.value = latest

    val sample =
      TelemetrySample(
        timestampMs = latest.timestampMs,
        currentMa = latest.currentNowMa ?: latest.currentAvgMa ?: 0,
        powerWatts = latest.powerWatts,
        voltageMv = latest.voltageMv,
        temperatureC = latest.temperatureC,
        levelPct = latest.levelPct,
        isCharging = latest.chargingState == ChargingStateLabel.CHARGING,
      )
    _samples.value = (_samples.value + sample).takeLast(60)

    trackChargingSession(latest)

    val warnings = evaluateWarnings(latest, settings)
    val highest = warnings.firstOrNull { it.severity == WarningSeverity.CRITICAL } ?: warnings.firstOrNull()
    if (highest != null && settings.systemNotificationsEnabled) {
      notificationHelper.notifyWarning(highest, vibrate = settings.hapticOnCritical)
    }
  }

  private fun trackChargingSession(latest: BatteryTelemetry) {
    val isPluggedCharging =
      latest.chargingState == ChargingStateLabel.CHARGING ||
        (latest.pluggedSource != PluggedSourceLabel.BATTERY &&
          latest.chargingState == ChargingStateLabel.FULL)

    if (isPluggedCharging) {
      val currentAbsMa = abs(latest.currentNowMa ?: latest.currentAvgMa ?: 0)
      val existing = activeSessionEntity
      if (existing == null) {
        voltageSum = latest.voltageMv.toLong()
        voltageCount = 1
        val created =
          ChargingSessionEntity(
            startTimeMs = latest.timestampMs,
            endTimeMs = latest.timestampMs,
            startLevelPct = latest.levelPct,
            endLevelPct = latest.levelPct,
            powerSource = latest.pluggedSource.displayName,
            peakPowerWatts = latest.powerWatts,
            peakCurrentMa = currentAbsMa,
            maxTempC = latest.temperatureC,
            avgVoltageMv = latest.voltageMv,
            estimatedHealthPct = latest.estimatedHealthPct,
          )
        viewModelScope.launch(Dispatchers.IO) {
          val id = sessionDao.insertSession(created)
          activeSessionEntity = created.copy(id = id)
        }
      } else {
        voltageSum += latest.voltageMv
        voltageCount += 1
        val updated =
          existing.copy(
            endTimeMs = latest.timestampMs,
            endLevelPct = latest.levelPct,
            powerSource = latest.pluggedSource.displayName,
            peakPowerWatts = max(existing.peakPowerWatts, latest.powerWatts),
            peakCurrentMa = max(existing.peakCurrentMa, currentAbsMa),
            maxTempC = max(existing.maxTempC, latest.temperatureC),
            avgVoltageMv = (voltageSum / voltageCount).toInt(),
            estimatedHealthPct = latest.estimatedHealthPct ?: existing.estimatedHealthPct,
          )
        activeSessionEntity = updated
        viewModelScope.launch(Dispatchers.IO) { sessionDao.updateSession(updated) }
      }
    } else if (activeSessionEntity != null) {
      activeSessionEntity = null
      voltageSum = 0L
      voltageCount = 0
    }
  }

  private fun evaluateWarnings(
    telemetry: BatteryTelemetry,
    settings: WarningSettings,
  ): List<ActiveWarning> {
    val list = mutableListOf<ActiveWarning>()

    if (!telemetry.hardwareHealth.isHealthy) {
      list +=
        ActiveWarning(
          id = "hw_health",
          title = "Hardware Battery Alert: ${telemetry.hardwareHealth.displayName}",
          message = "Android system diagnostics flagged an abnormal cell condition.",
          severity = WarningSeverity.CRITICAL,
          metricLabel = telemetry.hardwareHealth.displayName,
        )
    }

    if (settings.highTempEnabled && telemetry.temperatureC >= settings.highTempThresholdC) {
      val formatted = formatTemp(telemetry.temperatureC, settings.useFahrenheit)
      list +=
        ActiveWarning(
          id = "high_temp",
          title = "Thermal Warning",
          message =
            "Battery temperature exceeded ${formatTemp(settings.highTempThresholdC, settings.useFahrenheit)}. Pause heavy gaming or fast charging.",
          severity =
            if (telemetry.temperatureC >= settings.highTempThresholdC + 3f)
              WarningSeverity.CRITICAL
            else WarningSeverity.WARNING,
          metricLabel = formatted,
        )
    }

    if (settings.overVoltageEnabled && telemetry.voltageMv >= settings.overVoltageThresholdMv) {
      list +=
        ActiveWarning(
          id = "over_voltage",
          title = "Over-Voltage Spike",
          message = "Cell input voltage exceeded ${settings.overVoltageThresholdMv} mV.",
          severity = WarningSeverity.CRITICAL,
          metricLabel = "${telemetry.voltageMv} mV",
        )
    }

    if (
      settings.lowBatteryEnabled &&
        telemetry.chargingState == ChargingStateLabel.DISCHARGING &&
        telemetry.levelPct <= settings.lowBatteryThresholdPct
    ) {
      list +=
        ActiveWarning(
          id = "low_battery",
          title = "Low Charge Threshold",
          message = "Avoid deep discharges below ${settings.lowBatteryThresholdPct}% to preserve lithium longevity.",
          severity = WarningSeverity.WARNING,
          metricLabel = "${telemetry.levelPct}%",
        )
    }

    if (
      settings.chargeLimitEnabled &&
        telemetry.chargingState == ChargingStateLabel.CHARGING &&
        telemetry.levelPct >= settings.chargeLimitPct
    ) {
      list +=
        ActiveWarning(
          id = "charge_limit",
          title = "Charge Limit Reached",
          message = "Battery reached your ${settings.chargeLimitPct}% target. Unplugging reduces cathode stress.",
          severity = WarningSeverity.INFO,
          metricLabel = "${telemetry.levelPct}%",
        )
    }

    return list
  }

  fun updateSettings(transform: (WarningSettings) -> WarningSettings) {
    viewModelScope.launch { settingsRepo.updateSettings(transform) }
  }

  fun setCustomDesignCapacity(capacityMah: Int) {
    updateSettings { it.copy(customDesignCapacityMah = capacityMah.coerceIn(0, 25000)) }
    _statusToast.value =
      if (capacityMah > 0) "Rated capacity set to $capacityMah mAh"
      else "Restored auto-detected design capacity"
  }

  fun recordManualSessionSnapshot() {
    val t = _telemetry.value
    viewModelScope.launch(Dispatchers.IO) {
      sessionDao.insertSession(
        ChargingSessionEntity(
          startTimeMs = t.timestampMs,
          endTimeMs = t.timestampMs,
          startLevelPct = t.levelPct,
          endLevelPct = t.levelPct,
          powerSource = t.pluggedSource.displayName,
          peakPowerWatts = t.powerWatts,
          peakCurrentMa = abs(t.currentNowMa ?: t.currentAvgMa ?: 0),
          maxTempC = t.temperatureC,
          avgVoltageMv = t.voltageMv,
          estimatedHealthPct = t.estimatedHealthPct,
        )
      )
      _statusToast.value = "Telemetry snapshot saved to charging log"
    }
  }

  fun clearChargingHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      sessionDao.clearAllSessions()
      _statusToast.value = "Charging history cleared"
    }
  }

  fun triggerTestWarning() {
    val settings = settingsFlow.value
    val sampleWarning =
      ActiveWarning(
        id = "test_preview",
        title = "Test Warning Alert",
        message = "This is a live preview of your battery health warning banner and alert style.",
        severity = WarningSeverity.WARNING,
        metricLabel = formatTemp(settings.highTempThresholdC, settings.useFahrenheit),
      )
    _testWarning.value = sampleWarning
    if (settings.systemNotificationsEnabled) {
      notificationHelper.notifyWarning(sampleWarning, vibrate = settings.hapticOnCritical, force = true)
    } else if (settings.hapticOnCritical) {
      notificationHelper.triggerHapticPulse()
    }
    _statusToast.value = "Warning preview triggered"
  }

  fun dismissTestWarning() {
    _testWarning.value = null
  }

  // Developer Settings — Smallest Width (dp) actions
  fun refreshDisplayState() {
    val updated = displayController.readDisplayState()
    _displayState.value = updated
  }

  fun updateTargetSmallestWidth(swDp: Int) {
    _targetSwDp.value = swDp.coerceIn(280, 720)
  }

  fun applySmallestWidthDirect() {
    val target = _targetSwDp.value
    val result = displayController.applySmallestWidth(target)
    _displayFeedback.value = result
    refreshDisplayState()
  }

  fun resetSmallestWidthDirect() {
    val result = displayController.resetToNativeSmallestWidth()
    _displayFeedback.value = result
    refreshDisplayState()
    if (result is DisplayApplyResult.Success) {
      _targetSwDp.value = result.appliedSwDp
    }
  }

  fun applySmallestWidthViaRoot(targetSwDp: Int?) {
    viewModelScope.launch(Dispatchers.IO) {
      val result = displayController.applyViaRoot(targetSwDp)
      _displayFeedback.value = result
      refreshDisplayState()
    }
  }

  fun openDeveloperSettingsScreen(): Boolean = displayController.openDeveloperOptions()

  fun getAdbGrantCommand(): String = displayController.adbGrantCommand()

  fun getAdbDensityCommand(targetSwDp: Int): String = displayController.adbDensityCommand(targetSwDp)

  fun calculateDpiPreview(targetSwDp: Int): Int =
    displayController.calculateDpiForSmallestWidth(_displayState.value.minDimensionPx, targetSwDp)

  fun clearDisplayFeedback() {
    _displayFeedback.value = null
  }

  // Display Refresh Rate / FPS Lock actions
  fun refreshRefreshRateState() {
    _refreshRateState.value = refreshRateController.readRefreshRateState()
  }

  fun selectRefreshRateMode(targetHz: Int?) {
    updateSettings { it.copy(selectedRefreshRateHz = targetHz) }
    val state = _refreshRateState.value
    if (targetHz != null && state.hasSecureSettingsPermission) {
      val result = refreshRateController.applySystemRefreshRateLock(targetHz)
      _refreshRateFeedback.value = result
      refreshRefreshRateState()
    } else if (targetHz == null && state.hasSecureSettingsPermission && state.isSystemLocked) {
      val result = refreshRateController.applySystemRefreshRateLock(null)
      _refreshRateFeedback.value = result
      refreshRefreshRateState()
    } else {
      _statusToast.value =
        if (targetHz == null) "Switched to Dynamic Auto refresh rate"
        else "App window requesting ${targetHz} Hz — tap Lock System-Wide for games"
    }
  }

  fun lockRefreshRateSystemWide(targetHz: Int) {
    updateSettings { it.copy(selectedRefreshRateHz = targetHz) }
    val result = refreshRateController.applySystemRefreshRateLock(targetHz)
    _refreshRateFeedback.value = result
    refreshRefreshRateState()
  }

  fun resetRefreshRateSystemWide() {
    updateSettings { it.copy(selectedRefreshRateHz = null) }
    val result = refreshRateController.applySystemRefreshRateLock(null)
    _refreshRateFeedback.value = result
    refreshRefreshRateState()
  }

  fun lockRefreshRateViaRoot(targetHz: Int?) {
    updateSettings { it.copy(selectedRefreshRateHz = targetHz) }
    viewModelScope.launch(Dispatchers.IO) {
      val result = refreshRateController.applySystemRefreshRateViaRoot(targetHz)
      _refreshRateFeedback.value = result
      refreshRefreshRateState()
    }
  }

  fun getPreferredDisplayModeForSettings(selectedHz: Int?): RefreshRateModeInfo? {
    if (selectedHz == null) return null
    return refreshRateController.findModeForHz(selectedHz)
  }

  fun openSystemDisplaySettingsScreen(): Boolean = refreshRateController.openSystemDisplaySettings()

  fun getFpsAdbCommand(targetHz: Int): String = refreshRateController.adbLockFpsCommand(targetHz)

  fun clearStatusToast() {
    _statusToast.value = null
  }

  override fun onCleared() {
    super.onCleared()
    pollingJob?.cancel()
    try {
      getApplication<Application>().unregisterReceiver(batteryReceiver)
    } catch (_: Throwable) {
      // Receiver already unregistered
    }
    try {
      displayManager?.unregisterDisplayListener(displayListener)
    } catch (_: Throwable) {
      // Listener already unregistered
    }
  }

  private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

  private data class DisplayBundle(
    val displayState: DisplayWidthState,
    val targetSwDp: Int,
    val displayFeedback: DisplayApplyResult?,
    val refreshRateState: RefreshRateState,
    val refreshRateFeedback: RefreshRateApplyResult?,
  )
}

fun formatTemp(tempC: Float, useFahrenheit: Boolean): String {
  return if (useFahrenheit) {
    val f = tempC * 9f / 5f + 32f
    String.format("%.1f°F", f)
  } else {
    String.format("%.1f°C", tempC)
  }
}
