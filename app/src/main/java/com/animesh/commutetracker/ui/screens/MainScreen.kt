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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
                Text(
                    "Today's Commutes", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (todayRecords.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.DirectionsTransit,
                        message = "No commutes recorded today"
                    )
                }
            }

            items(todayRecords) { record ->
                CommuteItem(
                    commuteWithModes = record,
                    onClick = {
                        viewModel.showEditDialog(record)
                    },
                    onDelete = null
                )
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
            initialModes = commuteWithModes.modes,
            onModeSelected = { viewModel.loadRecentCosts(it) },
            onConfirm = { modes ->
                viewModel.saveCommuteDetails(commuteWithModes.record, modes, context)
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
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tracking Mode indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = mode.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Status indicator
                val isMonitoring = enabled && state == "IDLE"
                val isActive = state.contains("PENDING")
                val statusColor = when {
                    isActive -> MaterialTheme.colorScheme.secondary // Green
                    isMonitoring -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val statusText = when {
                    !enabled -> "Disabled"
                    state == "IDLE" -> "Monitoring"
                    state == "HOME_TO_OFFICE_PENDING" -> "In Transit (To Office)"
                    state == "OFFICE_TO_HOME_PENDING" -> "In Transit (To Home)"
                    else -> state
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.contains("PENDING")) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onEndCommute,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("End Ongoing Commute", fontWeight = FontWeight.SemiBold)
                }
            }

            if (mode == TrackingMode.WIFI && enabled) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            text = if(homeSsids.isEmpty()) "Not configured" else homeSsids.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row {
                        Text(
                            text = "Office",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            text = if(officeSsids.isEmpty()) "Not configured" else officeSsids.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isActive) {
                Text("MANUAL COMMUTE ACTIVE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Started at: $timeStr", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onStop, 
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Finish", fontWeight = FontWeight.SemiBold) }
                    
                    OutlinedButton(
                        onClick = onCancel, 
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
                }
            } else {
                Text("Start a manual commute", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStart, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Commute", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
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
