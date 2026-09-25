package com.animesh.commutetracker.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.animesh.commutetracker.data.repository.TrackingMode
import com.animesh.commutetracker.ui.viewmodel.MainViewModel
import com.animesh.commutetracker.util.CsvExporter
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onClearHistory: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val context = LocalContext.current
    val trackingMode by viewModel.trackingMode.collectAsState()
    val homeSsids by viewModel.homeSsids.collectAsState()
    val officeSsids by viewModel.officeSsids.collectAsState()
    val homeLocation by viewModel.homeLocation.collectAsState()
    val officeLocation by viewModel.officeLocation.collectAsState()
    val geofenceRadius by viewModel.geofenceRadius.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()

    val lastDetectedSsid by viewModel.lastDetectedSsid.collectAsState()

    var newHomeSsid by remember { mutableStateOf("") }
    var newOfficeSsid by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item { SettingHeader("TRACKING") }
            item {
                Text(
                    "Tracking Mode (Auto-Selected at Runtime)", 
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TrackingMode.entries.forEach { mode ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = trackingMode == mode, 
                                onClick = null,
                                enabled = trackingMode == mode,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                mode.name, 
                                style = MaterialTheme.typography.bodySmall,
                                color = if (trackingMode == mode) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item { SettingHeader("WI-FI LOCATIONS") }
            item { Text("Home SSIDs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(homeSsids.toList()) { ssid ->
                SsidItem(ssid) { viewModel.removeHomeSsid(ssid) }
            }
            item {
                SsidInputRow(newHomeSsid, { newHomeSsid = it }, { viewModel.addHomeSsid(newHomeSsid); newHomeSsid = "" })
            }
            item {
                TextButton(onClick = { viewModel.addHomeSsid(lastDetectedSsid) }, enabled = lastDetectedSsid != "None") {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Current: $lastDetectedSsid")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item { Text("Office SSIDs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(officeSsids.toList()) { ssid ->
                SsidItem(ssid) { viewModel.removeOfficeSsid(ssid) }
            }
            item {
                SsidInputRow(newOfficeSsid, { newOfficeSsid = it }, { viewModel.addOfficeSsid(newOfficeSsid); newOfficeSsid = "" })
            }
            item {
                TextButton(onClick = { viewModel.addOfficeSsid(lastDetectedSsid) }, enabled = lastDetectedSsid != "None") {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Current: $lastDetectedSsid")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item { SettingHeader("GEOFENCE LOCATIONS") }
            item {
                LocationPickerRow("Home", homeLocation) { 
                    getCurrentLocation(context) { lat, lng -> viewModel.setHomeLocation(lat, lng) }
                }
            }
            item {
                LocationPickerRow("Office", officeLocation) {
                    getCurrentLocation(context) { lat, lng -> viewModel.setOfficeLocation(lat, lng) }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Geofence Radius: ${geofenceRadius}m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = geofenceRadius.toFloat(),
                    onValueChange = { viewModel.setGeofenceRadius(it.toInt()) },
                    valueRange = 100f..500f,
                    steps = 4
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item { SettingHeader("DATA") }
            item {
                Button(
                    onClick = { CsvExporter.exportRecords(context, allRecords) }, 
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Export History as CSV", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }
            item {
                Button(
                    onClick = onClearHistory,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear History", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }

            item { SettingHeader("DIAGNOSTICS") }
            item {
                Button(
                    onClick = onNavigateToDebug, 
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Show Tracker Debug State", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }
            item {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("App Permissions & Battery Settings", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SettingHeader(text: String) {
    Text(
        text = text, 
        style = MaterialTheme.typography.titleSmall, 
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, 
        color = MaterialTheme.colorScheme.primary, 
        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun SsidInputRow(value: String, onValueChange: (String) -> Unit, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = value, 
            onValueChange = onValueChange, 
            label = { Text("Add SSID") }, 
            modifier = Modifier.weight(1f), 
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true
        )
        IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add") }
    }
}

@Composable
fun LocationPickerRow(label: String, location: Pair<Double, Double>?, onPick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location?.let { "%.4f, %.4f".format(it.first, it.second) } ?: "Not set", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onPick) {
                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Current", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun SsidItem(ssid: String, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(ssid, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
        }
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: android.content.Context, onLocation: (Double, Double) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) onLocation(loc.latitude, loc.longitude)
    }
}
