package com.example.medreminder.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.domain.model.DoseStatus
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.model.ScheduleType
import com.example.medreminder.domain.repository.MedicationRepository
import com.example.medreminder.domain.usecase.GetStreakUseCase
import com.example.medreminder.domain.usecase.LogDoseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val logDoseUseCase: LogDoseUseCase,
    getStreakUseCase: GetStreakUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val streak: StateFlow<Int> = getStreakUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val dashboardDoses: StateFlow<List<DashboardDose>> = combine(
        repository.getAllMedications(),
        _selectedDate.flatMapLatest { date ->
            repository.getLogsForDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        },
        _selectedDate
    ) { medications, logs, date ->
        buildDashboardDoses(medications, logs, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun changeDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun markAsTaken(medicationId: Long, scheduledTime: String) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            logDoseUseCase(medicationId, dateStr, scheduledTime, DoseStatus.TAKEN)
        }
    }

    fun markAsSnoozed(medicationId: Long, scheduledTime: String) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            logDoseUseCase(medicationId, dateStr, scheduledTime, DoseStatus.SNOOZED)
        }
    }

    fun logAsNeeded(medicationId: Long) {
        viewModelScope.launch {
            val dateStr = _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val nowTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            logDoseUseCase(medicationId, dateStr, nowTimeStr, DoseStatus.TAKEN)
        }
    }

    private fun buildDashboardDoses(
        medications: List<Medication>,
        logs: List<com.example.medreminder.domain.model.DoseLog>,
        date: LocalDate
    ): List<DashboardDose> {
        val list = mutableListOf<DashboardDose>()
        val dayOfWeek = date.dayOfWeek.value // 1 = Mon, 7 = Sun
        val isToday = date == LocalDate.now()
        val nowTime = LocalTime.now()

        medications.forEach { med ->
            // Convert start date to LocalDate safely
            val medStartDate = Instant.ofEpochMilli(med.startDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            if (date.isBefore(medStartDate)) {
                return@forEach
            }

            when (med.scheduleType) {
                ScheduleType.DAILY -> {
                    med.reminderTimes.forEach { timeStr ->
                        val log = logs.find { it.medicationId == med.id && it.scheduledTime == timeStr }
                        val status = determineStatus(log, timeStr, isToday, nowTime)
                        list.add(createDashboardDose(med, timeStr, status, isAsNeeded = false))
                    }
                }
                ScheduleType.SPECIFIC_DAYS -> {
                    if (med.specificDays.contains(dayOfWeek)) {
                        med.reminderTimes.forEach { timeStr ->
                            val log = logs.find { it.medicationId == med.id && it.scheduledTime == timeStr }
                            val status = determineStatus(log, timeStr, isToday, nowTime)
                            list.add(createDashboardDose(med, timeStr, status, isAsNeeded = false))
                        }
                    }
                }
                ScheduleType.AS_NEEDED -> {
                    val loggedAdHoc = logs.filter { it.medicationId == med.id }
                    var hasUnloggedOption = true
                    if (loggedAdHoc.isNotEmpty()) {
                        loggedAdHoc.forEach { log ->
                            list.add(createDashboardDose(med, log.scheduledTime, log.status, isAsNeeded = true))
                        }
                    }
                    if (hasUnloggedOption && isToday) {
                        list.add(createDashboardDose(med, "", DoseStatus.PENDING, isAsNeeded = true))
                    }
                }
            }
        }
        
        return list.sortedWith(
            compareBy<DashboardDose> { it.isAsNeeded }
                .thenBy { it.scheduledTime.ifEmpty { "23:59" } }
        )
    }

    private fun determineStatus(
        log: com.example.medreminder.domain.model.DoseLog?,
        timeStr: String,
        isToday: Boolean,
        nowTime: LocalTime
    ): DoseStatus {
        if (log != null) return log.status

        if (isToday) {
            val schedTime = LocalTime.parse(timeStr)
            return if (nowTime.isAfter(schedTime.plusMinutes(30))) {
                DoseStatus.MISSED
            } else {
                DoseStatus.PENDING
            }
        } else if (LocalDate.now().isAfter(_selectedDate.value)) {
            return DoseStatus.MISSED
        }
        return DoseStatus.PENDING
    }

    private fun createDashboardDose(
        med: Medication,
        timeStr: String,
        status: DoseStatus,
        isAsNeeded: Boolean
    ) = DashboardDose(
        medicationId = med.id,
        name = med.name,
        dosage = med.dosage,
        instructions = med.instructions,
        iconShape = med.iconShape,
        iconColor = med.iconColor,
        scheduledTime = timeStr,
        status = status,
        isAsNeeded = isAsNeeded,
        currentStock = med.currentStock,
        originalStock = med.originalStock,
        isInventoryTrackingEnabled = med.isInventoryTrackingEnabled,
        isStockLow = med.isStockLow(),
        remainingDays = med.getRemainingDaysOfStock()
    )
}

data class DashboardDose(
    val medicationId: Long,
    val name: String,
    val dosage: String,
    val instructions: String,
    val iconShape: String,
    val iconColor: Long,
    val scheduledTime: String,
    val status: DoseStatus,
    val isAsNeeded: Boolean,
    val currentStock: Int,
    val originalStock: Int,
    val isInventoryTrackingEnabled: Boolean,
    val isStockLow: Boolean,
    val remainingDays: Int
)
