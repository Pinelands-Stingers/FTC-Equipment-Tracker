package za.org.team25310.ftcequipmenttracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import za.org.team25310.ftcequipmenttracker.data.AppConfig
import za.org.team25310.ftcequipmenttracker.data.model.LoanEvent
import za.org.team25310.ftcequipmenttracker.data.model.LoanStatus
import za.org.team25310.ftcequipmenttracker.data.model.Part
import za.org.team25310.ftcequipmenttracker.data.model.Team
import za.org.team25310.ftcequipmenttracker.data.model.Vendor
import za.org.team25310.ftcequipmenttracker.ui.CatalogViewModel
import za.org.team25310.ftcequipmenttracker.ui.LoansViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLoanScreen(
    onDone: () -> Unit,
    catalogViewModel: CatalogViewModel = viewModel(),
    loansViewModel: LoansViewModel = viewModel(),
) {
    val context = LocalContext.current
    val parts by catalogViewModel.parts.collectAsState()
    val teams by catalogViewModel.teams.collectAsState()

    var selectedPart by remember { mutableStateOf<Part?>(null) }
    var isCustom by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customSku by remember { mutableStateOf("") }

    var loaner by remember { mutableStateOf<Team?>(null) }
    var borrower by remember { mutableStateOf<Team?>(null) }
    var loanDate by remember { mutableStateOf(0L) }
    var dueDate by remember { mutableStateOf(0L) }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }

    val busy by loansViewModel.busy.collectAsState()

    LaunchedEffect(teams) {
        if (loaner == null && teams.isNotEmpty()) {
            loaner = teams.firstOrNull { it.teamNumber == AppConfig.MY_TEAM_NUMBER } ?: AppConfig.myTeam
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) {
            photoUri = null
            photoFile = null
        }
    }

    fun startCamera() {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        photoFile = file
        photoUri = uri
        takePicture.launch(uri)
    }

    val requestCameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
        }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log a share") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Part", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isCustom,
                    onClick = { isCustom = false },
                    label = { Text("Catalog part") },
                )
                FilterChip(
                    selected = isCustom,
                    onClick = { isCustom = true },
                    label = { Text("Custom / 3D printed") },
                )
            }

            if (isCustom) {
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Part name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = customSku,
                    onValueChange = { customSku = it },
                    label = { Text("SKU (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SelectField(
                    label = "Select part",
                    options = parts,
                    selected = selectedPart,
                    optionLabel = { it.name },
                    onSelect = { selectedPart = it },
                )
            }

            HorizontalDivider()

            SelectField(
                label = "Loaner (lending out)",
                options = teams,
                selected = loaner,
                optionLabel = { it.displayName },
                onSelect = { loaner = it },
            )
            SelectField(
                label = "Borrower (receiving)",
                options = teams,
                selected = borrower,
                optionLabel = { it.displayName },
                onSelect = { borrower = it },
            )

            HorizontalDivider()

            DateField(label = "Loan date", value = loanDate, onChange = { loanDate = it })
            DateField(label = "Return / due date", value = dueDate, onChange = { dueDate = it })

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            Text("Item photo", style = MaterialTheme.typography.titleMedium)
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Item photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
            OutlinedButton(onClick = { launchCamera() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (photoUri == null) "Take photo" else "Retake photo")
            }

            HorizontalDivider()

            Button(
                onClick = {
                    val partName = if (isCustom) customName else selectedPart?.name.orEmpty()
                    val sku = if (isCustom) customSku else selectedPart?.sku.orEmpty()
                    val vendor = if (isCustom) Vendor.CUSTOM.name else selectedPart?.vendor?.name.orEmpty()
                    val event = LoanEvent(
                        partId = selectedPart?.id.orEmpty(),
                        partName = partName,
                        sku = sku,
                        vendor = vendor,
                        loanerTeamNumber = loaner?.teamNumber.orEmpty(),
                        loanerTeamName = loaner?.name.orEmpty(),
                        borrowerTeamNumber = borrower?.teamNumber.orEmpty(),
                        borrowerTeamName = borrower?.name.orEmpty(),
                        loanDate = loanDate,
                        dueDate = dueDate,
                        location = location,
                        notes = notes,
                        status = LoanStatus.PENDING_CONFIRMATION,
                        createdBy = FirebaseAuth.getInstance().currentUser?.email.orEmpty(),
                        createdAt = System.currentTimeMillis(),
                    )
                    loansViewModel.saveLoan(event, photoFile) { onDone() }
                },
                enabled = !busy &&
                    (if (isCustom) customName.isNotBlank() else selectedPart != null) &&
                    loaner != null &&
                    borrower != null &&
                    loanDate > 0L &&
                    dueDate > 0L,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "Saving..." else "Save share")
            }
        }
    }
}
