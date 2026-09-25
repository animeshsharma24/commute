package com.animesh.commutetracker.ui.viewmodel

import android.content.Context
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animesh.commutetracker.data.model.*
import com.animesh.commutetracker.data.repository.CommuteRepository
import com.animesh.commutetracker.data.repository.PreferenceManager
import com.animesh.commutetracker.data.repository.TrackingMode
import com.animesh.commutetracker.service.CommuteTrackerService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.net.wifi.WifiManager
import android.net.wifi.ScanResult
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

class MainViewModel(
    private val repository: CommuteRepository,
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModel() {

    private val TAG = "CommuteTracker"

    val trackingEnabled = preferenceManager.trackingEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val trackingMode = preferenceManager.trackingMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackingMode.WIFI)
    val currentState = preferenceManager.currentState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "IDLE")
    
    private val _isFirstRun = MutableStateFlow<Boolean?>(null)
    val isFirstRun: StateFlow<Boolean?> = _isFirstRun

    val homeSsids = preferenceManager.homeSsids.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val officeSsids = preferenceManager.officeSsids.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    
    val homeLocation = preferenceManager.homeLocation.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val officeLocation = preferenceManager.officeLocation.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val geofenceRadius = preferenceManager.geofenceRadius.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 150)

    val recentLogs = repository.recentLogs

    val lastCallbackTime = preferenceManager.lastCallbackTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val lastDetectedSsid = preferenceManager.lastDetectedSsid.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "None")
    val lastStateTransition = preferenceManager.lastStateTransition.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "None")
    val isServiceRunning = preferenceManager.isServiceRunning.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeManualStartTime = preferenceManager.activeManualStartTime.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _todayRecords = MutableStateFlow<List<CommuteWithModes>>(emptyList())
    val todayRecords: StateFlow<List<CommuteWithModes>> = _todayRecords

    private val _allRecords = MutableStateFlow<List<CommuteWithModes>>(emptyList())
    val allRecords: StateFlow<List<CommuteWithModes>> = _allRecords

    private val _recentCosts = MutableStateFlow<List<Int>>(emptyList())
    val recentCosts: StateFlow<List<Int>> = _recentCosts
    
    private val _activeDialogRecord = MutableStateFlow<CommuteWithModes?>(null)
    val activeDialogRecord: StateFlow<CommuteWithModes?> = _activeDialogRecord
    
    private val _availableWifis = MutableStateFlow<List<String>>(emptyList())
    val availableWifis: StateFlow<List<String>> = _availableWifis

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError

    private var finalizingRecordId: Long = -1L
    private val dismissedPendingRecordIds = mutableSetOf<Long>()
    
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanAsDefault("resultsUpdated", false)
            Log.d(TAG, "Wifi scan broadcast received. Success: $success")
            updateWifiResults()
        }
    }

    private fun Intent.getBooleanAsDefault(key: String, defaultValue: Boolean): Boolean {
        return if (hasExtra(key)) getBooleanExtra(key, defaultValue) else defaultValue
    }

    init {
        viewModelScope.launch {
            preferenceManager.isFirstRun.collect { firstRun ->
                _isFirstRun.value = firstRun
            }
        }
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.getRecordsByDate(today).collect { records ->
                _todayRecords.value = records.filter { r -> 
                    r.record.status == CommuteStatus.COMPLETED || r.record.status == CommuteStatus.PENDING_DETAILS 
                }
            }
        }
        viewModelScope.launch {
            repository.allRecords.collect { records ->
                _allRecords.value = records.filter { r -> 
                    r.record.status == CommuteStatus.COMPLETED || r.record.status == CommuteStatus.PENDING_DETAILS 
                }
            }
        }
        viewModelScope.launch {
            repository.pendingDetailsRecord.collect { pending ->
                if (_activeDialogRecord.value == null && pending != null 
                    && pending.record.id != finalizingRecordId
                    && !dismissedPendingRecordIds.contains(pending.record.id)) {
                    _activeDialogRecord.value = pending
                }
            }
        }
    }

    fun setTrackingMode(mode: TrackingMode) = viewModelScope.launch { 
        preferenceManager.setTrackingMode(mode)
        updateGeofences()
    }
    
    fun setGeofenceRadius(radius: Int) = viewModelScope.launch { 
        preferenceManager.setGeofenceRadius(radius)
        updateGeofences()
    }

    fun toggleTracking(enabled: Boolean) = viewModelScope.launch { preferenceManager.setTrackingEnabled(enabled) }

    fun addHomeSsid(ssid: String) = viewModelScope.launch { preferenceManager.addHomeSsid(ssid) }
    fun removeHomeSsid(ssid: String) = viewModelScope.launch { preferenceManager.removeHomeSsid(ssid) }
    fun addOfficeSsid(ssid: String) = viewModelScope.launch { preferenceManager.addOfficeSsid(ssid) }
    fun removeOfficeSsid(ssid: String) = viewModelScope.launch { preferenceManager.removeOfficeSsid(ssid) }

    fun setHomeLocation(lat: Double, lng: Double) = viewModelScope.launch { 
        preferenceManager.setHomeLocation(lat, lng)
        updateGeofences()
    }
    fun setOfficeLocation(lat: Double, lng: Double) = viewModelScope.launch { 
        preferenceManager.setOfficeLocation(lat, lng)
        updateGeofences()
    }

    private fun updateGeofences() {
        val intent = Intent(context, CommuteTrackerService::class.java).apply {
            putExtra("UPDATE_GEOFENCES", true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun startManualCommute() = viewModelScope.launch {
        preferenceManager.setManualStartTime(System.currentTimeMillis())
        repository.logEvent("MANUAL_START")
    }

    fun stopManualCommute(direction: CommuteDirection) = viewModelScope.launch {
        val startTime = activeManualStartTime.value
        val endTime = System.currentTimeMillis()
        val durationMinutes = ((endTime - startTime) / (1000 * 60)).toInt()
        val record = CommuteRecord(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endTime)),
            direction = direction,
            startTimestamp = startTime,
            arrivalTimestamp = endTime,
            durationMinutes = durationMinutes,
            status = CommuteStatus.PENDING_DETAILS,
            detectionMethod = DetectionMethod.MANUAL
        )
        repository.insertRecord(record)
        preferenceManager.setManualStartTime(0L)
        repository.logEvent("MANUAL_STOP")
    }

    fun endOngoingCommute() {
        val intent = Intent(context, CommuteTrackerService::class.java).apply {
            putExtra("STOP_ONGOING_COMMUTE", true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun cancelManualCommute() = viewModelScope.launch {
        preferenceManager.setManualStartTime(0L)
        repository.logEvent("MANUAL_CANCEL")
    }

    fun saveCommuteDetails(record: CommuteRecord, modes: List<CommuteMode>, context: Context) {
        _activeDialogRecord.value = null
        
        if (record.status == CommuteStatus.PENDING_DETAILS) {
            finalizingRecordId = record.id
            
            // Cancel notification immediately and synchronously
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(CommuteTrackerService.COMMUTE_ALERT_NOTIFICATION_ID)
            
            viewModelScope.launch {
                val updatedRecord = record.copy(status = CommuteStatus.COMPLETED)
                repository.updateRecord(updatedRecord)
                repository.deleteModesForCommute(record.id)
                repository.insertModes(modes.map { it.copy(commuteId = record.id) })
                repository.logEvent("COMMUTE_COMPLETED", "id=${record.id}")
                
                kotlinx.coroutines.delay(1000)
                finalizingRecordId = -1
            }
        } else {
            viewModelScope.launch {
                repository.updateRecord(record)
                repository.deleteModesForCommute(record.id)
                repository.insertModes(modes.map { it.copy(commuteId = record.id) })
            }
        }
    }

    fun insertManualCommute(
        date: String,
        direction: CommuteDirection,
        startTime: Long,
        endTime: Long,
        modes: List<CommuteMode>
    ) = viewModelScope.launch {
        val calculatedEndTime = if (endTime <= startTime) {
            startTime + 60000 // Ensure at least 1 minute if invalid end time is given
        } else {
            endTime
        }
        val durationMinutes = ((calculatedEndTime - startTime) / (1000 * 60)).toInt()
        val record = CommuteRecord(
            date = date,
            direction = direction,
            startTimestamp = startTime,
            arrivalTimestamp = calculatedEndTime,
            durationMinutes = durationMinutes,
            status = CommuteStatus.COMPLETED,
            detectionMethod = DetectionMethod.MANUAL
        )
        val id = repository.insertRecord(record)
        repository.insertModes(modes.map { it.copy(commuteId = id) })
        repository.logEvent("MANUAL_ENTRY_CREATED", "id=$id")
    }

    fun deleteCommute(record: CommuteRecord) = viewModelScope.launch {
        // Specifically check if the deleted record was the one currently in the dialog
        if (_activeDialogRecord.value?.record?.id == record.id) {
            _activeDialogRecord.value = null
        }
        repository.deleteRecord(record)
    }

    fun loadRecentCosts(mode: TransportMode) = viewModelScope.launch {
        _recentCosts.value = repository.getRecentCostsForTransport(mode)
    }

    fun scanWifi() {
        if (_isScanning.value) {
            Log.d(TAG, "Already scanning, ignoring request")
            return
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        _scanError.value = null
        
        // Diagnostics
        val isWifiEnabled = wifiManager.isWifiEnabled
        val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "Starting Wifi Scan diagnostics: WifiEnabled=$isWifiEnabled, LocationEnabled=$isLocationEnabled, HasPermission=$hasPermission")
        
        if (!isWifiEnabled) {
            _scanError.value = "Wi-Fi is disabled"
            Log.w(TAG, "Scan failed: Wi-Fi is disabled")
            return
        }
        
        if (!isLocationEnabled) {
            _scanError.value = "Location services are disabled (required for Wi-Fi scanning)"
            Log.w(TAG, "Scan failed: Location disabled")
            return
        }
        
        if (!hasPermission) {
            _scanError.value = "Location permission is required"
            Log.w(TAG, "Scan failed: No permission")
            return
        }

        try {
            _isScanning.value = true
            context.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            
            @Suppress("DEPRECATION")
            val success = wifiManager.startScan()
            Log.d(TAG, "wifiManager.startScan() returned: $success")
            
            if (!success) {
                // If startScan fails (throttled), we might still get results from a previous scan
                Log.w(TAG, "Scan request throttled, showing previous results if any")
                updateWifiResults()
                _isScanning.value = false
                context.unregisterReceiver(wifiScanReceiver)
            } else {
                // Wait for broadcast. Add a timeout just in case.
                viewModelScope.launch {
                    kotlinx.coroutines.delay(10000)
                    if (_isScanning.value) {
                        Log.w(TAG, "Wifi scan timed out")
                        _isScanning.value = false
                        try { context.unregisterReceiver(wifiScanReceiver) } catch (e: Exception) {}
                        updateWifiResults()
                    }
                }
            }
        } catch (e: SecurityException) {
            _isScanning.value = false
            _scanError.value = "Permission denied for Wi-Fi scan"
            Log.e(TAG, "Permission denied for wifi scan", e)
        } catch (e: Exception) {
            _isScanning.value = false
            _scanError.value = "Failed to start scan: ${e.message}"
            Log.e(TAG, "Failed to scan wifi", e)
        }
    }

    private fun updateWifiResults() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Cannot read scan results: Permission missing")
                _isScanning.value = false
                return
            }
            
            val results: List<ScanResult> = wifiManager.scanResults
            val ssids = results.mapNotNull { it.SSID }
                .filter { it.isNotBlank() && it != "<unknown ssid>" }
                .map { it.removeSurrounding("\"") }
                .distinct()
            
            _availableWifis.value = ssids
            Log.d(TAG, "Received ${results.size} scan results, ${ssids.size} unique SSIDs: $ssids")
            
            _isScanning.value = false
            try { context.unregisterReceiver(wifiScanReceiver) } catch (e: Exception) {}
        } catch (e: Exception) {
            Log.e(TAG, "Error reading scan results", e)
            _isScanning.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { context.unregisterReceiver(wifiScanReceiver) } catch (e: Exception) {}
    }

    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }
    fun clearLogs() = viewModelScope.launch { repository.clearLogs() }
    fun showEditDialog(record: CommuteWithModes) { _activeDialogRecord.value = record }
    fun dismissDialog() { 
        _activeDialogRecord.value?.record?.id?.let { dismissedPendingRecordIds.add(it) }
        _activeDialogRecord.value = null 
    }
    fun setFirstRunComplete() = viewModelScope.launch { preferenceManager.setFirstRunComplete() }
}
