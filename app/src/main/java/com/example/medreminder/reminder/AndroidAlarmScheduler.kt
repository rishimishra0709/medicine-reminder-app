package com.example.medreminder.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.model.ScheduleType
import com.example.medreminder.domain.repository.AlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleAlarms(medication: Medication) {
        if (medication.scheduleType == ScheduleType.AS_NEEDED) return

        medication.reminderTimes.forEach { timeString ->
            val nextTriggerTime = calculateNextTriggerTime(medication, timeString)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM
                putExtra(EXTRA_MEDICATION_ID, medication.id)
                putExtra(EXTRA_MEDICATION_NAME, medication.name)
                putExtra(EXTRA_SCHEDULED_TIME, timeString)
                putExtra(EXTRA_DOSAGE, medication.dosage)
                putExtra(EXTRA_INSTRUCTIONS, medication.instructions)
            }

            val requestCode = getAlarmRequestCode(medication.id, timeString)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextTriggerTime,
                            pendingIntent
                        )
                    } else {
                        // Fallback to inexact alarm if permission is denied
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            nextTriggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                }
                Log.d("AlarmScheduler", "Scheduled alarm for ${medication.name} at code $requestCode triggers at $nextTriggerTime")
            } catch (e: Exception) {
                Log.e("AlarmScheduler", "Failed to schedule alarm", e)
            }
        }
    }

    override fun cancelAlarms(medication: Medication) {
        medication.reminderTimes.forEach { timeString ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM
            }
            val requestCode = getAlarmRequestCode(medication.id, timeString)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("AlarmScheduler", "Cancelled alarm for ${medication.name} at code $requestCode")
            }
        }
    }

    override fun cancelCriticalAlarm(medicationId: Long, scheduledDate: String, scheduledTime: String) {
        val intent = Intent(context, CriticalReminderReceiver::class.java).apply {
            action = ACTION_CRITICAL_ALARM
        }
        val requestCode = getCriticalAlarmRequestCode(medicationId, scheduledDate, scheduledTime)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Cancelled critical alarm for medication $medicationId at code $requestCode")
        }
    }

    private fun calculateNextTriggerTime(medication: Medication, timeString: String): Long {
        val reminderTime = LocalTime.parse(timeString)
        val now = LocalDateTime.now()
        var scheduledDateTime = LocalDateTime.of(LocalDate.now(), reminderTime)

        if (medication.scheduleType == ScheduleType.DAILY) {
            if (now.isAfter(scheduledDateTime)) {
                scheduledDateTime = scheduledDateTime.plusDays(1)
            }
        } else if (medication.scheduleType == ScheduleType.SPECIFIC_DAYS) {
            val targetDays = medication.specificDays.map { dayOfWeekFromApp(it) }
            
            // Find next day matching target days
            var daysToAdd = 0
            while (daysToAdd <= 7) {
                val candidateDateTime = scheduledDateTime.plusDays(daysToAdd.toLong())
                if (targetDays.contains(candidateDateTime.dayOfWeek)) {
                    if (candidateDateTime.isAfter(now)) {
                        scheduledDateTime = candidateDateTime
                        break
                    }
                }
                daysToAdd++
            }
        }

        return scheduledDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun dayOfWeekFromApp(appDay: Int): DayOfWeek {
        // App day: 1 = Mon, 7 = Sun
        return when (appDay) {
            1 -> DayOfWeek.MONDAY
            2 -> DayOfWeek.TUESDAY
            3 -> DayOfWeek.WEDNESDAY
            4 -> DayOfWeek.THURSDAY
            5 -> DayOfWeek.FRIDAY
            6 -> DayOfWeek.SATURDAY
            7 -> DayOfWeek.SUNDAY
            else -> DayOfWeek.MONDAY
        }
    }

    companion object {
        const val ACTION_ALARM = "com.example.medreminder.ACTION_ALARM"
        const val ACTION_CRITICAL_ALARM = "com.example.medreminder.ACTION_CRITICAL_ALARM"
        
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_SCHEDULED_TIME = "scheduled_time"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_INSTRUCTIONS = "instructions"
        const val EXTRA_SCHEDULED_DATE = "scheduled_date"

        fun getAlarmRequestCode(medicationId: Long, timeString: String): Int {
            return (medicationId.toInt() * 1000) + timeString.replace(":", "").toInt()
        }

        fun getCriticalAlarmRequestCode(medicationId: Long, date: String, timeString: String): Int {
            // Unique code for critical alarms
            val dateHash = date.replace("-", "").hashCode()
            val timeInt = timeString.replace(":", "").toInt()
            return (medicationId.toInt() * 10000) + (dateHash % 1000) * 10 + (timeInt % 10)
        }
    }
}
