package com.animesh.commutetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animesh.commutetracker.data.model.*
import com.animesh.commutetracker.data.repository.TrackingMode
import com.animesh.commutetracker.ui.components.CommuteItem
import com.animesh.commutetracker.ui.components.TransportDialog
import com.animesh.commutetracker.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToManualEntry: () -> Unit
) {
    val context = LocalContext.current
    val trackingEnabled by viewModel.trackingEnabled.collectAsState()
    val trackingMode by viewModel.trackingMode.collectAsState()
    val currentState by viewModel.currentState.collectAsState()
    val homeSsids by viewModel.homeSsids.collectAsState()
    val officeSsids by viewModel.officeSsids.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val recentCosts by viewModel.recentCosts.collectAsState()
    val activeDialogRecord by viewModel.activeDialogRecord.collectAsState()
    val manualStartTime by viewModel.activeManualStartTime.collectAsState()

    var showManualDirectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commute") },
                actions = {
                    IconButton(onClick = onNavigateToStats) { Icon(Icons.Default.ShowChart, "Stats") }
                    IconButton(onClick = onNavigateToHistory) { Icon(Icons.Default.History, "History") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusCard(
                    enabled = trackingEnabled,
                    mode = trackingMode,
                    state = currentState,
                    homeSsids = homeSsids,
                    officeSsids = officeSsids,
                    onToggle = viewModel::toggleTracking,
                    onEndCommute = { viewModel.endOngoingCommute() }
                )
            }

            if (trackingMode == TrackingMode.MANUAL) {
                item {
                    ManualControlCard(
                        isActive = manualStartTime > 0,
                        startTime = manualStartTime,
                        onStart = { viewModel.startManualCommute() },
                        onStop = { showManualDirectionDialog = true },
                        onCancel = { viewModel.cancelManualCommute() }
                    )
                }
            }
            
            item {
                Button(
                    onClick = onNavigateToManualEntry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Missed Commute")
                }
            }

            if (activeDialogRecord != null && activeDialogRecord?.record?.status == CommuteStatus.PENDING_DETAILS) {
                item {
                    PendingDetailsBanner { /* Dialog handles auto-show */ }
                }
            }
            
            item {
                Text("Today's Commutes", style = MaterialTheme.typography.titleLarge)
            }

            if (todayRecords.isEmpty()) {
                item {
                    Text("No commutes recorded today.", modifier = Modifier.padding(vertical = 16.dp))
                }
            }

            items(todayRecords) { record ->
                CommuteItem(record) {
                    viewModel.showEditDialog(record)
                }
            }
        }
    }

    if (showManualDirectionDialog) {
        AlertDialog(
            onDismissRequest = { showManualDirectionDialog = false },
            title = { Text("Commute Direction") },
            text = { Text("Which direction were you traveling?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.stopManualCommute(CommuteDirection.HOME_TO_OFFICE)
                    showManualDirectionDialog = false 
                }) { Text("Home → Office") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.stopManualCommute(CommuteDirection.OFFICE_TO_HOME)
                    showManualDirectionDialog = false 
                }) { Text("Office → Home") }
            }
        )
    }

    activeDialogRecord?.let { commuteWithModes ->
        TransportDialog(
            recentCosts = recentCosts,
            totalDuration = commuteWithModes.record.durationMinutes,
            onModeSelected = { viewModel.loadRecentCosts(it) },
            onConfirm = { modes ->
                if (commuteWithModes.record.status == CommuteStatus.PENDING_DETAILS) {
                    viewModel.finalizeCommute(commuteWithModes.record, modes, context)
                } else {
                    viewModel.updateCommute(commuteWithModes.record, modes)
                }
            },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}

@Composable
fun StatusCard(
    enabled: Boolean,
    mode: TrackingMode,
    state: String,
    homeSsids: Set<String>,
    officeSsids: Set<String>,
    onToggle: (Boolean) -> Unit,
    onEndCommute: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tracking Mode: ${mode.name}", fontWeight = FontWeight.Bold)
                    Text("Status: $state", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (state.contains("PENDING")) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onEndCommute,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("END COMMUTE")
                }
            }
            if (mode == TrackingMode.WIFI && enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Home: ${homeSsids.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                Text("Office: ${officeSsids.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ManualControlCard(
    isActive: Boolean,
    startTime: Long,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    val timeStr = if (isActive) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTime)) else ""
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isActive) {
                Text("MANUAL COMMUTE ACTIVE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Started at: $timeStr", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStop, modifier = Modifier.weight(1f)) { Text("STOP") }
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                }
            } else {
                Text("Ready to start?", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START COMMUTE")
                }
            }
        }
    }
}

@Composable
fun PendingDetailsBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("1 commute needs details. Tap to complete.", fontWeight = FontWeight.Bold)
        }
    }
}
