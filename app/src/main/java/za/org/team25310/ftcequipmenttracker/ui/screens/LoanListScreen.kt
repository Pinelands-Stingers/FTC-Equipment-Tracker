package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import za.org.team25310.ftcequipmenttracker.data.model.LoanEvent
import za.org.team25310.ftcequipmenttracker.data.model.LoanStatus
import za.org.team25310.ftcequipmenttracker.ui.AuthViewModel
import za.org.team25310.ftcequipmenttracker.ui.CatalogViewModel
import za.org.team25310.ftcequipmenttracker.ui.LoansViewModel
import za.org.team25310.ftcequipmenttracker.ui.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanListScreen(
    onNewLoan: () -> Unit,
    onTeams: () -> Unit,
    onParts: () -> Unit,
    onOpenLoan: (String) -> Unit,
    onAbout: () -> Unit,
    onTeamSettings: () -> Unit,
    onSignedOut: () -> Unit,
    loansViewModel: LoansViewModel,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    catalogViewModel: CatalogViewModel,
) {
    val loans by loansViewModel.loans.collectAsState()
    val user by userViewModel.user.collectAsState()
    val loading by userViewModel.loading.collectAsState()
    val loansError by loansViewModel.error.collectAsState()
    val newTeamInvite by userViewModel.newTeamInvite.collectAsState()

    LaunchedEffect(Unit) { userViewModel.loadUser() }
    LaunchedEffect(user?.teamId) {
        val teamId = user?.teamId
        if (teamId != null) {
            loansViewModel.setTeamId(teamId)
            catalogViewModel.setTeamId(teamId)
        }
    }

    when {
        loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        user == null -> {
            OnboardingScreen(catalogViewModel = catalogViewModel, userViewModel = userViewModel)
        }

        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Loans") },
                        actions = {
                            IconButton(onClick = onParts) { Icon(Icons.Filled.Build, contentDescription = "Parts") }
                            IconButton(onClick = onTeams) { Icon(Icons.Filled.List, contentDescription = "Teams") }
                            IconButton(onClick = onTeamSettings) {
                                Icon(Icons.Filled.Group, contentDescription = "Team invite codes")
                            }
                            IconButton(onClick = onAbout) { Icon(Icons.Filled.Info, contentDescription = "About") }
                            IconButton(onClick = {
                                authViewModel.signOut()
                                onSignedOut()
                            }) {
                                Icon(Icons.Filled.ExitToApp, contentDescription = "Sign out")
                            }
                        },
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = onNewLoan) {
                        Icon(Icons.Filled.Add, contentDescription = "New loan")
                    }
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    loansError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    if (loans.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No loans yet. Tap + to log a share.", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(loans, key = { it.id }) { loan ->
                                LoanCard(loan = loan, onClick = { onOpenLoan(loan.id) })
                            }
                        }
                    }
                }
            }

            newTeamInvite?.let { invite ->
                AlertDialog(
                    onDismissRequest = { userViewModel.consumeNewTeamInvite() },
                    title = { Text("Team account created") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Share these codes with your teammates. Coaches and captains use the " +
                                    "leader code; members use the member code.",
                            )
                            InviteCodeRow(label = "Member code", code = invite.memberCode)
                            InviteCodeRow(
                                label = "Leader code (coach / captain)",
                                code = invite.coachCode,
                            )
                            Text(
                                "You can view or regenerate these later from the team icon on the " +
                                    "loans screen.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { userViewModel.consumeNewTeamInvite() }) {
                            Text("Done")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoanCard(loan: LoanEvent, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LoanThumbnail(loan)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(loan.partName.ifBlank { loan.sku }, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${loan.loanerTeamNumber} -> ${loan.borrowerTeamNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Due ${formatDate(loan.dueDate)}", style = MaterialTheme.typography.bodySmall)
                    StatusBadge(loan.status)
                }
            }
        }
    }
}

@Composable
private fun LoanThumbnail(loan: LoanEvent) {
    val bitmap = remember(loan.photoBase64) { base64ToBitmap(loan.photoBase64) }
    val thumbnailModifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(8.dp))
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Item photo",
            modifier = thumbnailModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = thumbnailModifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = loan.partName.firstOrNull()?.toString()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: LoanStatus) {
    val (label, color) = when (status) {
        LoanStatus.ACTIVE -> "Active" to MaterialTheme.colorScheme.primary
        LoanStatus.PENDING_CONFIRMATION -> "Awaiting confirmation" to MaterialTheme.colorScheme.tertiary
        LoanStatus.RETURNED -> "Returned" to MaterialTheme.colorScheme.secondary
        LoanStatus.OVERDUE -> "Overdue" to MaterialTheme.colorScheme.error
        else -> status.name to Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
