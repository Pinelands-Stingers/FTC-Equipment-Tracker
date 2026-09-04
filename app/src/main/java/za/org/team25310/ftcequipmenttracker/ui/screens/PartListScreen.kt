package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.org.team25310.ftcequipmenttracker.data.model.Vendor
import za.org.team25310.ftcequipmenttracker.ui.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartListScreen(
    onBack: () -> Unit,
    viewModel: CatalogViewModel,
) {
    val parts by viewModel.parts.collectAsState()
    var vendorFilter by remember { mutableStateOf<Vendor?>(null) }

    val filtered = if (vendorFilter == null) parts else parts.filter { it.vendor == vendorFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parts Catalog") },
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
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = vendorFilter == null,
                    onClick = { vendorFilter = null },
                    label = { Text("All") },
                )
                Vendor.entries.forEach { vendor ->
                    FilterChip(
                        selected = vendorFilter == vendor,
                        onClick = { vendorFilter = vendor },
                        label = { Text(vendor.displayName) },
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered) { part ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(part.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "SKU: ${part.sku.ifBlank { "n/a" }}  |  ${part.vendor.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (part.aliases.isNotEmpty()) {
                                Text(
                                    "Aliases: ${part.aliases.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
