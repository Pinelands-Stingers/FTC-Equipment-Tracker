package za.org.team25310.ftcequipmenttracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.org.team25310.ftcequipmenttracker.data.LoansRepository
import za.org.team25310.ftcequipmenttracker.data.model.LoanEvent

class LoansViewModel : ViewModel() {
    val loans: StateFlow<List<LoanEvent>> = LoansRepository.observeLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun saveLoan(event: LoanEvent, photoFile: File?, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            runCatching {
                var updated = event
                if (photoFile != null && photoFile.exists()) {
                    val base64 = LoansRepository.compressPhotoToBase64(photoFile)
                    updated = event.copy(photoBase64 = base64)
                }
                LoansRepository.saveLoan(updated)
            }
                .onSuccess { onDone(it) }
                .onFailure { _error.value = it.message ?: "Save failed" }
            _busy.value = false
        }
    }

    fun updateLoan(event: LoanEvent) {
        viewModelScope.launch {
            runCatching { LoansRepository.saveLoan(event) }
                .onFailure { _error.value = it.message ?: "Update failed" }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
