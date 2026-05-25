package com.example.medreminder.domain.repository

import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.domain.model.Medication
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    fun getAllMedications(): Flow<List<Medication>>
    suspend fun getMedicationById(id: Long): Medication?
    suspend fun insertMedication(medication: Medication): Long
    suspend fun updateMedication(medication: Medication)
    suspend fun deleteMedication(medication: Medication)
    
    fun getLogsForDate(date: String): Flow<List<DoseLog>>
    fun getLogsForMedication(medicationId: Long): Flow<List<DoseLog>>
    suspend fun insertDoseLog(doseLog: DoseLog): Long
    suspend fun updateDoseLog(doseLog: DoseLog)
    suspend fun getDoseLog(medicationId: Long, date: String, time: String): DoseLog?
    fun getAllLogs(): Flow<List<DoseLog>>
}
