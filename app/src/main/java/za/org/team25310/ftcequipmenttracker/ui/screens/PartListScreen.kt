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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.org.team25310.ftcequipmenttracker.data.model.Part
import za.org.team25310.ftcequipmenttracker.data.model.Vendor
import za.org.team25310.ftcequipmenttracker.ui.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartListScreen(
    onBack: () -> Unit,
    viewModel: CatalogViewModel,
) {
    val parts by viewModel.parts.collectAsState()
    val partsLoading by viewModel.partsLoading.collectAsState()
    var vendorFilter by remember { mutableStateOf<Vendor?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var editingPart by remember { mutableStateOf<Part?>(null) }

    val filtered = parts.filter { part ->
        (vendorFilter == null || part.vendor == vendorFilter) && part.matchesQuery(searchQuery)
    }

    editingPart?.let { part ->
        AliasEditorDialog(
            part = part,
            onSave = { viewModel.setAliases(part.id, it) },
            onDismiss = { editingPart = null },
        )
    }

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
            if (partsLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search name, SKU, tag or alias") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = vendorFilter == null,
                    onClick = { vendorFilter = null },
                    label = { Text("All") },
                )
                Vendor.entries.filter { it != Vendor.CUSTOM }.forEach { vendor ->
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
                items(filtered, key = { it.id }) { part ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
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
                                if (part.tags.isNotEmpty()) {
                                    Text(
                                        "Tags: ${part.tags.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            IconButton(onClick = { editingPart = part }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit aliases")
                            }
                        }
                    }
                }
            }
        }
    }
}
