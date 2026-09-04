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

    init {
        viewModelScope.launch {
            _parts.value = CatalogRepository.loadParts(getApplication())
            _teams.value = CatalogRepository.loadTeams(getApplication())
            _loading.value = false
        }
    }
}
