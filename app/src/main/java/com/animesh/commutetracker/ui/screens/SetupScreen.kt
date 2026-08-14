package com.animesh.commutetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animesh.commutetracker.data.repository.TrackingMode
import com.animesh.commutetracker.ui.viewmodel.MainViewModel

@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    var homeSsid by remember { mutableStateOf("") }
    var officeSsid by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TrackingMode.WIFI) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Commute Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Select Tracking Mode", style = MaterialTheme.typography.titleMedium)
            TrackingMode.entries.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == m, onClick = { mode = m })
                    Text(m.name)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (mode == TrackingMode.WIFI) {
                OutlinedTextField(
                    value = homeSsid,
                    onValueChange = { homeSsid = it },
                    label = { Text("Primary Home Wi-Fi SSID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = officeSsid,
                    onValueChange = { officeSsid = it },
                    label = { Text("Primary Office Wi-Fi SSID") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (mode == TrackingMode.LOCATION) {
                Text("You will configure locations in Settings.", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    viewModel.setTrackingMode(mode)
                    if (mode == TrackingMode.WIFI) {
                        if (homeSsid.isNotBlank()) viewModel.addHomeSsid(homeSsid)
                        if (officeSsid.isNotBlank()) viewModel.addOfficeSsid(officeSsid)
                    }
                    viewModel.setFirstRunComplete()
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = mode != TrackingMode.WIFI || (homeSsid.isNotBlank() && officeSsid.isNotBlank())
            ) {
                Text("GET STARTED")
            }
        }
    }
}
