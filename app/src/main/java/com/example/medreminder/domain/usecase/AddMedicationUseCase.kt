package com.example.medreminder.domain.usecase

import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.repository.AlarmScheduler
import com.example.medreminder.domain.repository.MedicationRepository
import javax.inject.Inject

class AddMedicationUseCase @Inject constructor(
    private val repository: MedicationRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend operator fun invoke(medication: Medication): Long {
        val id = repository.insertMedication(medication)
        val savedMedication = medication.copy(id = id)
        alarmScheduler.scheduleAlarms(savedMedication)
        return id
    }
}
