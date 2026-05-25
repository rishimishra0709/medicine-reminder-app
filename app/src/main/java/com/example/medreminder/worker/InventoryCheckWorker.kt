package com.example.medreminder.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medreminder.MainActivity
import com.example.medreminder.domain.repository.MedicationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class InventoryCheckWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MedicationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("InventoryWorker", "Executing inventory check worker...")
        try {
            val medications = repository.getAllMedications().first()
            val lowStockMeds = medications.filter { 
                it.isInventoryTrackingEnabled && it.getRemainingDaysOfStock() < 5 
            }

            if (lowStockMeds.isNotEmpty()) {
                lowStockMeds.forEach { medication ->
                    showLowStockNotification(medication.id, medication.name, medication.currentStock)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("InventoryWorker", "Inventory check failed", e)
            return Result.failure()
        }
    }

    private fun showLowStockNotification(medicationId: Long, medicationName: String, currentStock: Int) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = medicationId.toInt() + 20000

        val contentIntent = Intent(appContext, MainActivity::class.java).let {
            PendingIntent.getActivity(
                appContext,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(appContext, "INVENTORY_ALERT_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Low Stock Alert!")
            .setContentText("You only have $currentStock pills left of $medicationName. Refill soon!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        notificationManager.notify(notificationId, builder.build())
    }
}
