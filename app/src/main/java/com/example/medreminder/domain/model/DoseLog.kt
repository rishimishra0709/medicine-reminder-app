package com.example.medreminder.domain.model

enum class DoseStatus {
    TAKEN,
    SNOOZED,
    MISSED,
    PENDING
}

data class DoseLog(
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String, // Cached for easy notification display
    val scheduledDate: String, // "yyyy-MM-dd"
    val scheduledTime: String, // "HH:mm"
    val loggedTime: Long?, // Timestamp when status changed, or null if pending
    val status: DoseStatus
)
