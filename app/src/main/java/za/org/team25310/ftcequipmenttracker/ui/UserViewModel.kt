package za.org.team25310.ftcequipmenttracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import za.org.team25310.ftcequipmenttracker.data.UsersRepository
import za.org.team25310.ftcequipmenttracker.data.model.AppUser
import za.org.team25310.ftcequipmenttracker.data.model.Role

class UserViewModel : ViewModel() {

    private val _user = MutableStateFlow<AppUser?>(null)
    val user: StateFlow<AppUser?> = _user

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            _user.value = null
            _loading.value = false
            return
        }
        _loading.value = true
        viewModelScope.launch {
            runCatching { UsersRepository.getUser(uid) }
                .onSuccess { _user.value = it }
                .onFailure {
                    _user.value = null
                    _error.value = it.message
                }
            _loading.value = false
        }
    }

    fun completeOnboarding(teamId: String, role: Role, inviteCode: String, displayName: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { UsersRepository.createUser(teamId, role, inviteCode, displayName) }
                .onSuccess { _user.value = it }
                .onFailure { _error.value = it.message ?: "Onboarding failed" }
            _loading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
