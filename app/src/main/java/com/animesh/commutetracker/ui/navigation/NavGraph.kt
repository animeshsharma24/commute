package com.animesh.commutetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.animesh.commutetracker.ui.screens.*
import com.animesh.commutetracker.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Main : Screen("main")
    object History : Screen("history")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object ManualEntry : Screen("manual_entry")
    object Debug : Screen("debug")
    object Logs : Screen("logs")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val isFirstRun by viewModel.isFirstRun.collectAsState()
    
    if (isFirstRun == null) return

    val startDestination = if (isFirstRun == true) Screen.Setup.route else Screen.Main.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(viewModel) {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Setup.route) { inclusive = true }
                }
            }
        }
        composable(Screen.Main.route) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToManualEntry = { navController.navigate(Screen.ManualEntry.route) }
            )
        }
        composable(Screen.ManualEntry.route) {
            ManualCommuteScreen(viewModel) { navController.popBackStack() }
        }
        composable(Screen.History.route) {
            HistoryScreen(viewModel) { navController.popBackStack() }
        }
        composable(Screen.Stats.route) {
            StatsScreen(viewModel) { navController.popBackStack() }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onClearHistory = { viewModel.clearHistory() },
                onNavigateToDebug = { navController.navigate(Screen.Debug.route) }
            )
        }
        composable(Screen.Debug.route) {
            DebugScreen(
                viewModel = viewModel,
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Logs.route) {
            LogScreen(viewModel) { navController.popBackStack() }
        }
    }
}
