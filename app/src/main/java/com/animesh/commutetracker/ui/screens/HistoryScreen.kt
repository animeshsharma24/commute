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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commute History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        val groupedRecords = allRecords.groupBy { it.record.date }

        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            groupedRecords.forEach { (date, records) ->
                item {
                    Text(date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(records) { record ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CommuteItem(record) {
                            viewModel.showEditDialog(record)
                        }
                        IconButton(
                            onClick = { recordToDelete = record.record },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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

    activeDialogRecord?.let { commuteWithModes ->
        TransportDialog(
            recentCosts = recentCosts,
            totalDuration = commuteWithModes.record.durationMinutes,
            onModeSelected = { viewModel.loadRecentCosts(it) },
            onConfirm = { modes ->
                viewModel.updateCommute(commuteWithModes.record, modes)
            },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}
