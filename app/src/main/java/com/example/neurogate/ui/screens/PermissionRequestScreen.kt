package com.example.neurogate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neurogate.service.DetectionServiceManager
import com.example.neurogate.service.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestScreen(
    detectionServiceManager: DetectionServiceManager,
    permissionManager: PermissionManager,
    onPermissionsGranted: () -> Unit
) {
    var serviceStatus by remember { 
        mutableStateOf(detectionServiceManager.getServiceStatus())
    }
    
    var permissionStatus by remember {
        mutableStateOf(permissionManager.getPermissionStatus(detectionServiceManager))
    }
    
    // Check permissions periodically
    LaunchedEffect(Unit) {
        while (true) {
            serviceStatus = detectionServiceManager.getServiceStatus()
            permissionStatus = permissionManager.getPermissionStatus(detectionServiceManager)
            
            if (serviceStatus.isFullyConfigured) {
                onPermissionsGranted()
                break
            }
            kotlinx.coroutines.delay(1000) // Check every second
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Required") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (permissionStatus.isFirstLaunch) "Welcome to NeuroGate" else "Setup Required",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = if (permissionStatus.isFirstLaunch) 
                        "To protect you from AI misuse, we need a few permissions" 
                    else 
                        "Some permissions are required for full protection",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Permission Cards
            PermissionCard(
                title = "Accessibility Service",
                description = "Monitors text input across all apps to detect misuse",
                icon = Icons.Default.Accessibility,
                isGranted = serviceStatus.isAccessibilityEnabled,
                onRequest = { detectionServiceManager.openAccessibilitySettings() }
            )
            
            PermissionCard(
                title = "Display Over Other Apps",
                description = "Shows sliding notifications when misuse is detected",
                icon = Icons.Default.Notifications,
                isGranted = serviceStatus.isOverlayPermissionGranted,
                onRequest = { detectionServiceManager.requestOverlayPermission() }
            )
            
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (serviceStatus.isFullyConfigured) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (serviceStatus.isFullyConfigured) 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.Pending,
                        contentDescription = "Status",
                        tint = if (serviceStatus.isFullyConfigured) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = if (serviceStatus.isFullyConfigured) 
                            "All Set! 🎉" 
                        else 
                            "Setup Required",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = if (serviceStatus.isFullyConfigured) 
                            "NeuroGate is now protecting you across all apps" 
                        else 
                            "Please grant the required permissions above",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Skip Button (for testing)
            TextButton(
                onClick = onPermissionsGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now (Demo Mode)")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isGranted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Grant")
                }
            }
        }
    }
}
