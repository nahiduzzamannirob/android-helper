package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {
  @Query("SELECT * FROM charging_sessions ORDER BY startTimeMs DESC LIMIT 30")
  fun observeRecentSessions(): Flow<List<ChargingSessionEntity>>

  @Query("SELECT COUNT(*) FROM charging_sessions")
  fun observeSessionCount(): Flow<Int>

  @Query("SELECT MAX(peakPowerWatts) FROM charging_sessions")
  fun observeMaxPeakWatts(): Flow<Float?>

  @Query("SELECT MAX(maxTempC) FROM charging_sessions")
  fun observeMaxTemperature(): Flow<Float?>

  @Query("SELECT AVG(peakCurrentMa) FROM charging_sessions")
  fun observeAvgPeakCurrent(): Flow<Float?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: ChargingSessionEntity): Long

  @Update
  suspend fun updateSession(session: ChargingSessionEntity)

  @Query("DELETE FROM charging_sessions")
  suspend fun clearAllSessions()
}
