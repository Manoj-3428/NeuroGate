package com.example.neurogate.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager for the real-time detection service
 * Handles service lifecycle and accessibility permissions
 */
class DetectionServiceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "DetectionServiceManager"
        
        // Action constants
        const val ACTION_START_DETECTION = "com.example.neurogate.START_DETECTION"
        const val ACTION_STOP_DETECTION = "com.example.neurogate.STOP_DETECTION"
        const val ACTION_TOGGLE_DETECTION = "com.example.neurogate.TOGGLE_DETECTION"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        
        if (accessibilityEnabled == 1) {
            val service = "${context.packageName}/${RealTimeDetectionService::class.java.name}"
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            return settingValue?.contains(service) == true
        }
        
        return false
    }
    
    /**
     * Start the real-time detection service
     */
    fun startDetectionService() {
        if (!isAccessibilityServiceEnabled()) {
            Log.w(TAG, "Accessibility service not enabled")
            return
        }
        
        try {
            val intent = Intent(context, RealTimeDetectionService::class.java).apply {
                action = ACTION_START_DETECTION
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            Log.d(TAG, "Detection service started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting detection service", e)
        }
    }
    
    /**
     * Stop the real-time detection service
     */
    fun stopDetectionService() {
        try {
            val intent = Intent(context, RealTimeDetectionService::class.java).apply {
                action = ACTION_STOP_DETECTION
            }
            context.stopService(intent)
            
            Log.d(TAG, "Detection service stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping detection service", e)
        }
    }
    
    /**
     * Toggle the detection service on/off
     */
    fun toggleDetectionService() {
        if (isDetectionServiceRunning()) {
            stopDetectionService()
        } else {
            startDetectionService()
        }
    }
    
    /**
     * Check if detection service is currently running
     */
    fun isDetectionServiceRunning(): Boolean {
        // This is a simplified check - in a real app you might want to use
        // ActivityManager to check if the service is actually running
        return isAccessibilityServiceEnabled()
    }
    
    /**
     * Open accessibility settings
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Request overlay permission (for sliding notifications)
     */
    fun requestOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }
    
    /**
     * Check if overlay permission is granted
     */
    fun hasOverlayPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * Initialize the detection service with proper permissions
     */
    fun initializeDetectionService() {
        serviceScope.launch {
            try {
                // Check and request permissions
                if (!isAccessibilityServiceEnabled()) {
                    Log.i(TAG, "Accessibility service not enabled, requesting permission")
                    openAccessibilitySettings()
                    return@launch
                }
                
                if (!hasOverlayPermission()) {
                    Log.i(TAG, "Overlay permission not granted, requesting permission")
                    requestOverlayPermission()
                    return@launch
                }
                
                // Start the service
                startDetectionService()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing detection service", e)
            }
        }
    }
    
    /**
     * Get service status information
     */
    fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            isAccessibilityEnabled = isAccessibilityServiceEnabled(),
            isOverlayPermissionGranted = hasOverlayPermission(),
            isServiceRunning = isDetectionServiceRunning()
        )
    }
    
    /**
     * Force restart the detection service
     */
    fun forceRestartService() {
        try {
            Log.d(TAG, "Force restarting detection service")
            
            // Stop the service if it's running
            stopDetectionService()
            
            // Wait a moment
            Thread.sleep(1000)
            
            // Start the service again
            startDetectionService()
            
            Log.d(TAG, "Service force restarted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error force restarting service", e)
        }
    }
    
    /**
     * Data class for service status
     */
    data class ServiceStatus(
        val isAccessibilityEnabled: Boolean,
        val isOverlayPermissionGranted: Boolean,
        val isServiceRunning: Boolean
    ) {
        val isFullyConfigured: Boolean
            get() = isAccessibilityEnabled && isOverlayPermissionGranted && isServiceRunning
    }
}
