package com.animesh.commutetracker.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.animesh.commutetracker.data.repository.TrackingMode
import com.animesh.commutetracker.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var homeSsid by remember { mutableStateOf("") }
    var officeSsid by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(TrackingMode.WIFI) }
    
    val availableWifis by viewModel.availableWifis.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    var homeExpanded by remember { mutableStateOf(false) }
    var officeExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanWifi()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.scanWifi()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Commute", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wi-Fi Selection", style = MaterialTheme.typography.titleSmall)
                        if (isScanning) {
                            Text("Scanning...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else if (scanError != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(scanError!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                if (scanError!!.contains("disabled", ignoreCase = true)) {
                                    TextButton(
                                        onClick = {
                                            val intent = if (scanError!!.contains("Wi-Fi", ignoreCase = true)) {
                                                Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                                            } else {
                                                Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            }
                                            context.startActivity(intent)
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Enable", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.scanWifi() }, enabled = !isScanning) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Wi-Fi")
                        }
                    }
                }

                // Home SSID Selection
                ExposedDropdownMenuBox(
                    expanded = homeExpanded,
                    onExpandedChange = { homeExpanded = !homeExpanded }
                ) {
                    OutlinedTextField(
                        value = homeSsid,
                        onValueChange = { homeSsid = it },
                        label = { Text("Primary Home Wi-Fi SSID") },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    if (availableWifis.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = homeExpanded,
                            onDismissRequest = { homeExpanded = false }
                        ) {
                            availableWifis.forEach { ssid ->
                                DropdownMenuItem(
                                    text = { Text(ssid) },
                                    onClick = {
                                        homeSsid = ssid
                                        homeExpanded = false
                                    }
                                )
                            }
                        }
                    } else if (homeExpanded) {
                         ExposedDropdownMenu(
                            expanded = homeExpanded,
                            onDismissRequest = { homeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isScanning) "Scanning..." else "No Wi-Fi networks found") },
                                onClick = { homeExpanded = false },
                                enabled = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Office SSID Selection
                ExposedDropdownMenuBox(
                    expanded = officeExpanded,
                    onExpandedChange = { officeExpanded = !officeExpanded }
                ) {
                    OutlinedTextField(
                        value = officeSsid,
                        onValueChange = { officeSsid = it },
                        label = { Text("Primary Office Wi-Fi SSID") },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    if (availableWifis.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = officeExpanded,
                            onDismissRequest = { officeExpanded = false }
                        ) {
                            availableWifis.forEach { ssid ->
                                DropdownMenuItem(
                                    text = { Text(ssid) },
                                    onClick = {
                                        officeSsid = ssid
                                        officeExpanded = false
                                    }
                                )
                            }
                        }
                    } else if (officeExpanded) {
                        ExposedDropdownMenu(
                            expanded = officeExpanded,
                            onDismissRequest = { officeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isScanning) "Scanning..." else "No Wi-Fi networks found") },
                                onClick = { officeExpanded = false },
                                enabled = false
                            )
                        }
                    }
                }
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
