package com.example.neurogate.navigation

import HomeScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neurogate.ui.AIDetectionViewModel
import com.example.neurogate.ui.screens.DashboardScreen
import com.example.neurogate.ui.screens.InputScreen

import com.example.neurogate.ui.screens.PermissionRequestScreen
import com.example.neurogate.ui.screens.ActivityHistoryScreen
import com.example.neurogate.service.DetectionServiceManager
import com.example.neurogate.service.PermissionManager
import com.example.neurogate.ui.viewmodels.ActivityViewModel

sealed class Screen(val route: String) {
    object Permissions : Screen("permissions")
    object Home : Screen("home")
    object Input : Screen("input")
    object Dashboard : Screen("dashboard")

    object ActivityHistory : Screen("activity_history")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: AIDetectionViewModel,
    activityViewModel: ActivityViewModel,
    detectionServiceManager: DetectionServiceManager? = null,
    permissionManager: PermissionManager? = null
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Permissions.route
    ) {
        composable(Screen.Permissions.route) {
            if (detectionServiceManager != null && permissionManager != null) {
                PermissionRequestScreen(
                    detectionServiceManager = detectionServiceManager,
                    permissionManager = permissionManager,
                    onPermissionsGranted = {
                        permissionManager.markAppLaunched()
                        permissionManager.markPermissionsExplained()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Permissions.route) { inclusive = true }
                        }
                    }
                )
            }
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = activityViewModel,
                onNavigateToActivityHistory = {
                    navController.navigate(Screen.ActivityHistory.route)
                }
            )
        }
        
        composable(Screen.Input.route) {
            InputScreen(
                viewModel = viewModel,
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                }
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        

        
        composable(Screen.ActivityHistory.route) {
            ActivityHistoryScreen(
                viewModel = activityViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
