package za.org.team25310.ftcequipmenttracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import za.org.team25310.ftcequipmenttracker.data.model.Part

@Composable
fun AliasEditorDialog(
    part: Part,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var aliases by remember { mutableStateOf(part.aliases) }
    var input by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    part.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text("Team aliases", style = MaterialTheme.typography.labelMedium)

                if (aliases.isEmpty()) {
                    Text("No aliases yet", style = MaterialTheme.typography.bodySmall)
                } else {
                    aliases.forEach { alias ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(alias, modifier = Modifier.weight(1f))
                            TextButton(onClick = { aliases = aliases - alias }) { Text("Remove") }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Add alias") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = {
                            val trimmed = input.trim()
                            if (trimmed.isNotEmpty() && aliases.none { it.equals(trimmed, ignoreCase = true) }) {
                                aliases = aliases + trimmed
                                input = ""
                            }
                        },
                    ) { Text("Add") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            onSave(aliases)
                            onDismiss()
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("Save") }
                }
            }
        }
    }
}
