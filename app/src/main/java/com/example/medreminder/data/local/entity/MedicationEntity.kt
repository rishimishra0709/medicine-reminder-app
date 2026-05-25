package com.example.medreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.model.ScheduleType

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val instructions: String,
    val iconShape: String,
    val iconColor: Long,
    val scheduleType: ScheduleType,
    val specificDays: List<Int>,
    val reminderTimes: List<String>,
    val currentStock: Int,
    val originalStock: Int,
    val isInventoryTrackingEnabled: Boolean,
    val notes: String,
    val startDate: Long
) {
    fun toDomain(): Medication = Medication(
        id = id,
        name = name,
        dosage = dosage,
        instructions = instructions,
        iconShape = iconShape,
        iconColor = iconColor,
        scheduleType = scheduleType,
        specificDays = specificDays,
        reminderTimes = reminderTimes,
        currentStock = currentStock,
        originalStock = originalStock,
        isInventoryTrackingEnabled = isInventoryTrackingEnabled,
        notes = notes,
        startDate = startDate
    )

    companion object {
        fun fromDomain(medication: Medication): MedicationEntity = MedicationEntity(
            id = medication.id,
            name = medication.name,
            dosage = medication.dosage,
            instructions = medication.instructions,
            iconShape = medication.iconShape,
            iconColor = medication.iconColor,
            scheduleType = medication.scheduleType,
            specificDays = medication.specificDays,
            reminderTimes = medication.reminderTimes,
            currentStock = medication.currentStock,
            originalStock = medication.originalStock,
            isInventoryTrackingEnabled = medication.isInventoryTrackingEnabled,
            notes = medication.notes,
            startDate = medication.startDate
        )
    }
}
