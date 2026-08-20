package com.animesh.commutetracker.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.animesh.commutetracker.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: MainViewModel,
    onNavigateToLogs: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentState by viewModel.currentState.collectAsState()
    val homeSsids by viewModel.homeSsids.collectAsState()
    val officeSsids by viewModel.officeSsids.collectAsState()
    val trackingEnabled by viewModel.trackingEnabled.collectAsState()
    val activeDialogRecord by viewModel.activeDialogRecord.collectAsState()
    
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val lastCallbackTime by viewModel.lastCallbackTime.collectAsState()
    val lastDetectedSsid by viewModel.lastDetectedSsid.collectAsState()
    val lastStateTransition by viewModel.lastStateTransition.collectAsState()
    
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
    
    val channelImportance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.getNotificationChannel("commute_tracker_high")?.importance ?: -1
    } else {
        -1
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tracker Debug State") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = onNavigateToLogs) { Icon(Icons.Default.List, "Logs") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            item { DebugInfoRow("Service Running", isServiceRunning.toString()) }
            item { DebugInfoRow("Tracking Enabled", trackingEnabled.toString()) }
            item { DebugInfoRow("Current State", currentState) }
            item { DebugInfoRow("Current SSID", lastDetectedSsid) }
            item { DebugInfoRow("Home SSIDs", homeSsids.joinToString(", ")) }
            item { DebugInfoRow("Office SSIDs", officeSsids.joinToString(", ")) }
            item { DebugInfoRow("Pending Record ID", activeDialogRecord?.record?.id?.toString() ?: "None") }
            item { DebugInfoRow("Notifications Enabled", areNotificationsEnabled.toString()) }
            item { DebugInfoRow("High Channel Importance", channelImportance.toString() + " (4=High)") }
            item { DebugInfoRow("Last Callback", if(lastCallbackTime > 0) timeFormat.format(Date(lastCallbackTime)) else "Never") }
            item { DebugInfoRow("Last Transition", lastStateTransition) }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            item {
                Button(
                    onClick = onNavigateToLogs,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Diagnostic Logs")
                }
            }

            item {
                Button(
                    onClick = {
                        val intent = Intent(context, com.animesh.commutetracker.service.CommuteTrackerService::class.java).apply {
                            putExtra("SIMULATE_ARRIVAL", true)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Simulate Office Arrival")
                }
            }
            
            item {
                Button(
                    onClick = {
                        val intent = Intent(context, com.animesh.commutetracker.service.CommuteTrackerService::class.java).apply {
                            putExtra("DEBUG_NOTIFICATION", true)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Test Commute Notification")
                }
            }
        }
    }
}

@Composable
fun DebugInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider()
}
