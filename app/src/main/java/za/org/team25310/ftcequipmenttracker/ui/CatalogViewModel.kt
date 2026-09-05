package za.org.team25310.ftcequipmenttracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import za.org.team25310.ftcequipmenttracker.data.CatalogRepository
import za.org.team25310.ftcequipmenttracker.data.model.Part
import za.org.team25310.ftcequipmenttracker.data.model.Team

class CatalogViewModel(app: Application) : AndroidViewModel(app) {
    private val _parts = MutableStateFlow<List<Part>>(emptyList())
    val parts: StateFlow<List<Part>> = _parts

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            _parts.value = CatalogRepository.loadParts(getApplication())
            _teams.value = CatalogRepository.loadTeams(getApplication())
            _loading.value = false
        }
    }

    fun refreshTeams() {
        viewModelScope.launch {
            _refreshing.value = true
            _error.value = null
            runCatching { CatalogRepository.refreshTeams(getApplication()) }
                .onSuccess { _teams.value = it }
                .onFailure { _error.value = it.message ?: "Refresh failed" }
            _refreshing.value = false
        }
    }
}
