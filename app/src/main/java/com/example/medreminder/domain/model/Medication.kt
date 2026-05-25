package com.example.medreminder.domain.model

data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val instructions: String,
    val iconShape: String, // e.g. "capsule", "tablet", "circle"
    val iconColor: Long, // Color Hex (e.g. 0xFF80CBC4)
    val scheduleType: ScheduleType,
    val specificDays: List<Int>, // 1 = Mon, 2 = Tue, ..., 7 = Sun
    val reminderTimes: List<String>, // "HH:mm" formatted times
    val currentStock: Int,
    val originalStock: Int,
    val isInventoryTrackingEnabled: Boolean,
    val notes: String,
    val startDate: Long = System.currentTimeMillis()
) {
    fun getRemainingDaysOfStock(): Int {
        if (!isInventoryTrackingEnabled || reminderTimes.isEmpty()) return Int.MAX_VALUE
        val dosesPerDay = if (scheduleType == ScheduleType.AS_NEEDED) 1 else reminderTimes.size
        return currentStock / dosesPerDay
    }

    fun isStockLow(): Boolean {
        if (!isInventoryTrackingEnabled) return false
        return getRemainingDaysOfStock() < 5
    }
}
