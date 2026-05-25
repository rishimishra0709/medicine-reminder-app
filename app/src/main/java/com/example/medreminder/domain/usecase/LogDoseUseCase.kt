package com.example.medreminder.domain.usecase

import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.domain.model.DoseStatus
import com.example.medreminder.domain.repository.AlarmScheduler
import com.example.medreminder.domain.repository.MedicationRepository
import javax.inject.Inject

class LogDoseUseCase @Inject constructor(
    private val repository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(
        medicationId: Long,
        scheduledDate: String,
        scheduledTime: String,
        status: DoseStatus,
        loggedTime: Long = System.currentTimeMillis()
    ) {
        val medication = repository.getMedicationById(medicationId) ?: return

        val existingLog = repository.getDoseLog(medicationId, scheduledDate, scheduledTime)
        
        val newLog = if (existingLog != null) {
            existingLog.copy(status = status, loggedTime = loggedTime)
        } else {
            DoseLog(
                medicationId = medicationId,
                medicationName = medication.name,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                loggedTime = loggedTime,
                status = status
            )
        }

        // Deduct inventory when status becomes TAKEN
        if (status == DoseStatus.TAKEN && (existingLog == null || existingLog.status != DoseStatus.TAKEN)) {
            if (medication.isInventoryTrackingEnabled && medication.currentStock > 0) {
                val updatedMedication = medication.copy(
                    currentStock = (medication.currentStock - 1).coerceAtLeast(0)
                )
                repository.updateMedication(updatedMedication)
            }
            
            // Cancel any pending critical alarm since the medication has been taken
            alarmScheduler.cancelCriticalAlarm(medicationId, scheduledDate, scheduledTime)
        }

        if (newLog.id == 0L) {
            repository.insertDoseLog(newLog)
        } else {
            repository.updateDoseLog(newLog)
        }
    }
}
