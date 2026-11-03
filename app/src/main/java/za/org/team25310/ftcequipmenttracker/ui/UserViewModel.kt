package za.org.team25310.ftcequipmenttracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import za.org.team25310.ftcequipmenttracker.data.TeamAdminRepository
import za.org.team25310.ftcequipmenttracker.data.TeamInvitesRepository
import za.org.team25310.ftcequipmenttracker.data.UsersRepository
import za.org.team25310.ftcequipmenttracker.data.model.AppUser
import za.org.team25310.ftcequipmenttracker.data.model.Role
import za.org.team25310.ftcequipmenttracker.data.model.TeamInfo
import za.org.team25310.ftcequipmenttracker.data.model.TeamInvite

sealed interface TeamInviteStatus {
    data object Unknown : TeamInviteStatus
    data object Checking : TeamInviteStatus
    data object NotCreated : TeamInviteStatus
    data object Exists : TeamInviteStatus
}

class UserViewModel : ViewModel() {

    private val _user = MutableStateFlow<AppUser?>(null)
    val user: StateFlow<AppUser?> = _user

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _inviteStatus = MutableStateFlow<TeamInviteStatus>(TeamInviteStatus.Unknown)
    val inviteStatus: StateFlow<TeamInviteStatus> = _inviteStatus

    private val _newTeamInvite = MutableStateFlow<TeamInvite?>(null)
    val newTeamInvite: StateFlow<TeamInvite?> = _newTeamInvite

    private val _teamInvite = MutableStateFlow<TeamInvite?>(null)
    val teamInvite: StateFlow<TeamInvite?> = _teamInvite

    private val _verificationRequired = MutableStateFlow(false)
    val verificationRequired: StateFlow<Boolean> = _verificationRequired

    private val _teamInfo = MutableStateFlow<Map<String, TeamInfo>>(emptyMap())
    val teamInfo: StateFlow<Map<String, TeamInfo>> = _teamInfo

    private val _reportMessage = MutableStateFlow<String?>(null)
    val reportMessage: StateFlow<String?> = _reportMessage

    private var inviteCheckJob: Job? = null

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

    fun checkInvite(teamId: String) {
        inviteCheckJob?.cancel()
        if (teamId.isBlank()) {
            _inviteStatus.value = TeamInviteStatus.Unknown
            return
        }
        inviteCheckJob = viewModelScope.launch {
            _inviteStatus.value = TeamInviteStatus.Checking
            runCatching {
                val required = TeamAdminRepository.isVerificationRequired()
                val info = TeamAdminRepository.getTeamInfo(teamId)
                required to info
            }
                .onSuccess { (required, info) ->
                    _verificationRequired.value = required
                    _teamInfo.value = _teamInfo.value + (teamId to info)
                    _inviteStatus.value =
                        if (info.claimed) TeamInviteStatus.Exists else TeamInviteStatus.NotCreated
                }
                .onFailure {
                    _error.value = it.message
                    _inviteStatus.value = TeamInviteStatus.Unknown
                }
        }
    }

    fun createTeam(teamId: String, role: Role, displayName: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { UsersRepository.createUserForNewTeam(teamId, role, displayName) }
                .onSuccess { invite ->
                    _newTeamInvite.value = invite
                    _inviteStatus.value = TeamInviteStatus.Exists
                    refreshUser()
                }
                .onFailure { _error.value = it.message ?: "Could not create team" }
            _loading.value = false
        }
    }

    fun joinTeam(teamId: String, role: Role, inviteCode: String, displayName: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { UsersRepository.joinTeam(teamId, role, inviteCode, displayName) }
                .onSuccess { refreshUser() }
                .onFailure { _error.value = it.message ?: "Could not join team" }
            _loading.value = false
        }
    }

    fun loadTeamInvite(teamId: String) {
        if (teamId.isBlank()) {
            _teamInvite.value = null
            return
        }
        viewModelScope.launch {
            runCatching { TeamInvitesRepository.getInvite(teamId) }
                .onSuccess { _teamInvite.value = it }
                .onFailure { _error.value = it.message ?: "Could not load invite codes" }
        }
    }

    fun loadTeamInfo(teamIds: Collection<String>) {
        val ids = teamIds.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                val required = TeamAdminRepository.isVerificationRequired()
                val infos = ids.map { it to TeamAdminRepository.getTeamInfo(it) }
                required to infos
            }
                .onSuccess { (required, infos) ->
                    _verificationRequired.value = required
                    _teamInfo.value = _teamInfo.value + infos.toMap()
                }
                .onFailure { _error.value = it.message ?: "Could not load team info" }
        }
    }

    fun reportTeam(teamId: String, reason: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            runCatching {
                TeamAdminRepository.reportTeam(
                    teamId = teamId,
                    reportedByUid = currentUser.uid,
                    reportedByEmail = currentUser.email.orEmpty(),
                    reason = reason,
                )
            }
                .onSuccess {
                    _reportMessage.value = "Report sent. An organizer will review this team."
                }
                .onFailure {
                    _reportMessage.value = it.message ?: "Could not send report"
                }
        }
    }

    fun consumeReportMessage() {
        _reportMessage.value = null
    }

    fun generateTeamInvite(teamId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                TeamInvitesRepository.createInvite(
                    teamId = teamId,
                    createdBy = currentUser.uid,
                    createdByEmail = currentUser.email.orEmpty(),
                )
            }
                .onSuccess { _teamInvite.value = it }
                .onFailure { _error.value = it.message ?: "Could not generate codes" }
            _loading.value = false
        }
    }

    fun rotateTeamInvite(teamId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { TeamInvitesRepository.rotateInvite(teamId, uid) }
                .onSuccess { _teamInvite.value = it }
                .onFailure { _error.value = it.message ?: "Could not regenerate codes" }
            _loading.value = false
        }
    }

    fun consumeNewTeamInvite() {
        _newTeamInvite.value = null
    }

    private suspend fun refreshUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching { UsersRepository.getUser(uid) }
            .onSuccess { _user.value = it }
            .onFailure { _error.value = it.message }
    }

    fun clearError() {
        _error.value = null
    }
}
