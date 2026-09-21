package za.org.team25310.ftcequipmenttracker.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import za.org.team25310.ftcequipmenttracker.data.model.Part

fun formatDate(millis: Long): String =
    if (millis <= 0L) "" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))

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
        tags.any { it.lowercase().contains(q) }
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
    val results = remember(parts, query) {
        val base = if (query.isBlank()) parts else parts.filter { it.matchesQuery(query) }
        base.take(50)
    }
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
                if (results.isEmpty()) {
                    Text("No matches", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(results, key = { it.id }) { part ->
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
                }
            }
        }
    }
}
