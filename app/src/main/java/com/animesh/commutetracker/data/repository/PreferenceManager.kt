package com.animesh.commutetracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class TrackingMode {
    WIFI, LOCATION, MANUAL
}

class PreferenceManager(private val context: Context) {

    companion object {
        val TRACKING_MODE = stringPreferencesKey("tracking_mode")
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        
        val HOME_SSIDS = stringSetPreferencesKey("home_ssids")
        val OFFICE_SSIDS = stringSetPreferencesKey("office_ssids")
        
        val HOME_LAT = doublePreferencesKey("home_lat")
        val HOME_LNG = doublePreferencesKey("home_lng")
        val OFFICE_LAT = doublePreferencesKey("office_lat")
        val OFFICE_LNG = doublePreferencesKey("office_lng")
        val GEOFENCE_RADIUS = intPreferencesKey("geofence_radius")
        
        val CURRENT_STATE = stringPreferencesKey("current_state")
        val PENDING_START_TIME = longPreferencesKey("pending_start_time")
        val PENDING_DETECTION_METHOD = stringPreferencesKey("pending_detection_method")
        
        val ACTIVE_MANUAL_START_TIME = longPreferencesKey("active_manual_start_time")
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
        
        val LAST_CALLBACK_TIME = longPreferencesKey("last_callback_time")
        val LAST_DETECTED_SSID = stringPreferencesKey("last_detected_ssid")
        val LAST_STATE_TRANSITION = stringPreferencesKey("last_state_transition")
        val IS_SERVICE_RUNNING = booleanPreferencesKey("is_service_running")

        // Migration keys
        val LEGACY_HOME_SSID = stringPreferencesKey("home_ssid")
        val LEGACY_OFFICE_SSID = stringPreferencesKey("office_ssid")
    }

    val trackingMode: Flow<TrackingMode> = context.dataStore.data.map { 
        TrackingMode.valueOf(it[TRACKING_MODE] ?: TrackingMode.WIFI.name) 
    }
    val trackingEnabled: Flow<Boolean> = context.dataStore.data.map { it[TRACKING_ENABLED] ?: false }
    
    val homeSsids: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[HOME_SSIDS] ?: prefs[LEGACY_HOME_SSID]?.let { setOf(it) } ?: setOf("Airtel_anim_6996")
    }
    val officeSsids: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[OFFICE_SSIDS] ?: prefs[LEGACY_OFFICE_SSID]?.let { setOf(it) } ?: setOf("Access_NG")
    }

    val homeLocation: Flow<Pair<Double, Double>?> = context.dataStore.data.map { prefs ->
        val lat = prefs[HOME_LAT]
        val lng = prefs[HOME_LNG]
        if (lat != null && lng != null) Pair(lat, lng) else null
    }
    val officeLocation: Flow<Pair<Double, Double>?> = context.dataStore.data.map { prefs ->
        val lat = prefs[OFFICE_LAT]
        val lng = prefs[OFFICE_LNG]
        if (lat != null && lng != null) Pair(lat, lng) else null
    }
    val geofenceRadius: Flow<Int> = context.dataStore.data.map { it[GEOFENCE_RADIUS] ?: 150 }

    val currentState: Flow<String> = context.dataStore.data.map { it[CURRENT_STATE] ?: "IDLE" }
    val pendingStartTime: Flow<Long> = context.dataStore.data.map { it[PENDING_START_TIME] ?: 0L }
    val pendingDetectionMethod: Flow<String?> = context.dataStore.data.map { it[PENDING_DETECTION_METHOD] }
    
    val activeManualStartTime: Flow<Long> = context.dataStore.data.map { it[ACTIVE_MANUAL_START_TIME] ?: 0L }
    val isFirstRun: Flow<Boolean> = context.dataStore.data.map { it[IS_FIRST_RUN] ?: true }

    val lastCallbackTime: Flow<Long> = context.dataStore.data.map { it[LAST_CALLBACK_TIME] ?: 0L }
    val lastDetectedSsid: Flow<String> = context.dataStore.data.map { it[LAST_DETECTED_SSID] ?: "None" }
    val lastStateTransition: Flow<String> = context.dataStore.data.map { it[LAST_STATE_TRANSITION] ?: "None" }
    val isServiceRunning: Flow<Boolean> = context.dataStore.data.map { it[IS_SERVICE_RUNNING] ?: false }

    suspend fun setTrackingMode(mode: TrackingMode) {
        context.dataStore.edit { it[TRACKING_MODE] = mode.name }
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TRACKING_ENABLED] = enabled }
    }

    suspend fun addHomeSsid(ssid: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[HOME_SSIDS] ?: emptySet()
            prefs[HOME_SSIDS] = current + ssid
        }
    }

    suspend fun removeHomeSsid(ssid: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[HOME_SSIDS] ?: emptySet()
            prefs[HOME_SSIDS] = current - ssid
        }
    }

    suspend fun addOfficeSsid(ssid: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[OFFICE_SSIDS] ?: emptySet()
            prefs[OFFICE_SSIDS] = current + ssid
        }
    }

    suspend fun removeOfficeSsid(ssid: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[OFFICE_SSIDS] ?: emptySet()
            prefs[OFFICE_SSIDS] = current - ssid
        }
    }

    suspend fun setHomeLocation(lat: Double, lng: Double) {
        context.dataStore.edit { 
            it[HOME_LAT] = lat
            it[HOME_LNG] = lng
        }
    }

    suspend fun setOfficeLocation(lat: Double, lng: Double) {
        context.dataStore.edit { 
            it[OFFICE_LAT] = lat
            it[OFFICE_LNG] = lng
        }
    }

    suspend fun setGeofenceRadius(radius: Int) {
        context.dataStore.edit { it[GEOFENCE_RADIUS] = radius }
    }

    suspend fun setCurrentState(state: String) {
        context.dataStore.edit { it[CURRENT_STATE] = state }
    }

    suspend fun setPendingStartTime(time: Long) {
        context.dataStore.edit { it[PENDING_START_TIME] = time }
    }

    suspend fun setPendingDetectionMethod(method: String?) {
        context.dataStore.edit { 
            if (method == null) it.remove(PENDING_DETECTION_METHOD)
            else it[PENDING_DETECTION_METHOD] = method
        }
    }

    suspend fun setManualStartTime(time: Long) {
        context.dataStore.edit { it[ACTIVE_MANUAL_START_TIME] = time }
    }

    suspend fun setFirstRunComplete() {
        context.dataStore.edit { it[IS_FIRST_RUN] = false }
    }

    suspend fun updateDiagnosticInfo(ssid: String?, state: String?, transition: String?) {
        context.dataStore.edit { prefs ->
            ssid?.let { prefs[LAST_DETECTED_SSID] = it }
            state?.let { prefs[LAST_STATE_TRANSITION] = it }
            prefs[LAST_CALLBACK_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun setServiceRunning(running: Boolean) {
        context.dataStore.edit { it[IS_SERVICE_RUNNING] = running }
    }
}
