package com.example.medreminder.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.medreminder.MainActivity
import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.domain.model.DoseStatus
import com.example.medreminder.domain.repository.AlarmScheduler
import com.example.medreminder.domain.repository.MedicationRepository
import com.example.medreminder.domain.usecase.LogDoseUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MedicationRepository

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var logDoseUseCase: LogDoseUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllAlarms(context)
            return
        }

        val medId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        if (medId == -1L) return

        val medName = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_NAME) ?: "Medication"
        val scheduledTime = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME) ?: "00:00"
        val dosage = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_DOSAGE) ?: ""
        val instructions = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_INSTRUCTIONS) ?: ""
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        val notificationId = AndroidAlarmScheduler.getAlarmRequestCode(medId, scheduledTime)

        when (action) {
            AndroidAlarmScheduler.ACTION_ALARM -> {
                // 1. Log as pending in database
                CoroutineScope(Dispatchers.IO).launch {
                    val existingLog = repository.getDoseLog(medId, todayStr, scheduledTime)
                    if (existingLog == null) {
                        repository.insertDoseLog(
                            DoseLog(
                                medicationId = medId,
                                medicationName = medName,
                                scheduledDate = todayStr,
                                scheduledTime = scheduledTime,
                                loggedTime = null,
                                status = DoseStatus.PENDING
                            )
                        )
                    }

                    // 2. Schedule Critical Missed Dose Protocol (30 minutes from now)
                    scheduleCriticalMissedAlarm(context, medId, medName, todayStr, scheduledTime)
                }

                // 3. Show notification
                showReminderNotification(context, notificationId, medId, medName, scheduledTime, dosage, instructions)

                // 4. Reschedule this repeating alarm for next day/future time
                CoroutineScope(Dispatchers.IO).launch {
                    val medication = repository.getMedicationById(medId)
                    if (medication != null) {
                        alarmScheduler.scheduleAlarms(medication)
                    }
                }
            }

            ACTION_TAKE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    logDoseUseCase(medId, todayStr, scheduledTime, DoseStatus.TAKEN)
                    dismissNotification(context, notificationId)
                }
            }

            ACTION_SNOOZE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    logDoseUseCase(medId, todayStr, scheduledTime, DoseStatus.SNOOZED)
                    dismissNotification(context, notificationId)

                    // Reschedule main alarm for 10 minutes later (snooze)
                    snoozeAlarm(context, medId, medName, scheduledTime, dosage, instructions)
                }
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        notificationId: Int,
        medId: Long,
        medName: String,
        scheduledTime: String,
        dosage: String,
        instructions: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(context, MainActivity::class.java).let {
            PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val takeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TAKE
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, medId)
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_NAME, medName)
            putExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, medId)
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_NAME, medName)
            putExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AndroidAlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AndroidAlarmScheduler.EXTRA_INSTRUCTIONS, instructions)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtext = if (instructions.isNotEmpty()) "$dosage • $instructions" else dosage

        val builder = NotificationCompat.Builder(context, "MEDICATION_REMINDER_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time for your medication!")
            .setContentText("Please take $medName ($subtext)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_save, "Take Now", takePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze (10m)", snoozePendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    private fun snoozeAlarm(
        context: Context,
        medId: Long,
        medName: String,
        scheduledTime: String,
        dosage: String,
        instructions: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AndroidAlarmScheduler.ACTION_ALARM
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, medId)
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_NAME, medName)
            putExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(AndroidAlarmScheduler.EXTRA_DOSAGE, dosage)
            putExtra(AndroidAlarmScheduler.EXTRA_INSTRUCTIONS, instructions)
        }

        val requestCode = AndroidAlarmScheduler.getAlarmRequestCode(medId, scheduledTime) + 500000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 10 * 60 * 1000

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.d("AlarmReceiver", "Snoozed alarm for $medName in 10 minutes.")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Snooze scheduling failed", e)
        }
    }

    private fun scheduleCriticalMissedAlarm(
        context: Context,
        medId: Long,
        medName: String,
        date: String,
        time: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CriticalReminderReceiver::class.java).apply {
            action = AndroidAlarmScheduler.ACTION_CRITICAL_ALARM
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, medId)
            putExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_NAME, medName)
            putExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_DATE, date)
            putExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME, time)
        }

        val requestCode = AndroidAlarmScheduler.getCriticalAlarmRequestCode(medId, date, time)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + 30 * 60 * 1000

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.d("AlarmReceiver", "Scheduled critical missed check in 30 minutes for code $requestCode")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to schedule critical missed check", e)
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val medications = repository.getAllMedications().first()
            medications.forEach { medication ->
                alarmScheduler.scheduleAlarms(medication)
            }
            Log.d("AlarmReceiver", "Rescheduled all active alarms after boot.")
        }
    }

    private fun dismissNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    companion object {
        const val ACTION_TAKE = "com.example.medreminder.ACTION_TAKE"
        const val ACTION_SNOOZE = "com.example.medreminder.ACTION_SNOOZE"
    }
}
