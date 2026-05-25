package com.example.medreminder.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.medreminder.MainActivity
import com.example.medreminder.domain.model.DoseStatus
import com.example.medreminder.domain.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CriticalReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MedicationRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("CriticalReminderReceiver", "Received action: $action")

        if (action != AndroidAlarmScheduler.ACTION_CRITICAL_ALARM) return

        val medId = intent.getLongExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val medName = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_MEDICATION_NAME) ?: "Medication"
        val date = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_DATE) ?: ""
        val time = intent.getStringExtra(AndroidAlarmScheduler.EXTRA_SCHEDULED_TIME) ?: ""

        if (medId == -1L || date.isEmpty() || time.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            val log = repository.getDoseLog(medId, date, time)
            
            // If the user has NOT taken the dose yet (still PENDING or SNOOZED), trigger the critical alarm
            if (log != null && log.status != DoseStatus.TAKEN) {
                // Update status to MISSED in database
                val updatedLog = log.copy(status = DoseStatus.MISSED)
                repository.updateDoseLog(updatedLog)

                // Trigger critical alert notification
                showCriticalNotification(context, medId, medName, time)
            }
        }
    }

    private fun showCriticalNotification(context: Context, medId: Long, medName: String, time: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = medId.toInt() + 9000 // Distinct ID range for critical notifications

        val contentIntent = Intent(context, MainActivity::class.java).let {
            PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, "CRITICAL_REMINDER_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("CRITICAL REMINDER: Missed Dose")
            .setContentText("You missed your scheduled dose of $medName at $time! Please take it immediately.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000)) // Strong vibration pattern
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        notificationManager.notify(notificationId, builder.build())
    }
}
