package com.animesh.commutetracker.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.commutetracker.data.model.CommuteDirection
import com.animesh.commutetracker.data.model.CommuteMode
import com.animesh.commutetracker.data.model.TransportMode
import com.animesh.commutetracker.ui.components.TransportDialog
import com.animesh.commutetracker.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCommuteScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val recentCosts by viewModel.recentCosts.collectAsState()
    
    var direction by remember { mutableStateOf(CommuteDirection.HOME_TO_OFFICE) }
    var date by remember { mutableStateOf(Calendar.getInstance()) }
    var startTime by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.MINUTE, 0) }) }
    var endTime by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR, 1); set(Calendar.MINUTE, 0) }) }
    
    var modes by remember { mutableStateOf(listOf<CommuteMode>()) }
    var showAddModeDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Missed Commute") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                Text("Direction", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = direction == CommuteDirection.HOME_TO_OFFICE, onClick = { direction = CommuteDirection.HOME_TO_OFFICE })
                    Text("Home → Office")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = direction == CommuteDirection.OFFICE_TO_HOME, onClick = { direction = CommuteDirection.OFFICE_TO_HOME })
                    Text("Office → Home")
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Date & Time", fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, y, m, d ->
                                date = Calendar.getInstance().apply { set(y, m, d) }
                            }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(dateFormat.format(date.time))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                startTime = (startTime.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                            }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start: ${timeFormat.format(startTime.time)}")
                    }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                endTime = (endTime.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                            }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("End: ${timeFormat.format(endTime.time)}")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Transport Modes", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddModeDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Text("Add Mode")
                    }
                }
                if (modes.isEmpty()) {
                    Text("No modes added yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }

            items(modes) { mode ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.transportMode.name, fontWeight = FontWeight.Bold)
                            Text("${mode.durationMinutes} min · ₹${mode.cost}")
                        }
                        IconButton(onClick = { modes = modes.toMutableList().apply { remove(mode) } }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        viewModel.insertManualCommute(
                            date = dateFormat.format(date.time),
                            direction = direction,
                            startTime = startTime.timeInMillis,
                            endTime = endTime.timeInMillis,
                            modes = modes
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = modes.isNotEmpty() && endTime.after(startTime)
                ) {
                    Text("Save Commute")
                }
            }
        }
    }

    if (showAddModeDialog) {
        val totalTracked = ((endTime.timeInMillis - startTime.timeInMillis) / (1000 * 60)).toInt()
        TransportDialog(
            recentCosts = recentCosts,
            totalDuration = totalTracked,
            onConfirm = { addedModes ->
                modes = modes + addedModes
                showAddModeDialog = false
            },
            onDismiss = { showAddModeDialog = false }
        )
    }
}
