package com.example.medreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.domain.model.DoseStatus

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val scheduledDate: String, // "yyyy-MM-dd"
    val scheduledTime: String, // "HH:mm"
    val loggedTime: Long?,
    val status: DoseStatus
) {
    fun toDomain(): DoseLog = DoseLog(
        id = id,
        medicationId = medicationId,
        medicationName = medicationName,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        loggedTime = loggedTime,
        status = status
    )

    companion object {
        fun fromDomain(log: DoseLog): DoseLogEntity = DoseLogEntity(
            id = log.id,
            medicationId = log.medicationId,
            medicationName = log.medicationName,
            scheduledDate = log.scheduledDate,
            scheduledTime = log.scheduledTime,
            loggedTime = log.loggedTime,
            status = log.status
        )
    }
}
