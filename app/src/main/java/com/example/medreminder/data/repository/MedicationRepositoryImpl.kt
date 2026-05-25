package com.example.medreminder.data.repository

import com.example.medreminder.data.local.dao.MedicationDao
import com.example.medreminder.data.local.entity.DoseLogEntity
import com.example.medreminder.data.local.entity.MedicationEntity
import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MedicationRepositoryImpl @Inject constructor(
    private val dao: MedicationDao
) : MedicationRepository {

    override fun getAllMedications(): Flow<List<Medication>> {
        return dao.getAllMedications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMedicationById(id: Long): Medication? {
        return dao.getMedicationById(id)?.toDomain()
    }

    override suspend fun insertMedication(medication: Medication): Long {
        return dao.insertMedication(MedicationEntity.fromDomain(medication))
    }

    override suspend fun updateMedication(medication: Medication) {
        dao.updateMedication(MedicationEntity.fromDomain(medication))
    }

    override suspend fun deleteMedication(medication: Medication) {
        dao.deleteMedication(MedicationEntity.fromDomain(medication))
    }

    override fun getLogsForDate(date: String): Flow<List<DoseLog>> {
        return dao.getLogsForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLogsForMedication(medicationId: Long): Flow<List<DoseLog>> {
        return dao.getLogsForMedication(medicationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertDoseLog(doseLog: DoseLog): Long {
        return dao.insertDoseLog(DoseLogEntity.fromDomain(doseLog))
    }

    override suspend fun updateDoseLog(doseLog: DoseLog) {
        dao.updateDoseLog(DoseLogEntity.fromDomain(doseLog))
    }

    override suspend fun getDoseLog(medicationId: Long, date: String, time: String): DoseLog? {
        return dao.getDoseLog(medicationId, date, time)?.toDomain()
    }

    override fun getAllLogs(): Flow<List<DoseLog>> {
        return dao.getAllLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
