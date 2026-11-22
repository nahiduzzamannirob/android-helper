package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val startLevelPct: Int,
  val endLevelPct: Int,
  val powerSource: String,
  val peakPowerWatts: Float,
  val peakCurrentMa: Int,
  val maxTempC: Float,
  val avgVoltageMv: Int,
  val estimatedHealthPct: Int?,
)
