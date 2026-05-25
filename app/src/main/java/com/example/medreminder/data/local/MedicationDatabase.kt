package com.example.medreminder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.medreminder.data.local.converters.Converters
import com.example.medreminder.data.local.dao.MedicationDao
import com.example.medreminder.data.local.entity.DoseLogEntity
import com.example.medreminder.data.local.entity.MedicationEntity

@Database(
    entities = [MedicationEntity::class, DoseLogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedicationDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
}
