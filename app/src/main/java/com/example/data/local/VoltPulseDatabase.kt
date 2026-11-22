package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChargingSessionEntity::class], version = 1, exportSchema = false)
abstract class VoltPulseDatabase : RoomDatabase() {
  abstract fun chargingSessionDao(): ChargingSessionDao

  companion object {
    @Volatile private var INSTANCE: VoltPulseDatabase? = null

    fun getInstance(context: Context): VoltPulseDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                VoltPulseDatabase::class.java,
                "voltpulse_telemetry.db",
              )
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}
