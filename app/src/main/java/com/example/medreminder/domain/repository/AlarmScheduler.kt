package com.example.medreminder.domain.repository

import com.example.medreminder.domain.model.Medication

interface AlarmScheduler {
    fun scheduleAlarms(medication: Medication)
    fun cancelAlarms(medication: Medication)
    fun cancelCriticalAlarm(medicationId: Long, scheduledDate: String, scheduledTime: String)
}
