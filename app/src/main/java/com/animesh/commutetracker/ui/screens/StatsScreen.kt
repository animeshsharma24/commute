package com.animesh.commutetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animesh.commutetracker.data.model.CommuteWithModes
import com.animesh.commutetracker.data.model.TransportMode
import com.animesh.commutetracker.ui.viewmodel.MainViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.InsertChart
import com.animesh.commutetracker.ui.screens.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val allRecords by viewModel.allRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        if (allRecords.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateView(
                    icon = Icons.Default.InsertChart,
                    message = "No statistics available yet"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                val totalCommutes = allRecords.size
                val totalMinutes = allRecords.sumOf { it.record.durationMinutes }
                val totalCost = allRecords.sumOf { commute -> commute.modes.sumOf { it.cost } }
                
                item { 
                    StatCard("Overview", listOf(
                        "TOTAL COMMUTES" to totalCommutes.toString(),
                        "TOTAL TIME" to "${totalMinutes / 60}h ${totalMinutes % 60}m",
                        "TOTAL SPENT" to "₹$totalCost",
                        "AVG DURATION" to if (totalCommutes > 0) "${totalMinutes / totalCommutes} min" else "-",
                        "AVG COST" to if (totalCommutes > 0) "₹${totalCost / totalCommutes}" else "-"
                    ))
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
                
                val allModes = allRecords.flatMap { it.modes }
                val transportGroups = allModes.groupBy { it.transportMode }
                
                if (allModes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Transport Breakdown", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    
                    val maxCount = transportGroups.maxOfOrNull { it.value.size } ?: 1
                    
                    transportGroups.forEach { (mode, modesList) ->
                        item {
                            val modeCount = modesList.size
                            val modeCost = modesList.sumOf { it.cost }
                            val percentage = (modeCount.toFloat() / allModes.size * 100).toInt()
                            val fraction = modeCount.toFloat() / maxCount.toFloat()
                            
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$percentage%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Text(
                                    text = "$modeCount trips · ₹$modeCost total",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, stats: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            stats.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { (label, value) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = value, 
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
