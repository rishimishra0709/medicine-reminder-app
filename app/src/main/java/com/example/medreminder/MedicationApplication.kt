package com.example.medreminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medreminder.worker.InventoryCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MedicationApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleInventoryCheck()
    }

    private fun scheduleInventoryCheck() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val inventoryWorkRequest = PeriodicWorkRequestBuilder<InventoryCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "INVENTORY_CHECK_WORK",
            ExistingPeriodicWorkPolicy.KEEP,
            inventoryWorkRequest
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Standard channel for regular reminders
            val name = "Medication Reminders"
            val descriptionText = "Notifications for scheduled medication doses"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("MEDICATION_REMINDER_CHANNEL", name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            // Critical channel for missed dose reminders
            val criticalName = "Critical Missed Dose Alarms"
            val criticalDescriptionText = "Urgent alerts when medications are missed"
            val criticalImportance = NotificationManager.IMPORTANCE_HIGH
            val criticalChannel = NotificationChannel("CRITICAL_REMINDER_CHANNEL", criticalName, criticalImportance).apply {
                description = criticalDescriptionText
                enableVibration(true)
            }

            // Low-Stock channel
            val stockName = "Inventory Alerts"
            val stockDescriptionText = "Notifications when medication stocks are running low"
            val stockImportance = NotificationManager.IMPORTANCE_DEFAULT
            val stockChannel = NotificationChannel("INVENTORY_ALERT_CHANNEL", stockName, stockImportance).apply {
                description = stockDescriptionText
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(criticalChannel)
            notificationManager.createNotificationChannel(stockChannel)
        }
    }
}
