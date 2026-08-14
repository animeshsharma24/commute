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
import android.util.Log

class MainViewModel(
    private val repository: CommuteRepository,
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModel() {

    private val TAG = "CommuteTracker"

    val trackingEnabled = preferenceManager.trackingEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val trackingMode = preferenceManager.trackingMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackingMode.WIFI)
    val currentState = preferenceManager.currentState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "IDLE")
    val isFirstRun = preferenceManager.isFirstRun.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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

    private val _todayRecords = MutableStateFlow<List<CommuteRecord>>(emptyList())
    val todayRecords: StateFlow<List<CommuteRecord>> = _todayRecords

    private val _allRecords = MutableStateFlow<List<CommuteRecord>>(emptyList())
    val allRecords: StateFlow<List<CommuteRecord>> = _allRecords

    private val _recentCosts = MutableStateFlow<List<Int>>(emptyList())
    val recentCosts: StateFlow<List<Int>> = _recentCosts
    
    private val _activeDialogRecord = MutableStateFlow<CommuteRecord?>(null)
    val activeDialogRecord: StateFlow<CommuteRecord?> = _activeDialogRecord
    
    private var finalizingRecordId: Long = -1L

    init {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.getRecordsByDate(today).collect { records ->
                _todayRecords.value = records.filter { r -> 
                    r.status == CommuteStatus.COMPLETED || r.status == CommuteStatus.PENDING_DETAILS 
                }
            }
        }
        viewModelScope.launch {
            repository.allRecords.collect { records ->
                _allRecords.value = records.filter { r -> 
                    r.status == CommuteStatus.COMPLETED || r.status == CommuteStatus.PENDING_DETAILS 
                }
            }
        }
        viewModelScope.launch {
            repository.pendingDetailsRecord.collect { pending ->
                if (_activeDialogRecord.value == null && pending != null && pending.id != finalizingRecordId) {
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

    fun cancelManualCommute() = viewModelScope.launch {
        preferenceManager.setManualStartTime(0L)
        repository.logEvent("MANUAL_CANCEL")
    }

    fun finalizeCommute(record: CommuteRecord, mode: TransportMode, cost: Int, context: Context) {
        finalizingRecordId = record.id
        _activeDialogRecord.value = null
        viewModelScope.launch {
            record.transportMode = mode
            record.cost = cost
            record.status = CommuteStatus.COMPLETED
            repository.updateRecord(record)
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(CommuteTrackerService.COMMUTE_ALERT_NOTIFICATION_ID)
            repository.logEvent("COMMUTE_COMPLETED", "id=${record.id}")
            
            kotlinx.coroutines.delay(1000)
            finalizingRecordId = -1
        }
    }

    fun updateCommute(record: CommuteRecord, mode: TransportMode, cost: Int) {
        _activeDialogRecord.value = null
        viewModelScope.launch {
            record.transportMode = mode
            record.cost = cost
            repository.updateRecord(record)
        }
    }

    fun deleteCommute(record: CommuteRecord) = viewModelScope.launch {
        repository.deleteRecord(record)
    }

    fun loadRecentCosts(mode: TransportMode) = viewModelScope.launch {
        _recentCosts.value = repository.getRecentCostsForTransport(mode)
    }

    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }
    fun clearLogs() = viewModelScope.launch { repository.clearLogs() }
    fun showEditDialog(record: CommuteRecord) { _activeDialogRecord.value = record }
    fun dismissDialog() { _activeDialogRecord.value = null }
    fun setFirstRunComplete() = viewModelScope.launch { preferenceManager.setFirstRunComplete() }
}
