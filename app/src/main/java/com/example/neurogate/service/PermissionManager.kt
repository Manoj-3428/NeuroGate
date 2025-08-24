package com.example.neurogate.service

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

/**
 * Manages permission state and provides a smooth onboarding experience
 */
class PermissionManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "neurogate_permissions"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PERMISSIONS_EXPLAINED = "permissions_explained"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // State for UI
    private val _isFirstLaunch = mutableStateOf(isFirstLaunch())
    val isFirstLaunch: State<Boolean> = _isFirstLaunch
    
    private val _permissionsExplained = mutableStateOf(arePermissionsExplained())
    val permissionsExplained: State<Boolean> = _permissionsExplained
    
    /**
     * Check if this is the first time launching the app
     */
    private fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    /**
     * Mark that the app has been launched
     */
    fun markAppLaunched() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        _isFirstLaunch.value = false
    }
    
    /**
     * Check if permissions have been explained to the user
     */
    private fun arePermissionsExplained(): Boolean {
        return prefs.getBoolean(KEY_PERMISSIONS_EXPLAINED, false)
    }
    
    /**
     * Mark that permissions have been explained
     */
    fun markPermissionsExplained() {
        prefs.edit().putBoolean(KEY_PERMISSIONS_EXPLAINED, true).apply()
        _permissionsExplained.value = true
    }
    
    /**
     * Check if we should show the permission request screen
     */
    fun shouldShowPermissionScreen(detectionServiceManager: DetectionServiceManager): Boolean {
        val serviceStatus = detectionServiceManager.getServiceStatus()
        
        // Show if it's first launch OR if permissions are not fully configured
        return isFirstLaunch() || !serviceStatus.isFullyConfigured
    }
    
    /**
     * Get permission status summary
     */
    fun getPermissionStatus(detectionServiceManager: DetectionServiceManager): PermissionStatus {
        val serviceStatus = detectionServiceManager.getServiceStatus()
        
        return PermissionStatus(
            isFirstLaunch = isFirstLaunch(),
            isAccessibilityEnabled = serviceStatus.isAccessibilityEnabled,
            isOverlayPermissionGranted = serviceStatus.isOverlayPermissionGranted,
            isServiceRunning = serviceStatus.isServiceRunning,
            isFullyConfigured = serviceStatus.isFullyConfigured,
            permissionsExplained = arePermissionsExplained()
        )
    }
    
    /**
     * Data class for permission status
     */
    data class PermissionStatus(
        val isFirstLaunch: Boolean,
        val isAccessibilityEnabled: Boolean,
        val isOverlayPermissionGranted: Boolean,
        val isServiceRunning: Boolean,
        val isFullyConfigured: Boolean,
        val permissionsExplained: Boolean
    ) {
        val needsSetup: Boolean
            get() = !isFullyConfigured
        
        val canProceed: Boolean
            get() = isFullyConfigured || permissionsExplained
    }
}
