package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("FTC Equipment Tracker", style = MaterialTheme.typography.headlineMedium)
            Text("By FTC Team #25310 Pinelands Stingers", style = MaterialTheme.typography.titleMedium)
            Text(
                "Track the parts your team loans to and borrows from other FTC teams in South Africa.",
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            Text("Data source", style = MaterialTheme.typography.titleMedium)
            Text(
                "Team data is provided by the FIRST Tech Challenge Events API.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "FTC Events API",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://ftc-events.firstinspires.org/services/API")
                },
            )

            HorizontalDivider()

            Text("Disclaimer", style = MaterialTheme.typography.titleMedium)
            Text(
                "FTC Equipment Tracker is an independent project built by FTC Team #25310 and is not " +
                    "affiliated with, endorsed by, or sponsored by goBILDA, REV Robotics, AndyMark, " +
                    "Studica, FIRST, or any other manufacturer or organization mentioned in the app. " +
                    "Product names, brands, and trademarks belong to their respective owners.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
