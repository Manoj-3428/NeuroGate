package com.example.neurogate.service

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import com.example.neurogate.R
import com.example.neurogate.ai.LanguageModelService
import com.example.neurogate.data.PromptAnalysisResponse
import com.example.neurogate.data.MisuseCategory
import com.example.neurogate.data.DetectedActivity
import com.example.neurogate.data.ActivityRepository
import com.example.neurogate.data.AppDatabase
import kotlinx.coroutines.*
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.toColorInt
import androidx.core.app.NotificationCompat

/**
 * Real-time content detection service
 * Monitors all text input across the device
 * Shows sliding notifications for detected misuse
 */
class RealTimeDetectionService : AccessibilityService() {
    
         private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
         private lateinit var languageModelService: LanguageModelService
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private lateinit var activityRepository: ActivityRepository
    
    // UI Components for sliding notification
    private var windowManager: WindowManager? = null
    private var notificationView: View? = null
    private var isNotificationShowing = false
    private var progressAnimator: ValueAnimator? = null
    private var lastNotificationTime = 0L
    private val notificationCooldown = 2000L // 2 seconds cooldown
    
    // Detection settings
    private var isDetectionEnabled = true
    private var lastDetectedText = ""
    private var currentInputText = ""
    private var lastInputTime = 0L
    private var isProcessingDetection = false
    
    // Undo functionality
    private data class ClearedTextData(
        val originalText: String,
        val sourceNode: AccessibilityNodeInfo?,
        val packageName: String,
        val timestamp: Long
    )
    
    private var lastClearedTextData: ClearedTextData? = null
    private var lastPendingActivityId: Long = -1L // Track the last pending activity ID
    // Removed blockedFields - now using fieldUndoFlags only
    // Field-based flag system for undo protection
    private val fieldUndoFlags = mutableMapOf<String, Boolean>() // Track undo flags for each field
    
    companion object {
        private const val TAG = "RealTimeDetection"
        
        // Detection categories with colors - Consistent with pie chart
        val categoryColors = mapOf(
            "PERSONAL_DATA" to "#4FC3F7".toColorInt(), // Vibrant Blue
            "HACKING" to "#FF9800".toColorInt(), // Orange
            "IMAGE_VIDEO_MISUSE" to "#4CAF50".toColorInt(), // Green
            "EXPLOSIVES" to "#F44336".toColorInt(), // Red
            "DRUGS" to "#9C27B0".toColorInt(), // Purple
            "DEEPFAKE" to "#4CAF50".toColorInt(), // Green
            "CELEBRITY_IMPERSONATION" to "#E91E63".toColorInt(), // Pink
            "HARMFUL_CONTENT" to "#FF5722".toColorInt(), // Deep Orange
            "COPYRIGHT_VIOLATION" to "#673AB7".toColorInt(), // Deep Purple
            "PRIVACY_VIOLATION" to "#00BCD4".toColorInt(), // Cyan
            MisuseCategory.PRIVACY_VIOLATION to "#00BCD4".toColorInt(), // Cyan
            MisuseCategory.HARMFUL_CONTENT to "#FF5722".toColorInt(), // Deep Orange
            MisuseCategory.DEEPFAKE to "#4CAF50".toColorInt(), // Green
            MisuseCategory.CELEBRITY_IMPERSONATION to "#E91E63".toColorInt(), // Pink
            MisuseCategory.NONE to "#607D8B".toColorInt() // Blue Grey
        )
        
        val categoryIcons = mapOf(
            MisuseCategory.PRIVACY_VIOLATION to "🔒",
            MisuseCategory.HARMFUL_CONTENT to "⚠️",
            MisuseCategory.DEEPFAKE to "🎭",
            MisuseCategory.CELEBRITY_IMPERSONATION to "👤",
            MisuseCategory.NONE to "✅"
        )
    }
    
         override fun onCreate() {
         super.onCreate()
         Log.d(TAG, "RealTimeDetectionService created")
         
         languageModelService = LanguageModelService(this)
        
        // Initialize activity repository
        val database = AppDatabase.getDatabase(this)
        activityRepository = ActivityRepository(database.detectedActivityDao())
         
         // Initialize window manager for sliding notifications
         windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
         
         // Clear any existing notifications on service start
         removeNotificationView()
         
         // Start foreground service to prevent being killed
         startForegroundService()
         
         // Acquire wake lock to prevent service from being killed
         acquireWakeLock()
         
         // Start periodic health check
         startHealthCheck()
         
         // Ensure service stays active
         ensureServiceActive()
     }
    
         private fun startHealthCheck() {
         serviceScope.launch {
             while (isActive) {
                 try {
                     // Log service status every 15 seconds
                     Log.d(TAG, "Service health check - Detection enabled: $isDetectionEnabled, Processing: $isProcessingDetection")
                     
                     // Reset processing flag if stuck
                     if (isProcessingDetection && (System.currentTimeMillis() - lastInputTime) > 5000) {
                         Log.w(TAG, "Resetting stuck processing flag")
                         isProcessingDetection = false
                     }
                     
                     // Force reset detection state periodically
                     if (System.currentTimeMillis() - lastInputTime > 60000) {
                         Log.d(TAG, "Resetting detection state due to inactivity")
                         resetDetectionState()
                     }
                     
                     // Check and reset undo flags for empty fields
                     checkAndResetUndoFlags()
                     
                     delay(15000) // Check every 15 seconds
                 } catch (e: Exception) {
                     Log.e(TAG, "Error in health check", e)
                 }
             }
         }
     }
     
