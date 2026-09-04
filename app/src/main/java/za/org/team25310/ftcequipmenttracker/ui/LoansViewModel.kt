package za.org.team25310.ftcequipmenttracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import za.org.team25310.ftcequipmenttracker.data.LoansRepository
import za.org.team25310.ftcequipmenttracker.data.model.LoanEvent
import za.org.team25310.ftcequipmenttracker.data.model.LoanStatus

class LoansViewModel : ViewModel() {

    private val _loans = MutableStateFlow<List<LoanEvent>>(emptyList())
    val loans: StateFlow<List<LoanEvent>> = _loans

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var teamId: String? = null
    private var loansJob: Job? = null

    fun setTeamId(id: String) {
        if (teamId == id) return
        teamId = id
        loansJob?.cancel()
        loansJob = viewModelScope.launch {
            LoansRepository.observeLoans(id)
                .catch { e -> _error.value = e.message ?: "Failed to load loans" }
                .collect { _loans.value = it }
        }
    }

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
        val updated = if (
            event.status == LoanStatus.PENDING_CONFIRMATION &&
            event.loanerCoachConfirmed &&
            event.borrowerCoachConfirmed
        ) {
            event.copy(status = LoanStatus.ACTIVE)
        } else {
            event
        }
        viewModelScope.launch {
            runCatching { LoansRepository.saveLoan(updated) }
                .onFailure { _error.value = it.message ?: "Update failed" }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
