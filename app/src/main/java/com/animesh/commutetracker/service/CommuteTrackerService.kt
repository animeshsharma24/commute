package com.animesh.commutetracker.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.animesh.commutetracker.MainActivity
import com.animesh.commutetracker.data.database.AppDatabase
import com.animesh.commutetracker.data.model.*
import com.animesh.commutetracker.data.repository.CommuteRepository
import com.animesh.commutetracker.data.repository.PreferenceManager
import com.animesh.commutetracker.data.repository.TrackingMode
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

class CommuteTrackerService : Service() {

    companion object {
        const val MONITORING_NOTIFICATION_ID = 101
        const val COMMUTE_ALERT_NOTIFICATION_ID = 102
        const val TAG = "CommuteTracker"
        const val CHANNEL_ID_LOW = "commute_tracker_low"
        const val CHANNEL_ID_HIGH = "commute_tracker_high"
        
        const val GEOFENCE_HOME = "HOME"
        const val GEOFENCE_OFFICE = "OFFICE"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val processingMutex = Mutex()
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var repository: CommuteRepository
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var geofencingClient: GeofencingClient
    
    private var lastKnownSsid: String? = null
    private var isCallbackRegistered = false

    private val stateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION ||
                intent.action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION) {
                evaluateMechanismUsability()
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            log("NETWORK_AVAILABLE", "network=$network")
            reconcileWiFi()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            var ssid: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = capabilities.transportInfo as? WifiInfo
                ssid = wifiInfo?.ssid?.removeSurrounding("\"")
            }
            if (ssid == null || ssid == "<unknown ssid>") {
                ssid = getWifiSsidDirectly()
            }
            if (ssid != null && ssid != "<unknown ssid>") {
                processWiFiUpdate(ssid)
            }
        }

        override fun onLost(network: Network) {
            log("NETWORK_LOST", "network=$network")
            processWiFiUpdate(null)
        }
    }

    private fun log(event: String, message: String = "") {
        Log.d(TAG, "[$event] $message")
        serviceScope.launch { repository.logEvent(event, message) }
    }

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        repository = CommuteRepository(AppDatabase.getDatabase(this).commuteDao())
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        geofencingClient = LocationServices.getGeofencingClient(this)
        
        serviceScope.launch { preferenceManager.setServiceRunning(true) }
        
        log("SERVICE_STARTED")
        createNotificationChannels()
        startServiceInForeground()
        
        registerNetworkCallback()
        reconcileWiFi()
        
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        }
        registerReceiver(stateChangeReceiver, filter)
        evaluateMechanismUsability()
        
        updateGeofences()
    }

    private fun isWifiUsable(): Boolean {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isWifiEnabled = wifiManager.isWifiEnabled
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        }
        val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return isWifiEnabled && isLocationEnabled && hasLocationPermission
    }

    private fun isGeoUsable(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        }
        val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return isLocationEnabled && hasLocationPermission
    }

    private fun evaluateMechanismUsability() {
        val wifiUsable = isWifiUsable()
        val geoUsable = isGeoUsable()

        val newMode = if (wifiUsable) TrackingMode.WIFI else if (geoUsable) TrackingMode.LOCATION else TrackingMode.MANUAL
        serviceScope.launch {
            preferenceManager.setTrackingMode(newMode)
        }
        
        if (!wifiUsable && !geoUsable) {
            showEnableServicesNotification()
        } else {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(103)
        }
        
        updateGeofences()
    }

    private fun showEnableServicesNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_LOW)
            .setContentTitle("Tracking Mode: Manual")
            .setContentText("Enable Wi-Fi or Location for automatic tracking")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        manager.notify(103, notification)
    }

    private fun registerNetworkCallback() {
        if (!isCallbackRegistered) {
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isCallbackRegistered = true
            log("NETWORK_CALLBACK_REGISTERED")
        }
    }

    private fun reconcileWiFi() {
        val ssid = getWifiSsidDirectly()
        log("CURRENT_WIFI", "SSID=$ssid")
        processWiFiUpdate(ssid)
    }

    private fun getWifiSsidDirectly(): String? {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo
        val ssid = wifiInfo.ssid?.removeSurrounding("\"")
        return if (ssid == "<unknown ssid>" || ssid == null) null else ssid
    }

    private fun startServiceInForeground() {
        val notification = createMonitoringNotification("Commute is monitoring")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(MONITORING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(MONITORING_NOTIFICATION_ID, notification)
        }
    }

    private fun processWiFiUpdate(newSsid: String?) {
        val oldSsid = lastKnownSsid
        lastKnownSsid = newSsid
        
        serviceScope.launch {
            processingMutex.withLock {
                if (!isWifiUsable()) {
                    preferenceManager.updateDiagnosticInfo(newSsid ?: "None", null, if(oldSsid != newSsid) "$oldSsid -> $newSsid" else null)
                    return@withLock
                }
                
                val homeSsids = preferenceManager.homeSsids.first()
                val officeSsids = preferenceManager.officeSsids.first()
                val state = preferenceManager.currentState.first()
                val currentTime = System.currentTimeMillis()

                preferenceManager.updateDiagnosticInfo(newSsid ?: "None", state, if(oldSsid != newSsid) "$oldSsid -> $newSsid" else null)

                val currentlyAtHome = newSsid != null && homeSsids.contains(newSsid)
                val currentlyAtOffice = newSsid != null && officeSsids.contains(newSsid)
                val isDisconnected = newSsid == null || (!currentlyAtHome && !currentlyAtOffice)

                log("STATE_CHECK", "State=$state, SSID=$newSsid, isHome=$currentlyAtHome, isOffice=$currentlyAtOffice")

                // 1. Leaving Start Point
                if (state == "IDLE") {
                    if (oldSsid != null && homeSsids.contains(oldSsid) && isDisconnected) {
                        startPendingCommute(currentTime, "HOME_TO_OFFICE_PENDING", DetectionMethod.WIFI)
                    } else if (oldSsid != null && officeSsids.contains(oldSsid) && isDisconnected) {
                        startPendingCommute(currentTime, "OFFICE_TO_HOME_PENDING", DetectionMethod.WIFI)
                    }
                }
                // 2. Arriving at Destination
                else if (currentlyAtOffice && state == "HOME_TO_OFFICE_PENDING") {
                    finalizeCommute(CommuteDirection.HOME_TO_OFFICE, DetectionMethod.WIFI)
                } else if (currentlyAtHome && state == "OFFICE_TO_HOME_PENDING") {
                    finalizeCommute(CommuteDirection.OFFICE_TO_HOME, DetectionMethod.WIFI)
                }
                // 3. Reconnect to Start Point (Cancel)
                else if (currentlyAtHome && state == "HOME_TO_OFFICE_PENDING") {
                    preferenceManager.setCurrentState("IDLE")
                    log("RECONNECT_CANCEL", "Home (WIFI)")
                } else if (currentlyAtOffice && state == "OFFICE_TO_HOME_PENDING") {
                    preferenceManager.setCurrentState("IDLE")
                    log("RECONNECT_CANCEL", "Office (WIFI)")
                }
            }
        }
    }

    private fun processGeofenceUpdate(geofenceId: String, transition: Int) {
        log("GEOFENCE_EVENT", "ID=$geofenceId, Transition=$transition")
        serviceScope.launch {
            processingMutex.withLock {
                if (!isGeoUsable()) return@withLock
                
                val isAtHome = geofenceId == GEOFENCE_HOME && transition == Geofence.GEOFENCE_TRANSITION_ENTER
                val isAtOffice = geofenceId == GEOFENCE_OFFICE && transition == Geofence.GEOFENCE_TRANSITION_ENTER
                val isLeavingHome = geofenceId == GEOFENCE_HOME && transition == Geofence.GEOFENCE_TRANSITION_EXIT
                val isLeavingOffice = geofenceId == GEOFENCE_OFFICE && transition == Geofence.GEOFENCE_TRANSITION_EXIT

                val state = preferenceManager.currentState.first()
                val currentTime = System.currentTimeMillis()
                
                val homeSsids = preferenceManager.homeSsids.first()
                val officeSsids = preferenceManager.officeSsids.first()
                val currentSsid = lastKnownSsid
                val wifiConnectedToHome = currentSsid != null && homeSsids.contains(currentSsid)
                val wifiConnectedToOffice = currentSsid != null && officeSsids.contains(currentSsid)

                if (isLeavingHome && state == "IDLE") {
                    if (isWifiUsable() && wifiConnectedToHome) {
                        log("GEOFENCE_EVENT", "Ignored: Wi-Fi still connected to Home")
                        return@withLock
                    }
                    startPendingCommute(currentTime, "HOME_TO_OFFICE_PENDING", DetectionMethod.LOCATION)
                } else if (isLeavingOffice && state == "IDLE") {
                    if (isWifiUsable() && wifiConnectedToOffice) {
                        log("GEOFENCE_EVENT", "Ignored: Wi-Fi still connected to Office")
                        return@withLock
                    }
                    startPendingCommute(currentTime, "OFFICE_TO_HOME_PENDING", DetectionMethod.LOCATION)
                } else if (isAtOffice && state == "HOME_TO_OFFICE_PENDING") {
                    finalizeCommute(CommuteDirection.HOME_TO_OFFICE, DetectionMethod.LOCATION)
                } else if (isAtHome && state == "OFFICE_TO_HOME_PENDING") {
                    finalizeCommute(CommuteDirection.OFFICE_TO_HOME, DetectionMethod.LOCATION)
                } else if (isAtHome && state == "HOME_TO_OFFICE_PENDING") {
                    preferenceManager.setCurrentState("IDLE")
                    log("RECONNECT_CANCEL", "Home (GPS)")
                } else if (isAtOffice && state == "OFFICE_TO_HOME_PENDING") {
                    preferenceManager.setCurrentState("IDLE")
                    log("RECONNECT_CANCEL", "Office (GPS)")
                }
            }
        }
    }

    private suspend fun startPendingCommute(time: Long, newState: String, method: DetectionMethod) {
        log("STATE_CHANGED", "IDLE -> $newState via $method")
        preferenceManager.setCurrentState(newState)
        preferenceManager.setPendingStartTime(time)
        preferenceManager.setPendingDetectionMethod(method.name)
    }

    private suspend fun finalizeCommute(direction: CommuteDirection, method: DetectionMethod) {
        val startTime = preferenceManager.pendingStartTime.first()
        val endTime = System.currentTimeMillis()
        
        preferenceManager.setCurrentState("IDLE")
        log("DESTINATION_DETECTED", direction.name)

        if (endTime - startTime > 4L * 3600 * 1000) {
            log("PENDING_COMMUTE_CANCELLED", "Timeout > 4h")
            return
        }

        val durationMinutes = ((endTime - startTime) / (1000 * 60)).toInt()
        val record = CommuteRecord(
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endTime)),
            direction = direction,
            startTimestamp = startTime,
            arrivalTimestamp = endTime,
            durationMinutes = durationMinutes,
            status = CommuteStatus.PENDING_DETAILS,
            detectionMethod = method
        )
        
        val id = repository.insertRecord(record)
        log("PENDING_COMMUTE_CREATED", "id=$id")
        showCommuteAlert(direction, id)
    }

    private fun showCommuteAlert(direction: CommuteDirection, recordId: Long) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("PENDING_RECORD_ID", recordId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val dirText = if (direction == CommuteDirection.HOME_TO_OFFICE) "Home → Office" else "Office → Home"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_HIGH)
            .setContentTitle("Commute completed")
            .setContentText("$dirText — enter transport and cost")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_edit, "ENTER DETAILS", pendingIntent)
            .build()

        manager.notify(COMMUTE_ALERT_NOTIFICATION_ID, notification)
        log("NOTIFICATION_POSTED", "id=102")
    }

    private fun createMonitoringNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID_LOW)
            .setContentTitle("Commute")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(NotificationChannel(CHANNEL_ID_LOW, "Commute Monitoring", NotificationManager.IMPORTANCE_LOW))
            manager?.createNotificationChannel(NotificationChannel(CHANNEL_ID_HIGH, "Commute Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateGeofences() {
        serviceScope.launch {
            if (!isGeoUsable()) {
                geofencingClient.removeGeofences(getGeofencePendingIntent())
                return@launch
            }
            
            val home = preferenceManager.homeLocation.first()
            val office = preferenceManager.officeLocation.first()
            val radius = preferenceManager.geofenceRadius.first().toFloat()
            
            val geofences = mutableListOf<Geofence>()
            home?.let { geofences.add(createGeofence(GEOFENCE_HOME, it.first, it.second, radius)) }
            office?.let { geofences.add(createGeofence(GEOFENCE_OFFICE, it.first, it.second, radius)) }
            
            if (geofences.isNotEmpty()) {
                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofences)
                    .build()
                geofencingClient.addGeofences(request, getGeofencePendingIntent())
                log("GEOFENCES_UPDATED", "Count=${geofences.size}")
            }
        }
    }

    private fun createGeofence(id: String, lat: Double, lng: Double, radius: Float) = Geofence.Builder()
        .setRequestId(id)
        .setCircularRegion(lat, lng, radius)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        .build()

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServiceInForeground()
        
        intent?.let {
            if (it.hasExtra("GEOFENCE_ID")) {
                val id = it.getStringExtra("GEOFENCE_ID")!!
                val trans = it.getIntExtra("GEOFENCE_TRANSITION", -1)
                processGeofenceUpdate(id, trans)
            }
            if (it.hasExtra("SIMULATE_ARRIVAL")) {
                serviceScope.launch { finalizeCommute(CommuteDirection.HOME_TO_OFFICE, DetectionMethod.MANUAL) }
            }
            if (it.hasExtra("UPDATE_GEOFENCES")) {
                updateGeofences()
            }
            if (it.hasExtra("STOP_ONGOING_COMMUTE")) {
                serviceScope.launch {
                    processingMutex.withLock {
                        val state = preferenceManager.currentState.first()
                        if (state == "HOME_TO_OFFICE_PENDING") {
                            finalizeCommute(CommuteDirection.HOME_TO_OFFICE, DetectionMethod.MANUAL)
                        } else if (state == "OFFICE_TO_HOME_PENDING") {
                            finalizeCommute(CommuteDirection.OFFICE_TO_HOME, DetectionMethod.MANUAL)
                        } else if (preferenceManager.activeManualStartTime.first() > 0) {
                            // This handles the case where a manual commute is running but not yet finalized
                            // But usually stopManualCommute is called from ViewModel. 
                            // However, requirement says "clearly accessible action to manually end/stop an ongoing commute".
                            // If it's a manual tracking commute, it might not be in a "PENDING" state in the service's sense if the service isn't tracking it.
                            // But the service is monitoring.
                            log("MANUAL_STOP_REQUESTED", "State=$state")
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        log("SERVICE_STOPPED")
        serviceScope.launch { preferenceManager.setServiceRunning(false) }
        connectivityManager.unregisterNetworkCallback(networkCallback)
        try {
            unregisterReceiver(stateChangeReceiver)
        } catch (_: Exception) {
            // Ignored
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
