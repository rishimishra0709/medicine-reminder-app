package com.example.medreminder.domain.usecase

import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMedicationsUseCase @Inject constructor(
    private val repository: MedicationRepository
) {
    operator fun invoke(): Flow<List<Medication>> {
        return repository.getAllMedications()
    }
}
