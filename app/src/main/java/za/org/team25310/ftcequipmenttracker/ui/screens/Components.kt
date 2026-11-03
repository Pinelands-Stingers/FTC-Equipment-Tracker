package za.org.team25310.ftcequipmenttracker.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import za.org.team25310.ftcequipmenttracker.data.model.Part
import za.org.team25310.ftcequipmenttracker.data.model.TeamInfo
import za.org.team25310.ftcequipmenttracker.data.model.Vendor

fun formatDate(millis: Long): String =
    if (millis <= 0L) "" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

fun formatInviteCode(code: String): String =
    if (code.length == 8) "${code.substring(0, 4)}-${code.substring(4)}" else code

@Composable
fun InviteCodeRow(label: String, code: String) {
    val clipboard = LocalClipboardManager.current
    val formatted = remember(code) { formatInviteCode(code) }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatted, style = MaterialTheme.typography.headlineSmall)
            }
            IconButton(onClick = { clipboard.setText(AnnotatedString(formatted)) }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $label")
            }
        }
    }
}

@Composable
fun TeamClaimRow(
    teamLabel: String,
    info: TeamInfo?,
    verificationRequired: Boolean,
    onReport: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(teamLabel, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (info != null) {
                    VerificationBadge(verified = info.verified, required = verificationRequired)
                }
            }
            when {
                info == null -> Text(
                    "Loading team account...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                !info.claimed -> Text(
                    "No account claimed yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                info.claimedByEmail.isBlank() -> Text(
                    "Claimed ${formatDate(info.claimedAt)} by an unrecorded account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> Text(
                    "Claimed by ${info.claimedByEmail} on ${formatDate(info.claimedAt)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (verificationRequired && info != null && !info.verified) {
                Text(
                    "Confirmations and signatures are paused for unverified teams.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            TextButton(
                onClick = onReport,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Report a problem", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun VerificationBadge(verified: Boolean, required: Boolean) {
    val (label, color) = when {
        verified -> "Verified" to MaterialTheme.colorScheme.primary
        required -> "Not verified" to MaterialTheme.colorScheme.error
        else -> "Unverified" to MaterialTheme.colorScheme.tertiary
    }
    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun ReportTeamDialog(
    teamLabel: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $teamLabel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Tell an organizer what looks wrong, including anything that shows this team " +
                        "account isn't genuine.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("What's wrong?") },
                    supportingText = { Text("At least 10 characters.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(reason.trim()) },
                enabled = reason.trim().length >= 10,
            ) {
                Text("Send report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun <T> SelectField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = selected?.let(optionLabel) ?: "Select...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: Long,
    onChange: (Long) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (value <= 0L) "Select date" else formatDate(value))
        }
    }
    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = if (value <= 0L) System.currentTimeMillis() else value,
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onChange)
                    show = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun SignaturePad(
    strokes: List<List<Offset>>,
    currentStroke: List<Offset>,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onStrokeStart(down.position)
                    var last = down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) break
                        val position = change.position
                        if (position != last) {
                            onStrokeMove(position)
                            last = position
                        }
                        change.consume()
                    }
                    onStrokeEnd()
                }
            },
    ) {
        drawRect(Color.White)
        val strokePaint = Stroke(width = 4f)
        strokes.forEach { pts -> drawStroke(pts, strokePaint) }
        drawStroke(currentStroke, strokePaint)
    }
}

private fun DrawScope.drawStroke(points: List<Offset>, paint: Stroke) {
    when {
        points.size == 1 -> drawCircle(Color.Black, radius = 2f, center = points.first())
        points.size > 1 -> {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(path, Color.Black, style = paint)
        }
    }
}

fun renderSignatureBitmap(strokes: List<List<Offset>>, size: IntSize): Bitmap? {
    if (size.width <= 0 || size.height <= 0) return null
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    strokes.forEach { pts ->
        when {
            pts.size == 1 -> canvas.drawPoint(pts.first().x, pts.first().y, paint)
            pts.size > 1 -> {
                val path = android.graphics.Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                }
                canvas.drawPath(path, paint)
            }
        }
    }
    return bitmap
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
    return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}

fun base64ToBitmap(encoded: String): Bitmap? {
    if (encoded.isBlank()) return null
    return runCatching {
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

fun Part.matchesQuery(query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return name.lowercase().contains(q) ||
        sku.lowercase().contains(q) ||
        tags.any { it.lowercase().contains(q) } ||
        aliases.any { it.lowercase().contains(q) }
}

@Composable
fun PartPickerField(
    label: String,
    parts: List<Part>,
    selected: Part?,
    onSelect: (Part) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = selected?.let { "${it.name} (${it.vendor.displayName})" } ?: "Select part...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (showDialog) {
        PartPickerDialog(
            parts = parts,
            onSelect = {
                onSelect(it)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun PartPickerDialog(
    parts: List<Part>,
    onSelect: (Part) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var vendorFilter by remember { mutableStateOf<Vendor?>(null) }
    val matches = remember(parts, query, vendorFilter) {
        parts.filter { part ->
            (vendorFilter == null || part.vendor == vendorFilter) && part.matchesQuery(query)
        }
    }
    val shown = matches.take(50)
    val hiddenCount = matches.size - shown.size
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search parts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                Spacer(Modifier.height(8.dp))
                if (shown.isEmpty()) {
                    Text("No matches", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                    ) {
                        items(shown, key = { it.id }) { part ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(part) }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    part.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${part.vendor.displayName}${if (part.sku.isNotBlank()) " | ${part.sku}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    if (hiddenCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "+$hiddenCount more (refine your search)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
