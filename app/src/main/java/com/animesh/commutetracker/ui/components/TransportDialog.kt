package com.animesh.commutetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.animesh.commutetracker.data.model.CommuteMode
import com.animesh.commutetracker.data.model.TransportMode

@Composable
fun TransportDialog(
    recentCosts: List<Int>,
    totalDuration: Int,
    onModeSelected: (TransportMode) -> Unit = {},
    onConfirm: (List<CommuteMode>) -> Unit,
    onDismiss: () -> Unit
) {
    var modes by remember { mutableStateOf(emptyList<CommuteMode>()) }
    var showAddMode by remember { mutableStateOf(modes.isEmpty()) }
    
    var selectedMode by remember { mutableStateOf<TransportMode?>(null) }
    var durationText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("Commute Details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (modes.isNotEmpty()) {
                    Text("Added Modes:", fontWeight = FontWeight.Bold)
                    modes.forEachIndexed { index, mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${mode.transportMode.name} — ${mode.durationMinutes} min — ₹${mode.cost}")
                            IconButton(onClick = { modes = modes.toMutableList().apply { removeAt(index) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                    val currentTotal = modes.sumOf { it.durationMinutes }
                    Text("Total Duration so far: $currentTotal min", style = MaterialTheme.typography.bodySmall)
                    if (currentTotal > totalDuration && totalDuration > 0) {
                        Text("Warning: Exceeds tracked duration ($totalDuration min)", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                if (showAddMode) {
                    Text("Add Transport Mode", style = MaterialTheme.typography.titleSmall)
                    
                    // Transport Selection
                    Text("Transport:", modifier = Modifier.padding(top = 8.dp))
                    LazyRow(modifier = Modifier.padding(vertical = 4.dp)) {
                        items(TransportMode.entries) { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { 
                                    selectedMode = mode
                                    onModeSelected(mode)
                                    if (mode == TransportMode.WALK) costText = "0"
                                },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) durationText = it },
                        label = { Text("Duration (min)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )

                    OutlinedTextField(
                        value = costText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) costText = it },
                        label = { Text("Cost (INR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    
                    if (recentCosts.isNotEmpty() && selectedMode != null) {
                        Text("Recent Costs:", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall)
                        LazyRow(modifier = Modifier.padding(top = 4.dp)) {
                            items(recentCosts) { cost ->
                                SuggestionChip(
                                    onClick = { costText = cost.toString() },
                                    label = { Text("₹$cost") },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull() ?: 0
                            val cost = costText.toIntOrNull() ?: 0
                            selectedMode?.let {
                                modes = modes.toMutableList().apply { 
                                    add(CommuteMode(transportMode = it, durationMinutes = duration, cost = cost, commuteId = 0)) 
                                }
                                selectedMode = null
                                durationText = ""
                                costText = ""
                                showAddMode = false
                            }
                        },
                        enabled = selectedMode != null && durationText.isNotBlank() && costText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Add Mode")
                    }
                } else {
                    Button(
                        onClick = { showAddMode = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Another Mode")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isProcessing) return@TextButton
                    isProcessing = true
                    onConfirm(modes)
                },
                enabled = !isProcessing && modes.isNotEmpty()
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Save Commute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("Cancel") }
        }
    )
}
