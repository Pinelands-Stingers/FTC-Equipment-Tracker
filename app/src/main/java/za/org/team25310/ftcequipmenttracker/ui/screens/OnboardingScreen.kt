package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.org.team25310.ftcequipmenttracker.data.model.Role
import za.org.team25310.ftcequipmenttracker.data.model.Team
import za.org.team25310.ftcequipmenttracker.ui.CatalogViewModel
import za.org.team25310.ftcequipmenttracker.ui.UserViewModel

@Composable
fun OnboardingScreen(
    catalogViewModel: CatalogViewModel,
    userViewModel: UserViewModel,
) {
    val teams by catalogViewModel.teams.collectAsState()
    val loading by userViewModel.loading.collectAsState()
    val error by userViewModel.error.collectAsState()

    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var role by remember { mutableStateOf(Role.MEMBER) }
    var displayName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

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
                "Choose your team and role, then enter your team's invite code.",
                style = MaterialTheme.typography.bodyMedium,
            )

            SelectField(
                label = "Your team",
                options = teams,
                selected = selectedTeam,
                optionLabel = { it.displayName },
                onSelect = { selectedTeam = it },
            )

            SelectField(
                label = "Your role",
                options = Role.entries.toList(),
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

            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                label = { Text("Team invite code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    userViewModel.completeOnboarding(
                        teamId = selectedTeam?.teamNumber.orEmpty(),
                        role = role,
                        inviteCode = inviteCode.trim(),
                        displayName = displayName.trim(),
                    )
                },
                enabled = !loading &&
                    selectedTeam != null &&
                    inviteCode.isNotBlank() &&
                    displayName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (loading) "Please wait..." else "Continue")
            }
        }
    }
}
