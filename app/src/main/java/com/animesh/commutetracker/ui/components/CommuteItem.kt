package com.animesh.commutetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animesh.commutetracker.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuteItem(commuteWithModes: CommuteWithModes, onClick: () -> Unit) {
    val record = commuteWithModes.record
    val modes = commuteWithModes.modes
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val directionText = if (record.direction == CommuteDirection.HOME_TO_OFFICE) "Home → Office" else "Office → Home"
    val methodIcon = when(record.detectionMethod) {
        DetectionMethod.WIFI -> Icons.Default.Wifi
        DetectionMethod.LOCATION -> Icons.Default.Place
        DetectionMethod.MANUAL -> Icons.Default.PanTool
    }

    val transportSummary = if (modes.isEmpty()) {
        "No details"
    } else if (modes.size == 1) {
        "${modes[0].transportMode.name.lowercase().replaceFirstChar { it.uppercase() }} · ₹${modes[0].cost}"
    } else {
        "${modes.size} modes · ₹${modes.sumOf { it.cost }}"
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(directionText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${timeFormat.format(Date(record.startTimestamp))} → ${timeFormat.format(Date(record.arrivalTimestamp))}", color = MaterialTheme.colorScheme.secondary)
                Text("${record.durationMinutes} min · $transportSummary")
                
                if (modes.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    modes.forEach { mode ->
                        Text(
                            "${mode.transportMode.name.lowercase().replaceFirstChar { it.uppercase() }} (${mode.durationMinutes}m, ₹${mode.cost})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            Icon(methodIcon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        }
    }
}
