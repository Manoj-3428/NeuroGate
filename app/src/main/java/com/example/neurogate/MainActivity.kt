package com.example.neurogate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.neurogate.navigation.AppNavigation
import com.example.neurogate.ui.AIDetectionViewModel
import com.example.neurogate.ui.AIDetectionViewModelFactory
import com.example.neurogate.ui.theme.NeuroGateTheme
import com.example.neurogate.service.DetectionServiceManager
import com.example.neurogate.service.PermissionManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private val viewModel: AIDetectionViewModel by viewModels { 
        AIDetectionViewModelFactory(this)
    }
    
    private lateinit var detectionServiceManager: DetectionServiceManager
    private lateinit var permissionManager: PermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize managers
        detectionServiceManager = DetectionServiceManager(this)
        permissionManager = PermissionManager(this)
        
        setContent {
            NeuroGateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        detectionServiceManager = detectionServiceManager,
                        permissionManager = permissionManager
                    )
                }
            }
        }
        
        // Check if permissions are already granted and start service if needed
        checkAndStartService()
    }
    
    private fun checkAndStartService() {
        val status = detectionServiceManager.getServiceStatus()
        if (status.isFullyConfigured) {
            // Permissions already granted, start the service
            detectionServiceManager.startDetectionService()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Check service status when activity resumes
        detectionServiceManager.getServiceStatus()
    }
}