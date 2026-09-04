package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import za.org.team25310.ftcequipmenttracker.data.model.LoanEvent
import za.org.team25310.ftcequipmenttracker.data.model.LoanStatus
import za.org.team25310.ftcequipmenttracker.ui.LoansViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: String,
    onBack: () -> Unit,
    loansViewModel: LoansViewModel = viewModel(),
) {
    val loans by loansViewModel.loans.collectAsState()
    val loan = loans.firstOrNull { it.id == loanId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loan details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (loan == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loan not found")
            }
        } else {
            LoanDetailContent(
                loan = loan,
                onUpdate = { loansViewModel.updateLoan(it) },
            )
        }
    }
}

@Composable
private fun LoanDetailContent(
    loan: LoanEvent,
    onUpdate: (LoanEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(loan.partName.ifBlank { loan.sku }, style = MaterialTheme.typography.headlineSmall)
        if (loan.sku.isNotBlank()) {
            Text("SKU: ${loan.sku}", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "${loan.loanerTeamNumber} -> ${loan.borrowerTeamNumber}",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "Loaned ${formatDate(loan.loanDate)} — due ${formatDate(loan.dueDate)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (loan.location.isNotBlank()) {
            Text("Location: ${loan.location}", style = MaterialTheme.typography.bodyMedium)
        }
        if (loan.notes.isNotBlank()) {
            Text("Notes: ${loan.notes}", style = MaterialTheme.typography.bodyMedium)
        }

        val photoBitmap = remember(loan.photoBase64) { base64ToBitmap(loan.photoBase64) }
        if (photoBitmap != null) {
            Image(
                bitmap = photoBitmap.asImageBitmap(),
                contentDescription = "Item photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        HorizontalDivider()

        Text("Coach confirmations", style = MaterialTheme.typography.titleMedium)
        CoachConfirmation(
            label = "Loaner coach (#${loan.loanerTeamNumber})",
            confirmed = loan.loanerCoachConfirmed,
            onConfirm = { onUpdate(loan.copy(loanerCoachConfirmed = true)) },
        )
        CoachConfirmation(
            label = "Borrower coach (#${loan.borrowerTeamNumber})",
            confirmed = loan.borrowerCoachConfirmed,
            onConfirm = { onUpdate(loan.copy(borrowerCoachConfirmed = true)) },
        )

        HorizontalDivider()

        Text("Captain signatures", style = MaterialTheme.typography.titleMedium)
        SignatureSection(
            title = "Loaner captain signature",
            base64 = loan.loanerSignature,
            onSigned = { bmp -> onUpdate(loan.copy(loanerSignature = bitmapToBase64(bmp))) },
        )
        SignatureSection(
            title = "Borrower captain signature",
            base64 = loan.borrowerSignature,
            onSigned = { bmp -> onUpdate(loan.copy(borrowerSignature = bitmapToBase64(bmp))) },
        )

        HorizontalDivider()

        if (loan.status != LoanStatus.RETURNED) {
            Button(
                onClick = { onUpdate(loan.copy(status = LoanStatus.RETURNED)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Mark as returned")
            }
        } else {
            Text("Returned", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun CoachConfirmation(
    label: String,
    confirmed: Boolean,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (confirmed) {
            Text("Confirmed", color = MaterialTheme.colorScheme.primary)
        } else {
            Button(onClick = onConfirm) { Text("Confirm") }
        }
    }
}

@Composable
private fun SignatureSection(
    title: String,
    base64: String,
    onSigned: (android.graphics.Bitmap) -> Unit,
) {
    val existing = remember(base64) { base64ToBitmap(base64) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }

    Text(title, style = MaterialTheme.typography.bodyMedium)

    if (existing != null) {
        Image(
            bitmap = existing.asImageBitmap(),
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
        )
    } else {
        SignaturePad(
            strokes = strokes,
            currentStroke = currentStroke,
            onStrokeStart = { currentStroke = listOf(it) },
            onStrokeMove = { p -> currentStroke = currentStroke + p },
            onStrokeEnd = {
                if (currentStroke.isNotEmpty()) {
                    strokes.add(currentStroke)
                    currentStroke = emptyList()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .onSizeChanged { padSize = it },
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                val bmp = renderSignatureBitmap(strokes.toList(), padSize)
                if (bmp != null) onSigned(bmp)
            },
            enabled = strokes.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Confirm signature")
        }
    }
}
