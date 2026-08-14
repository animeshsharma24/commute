package com.animesh.commutetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.animesh.commutetracker.data.model.TransportMode

@Composable
fun TransportDialog(
    recentCosts: List<Int>,
    onModeSelected: (TransportMode) -> Unit = {},
    onConfirm: (TransportMode, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf<TransportMode?>(null) }
    var costText by remember { mutableStateOf("") }
    var showCostInput by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text(if (!showCostInput) "How did you travel?" else "How much did you spend?") },
        text = {
            Column {
                if (!showCostInput) {
                    TransportMode.entries.forEach { mode ->
                        Button(
                            onClick = {
                                if (isProcessing) return@Button
                                selectedMode = mode
                                onModeSelected(mode)
                                if (mode == TransportMode.SHUTTLE) {
                                    isProcessing = true
                                    onConfirm(mode, 0)
                                } else {
                                    showCostInput = true
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) costText = it },
                        label = { Text("Cost in INR") },
                        enabled = !isProcessing,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (recentCosts.isNotEmpty()) {
                        Text("Recent (One-tap):", modifier = Modifier.padding(top = 16.dp))
                        LazyRow(modifier = Modifier.padding(top = 8.dp)) {
                            items(recentCosts) { cost ->
                                if (cost > 0) { // Shuttle ₹0 excluded as per requirement
                                    FilterChip(
                                        selected = false,
                                        enabled = !isProcessing,
                                        onClick = { 
                                            if (!isProcessing) {
                                                isProcessing = true
                                                selectedMode?.let { onConfirm(it, cost) }
                                            }
                                        },
                                        label = { Text("₹$cost") },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCostInput) {
                TextButton(
                    onClick = {
                        if (isProcessing) return@TextButton
                        isProcessing = true
                        val cost = costText.toIntOrNull() ?: 0
                        selectedMode?.let { onConfirm(it, cost) }
                    },
                    enabled = !isProcessing && (costText.isNotBlank() || selectedMode != null)
                ) {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    else Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("Cancel") }
        }
    )
}
