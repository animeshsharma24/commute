package com.animesh.commutetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.commutetracker.data.model.CommuteRecord
import androidx.compose.material.icons.filled.History
import com.animesh.commutetracker.ui.screens.EmptyStateView
import com.animesh.commutetracker.data.model.CommuteStatus
import com.animesh.commutetracker.data.model.CommuteWithModes
import com.animesh.commutetracker.ui.components.CommuteItem
import com.animesh.commutetracker.ui.components.TransportDialog
import com.animesh.commutetracker.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val allRecords by viewModel.allRecords.collectAsState()
    val activeDialogRecord by viewModel.activeDialogRecord.collectAsState()
    val recentCosts by viewModel.recentCosts.collectAsState()
    val context = LocalContext.current

    var recordToDelete by remember { mutableStateOf<CommuteRecord?>(null) }

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commute History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (allRecords.isNotEmpty()) {
                        TextButton(onClick = { showClearHistoryDialog = true }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        val groupedRecords = allRecords.groupBy { it.record.date }

        if (allRecords.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    icon = Icons.Default.History,
                    message = "No commute history yet"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                groupedRecords.forEach { (date, records) ->
                    item {
                        Text(
                            text = date, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.SemiBold, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    items(records) { record ->
                        CommuteItem(
                            commuteWithModes = record, 
                            onClick = {
                                if (recordToDelete == null) {
                                    viewModel.showEditDialog(record)
                                }
                            },
                            onDelete = {
                                recordToDelete = record.record
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    recordToDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Commute?") },
            text = { Text("Are you sure you want to delete this commute record?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCommute(record); recordToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("Are you sure you want to permanently delete all your commute records? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    activeDialogRecord?.let { commuteWithModes ->
        TransportDialog(
            recentCosts = recentCosts,
            totalDuration = commuteWithModes.record.durationMinutes,
            initialModes = commuteWithModes.modes,
            onModeSelected = { viewModel.loadRecentCosts(it) },
            onConfirm = { modes ->
                viewModel.saveCommuteDetails(commuteWithModes.record, modes, context)
            },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}
