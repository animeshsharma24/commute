package com.animesh.commutetracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.animesh.commutetracker.data.database.AppDatabase
import com.animesh.commutetracker.data.repository.CommuteRepository
import com.animesh.commutetracker.data.repository.PreferenceManager
import com.animesh.commutetracker.service.CommuteTrackerService
import com.animesh.commutetracker.ui.navigation.NavGraph
import com.animesh.commutetracker.ui.theme.CommuteTrackerTheme
import com.animesh.commutetracker.ui.viewmodel.MainViewModel
import com.animesh.commutetracker.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val TAG = "CommuteTrackerUI"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permissions callback: $permissions")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = CommuteRepository(database.commuteDao())
        val preferenceManager = PreferenceManager(this)
        val factory = MainViewModelFactory(repository, preferenceManager, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        checkPermissions()

        setContent {
            CommuteTrackerTheme {
                val navController = rememberNavController()
                Surface {
                    NavGraph(navController, viewModel)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.trackingEnabled.collect { enabled ->
                if (enabled) startTrackingService()
                else stopTrackingService()
            }
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, CommuteTrackerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopTrackingService() {
        stopService(Intent(this, CommuteTrackerService::class.java))
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
        
        // Background location requires separate request after foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Suggest user to enable in settings or show a dialog first
        }
    }
}
