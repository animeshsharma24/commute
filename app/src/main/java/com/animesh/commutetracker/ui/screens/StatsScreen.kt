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
import com.animesh.commutetracker.data.model.CommuteRecord
import com.animesh.commutetracker.ui.viewmodel.MainViewModel
import java.util.*

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
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            val totalCommutes = allRecords.size
            val totalMinutes = allRecords.sumOf { it.durationMinutes }
            val totalCost = allRecords.sumOf { it.cost ?: 0 }
            
            item { 
                StatCard("Overview", listOf(
                    "Total Commutes" to totalCommutes.toString(),
                    "Total Time" to "${totalMinutes / 60}h ${totalMinutes % 60}m",
                    "Total Spent" to "₹$totalCost",
                    "Avg Duration" to if (totalCommutes > 0) "${totalMinutes / totalCommutes} min" else "-",
                    "Avg Cost" to if (totalCommutes > 0) "₹${totalCost / totalCommutes}" else "-"
                ))
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            val transportGroups = allRecords.groupBy { it.transportMode }
            item {
                Text("Transport Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                transportGroups.forEach { (mode, records) ->
                    val modeCount = records.size
                    val modeCost = records.sumOf { it.cost ?: 0 }
                    ListItem(
                        headlineContent = { Text(mode?.name ?: "Unknown") },
                        supportingContent = { Text("$modeCount commutes · ₹$modeCost total") },
                        trailingContent = { Text("${(modeCount.toFloat() / totalCommutes * 100).toInt()}%") }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, stats: List<Pair<String, String>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            stats.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { (label, value) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Text(value, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
