package com.example.medreminder.data.local.dao

import androidx.room.*
import com.example.medreminder.data.local.entity.DoseLogEntity
import com.example.medreminder.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    // Dose Log Queries
    @Query("SELECT * FROM dose_logs WHERE scheduledDate = :date ORDER BY scheduledTime ASC")
    fun getLogsForDate(date: String): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId ORDER BY scheduledDate DESC, scheduledTime DESC")
    fun getLogsForMedication(medicationId: Long): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND scheduledDate = :date AND scheduledTime = :time LIMIT 1")
    suspend fun getDoseLog(medicationId: Long, date: String, time: String): DoseLogEntity?

    @Query("SELECT * FROM dose_logs ORDER BY scheduledDate DESC, scheduledTime DESC")
    fun getAllLogs(): Flow<List<DoseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(doseLog: DoseLogEntity): Long

    @Update
    suspend fun updateDoseLog(doseLog: DoseLogEntity)
}
