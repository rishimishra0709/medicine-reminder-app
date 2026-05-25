package com.example.medreminder.ui.addmed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.model.ScheduleType
import com.example.medreminder.domain.usecase.AddMedicationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMedicationViewModel @Inject constructor(
    private val addMedicationUseCase: AddMedicationUseCase
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _dosage = MutableStateFlow("")
    val dosage: StateFlow<String> = _dosage.asStateFlow()

    private val _instructions = MutableStateFlow("")
    val instructions: StateFlow<String> = _instructions.asStateFlow()

    private val _selectedShape = MutableStateFlow("capsule")
    val selectedShape: StateFlow<String> = _selectedShape.asStateFlow()

    private val _selectedColor = MutableStateFlow(0xFF80CBC4) // ColorPillMint
    val selectedColor: StateFlow<Long> = _selectedColor.asStateFlow()

    private val _scheduleType = MutableStateFlow(ScheduleType.DAILY)
    val scheduleType: StateFlow<ScheduleType> = _scheduleType.asStateFlow()

    private val _specificDays = MutableStateFlow<List<Int>>(emptyList())
    val specificDays: StateFlow<List<Int>> = _specificDays.asStateFlow()

    private val _reminderTimes = MutableStateFlow<List<String>>(listOf("09:00"))
    val reminderTimes: StateFlow<List<String>> = _reminderTimes.asStateFlow()

    private val _currentStock = MutableStateFlow("")
    val currentStock: StateFlow<String> = _currentStock.asStateFlow()

    private val _isInventoryTrackingEnabled = MutableStateFlow(false)
    val isInventoryTrackingEnabled: StateFlow<Boolean> = _isInventoryTrackingEnabled.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Boolean>()
    val saveSuccess: SharedFlow<Boolean> = _saveSuccess.asSharedFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setName(value: String) { _name.value = value }
    fun setDosage(value: String) { _dosage.value = value }
    fun setInstructions(value: String) { _instructions.value = value }
    fun setSelectedShape(value: String) { _selectedShape.value = value }
    fun setSelectedColor(value: Long) { _selectedColor.value = value }
    
    fun setScheduleType(value: ScheduleType) { 
        _scheduleType.value = value
        if (value == ScheduleType.SPECIFIC_DAYS && _specificDays.value.isEmpty()) {
            // Default to Monday (1)
            _specificDays.value = listOf(1)
        }
    }

    fun toggleSpecificDay(day: Int) {
        val current = _specificDays.value.toMutableList()
        if (current.contains(day)) {
            current.remove(day)
        } else {
            current.add(day)
        }
        _specificDays.value = current.sorted()
    }

    fun addReminderTime(time: String) {
        if (!_reminderTimes.value.contains(time)) {
            _reminderTimes.value = (_reminderTimes.value + time).sorted()
        }
    }

    fun removeReminderTime(time: String) {
        if (_reminderTimes.value.size > 1) {
            _reminderTimes.value = _reminderTimes.value - time
        }
    }

    fun setCurrentStock(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _currentStock.value = value
        }
    }

    fun toggleInventoryTracking(enabled: Boolean) {
        _isInventoryTrackingEnabled.value = enabled
    }

    fun setNotes(value: String) { _notes.value = value }

    fun saveMedication() {
        if (_name.value.isBlank()) {
            _error.value = "Medicine name is required"
            return
        }
        if (_dosage.value.isBlank()) {
            _error.value = "Dosage is required"
            return
        }
        if (_scheduleType.value == ScheduleType.SPECIFIC_DAYS && _specificDays.value.isEmpty()) {
            _error.value = "Please select at least one day"
            return
        }
        if (_reminderTimes.value.isEmpty() && _scheduleType.value != ScheduleType.AS_NEEDED) {
            _error.value = "Please add at least one reminder time"
            return
        }

        viewModelScope.launch {
            try {
                val stockCount = _currentStock.value.toIntOrNull() ?: 0
                val medication = Medication(
                    name = _name.value.trim(),
                    dosage = _dosage.value.trim(),
                    instructions = _instructions.value.trim(),
                    iconShape = _selectedShape.value,
                    iconColor = _selectedColor.value,
                    scheduleType = _scheduleType.value,
                    specificDays = _specificDays.value,
                    reminderTimes = if (_scheduleType.value == ScheduleType.AS_NEEDED) emptyList() else _reminderTimes.value,
                    currentStock = stockCount,
                    originalStock = stockCount,
                    isInventoryTrackingEnabled = _isInventoryTrackingEnabled.value,
                    notes = _notes.value.trim()
                )
                addMedicationUseCase(medication)
                _saveSuccess.emit(true)
            } catch (e: Exception) {
                _error.value = "Failed to save medication: ${e.message}"
            }
        }
    }
}
