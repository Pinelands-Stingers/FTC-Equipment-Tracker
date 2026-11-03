package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.org.team25310.ftcequipmenttracker.ui.CatalogViewModel
import za.org.team25310.ftcequipmenttracker.ui.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSettingsScreen(
    onBack: () -> Unit,
    userViewModel: UserViewModel,
    catalogViewModel: CatalogViewModel,
) {
    val user by userViewModel.user.collectAsState()
    val invite by userViewModel.teamInvite.collectAsState()
    val loading by userViewModel.loading.collectAsState()
    val error by userViewModel.error.collectAsState()
    val teams by catalogViewModel.teams.collectAsState()
    val verificationRequired by userViewModel.verificationRequired.collectAsState()
    val teamInfoMap by userViewModel.teamInfo.collectAsState()
    val reportMessage by userViewModel.reportMessage.collectAsState()

    var showRotateDialog by remember { mutableStateOf(false) }
    var reportTeamId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user?.teamId, user?.isAuthority) {
        val current = user ?: return@LaunchedEffect
        if (current.teamId.isBlank()) return@LaunchedEffect
        userViewModel.loadTeamInfo(listOf(current.teamId))
        if (current.isAuthority) userViewModel.loadTeamInvite(current.teamId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val currentUser = user
            val teamLabel = teams.firstOrNull { it.teamNumber == currentUser?.teamId }?.displayName
                ?: "#${currentUser?.teamId.orEmpty()}"
            Text(teamLabel, style = MaterialTheme.typography.titleMedium)

            val teamInfo = teamInfoMap[currentUser?.teamId]
            TeamClaimRow(
                teamLabel = "This team's account",
                info = teamInfo,
                verificationRequired = verificationRequired,
                onReport = {
                    currentUser?.teamId?.takeIf { it.isNotBlank() }?.let { reportTeamId = it }
                },
            )

            reportMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = { userViewModel.consumeReportMessage() }) {
                    Text("Dismiss")
                }
            }

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            when {
                currentUser == null -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                !currentUser.isAuthority -> {
                    Text(
                        "Only coaches and captains can view or regenerate invite codes. " +
                            "Ask your coach for the code you need.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                else -> {
                    val codesBlocked = verificationRequired && teamInfo?.verified != true

                    Text(
                        "Share the member code with your team's private group chat or at a build " +
                            "meeting. Share the leader code only with coaches and captains. Never post " +
                            "either code publicly.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (codesBlocked) {
                        Text(
                            "This team is not verified yet, so invite codes can't be created or " +
                                "rotated. Ask an organizer to verify the team first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    HorizontalDivider()

                    if (loading && invite == null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (invite == null) {
                        Text(
                            "No invite codes have been generated for this team yet.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(
                            onClick = { userViewModel.generateTeamInvite(currentUser.teamId) },
                            enabled = !loading && !codesBlocked,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (loading) "Please wait..." else "Generate codes")
                        }
                    } else {
                        InviteCodeRow(label = "Member code", code = invite?.memberCode.orEmpty())
                        InviteCodeRow(
                            label = "Leader code (coach / captain)",
                            code = invite?.coachCode.orEmpty(),
                        )

                        HorizontalDivider()

                        Text(
                            "If a code leaks, regenerate it. Old codes stop working immediately. " +
                                "People who already joined keep their accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Button(
                            onClick = { showRotateDialog = true },
                            enabled = !loading && !codesBlocked,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (loading) "Please wait..." else "Regenerate codes")
                        }
                    }
                }
            }
        }
    }

    if (showRotateDialog) {
        AlertDialog(
            onDismissRequest = { showRotateDialog = false },
            title = { Text("Regenerate invite codes?") },
            text = {
                Text(
                    "The current member and leader codes will stop working immediately. " +
                        "Anyone who already joined keeps their account.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRotateDialog = false
                        user?.teamId?.let { userViewModel.rotateTeamInvite(it) }
                    },
                ) {
                    Text("Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRotateDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    reportTeamId?.let { teamId ->
        ReportTeamDialog(
            teamLabel = "#$teamId",
            onDismiss = { reportTeamId = null },
            onSubmit = { reason ->
                userViewModel.reportTeam(teamId, reason)
                reportTeamId = null
            },
        )
    }
}
