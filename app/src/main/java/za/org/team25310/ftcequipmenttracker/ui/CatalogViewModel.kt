package za.org.team25310.ftcequipmenttracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import za.org.team25310.ftcequipmenttracker.data.AliasesRepository
import za.org.team25310.ftcequipmenttracker.data.CatalogRepository
import za.org.team25310.ftcequipmenttracker.data.PartsRepository
import za.org.team25310.ftcequipmenttracker.data.model.Part
import za.org.team25310.ftcequipmenttracker.data.model.Team

class CatalogViewModel(app: Application) : AndroidViewModel(app) {

    private val _baseParts = MutableStateFlow<List<Part>>(emptyList())
    private val _aliases = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val parts: StateFlow<List<Part>> = combine(_baseParts, _aliases) { parts, aliases ->
        if (aliases.isEmpty()) {
            parts
        } else {
            parts.map { part ->
                val teamAliases = aliases[part.id]
                if (teamAliases.isNullOrEmpty()) part else part.copy(aliases = teamAliases)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _partsLoading = MutableStateFlow(true)
    val partsLoading: StateFlow<Boolean> = _partsLoading

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var teamId: String? = null

    init {
        viewModelScope.launch {
            runCatching { PartsRepository.loadParts(getApplication()) }
                .onSuccess { _baseParts.value = it }
                .onFailure { _error.value = it.message }
            _partsLoading.value = false

            _teams.value = CatalogRepository.loadTeams(getApplication())
            _loading.value = false
        }
    }

    fun setTeamId(id: String) {
        if (teamId == id) return
        teamId = id
        viewModelScope.launch {
            runCatching { AliasesRepository.loadAliases(id) }
                .onSuccess { _aliases.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun setAliases(partId: String, aliases: List<String>) {
        val currentTeam = teamId ?: return
        val cleaned = aliases.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        _aliases.value = _aliases.value.toMutableMap().apply {
            if (cleaned.isEmpty()) remove(partId) else put(partId, cleaned)
        }
        viewModelScope.launch {
            runCatching { AliasesRepository.saveAliases(currentTeam, partId, cleaned) }
                .onFailure { _error.value = it.message ?: "Could not save alias" }
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
