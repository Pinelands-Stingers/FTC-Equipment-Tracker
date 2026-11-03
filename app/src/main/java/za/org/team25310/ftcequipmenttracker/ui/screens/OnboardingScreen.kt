package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import za.org.team25310.ftcequipmenttracker.data.model.Role
import za.org.team25310.ftcequipmenttracker.data.model.Team
import za.org.team25310.ftcequipmenttracker.ui.CatalogViewModel
import za.org.team25310.ftcequipmenttracker.ui.TeamInviteStatus
import za.org.team25310.ftcequipmenttracker.ui.UserViewModel

@Composable
fun OnboardingScreen(
    catalogViewModel: CatalogViewModel,
    userViewModel: UserViewModel,
) {
    val teams by catalogViewModel.teams.collectAsState()
    val loading by userViewModel.loading.collectAsState()
    val error by userViewModel.error.collectAsState()
    val inviteStatus by userViewModel.inviteStatus.collectAsState()
    val verificationRequired by userViewModel.verificationRequired.collectAsState()
    val teamInfoMap by userViewModel.teamInfo.collectAsState()

    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var role by remember { mutableStateOf(Role.COACH) }
    var displayName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

    LaunchedEffect(selectedTeam?.teamNumber) {
        selectedTeam?.teamNumber?.let { userViewModel.checkInvite(it) }
    }
    LaunchedEffect(inviteStatus) {
        if (inviteStatus == TeamInviteStatus.NotCreated && role == Role.MEMBER) {
            role = Role.COACH
        }
    }

    val roleOptions = if (inviteStatus == TeamInviteStatus.NotCreated) {
        listOf(Role.COACH, Role.CAPTAIN)
    } else {
        Role.entries.toList()
    }

    val teamNumber = selectedTeam?.teamNumber.orEmpty()
    val teamInfo = teamInfoMap[teamNumber]
    val blockedByVerification = verificationRequired && teamInfo?.verified != true
    val canSubmit = !loading &&
        teamNumber.isNotBlank() &&
        displayName.isNotBlank() &&
        !blockedByVerification &&
        (inviteStatus == TeamInviteStatus.NotCreated ||
            (inviteStatus == TeamInviteStatus.Exists && inviteCode.isNotBlank()))

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Set up your account", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Choose your team and role. Each team has its own invite codes, generated automatically " +
                    "when the first person sets up the team account.",
                style = MaterialTheme.typography.bodyMedium,
            )

            SelectField(
                label = "Your team",
                options = teams,
                selected = selectedTeam,
                optionLabel = { it.displayName },
                onSelect = {
                    selectedTeam = it
                    inviteCode = ""
                    userViewModel.clearError()
                },
            )

            SelectField(
                label = "Your role",
                options = roleOptions,
                selected = role,
                optionLabel = { it.label },
                onSelect = { role = it },
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Your name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            when (inviteStatus) {
                TeamInviteStatus.Unknown -> {
                    Text(
                        "Select your team to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TeamInviteStatus.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Checking this team...", style = MaterialTheme.typography.bodySmall)
                    }
                }

                TeamInviteStatus.NotCreated -> {
                    if (blockedByVerification) {
                        VerificationWarning(teamNumber)
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("New team account", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "No account exists for this team yet. Create it now and the app will " +
                                        "generate a member code and a leader code for your team. Share them " +
                                        "with your teammates so they can join.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                TeamInviteStatus.Exists -> {
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = { Text("Team invite code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (blockedByVerification) {
                        VerificationWarning(teamNumber)
                    }
                    Text(
                        "Coaches and captains enter the leader code. Members enter the member code. " +
                            "Ask your coach if you don't have it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (inviteStatus == TeamInviteStatus.NotCreated) {
                        userViewModel.createTeam(
                            teamId = teamNumber,
                            role = role,
                            displayName = displayName.trim(),
                        )
                    } else {
                        userViewModel.joinTeam(
                            teamId = teamNumber,
                            role = role,
                            inviteCode = inviteCode.trim(),
                            displayName = displayName.trim(),
                        )
                    }
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val label = when {
                    loading -> "Please wait..."
                    inviteStatus == TeamInviteStatus.NotCreated -> "Create team account"
                    else -> "Join team"
                }
                Text(label)
            }
        }
    }
}

@Composable
private fun VerificationWarning(teamNumber: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Team not verified", style = MaterialTheme.typography.titleSmall)
            Text(
                "Team #$teamNumber hasn't been verified by an organizer yet. Ask them to verify " +
                    "it before creating or joining its account.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