     private fun startForegroundService() {
         try {
             // Create notification channel for foreground service
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                 val channel = android.app.NotificationChannel(
                     "neurogate_service",
                     "NeuroGate Detection Service",
                     android.app.NotificationManager.IMPORTANCE_LOW
                 ).apply {
                     description = "Keeps the detection service running"
                     setShowBadge(false)
                 }
                 
                 val notificationManager = getSystemService(android.app.NotificationManager::class.java)
                 notificationManager.createNotificationChannel(channel)
             }
             
             // Create notification
             val notification = NotificationCompat.Builder(this, "neurogate_service")
                 .setContentTitle("NeuroGate Active")
                 .setContentText("Monitoring for harmful content")
                 .setSmallIcon(android.R.drawable.ic_dialog_info)
                 .setPriority(NotificationCompat.PRIORITY_LOW)
                 .setOngoing(true)
                 .setSilent(true)
                 .build()
             
             // Start foreground service
             startForeground(1001, notification)
             Log.d(TAG, "Foreground service started")
             
         } catch (e: Exception) {
             Log.e(TAG, "Error starting foreground service", e)
         }
     }
     
     private fun acquireWakeLock() {
         try {
             val powerManager = getSystemService(android.os.PowerManager::class.java)
             wakeLock = powerManager.newWakeLock(
                 android.os.PowerManager.PARTIAL_WAKE_LOCK,
                 "NeuroGate::DetectionServiceWakeLock"
             )
             wakeLock?.acquire(10*60*1000L /*10 minutes*/)
             Log.d(TAG, "Wake lock acquired")
         } catch (e: Exception) {
             Log.e(TAG, "Error acquiring wake lock", e)
         }
     }
     
     private fun ensureServiceActive() {
         serviceScope.launch {
             while (isActive) {
                 try {
                     // Log service activity to keep it alive
                     Log.d(TAG, "Service active - monitoring all apps and websites")
                     
                     // Check if service is still enabled
                     if (!isDetectionEnabled) {
                         Log.w(TAG, "Detection disabled, re-enabling")
                         isDetectionEnabled = true
                     }
                     
                     // Check if accessibility service is still enabled
                     if (!isAccessibilityServiceEnabled()) {
                         Log.w(TAG, "Accessibility service disabled, attempting to restart")
                         restartAccessibilityService()
                     }
                     
                     delay(30000) // Check every 30 seconds
                 } catch (e: Exception) {
                     Log.e(TAG, "Error in service activity check", e)
                 }
             }
         }
     }
     
     private fun isAccessibilityServiceEnabled(): Boolean {
         val accessibilityEnabled = android.provider.Settings.Secure.getInt(
             contentResolver,
             android.provider.Settings.Secure.ACCESSIBILITY_ENABLED, 0
         )
         
         if (accessibilityEnabled == 1) {
             val service = "${packageName}/${RealTimeDetectionService::class.java.name}"
             val settingValue = android.provider.Settings.Secure.getString(
                 contentResolver,
                 android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
             )
             
             return settingValue?.contains(service) == true
         }
         
         return false
     }
     
     private fun restartAccessibilityService() {
         try {
             // Send broadcast to restart the service
             val intent = android.content.Intent("com.example.neurogate.RESTART_SERVICE")
             sendBroadcast(intent)
             Log.d(TAG, "Broadcast sent to restart service")
         } catch (e: Exception) {
             Log.e(TAG, "Error restarting accessibility service", e)
         }
     }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isDetectionEnabled) return
        
        val packageName = event.packageName?.toString() ?: "unknown"
        
        // Log all events for debugging
        Log.d(TAG, "Event from $packageName: ${event.eventType} - Text: '${event.text?.joinToString("")?.take(20)}...'")
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChange(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                handleTextSelection(event)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleViewFocused(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Handle for all apps, not just specific ones
                handleWindowContentChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Handle for all apps
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // Handle scroll events which might indicate text input
                handleViewScrolled(event)
            }
        }
    }
    
    override fun onKeyEvent(event: android.view.KeyEvent?): Boolean {
        event?.let { keyEvent ->
            // Log key events for debugging
            if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "Key pressed: ${keyEvent.keyCode} in ${packageName}")
            }
        }
        return super.onKeyEvent(event)
    }
    
         private fun handleTextChange(event: AccessibilityEvent) {
         val text = event.text?.joinToString("") ?: ""
         val packageName = event.packageName?.toString() ?: ""
         
         // Check and reset flags for empty fields immediately
         if (text.isBlank()) {
             checkAndResetUndoFlagsImmediately(event.source, packageName)
         }
         
         // Skip if same text or empty
         if (text.isBlank() || text == lastDetectedText) return
         
         // Skip URLs, domains, and long content that's not being typed
         if (text.contains("http") || text.contains("www") || text.contains(".com") || 
             text.contains(".org") || text.contains(".net") || text.contains(".in") ||
             text.length > 100 || text.contains("/")) {
             Log.d(TAG, "Skipping URL/domain/long content: '${text.take(30)}...'")
             return
         }
         
         // Update current input text
         currentInputText = text
         lastInputTime = System.currentTimeMillis()
         
         Log.d(TAG, "Text change in $packageName: '${text.take(30)}...' (length: ${text.length})")
         
         // More aggressive detection for web browsers
         val minLength = if (isWebBrowser(packageName)) 3 else 2
         
         // Check if text is long enough and looks like user input (not a URL/domain)
         if (text.length >= minLength && !text.contains("/") && !text.contains(".") && 
             !text.matches(Regex("^[a-zA-Z0-9.-]+$"))) {
             lastDetectedText = text
             isProcessingDetection = true
             
             // Check if the field is blocked before analyzing
             if (!isFieldBlocked(event.source, packageName)) {
                 serviceScope.launch {
                     try {
                         analyzeTextInRealTime(text, event.source, packageName)
                     } finally {
                         isProcessingDetection = false
                     }
                 }
             } else {
                 Log.d(TAG, "Field blocked from analysis in $packageName: '${text.take(30)}...'")
                 isProcessingDetection = false
             }
         }
     }
    
         private fun isWebBrowser(packageName: String): Boolean {
         return packageName.contains("chrome") || 
                packageName.contains("firefox") || 
                packageName.contains("samsung") || 
                packageName.contains("browser") ||
                packageName.contains("edge") ||
                packageName.contains("opera") ||
                packageName.contains("brave") ||
                packageName.contains("ucbrowser") ||
                packageName.contains("maxthon") ||
                packageName.contains("dolphin") ||
                packageName.contains("webview") ||
                packageName.contains("webview2") ||
                packageName.contains("chromium") ||
                packageName.contains("webkit")
     }
    
    private fun handleTextSelection(event: AccessibilityEvent) {
        val nodeInfo = event.source ?: return
        val selectedText = getSelectedText(nodeInfo)
        val packageName = event.packageName?.toString() ?: ""
        
        if (selectedText.isNotBlank() && selectedText != lastDetectedText) {
            // Check if the field is blocked before analyzing
            if (!isFieldBlocked(nodeInfo, packageName)) {
                lastDetectedText = selectedText
                serviceScope.launch {
                    analyzeTextInRealTime(selectedText, nodeInfo, packageName)
                }
            } else {
                Log.d(TAG, "Field blocked from selection analysis in $packageName: '${selectedText.take(30)}...'")
            }
        }
    }
    
    private fun handleViewFocused(event: AccessibilityEvent) {
        // When a text field gets focus, check if it has content
        val nodeInfo = event.source ?: return
        if (nodeInfo.isEditable) {
            val text = nodeInfo.text?.toString() ?: ""
            val packageName = event.packageName?.toString() ?: ""
            
            if (text.isNotBlank() && text.length > 5) {
                // Check if the field is blocked before analyzing
                if (!isFieldBlocked(nodeInfo, packageName)) {
                    serviceScope.launch {
                        analyzeTextInRealTime(text, nodeInfo, packageName)
                    }
                } else {
                    Log.d(TAG, "Field blocked from focus analysis in $packageName: '${text.take(30)}...'")
                }
            }
        }
    }
    
    private fun handleViewClicked(event: AccessibilityEvent) {
        // Check content when user clicks on text fields
        val nodeInfo = event.source ?: return
        if (nodeInfo.isEditable) {
            val text = nodeInfo.text?.toString() ?: ""
            val packageName = event.packageName?.toString() ?: ""
            
            if (text.isNotBlank() && text.length > 5) {
                // Check if the field is blocked before analyzing
                if (!isFieldBlocked(nodeInfo, packageName)) {
                    serviceScope.launch {
                        analyzeTextInRealTime(text, nodeInfo, packageName)
                    }
                } else {
                    Log.d(TAG, "Field blocked from click analysis in $packageName: '${text.take(30)}...'")
                }
            }
        }
    }
    
         private fun handleWindowContentChanged(event: AccessibilityEvent) {
         val packageName = event.packageName?.toString() ?: ""
         
         Log.d(TAG, "Window content changed in $packageName")
         
         // More aggressive handling for web browsers
         if (isWebBrowser(packageName)) {
             Log.d(TAG, "Enhanced web browser content change handling")
             
             // Analyze all editable fields (not just focused ones) for web browsers
             findAndAnalyzeEditableFields(rootInActiveWindow, packageName)
             
             // Also check for text in the event itself for web browsers
             val text = event.text?.joinToString("") ?: ""
             if (text.isNotBlank() && text.length >= 1 && text != lastDetectedText && 
                 !text.contains("http") && !text.contains("www")) {
                 // Check if the field is blocked before analyzing
                 if (!isFieldBlocked(event.source, packageName)) {
                     serviceScope.launch {
                         analyzeTextInRealTime(text, event.source, packageName)
                     }
                 } else {
                     Log.d(TAG, "Field blocked from web content analysis in $packageName: '${text.take(30)}...'")
                 }
             }
             
             // Additional web-specific detection
             serviceScope.launch {
                 delay(100) // Small delay to let web content settle
                 findAndAnalyzeEditableFields(rootInActiveWindow, packageName)
             }
         } else {
             // For regular apps, only analyze focused editable fields
             findAndAnalyzeEditableFields(rootInActiveWindow, packageName)
         }
     }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        // Handle for all apps - more aggressive detection
        val packageName = event.packageName?.toString() ?: ""
        
        Log.d(TAG, "Window state changed in $packageName")
        
        // Small delay to let the page load
        serviceScope.launch {
            delay(500)
            findAndAnalyzeEditableFields(rootInActiveWindow, packageName)
        }
    }
    
    private fun handleViewScrolled(event: AccessibilityEvent) {
        // Handle scroll events which might indicate text input
        val packageName = event.packageName?.toString() ?: ""
        
        Log.d(TAG, "View scrolled in $packageName")
        
        // Check for text in scrollable content
        val text = event.text?.joinToString("") ?: ""
        if (text.isNotBlank() && text.length > 2 && text != lastDetectedText) {
            // Check if the field is blocked before analyzing
            if (!isFieldBlocked(event.source, packageName)) {
                serviceScope.launch {
                    analyzeTextInRealTime(text, event.source, packageName)
                }
            } else {
                Log.d(TAG, "Field blocked from scroll analysis in $packageName: '${text.take(30)}...'")
            }
        }
    }
    
    private fun getSelectedText(nodeInfo: AccessibilityNodeInfo): String {
        val start = nodeInfo.textSelectionStart
        val end = nodeInfo.textSelectionEnd
        
        return if (start >= 0 && end > start) {
            val text = nodeInfo.text?.toString() ?: ""
            text.substring(start, end)
        } else {
            ""
        }
    }
    
         private fun findAndAnalyzeEditableFields(rootNode: AccessibilityNodeInfo?, packageName: String) {
         if (rootNode == null) return
         
         try {
             val isWeb = isWebBrowser(packageName)
             val minLength = if (isWeb) 3 else 2
             
             // More aggressive detection for web browsers
             if (rootNode.isEditable) {
                 val text = rootNode.text?.toString() ?: ""
                 val isFocused = rootNode.isFocused
                 
                 // Skip URLs, domains, and navigation elements
                 if (text.contains("http") || text.contains("www") || text.contains(".com") || 
                     text.contains(".org") || text.contains(".net") || text.contains(".in") ||
                     text.contains("/") || text.matches(Regex("^[a-zA-Z0-9.-]+$"))) {
                     return
                 }
                 
                 // For web browsers, check both focused and non-focused editable fields
                 if (text.isNotBlank() && text.length >= minLength && text != lastDetectedText && 
                     (isWeb || isFocused)) {
                     
                     // Check if this field is blocked from detection
                     if (isFieldBlocked(rootNode, packageName)) {
                         Log.d(TAG, "Field blocked from detection in $packageName: '${text.take(30)}...'")
                         return
                     }
                     

                     
                     Log.d(TAG, "Found editable field in $packageName: '${text.take(30)}...' (focused: $isFocused, web: $isWeb)")
                     serviceScope.launch {
                         analyzeTextInRealTime(text, rootNode, packageName)
                     }
                 }
             }
             
             // For web browsers, also check non-editable nodes that might contain input text
             if (isWeb) {
                 val text = rootNode.text?.toString() ?: ""
                 val className = rootNode.className?.toString() ?: ""
                 
                 // Skip URLs and domains
                 if (text.contains("http") || text.contains("www") || text.contains(".com") || 
                     text.contains(".org") || text.contains(".net") || text.contains(".in") ||
                     text.contains("/") || text.matches(Regex("^[a-zA-Z0-9.-]+$"))) {
                     return
                 }
                 
                 // Check for web input patterns
                 val isWebInput = className.contains("input") || 
                                 className.contains("textarea") || 
                                 className.contains("text") ||
                                 className.contains("search") ||
                                 rootNode.contentDescription?.toString()?.contains("input") == true ||
                                 rootNode.contentDescription?.toString()?.contains("search") == true
                 
                 if (isWebInput && text.isNotBlank() && text.length >= minLength && text != lastDetectedText) {
                     
                     // Check if this field is blocked from detection
                     if (isFieldBlocked(rootNode, packageName)) {
                         Log.d(TAG, "Web field blocked from detection in $packageName: '${text.take(30)}...'")
                         return
                     }
                     

                     
                     Log.d(TAG, "Found web input field in $packageName: '${text.take(30)}...' (class: $className)")
                     serviceScope.launch {
                         analyzeTextInRealTime(text, rootNode, packageName)
                     }
                 }
             }
             
             // Recursively check child nodes (limit depth to prevent excessive recursion)
             for (i in 0 until minOf(rootNode.childCount, 10)) {
                 findAndAnalyzeEditableFields(rootNode.getChild(i), packageName)
             }
         } catch (e: Exception) {
             Log.e(TAG, "Error finding editable fields", e)
         }
     }
    
    private suspend fun analyzeTextInRealTime(text: String, source: AccessibilityNodeInfo?, packageName: String = "") {
        try {
            Log.d(TAG, "Analyzing text in $packageName: '${text.take(50)}...'")
            
            // Check for manual reset command
            if (text.trim() == "RESET_FLAG" || text.trim() == "reset_flag") {
                val fieldId = generateFieldId(source, packageName)
                if (fieldUndoFlags[fieldId] == true) {
                    fieldUndoFlags[fieldId] = false
                    Log.d(TAG, "🔄 MANUAL FLAG RESET: $fieldId = false (user requested reset)")
                    showManualResetFeedback()
                    return
                }
            }
            
            // Check for DeepSeek specific reset command
            if (text.trim() == "DEEPSEEK_RESET" && packageName == "com.deepseek.chat") {
                // Reset all DeepSeek flags
                val flagsToReset = fieldUndoFlags.filter { it.key.contains("com.deepseek.chat") }
                flagsToReset.forEach { (fieldId, _) ->
                    fieldUndoFlags[fieldId] = false
                    Log.d(TAG, "🔄 DEEPSEEK MANUAL RESET: $fieldId = false")
                }
                showManualResetFeedback()
                return
            }
            
            val analysis = languageModelService.analyzePromptWithLLM(text)
            
            if (analysis.isMisuse && analysis.confidence > 0.75) {
                Log.w(TAG, "🚨 DETECTED MISUSE in $packageName: ${analysis.category} (confidence: ${analysis.confidence})")
                
                // Capture the text before clearing
                Log.d(TAG, "📝 Capturing text - currentInputText length: ${currentInputText.length}, content: '${currentInputText.take(50)}...'")
                val detectedText = currentInputText
                
                // Store detected activity in background with captured text
                Log.d(TAG, "💾 Storing to DB - Length: ${detectedText.length}, Content: '${detectedText.take(50)}...'")
                storeDetectedActivity(detectedText, analysis.category.name, packageName, analysis.confidence)
                
                // Show sliding notification with captured text
                showSlidingNotification(analysis, detectedText)
                
                // Clear input field if possible
                clearInputField(source)
                
                // Reset detection state to allow future detections
                lastDetectedText = ""
                currentInputText = ""
                
                // Short cooldown to prevent spam but allow new detections
                delay(500)
            } else {
                Log.d(TAG, "✅ SAFE content in $packageName (confidence: ${analysis.confidence})")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing text in $packageName", e)
            // Reset processing flag on error
            isProcessingDetection = false
        }
    }
    
         private fun showSlidingNotification(analysis: PromptAnalysisResponse, detectedText: String) {
         val currentTime = System.currentTimeMillis()
         
         // Prevent multiple notifications and respect cooldown
         if (isNotificationShowing) {
             Log.d(TAG, "Notification already showing, skipping")
             return
         }
         
         // Check cooldown
         if (currentTime - lastNotificationTime < notificationCooldown) {
             Log.d(TAG, "Notification cooldown active, skipping")
             return
         }
         
         // Force remove any existing notification first
         removeNotificationView()
         
         serviceScope.launch(Dispatchers.Main) {
             try {
                 // Double check to prevent race conditions
                 if (isNotificationShowing) {
                     Log.d(TAG, "Notification already showing in coroutine, skipping")
                     return@launch
                 }
                 
                 createNotificationView(analysis, detectedText)
                 animateNotificationIn()
             } catch (e: Exception) {
                 Log.e(TAG, "Error showing notification", e)
                 // Reset state on error
                 isNotificationShowing = false
                 notificationView = null
             }
         }
     }
    
         private fun createNotificationView(analysis: PromptAnalysisResponse, detectedText: String) {
         val inflater = LayoutInflater.from(this)
         notificationView = inflater.inflate(R.layout.sliding_notification, null)
         
         // Set up notification content
         val categoryTitle = notificationView?.findViewById<TextView>(R.id.categoryTitle)
         val deleteReason = notificationView?.findViewById<TextView>(R.id.deleteReason)
         val deletedContent = notificationView?.findViewById<TextView>(R.id.deletedContent)
         
         // Set category-specific content
         val category = analysis.category
         val categoryString = category.name
         val color = categoryColors[categoryString] ?: categoryColors[category] ?: Color.GRAY
         
         categoryTitle?.text = getCategoryTitle(category)
         deleteReason?.text = analysis.reason
         
         // Log the detected text for debugging
         Log.d(TAG, "🔍 Notification text - Length: ${detectedText.length}, Content: '${detectedText.take(50)}...'")
         
         deletedContent?.text = "User typed: \"${detectedText.take(150)}${if (detectedText.length > 150) "..." else ""}\""
         
         // Set up close button functionality
         val closeButton = notificationView?.findViewById<ImageButton>(R.id.closeButton)
         closeButton?.setOnClickListener {
             Log.d(TAG, "Close button clicked")
             progressAnimator?.cancel()
             animateNotificationOut()
         }
         
         // Set up undo button functionality
         val undoButton = notificationView?.findViewById<Button>(R.id.undoButton)
         undoButton?.setOnClickListener {
             Log.d(TAG, "Undo button clicked")
             progressAnimator?.cancel()
             undoLastClear()
             animateNotificationOut()
         }
         
         // Set up dismiss button functionality
         val dismissButton = notificationView?.findViewById<Button>(R.id.dismissButton)
         dismissButton?.setOnClickListener {
             Log.d(TAG, "Dismiss button clicked")
             progressAnimator?.cancel()
             animateNotificationOut()
         }
         
         // Set background color with modern card design
         val background = notificationView?.findViewById<View>(R.id.notificationBackground)
         background?.background = createModernCardBackground(color)
         
         // Get screen dimensions
         val displayMetrics = resources.displayMetrics
         val screenWidth = displayMetrics.widthPixels
         val screenHeight = displayMetrics.heightPixels
         
         // Set up window parameters for center positioning with 70% width and 50% height
         val params = WindowManager.LayoutParams().apply {
             type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
             } else {
                 WindowManager.LayoutParams.TYPE_PHONE
             }
             flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                     WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
             format = PixelFormat.TRANSLUCENT
             width = (screenWidth * 0.7).toInt()  // 70% of screen width
             height = (screenHeight * 0.5).toInt() // 50% of screen height
             gravity = Gravity.CENTER
             x = 0
             y = 0
         }
         
         // Add view to window
         windowManager?.addView(notificationView, params)
         isNotificationShowing = true
         lastNotificationTime = System.currentTimeMillis()
         
         // Auto-hide after exactly 10 seconds with progress animation
         serviceScope.launch {
             val progressBar = notificationView?.findViewById<ProgressBar>(R.id.autoHideProgress)
             
             // Animate progress bar from 100 to 0 over 10 seconds
             progressAnimator = ValueAnimator.ofInt(100, 0)
             progressAnimator?.duration = 10000 // Exactly 10 seconds
             progressAnimator?.interpolator = AccelerateDecelerateInterpolator()
             
             progressAnimator?.addUpdateListener { animation ->
                 val progress = animation.animatedValue as Int
                 progressBar?.progress = progress
             }
             
             progressAnimator?.addListener(object : android.animation.Animator.AnimatorListener {
                 override fun onAnimationStart(animation: android.animation.Animator) {}
                 override fun onAnimationEnd(animation: android.animation.Animator) {
                     animateNotificationOut()
                 }
                 override fun onAnimationCancel(animation: android.animation.Animator) {}
                 override fun onAnimationRepeat(animation: android.animation.Animator) {}
             })
             
             progressAnimator?.start()
         }
     }
    
         private fun animateNotificationIn() {
         val view = notificationView ?: return
         
         // Start with 0 alpha and scale 0.8
         view.alpha = 0f
         view.scaleX = 0.8f
         view.scaleY = 0.8f
         
         // Animate to full alpha and scale
         val alphaAnimator = ValueAnimator.ofFloat(0f, 1f)
         val scaleAnimator = ValueAnimator.ofFloat(0.8f, 1f)
         
         alphaAnimator.duration = 300
         scaleAnimator.duration = 300
         
         alphaAnimator.interpolator = AccelerateDecelerateInterpolator()
         scaleAnimator.interpolator = AccelerateDecelerateInterpolator()
         
         alphaAnimator.addUpdateListener { animation ->
             view.alpha = animation.animatedValue as Float
         }
         
         scaleAnimator.addUpdateListener { animation ->
             val scale = animation.animatedValue as Float
             view.scaleX = scale
             view.scaleY = scale
         }
         
         alphaAnimator.start()
         scaleAnimator.start()
     }
    
         private fun animateNotificationOut() {
         val view = notificationView ?: return
         
         // Check if view is still attached before animating
         if (view.parent == null) {
             Log.d(TAG, "View not attached, removing directly")
             removeNotificationView()
             return
         }
         
         // Animate alpha and scale out
         val alphaAnimator = ValueAnimator.ofFloat(1f, 0f)
         val scaleAnimator = ValueAnimator.ofFloat(1f, 0.8f)
         
         alphaAnimator.duration = 200
         scaleAnimator.duration = 200
         
         alphaAnimator.interpolator = AccelerateDecelerateInterpolator()
         scaleAnimator.interpolator = AccelerateDecelerateInterpolator()
         
         alphaAnimator.addUpdateListener { animation ->
             try {
                 if (view.parent != null) {
                     view.alpha = animation.animatedValue as Float
                 }
             } catch (e: Exception) {
                 Log.e(TAG, "Error updating alpha animation", e)
                 removeNotificationView()
             }
         }
         
         scaleAnimator.addUpdateListener { animation ->
             try {
                 if (view.parent != null) {
                     val scale = animation.animatedValue as Float
                     view.scaleX = scale
                     view.scaleY = scale
                 }
             } catch (e: Exception) {
                 Log.e(TAG, "Error updating scale animation", e)
                 removeNotificationView()
             }
         }
         
         alphaAnimator.addListener(object : android.animation.Animator.AnimatorListener {
             override fun onAnimationStart(animation: android.animation.Animator) {}
             override fun onAnimationEnd(animation: android.animation.Animator) {
                 removeNotificationView()
             }
             override fun onAnimationCancel(animation: android.animation.Animator) {}
             override fun onAnimationRepeat(animation: android.animation.Animator) {}
         })
         
         alphaAnimator.start()
         scaleAnimator.start()
     }
    
    private fun removeNotificationView() {
        try {
            // Cancel progress animator
            progressAnimator?.cancel()
            progressAnimator = null
            
            // Remove view from window manager
            notificationView?.let { view ->
                try {
                    windowManager?.removeView(view)
                    Log.d(TAG, "Notification view removed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing view from window manager", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in removeNotificationView", e)
        } finally {
            // Always reset state
            notificationView = null
            isNotificationShowing = false
            
            // Mark the pending activity as notification closed (final storage)
            if (lastPendingActivityId > 0) {
                serviceScope.launch(Dispatchers.IO) {
                    activityRepository.markAsNotificationClosed(lastPendingActivityId)
                    Log.d(TAG, "✅ Marked activity $lastPendingActivityId as notification closed (final storage)")
                    lastPendingActivityId = -1L // Reset the ID
                }
            }
            
            Log.d(TAG, "Notification state reset")
        }
    }
    
         private fun createModernCardBackground(color: Int): GradientDrawable {
         return GradientDrawable().apply {
             shape = GradientDrawable.RECTANGLE
             cornerRadius = 20f // Rounded corners for modern look
             setColor(color)
              // Subtle white border with opacity
         }
     }
     
     private fun createGradientBackground(color: Int): GradientDrawable {
         return GradientDrawable().apply {
             shape = GradientDrawable.RECTANGLE
             cornerRadius = 16f
             setColor(color)
             setStroke(2, Color.WHITE)
         }
     }
    
    private fun getCategoryTitle(category: MisuseCategory): String {
        return when (category) {
            MisuseCategory.PRIVACY_VIOLATION -> "Privacy Violation Detected"
            MisuseCategory.HARMFUL_CONTENT -> "Harmful Content Detected"
            MisuseCategory.DEEPFAKE -> "Image/Video Misuse Detected"
            MisuseCategory.CELEBRITY_IMPERSONATION -> "Celebrity Impersonation Detected"
            MisuseCategory.NONE -> "Content Analysis Complete"
            MisuseCategory.COPYRIGHT_VIOLATION -> "Copyright Violation Detected"
        }
    }
    
    private fun clearInputField(source: AccessibilityNodeInfo?) {
        try {
            source?.let { node ->
                Log.d(TAG, "Attempting to clear input field")
                
                val currentPackage = rootInActiveWindow?.packageName?.toString() ?: ""
                val isWeb = isWebBrowser(currentPackage)
                
                // Store the original text for undo functionality
                val originalText = node.text?.toString() ?: ""
                val fieldId = generateFieldId(node, currentPackage)
                
                // Store cleared text data for undo
                lastClearedTextData = ClearedTextData(
                    originalText = originalText,
                    sourceNode = node,
                    packageName = currentPackage,
                    timestamp = System.currentTimeMillis()
                )
                
                // Note: Field blocking is now handled by fieldUndoFlags system
                Log.d(TAG, "Field cleared: $fieldId - undo functionality available")
                
                // Method 1: Direct clearing
                if (node.isEditable) {
                    val arguments = Bundle()
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    Log.d(TAG, "Direct clear attempt: $success")
                    
                    if (success) return
                }
                
                // Method 2: Select all and delete
                if (node.isEditable) {
                    // Select all text
                    val selectAllArgs = Bundle()
                    selectAllArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                    selectAllArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, node.text?.length ?: 0)
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectAllArgs)
                    
                    // Delete selected text
                    val deleteArgs = Bundle()
                    deleteArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, deleteArgs)
                    Log.d(TAG, "Select all + delete attempt: $success")
                    
                    if (success) return
                }
                
                // Method 3: Try parent nodes
                var parent = node.parent
                var depth = 0
                while (parent != null && depth < 5) {
                    if (parent.isEditable) {
                        val arguments = Bundle()
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                        val success = parent.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                        Log.d(TAG, "Parent clear attempt (depth $depth): $success")
                        
                        if (success) return
                    }
                    parent = parent.parent
                    depth++
                }
                
                // Method 4: Find all editable fields in current window and clear them
                rootInActiveWindow?.let { root ->
                    clearAllEditableFields(root)
                }
                
                // Method 5: For web browsers, use enhanced web input clearing
                if (isWeb) {
                    Log.d(TAG, "Using enhanced web input clearing for $currentPackage")
                    clearWebInputFields(rootInActiveWindow)
                    
                    // Additional web-specific clearing
                    serviceScope.launch {
                        delay(200) // Small delay to let web content settle
                        clearWebInputFields(rootInActiveWindow)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing input field", e)
        }
    }
    
    private fun clearWebInputFields(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return
        
        try {
            // Look for common web input field class names and patterns
            val className = rootNode.className?.toString() ?: ""
            val contentDesc = rootNode.contentDescription?.toString() ?: ""
            val text = rootNode.text?.toString() ?: ""
            
            // More comprehensive web input detection
            val isWebInput = className.contains("EditText") || 
                            className.contains("input") || 
                            className.contains("textarea") || 
                            className.contains("text") ||
                            className.contains("WebView") ||
                            className.contains("WebKit") ||
                            contentDesc.contains("search") ||
                            contentDesc.contains("input") ||
                            contentDesc.contains("text") ||
                            text.isNotBlank()
            
            if (isWebInput && rootNode.isEditable) {
                // Method 1: Direct clear
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                var success = rootNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                Log.d(TAG, "Web input clear attempt 1: $success for class: $className")
                
                if (!success) {
                    // Method 2: Select all then clear
                    val selectArgs = Bundle()
                    selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                    selectArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, text.length)
                    rootNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)
                    
                    val clearArgs = Bundle()
                    clearArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                    success = rootNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clearArgs)
                    Log.d(TAG, "Web input clear attempt 2: $success for class: $className")
                }
                
                                 if (!success) {
                     // Method 3: Try focus then clear
                     rootNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                     // Note: delay() removed as this method is not suspend
                     val focusArgs = Bundle()
                     focusArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                     success = rootNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, focusArgs)
                     Log.d(TAG, "Web input clear attempt 3: $success for class: $className")
                 }
            }
            
            // Recursively check child nodes
            for (i in 0 until rootNode.childCount) {
                clearWebInputFields(rootNode.getChild(i))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing web input fields", e)
        }
    }
    
    private fun clearAllEditableFields(node: AccessibilityNodeInfo?) {
        if (node == null) return
        
        try {
            if (node.isEditable && !node.text.isNullOrBlank()) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                Log.d(TAG, "Bulk clear attempt: $success for field with text: '${node.text?.toString()?.take(20)}...'")
            }
            
            // Recursively check child nodes
            for (i in 0 until node.childCount) {
                clearAllEditableFields(node.getChild(i))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in bulk clear", e)
        }
    }
    
    // Notification methods removed since we're not using foreground service
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
    
         override fun onDestroy() {
         super.onDestroy()
         serviceScope.cancel()
         removeNotificationView()
         
         // Release wake lock
         try {
             wakeLock?.release()
             wakeLock = null
             Log.d(TAG, "Wake lock released")
         } catch (e: Exception) {
             Log.e(TAG, "Error releasing wake lock", e)
         }
         
         Log.d(TAG, "Service destroyed")
     }
    
    // Public method to reset detection state
    fun resetDetectionState() {
        lastDetectedText = ""
        currentInputText = ""
        isProcessingDetection = false
        lastInputTime = 0L
        Log.d(TAG, "Detection state reset")
    }
    
         // Public method to force restart detection
     fun restartDetection() {
         resetDetectionState()
         isDetectionEnabled = true
         Log.d(TAG, "Detection restarted")
     }
     
     // Public method to force restart the entire service
     fun forceRestartService() {
         Log.w(TAG, "Force restarting service")
         serviceScope.launch {
             try {
                 // Stop current service
                 onDestroy()
                 
                 // Small delay
                 delay(1000)
                 
                 // Restart detection
                 restartDetection()
                 
                 Log.d(TAG, "Service force restart completed")
             } catch (e: Exception) {
                 Log.e(TAG, "Error in force restart", e)
             }
         }
     }
    
    // onBind is final in AccessibilityService, so we don't override it
    
    private fun storeDetectedActivity(content: String, category: String, packageName: String, confidence: Double) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Store as pending activity (notification not closed yet)
                val activityId = activityRepository.insertPendingActivity(
                    text = content,
                    category = category,
                    packageName = packageName,
                    confidence = confidence
                )
                
                if (activityId > 0) {
                    Log.d(TAG, "📝 Stored pending activity: $category in $packageName - '${content.take(30)}...' (ID: $activityId)")
                    // Store the activity ID for later use when notification closes
                    lastPendingActivityId = activityId
                } else {
                    Log.e(TAG, "Failed to store pending activity")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error storing detected activity", e)
            }
        }
    }
    
    private fun updateActivityClearedStatus(content: String, packageName: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                // This would update the isCleared status if we had an update method
                // For now, we'll just log that clearing was attempted
                Log.d(TAG, "Attempted to clear content: '${content.take(30)}...' in $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating activity status", e)
            }
        }
    }
    
    // Undo functionality
    private fun undoLastClear() {
        try {
            val clearedData = lastClearedTextData
            if (clearedData != null) {
                Log.d(TAG, "Undoing last clear operation")
                
                // Try to restore the original text
                val success = restoreTextToField(clearedData.originalText, clearedData.sourceNode)
                
                if (success) {
                    Log.d(TAG, "Successfully restored text: '${clearedData.originalText.take(30)}...'")
                    
                    // Set undo flag to true to prevent re-detection of this field
                    val fieldId = generateFieldId(clearedData.sourceNode, clearedData.packageName)
                    fieldUndoFlags[fieldId] = true
                    Log.d(TAG, "🚫 UNDO FLAG SET: $fieldId = true (field protected until empty)")
                    
                    // Mark the pending activity as undo used
                    if (lastPendingActivityId > 0) {
                        serviceScope.launch(Dispatchers.IO) {
                            activityRepository.markAsUndoUsed(lastPendingActivityId)
                            Log.d(TAG, "🔄 Marked activity $lastPendingActivityId as undo used")
                        }
                    }
                    
                    // Show success feedback
                    showUndoSuccessFeedback()
                } else {
                    Log.w(TAG, "Failed to restore text, field might be unavailable")
                    showUndoFailureFeedback()
                }
                
                // Clear the stored data
                lastClearedTextData = null
            } else {
                Log.w(TAG, "No cleared text data available for undo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during undo operation", e)
        }
    }
    
    private fun restoreTextToField(originalText: String, sourceNode: AccessibilityNodeInfo?): Boolean {
        try {
            sourceNode?.let { node ->
                // Method 1: Direct text restoration
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                var success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                
                if (success) {
                    Log.d(TAG, "Direct text restoration successful")
                    return true
                }
                
                // Method 2: Try parent nodes
                var parent = node.parent
                var depth = 0
                while (parent != null && depth < 5) {
                    if (parent.isEditable) {
                        val parentArgs = Bundle()
                        parentArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                        success = parent.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, parentArgs)
                        
                        if (success) {
                            Log.d(TAG, "Parent text restoration successful (depth $depth)")
                            return true
                        }
                    }
                    parent = parent.parent
                    depth++
                }
                
                // Method 3: Enhanced window search for ChatGPT and other apps
                rootInActiveWindow?.let { root ->
                    success = findAndRestoreTextInWindowEnhanced(root, originalText, sourceNode)
                    if (success) {
                        Log.d(TAG, "Enhanced window text restoration successful")
                        return true
                    }
                }
                
                // Method 4: Try to find the field by package name and common patterns
                val packageName = sourceNode.packageName?.toString() ?: ""
                success = findFieldByPackagePattern(packageName, originalText)
                if (success) {
                    Log.d(TAG, "Package pattern text restoration successful")
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring text to field", e)
            return false
        }
    }
    
    private fun findAndRestoreTextInWindowEnhanced(rootNode: AccessibilityNodeInfo?, originalText: String, originalNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        
        try {
            // Method 1: Find empty editable fields
            if (rootNode.isEditable && rootNode.text.isNullOrBlank()) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                val success = rootNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                
                if (success) {
                    Log.d(TAG, "Found empty editable field and restored text")
                    return true
                }
            }
            
            // Method 2: Find fields with similar properties to original
            if (originalNode != null) {
                if (rootNode.isEditable && 
                    rootNode.className == originalNode.className &&
                    rootNode.viewIdResourceName == originalNode.viewIdResourceName) {
                    
                    val arguments = Bundle()
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                    val success = rootNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    
                    if (success) {
                        Log.d(TAG, "Found matching field and restored text")
                        return true
                    }
                }
            }
            
            // Method 3: Find focused editable fields
            if (rootNode.isEditable && rootNode.isFocused) {
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                val success = rootNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                
                if (success) {
                    Log.d(TAG, "Found focused editable field and restored text")
                    return true
                }
            }
            
            // Recursively check child nodes
            for (i in 0 until rootNode.childCount) {
                val success = findAndRestoreTextInWindowEnhanced(rootNode.getChild(i), originalText, originalNode)
                if (success) return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding and restoring text in window", e)
        }
        
        return false
    }
    
    private fun findFieldByPackagePattern(packageName: String, originalText: String): Boolean {
        try {
            rootInActiveWindow?.let { root ->
                return findFieldByPackagePatternRecursive(root, packageName, originalText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding field by package pattern", e)
        }
        return false
    }
    
    private fun findFieldByPackagePatternRecursive(node: AccessibilityNodeInfo?, packageName: String, originalText: String): Boolean {
        if (node == null) return false
        
        try {
            // Common patterns for different apps
            when (packageName) {
                "com.openai.chatgpt" -> {
                    // ChatGPT specific patterns
                    if (node.isEditable && (node.viewIdResourceName?.contains("input") == true || 
                                          node.viewIdResourceName?.contains("edit") == true ||
                                          node.className?.contains("EditText") == true)) {
                        val arguments = Bundle()
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                        
                        if (success) {
                            Log.d(TAG, "Found ChatGPT input field and restored text")
                            return true
                        }
                    }
                }
                "com.whatsapp" -> {
                    // WhatsApp specific patterns
                    if (node.isEditable && (node.viewIdResourceName?.contains("entry") == true ||
                                          node.className?.contains("EditText") == true)) {
                        val arguments = Bundle()
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                        
                        if (success) {
                            Log.d(TAG, "Found WhatsApp input field and restored text")
                            return true
                        }
                    }
                }
                "com.deepseek.chat" -> {
                    // DeepSeek specific patterns
                    if (node.isEditable && (node.viewIdResourceName?.contains("input") == true || 
                                          node.viewIdResourceName?.contains("edit") == true ||
                                          node.className?.contains("EditText") == true ||
                                          node.className?.contains("TextView") == true)) {
                        val arguments = Bundle()
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                        
                        if (success) {
                            Log.d(TAG, "Found DeepSeek input field and restored text")
                            return true
                        }
                    }
                }
                else -> {
                    // Generic pattern for other apps
                    if (node.isEditable && node.isFocused) {
                        val arguments = Bundle()
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, originalText)
                        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                        
                        if (success) {
                            Log.d(TAG, "Found focused editable field and restored text")
                            return true
                        }
                    }
                }
            }
            
            // Recursively check child nodes
            for (i in 0 until node.childCount) {
                val success = findFieldByPackagePatternRecursive(node.getChild(i), packageName, originalText)
                if (success) return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in package pattern search", e)
        }
        
        return false
    }
    
    private fun generateFieldId(node: AccessibilityNodeInfo?, packageName: String): String {
        try {
            val viewId = node?.viewIdResourceName ?: ""
            val className = node?.className?.toString() ?: ""
            
            // Create a unique identifier for the field
            // For DeepSeek, use a more specific identifier
            return when (packageName) {
                "com.deepseek.chat" -> {
                    // DeepSeek specific field ID
                    "${packageName}_${viewId}_${className}_deepseek"
                }
                else -> {
                    // Default field ID for other apps
                    "${packageName}_${viewId}_${className}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating field ID", e)
            return "${packageName}_unknown_${System.currentTimeMillis()}"
        }
    }
    
    private fun isFieldBlocked(node: AccessibilityNodeInfo?, packageName: String): Boolean {
        val fieldId = generateFieldId(node, packageName)
        
        Log.d(TAG, "🔍 Checking if field $fieldId is blocked in $packageName")
        
        // Check if field has undo flag set (true = blocked from detection)
        if (fieldUndoFlags[fieldId] == true) {
            Log.d(TAG, "🚫 Field $fieldId has undo flag set, checking if empty")
            // Check if the field is now empty and reset flag if needed
            if (isFieldActuallyEmpty(node, packageName)) {
                fieldUndoFlags[fieldId] = false
                Log.d(TAG, "✅ Field $fieldId is now empty, resetting undo flag immediately")
                return false
            }
            
            Log.d(TAG, "🚫 Field $fieldId blocked due to undo flag")
            return true
        }
        
        Log.d(TAG, "✅ Field $fieldId not blocked")
        return false
    }
    
    // Removed old blockedFields system - now using fieldUndoFlags only
    
    private fun checkAndResetUndoFlagsImmediately(node: AccessibilityNodeInfo?, packageName: String) {
        try {
            Log.d(TAG, "🔄 Checking for flag reset in $packageName - Active flags: ${fieldUndoFlags.filter { it.value }.keys}")
            
            val flagsToReset = mutableSetOf<String>()
            
            fieldUndoFlags.forEach { (fieldId, isBlocked) ->
                if (isBlocked) {
                    // Check if this field matches the current package
                    val parts = fieldId.split("_", limit = 3)
                    if (parts.size >= 3 && parts[0] == packageName) {
                        Log.d(TAG, "🔍 Checking field $fieldId for emptiness")
                        // Check if the current field is empty
                        if (isFieldActuallyEmpty(node, packageName)) {
                            flagsToReset.add(fieldId)
                            Log.d(TAG, "✅ Field $fieldId is now empty, resetting undo flag immediately")
                        } else {
                            Log.d(TAG, "❌ Field $fieldId is not empty yet")
                        }
                    }
                }
            }
            
            // Reset flags for empty fields
            flagsToReset.forEach { fieldId ->
                fieldUndoFlags[fieldId] = false
                Log.d(TAG, "🔄 Flag reset: $fieldId = false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking and resetting undo flags immediately", e)
        }
    }
    
    private fun checkAndResetUndoFlags() {
        try {
            val flagsToReset = mutableSetOf<String>()
            
            fieldUndoFlags.forEach { (fieldId, isBlocked) ->
                if (isBlocked) {
                    // Check if the field is now empty
                    val parts = fieldId.split("_", limit = 3)
                    if (parts.size >= 3) {
                        val packageName = parts[0]
                        val viewId = parts[1]
                        val className = parts[2]
                        
                        // Try to find the field and check if it's empty
                        rootInActiveWindow?.let { root ->
                            val isEmpty = checkFieldIsEmpty(root, viewId, className)
                            if (isEmpty) {
                                flagsToReset.add(fieldId)
                                Log.d(TAG, "Field $fieldId is now empty, resetting undo flag")
                            }
                        }
                    }
                }
            }
            
            // Reset flags for empty fields
            flagsToReset.forEach { fieldId ->
                fieldUndoFlags[fieldId] = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking and resetting undo flags", e)
        }
    }
    
    private fun isFieldActuallyEmpty(node: AccessibilityNodeInfo?, packageName: String): Boolean {
        if (node == null) return false
        
        try {
            // Check if this node is editable and has text
            if (node.isEditable) {
                val text = node.text?.toString() ?: ""
                
                // Enhanced emptiness check for different apps
                val isEmpty = when (packageName) {
                    "com.whatsapp" -> {
                        // WhatsApp shows "Message..." by default, so we need to check if it's just the placeholder
                        text.isBlank() || text.trim() == "Message..." || text.trim() == "Message" || 
                        text.trim().isEmpty() || text.trim().length <= 10 // Consider short text as empty
                    }
                    "com.instagram.android" -> {
                        // Instagram shows "Message..." by default
                        text.isBlank() || text.trim() == "Message..." || text.trim() == "Message" || 
                        text.trim().isEmpty() || text.trim().length <= 10
                    }
                    "com.google.android.apps.messaging" -> {
                        // Google Messages shows "Text message" when empty
                        text.isBlank() || text.trim() == "Text message" || text.trim() == "Message" || 
                        text.trim().isEmpty() || text.trim().length <= 15
                    }
                    "com.openai.chatgpt" -> {
                        // ChatGPT - consider short text as empty (user likely cleared it)
                        text.isBlank() || text.trim().isEmpty() || text.trim().length <= 5
                    }
                    "com.deepseek.chat" -> {
                        // DeepSeek - consider short text as empty and check for placeholder
                        text.isBlank() || text.trim().isEmpty() || text.trim().length <= 5 || 
                        text.trim() == "Message DeepSeek..." || text.trim() == "Message DeepSeek"
                    }
                    else -> {
                        // Default check for other apps
                        text.isBlank() || text.trim().isEmpty()
                    }
                }
                
                Log.d(TAG, "🔍 Field emptiness check for $packageName: '${text.take(30)}...' (isEmpty: $isEmpty)")
                return isEmpty
            }
            
            // If not editable, check if it's a container with editable children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null && isFieldActuallyEmpty(child, packageName)) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking field actual emptiness", e)
        }
        
        return false
    }
    
    private fun checkFieldIsEmpty(rootNode: AccessibilityNodeInfo?, viewId: String, className: String): Boolean {
        if (rootNode == null) return false
        
        try {
            if (rootNode.viewIdResourceName == viewId && rootNode.className?.toString() == className) {
                val text = rootNode.text?.toString() ?: ""
                
                // Get package name from the root node
                val packageName = rootNode.packageName?.toString() ?: ""
                
                // Enhanced emptiness check for different apps
                return when (packageName) {
                    "com.whatsapp" -> {
                        // WhatsApp shows "Message..." when empty
                        text.isBlank() || text.trim() == "Message..." || text.trim() == "Message"
                    }
                    "com.instagram.android" -> {
                        // Instagram shows "Message..." when empty
                        text.isBlank() || text.trim() == "Message..." || text.trim() == "Message"
                    }
                    "com.google.android.apps.messaging" -> {
                        // Google Messages shows "Text message" when empty
                        text.isBlank() || text.trim() == "Text message" || text.trim() == "Message"
                    }
                    "com.openai.chatgpt" -> {
                        // ChatGPT - consider short text as empty (user likely cleared it)
                        text.isBlank() || text.trim().isEmpty() || text.trim().length <= 5
                    }
                    "com.deepseek.chat" -> {
                        // DeepSeek - consider short text as empty and check for placeholder
                        text.isBlank() || text.trim().isEmpty() || text.trim().length <= 5 || 
                        text.trim() == "Message DeepSeek..." || text.trim() == "Message DeepSeek"
                    }
                    else -> {
                        // Default check for other apps
                        text.isBlank()
                    }
                }
            }
            
            // Recursively check child nodes
            for (i in 0 until rootNode.childCount) {
                val isEmpty = checkFieldIsEmpty(rootNode.getChild(i), viewId, className)
                if (isEmpty) return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking field emptiness", e)
        }
        
        return false
    }
    
    private fun showUndoSuccessFeedback() {
        // Show a brief success message
        serviceScope.launch(Dispatchers.Main) {
            try {
                val toast = Toast.makeText(this@RealTimeDetectionService, "Text restored successfully (field protected until cleared)", Toast.LENGTH_LONG)
                toast.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing undo success feedback", e)
            }
        }
    }
    
    private fun showUndoFailureFeedback() {
        // Show a brief failure message
        serviceScope.launch(Dispatchers.Main) {
            try {
                val toast = Toast.makeText(this@RealTimeDetectionService, "Could not restore text - field unavailable", Toast.LENGTH_SHORT)
                toast.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing undo failure feedback", e)
            }
        }
    }
    
    private fun showManualResetFeedback() {
        // Show a brief success message for manual reset
        serviceScope.launch(Dispatchers.Main) {
            try {
                val toast = Toast.makeText(this@RealTimeDetectionService, "Field protection reset successfully", Toast.LENGTH_SHORT)
                toast.show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing manual reset feedback", e)
            }
        }
    }
}
