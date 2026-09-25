package com.animesh.commutetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.animesh.commutetracker.data.model.CommuteMode
import com.animesh.commutetracker.data.model.TransportMode

@Composable
fun TransportDialog(
    recentCosts: List<Int>,
    totalDuration: Int,
    initialModes: List<CommuteMode> = emptyList(),
    onModeSelected: (TransportMode) -> Unit = {},
    onConfirm: (List<CommuteMode>) -> Unit,
    onDismiss: () -> Unit
) {
    var modes by remember { mutableStateOf(initialModes) }
    var showAddMode by remember { mutableStateOf(modes.isEmpty()) }
    
    var selectedMode by remember { mutableStateOf<TransportMode?>(null) }
    var durationText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val isCurrentModeValid = selectedMode != null && (costText.isNotBlank() || selectedMode == TransportMode.SHUTTLE || selectedMode == TransportMode.WALK)
    val currentDuration = durationText.toIntOrNull() ?: 0
    val committedSum = modes.sumOf { it.durationMinutes ?: 0 }
    val potentialTotal = committedSum + if (showAddMode) currentDuration else 0
    val isDurationExceeded = totalDuration > 0 && potentialTotal > totalDuration

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { 
            Text(
                "Commute Details", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ) 
        },
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (modes.isNotEmpty()) {
                    Text(
                        "Added Modes", 
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            modes.forEachIndexed { index, mode ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val durationDisplay = mode.durationMinutes?.let { "$it min • " } ?: ""
                                    Text(
                                        text = "${mode.transportMode.name.lowercase().replaceFirstChar { it.uppercase() }} — $durationDisplay₹${mode.cost}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = { modes = modes.toMutableList().apply { removeAt(index) } },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    if (committedSum > 0) {
                        Text(
                            "Total Added Duration: $committedSum min", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                }

                if (showAddMode) {
                    Text(
                        if (modes.isEmpty()) "Transport Mode" else "Add Transport Mode", 
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyRow(
                        modifier = Modifier.padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(TransportMode.entries) { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { 
                                    selectedMode = mode
                                    onModeSelected(mode)
                                    if (mode == TransportMode.SHUTTLE || mode == TransportMode.WALK) {
                                        costText = "0"
                                    } else if (costText == "0") {
                                        costText = ""
                                    }
                                },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) durationText = it },
                        label = { Text("Duration (min, optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        isError = isDurationExceeded,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (isDurationExceeded) {
                        Text(
                            "Durations total $potentialTotal min, but tracked commute is $totalDuration min.", 
                            color = MaterialTheme.colorScheme.error, 
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = costText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) costText = it },
                        label = { Text("Cost (INR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        readOnly = selectedMode == TransportMode.SHUTTLE || selectedMode == TransportMode.WALK,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (recentCosts.isNotEmpty() && selectedMode != null && selectedMode != TransportMode.SHUTTLE && selectedMode != TransportMode.WALK) {
                        Text("Recent Costs:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentCosts) { cost ->
                                SuggestionChip(
                                    onClick = { costText = cost.toString() },
                                    label = { Text("₹$cost") }
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            selectedMode?.let {
                                modes = modes.toMutableList().apply { 
                                    add(CommuteMode(transportMode = it, durationMinutes = durationText.toIntOrNull(), cost = costText.toIntOrNull() ?: 0, commuteId = 0)) 
                                }
                                selectedMode = null
                                durationText = ""
                                costText = ""
                            }
                        },
                        enabled = isCurrentModeValid && !isDurationExceeded,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (modes.isEmpty()) "Add Mode" else "Add Another Mode", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAddMode = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Another Mode", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            val hasValidCurrent = showAddMode && isCurrentModeValid && !isDurationExceeded
            val totalModesCount = modes.size + if (hasValidCurrent) 1 else 0
            val canSave = !isProcessing && totalModesCount > 0 && !isDurationExceeded

            Button(
                onClick = {
                    if (isProcessing) return@Button
                    isProcessing = true
                    val finalModes = modes.toMutableList()
                    if (hasValidCurrent) {
                        selectedMode?.let {
                            finalModes.add(CommuteMode(transportMode = it, durationMinutes = durationText.toIntOrNull(), cost = costText.toIntOrNull() ?: 0, commuteId = 0))
                        }
                    }
                    onConfirm(finalModes)
                },
                enabled = canSave,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Save Commute", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { 
                Text("Cancel", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) 
            }
        }
    )
}
