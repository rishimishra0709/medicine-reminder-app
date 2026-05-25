package com.example.medreminder.domain.usecase

import com.example.medreminder.domain.model.DoseStatus
import com.example.medreminder.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class GetStreakUseCase @Inject constructor(
    private val repository: MedicationRepository
) {
    operator fun invoke(): Flow<Int> {
        return repository.getAllLogs().map { logs ->
            if (logs.isEmpty()) return@map 0
            
            val logsByDate = logs.groupBy { it.scheduledDate }
            var streak = 0
            var checkDate = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            
            while (true) {
                val dateString = checkDate.format(formatter)
                val dayLogs = logsByDate[dateString]
                
                if (dayLogs != null) {
                    val allTaken = dayLogs.all { it.status == DoseStatus.TAKEN }
                    val hasMissed = dayLogs.any { it.status == DoseStatus.MISSED }
                    
                    if (allTaken) {
                        streak++
                    } else if (hasMissed) {
                        break
                    } else {
                        // Pending doses on today don't break the streak yet, but do on past days
                        if (checkDate != LocalDate.now()) {
                            break
                        }
                    }
                } else {
                    // No logs for this date
                    if (checkDate != LocalDate.now()) {
                        // Yesterday or earlier had no doses logged, so the streak stops
                        break
                    }
                }
                
                checkDate = checkDate.minusDays(1)
                if (streak > 3650) break // Guard boundary
            }
            
            streak
        }
    }
}
