package com.animesh.commutetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
fun CommuteItem(
    commuteWithModes: CommuteWithModes, 
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val record = commuteWithModes.record
    val modes = commuteWithModes.modes
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val directionText = if (record.direction == CommuteDirection.HOME_TO_OFFICE) "Home → Office" else "Office → Home"
    val methodIcon = when(record.detectionMethod) {
        DetectionMethod.WIFI -> Icons.Default.Wifi
        DetectionMethod.LOCATION -> Icons.Default.Place
        DetectionMethod.MANUAL -> Icons.Default.PanTool
    }

    val methodLabel = when(record.detectionMethod) {
        DetectionMethod.WIFI -> "Wi-Fi"
        DetectionMethod.LOCATION -> "GPS"
        DetectionMethod.MANUAL -> "Manual"
    }

    val transportSummary = if (modes.isEmpty()) {
        "No details"
    } else if (modes.size == 1) {
        "${modes[0].transportMode.name.lowercase().replaceFirstChar { it.uppercase() }} • ₹${modes[0].cost}"
    } else {
        "${modes.size} modes • ₹${modes.sumOf { it.cost }}"
    }

    Card(
        onClick = onClick, 
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp)) {
            // Left content column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = directionText, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                
                Text(
                    text = "${timeFormat.format(Date(record.startTimestamp))} → ${timeFormat.format(Date(record.arrivalTimestamp))}", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${record.durationMinutes} min",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = " • $transportSummary",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (modes.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        modes.forEach { mode ->
                            val durationStr = mode.durationMinutes?.let { "${it}m, " } ?: ""
                            Text(
                                text = "${mode.transportMode.name.lowercase().replaceFirstChar { it.uppercase() }} ($durationStr₹${mode.cost})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            // Right action/source column
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight().padding(start = 12.dp)
            ) {
                if (onDelete != null) {
                    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete, 
                                contentDescription = "Delete", 
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), 
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Generic Source Badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = methodIcon, 
                            contentDescription = methodLabel,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = methodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
