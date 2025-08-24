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
import com.example.neurogate.data.ActivityStorage
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
    private lateinit var activityStorage: ActivityStorage
    
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
    private var detectionCooldown = 1000L // 1 second cooldown
    private var currentInputText = ""
    private var lastInputTime = 0L
    private var isProcessingDetection = false
    
    companion object {
        private const val TAG = "RealTimeDetection"
        
        // Detection categories with colors
        val categoryColors = mapOf(
            MisuseCategory.PRIVACY_VIOLATION to "#FF6B6B".toColorInt(), // Red
            MisuseCategory.HARMFUL_CONTENT to "#FF8E53".toColorInt(), // Orange
            MisuseCategory.DEEPFAKE to "#4ECDC4".toColorInt(), // Teal
            MisuseCategory.CELEBRITY_IMPERSONATION to "#45B7D1".toColorInt(), // Blue
            MisuseCategory.NONE to "#96CEB4".toColorInt() // Green
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
        
        // Initialize activity storage
        activityStorage = ActivityStorage.getInstance(this)
         
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
        val currentTime = System.currentTimeMillis()
        
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
         val currentTime = System.currentTimeMillis()
         
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
         lastInputTime = currentTime
         
         Log.d(TAG, "Text change in $packageName: '${text.take(30)}...' (length: ${text.length})")
         
         // More aggressive detection for web browsers
         val minLength = if (isWebBrowser(packageName)) 3 else 2
         
         // Check if text is long enough and looks like user input (not a URL/domain)
         if (text.length >= minLength && !text.contains("/") && !text.contains(".") && 
             !text.matches(Regex("^[a-zA-Z0-9.-]+$"))) {
             lastDetectedText = text
             isProcessingDetection = true
             
             serviceScope.launch {
                 try {
                     analyzeTextInRealTime(text, event.source, packageName)
                 } finally {
                     isProcessingDetection = false
                 }
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
            lastDetectedText = selectedText
            serviceScope.launch {
                analyzeTextInRealTime(selectedText, nodeInfo, packageName)
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
                serviceScope.launch {
                    analyzeTextInRealTime(text, nodeInfo, packageName)
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
                serviceScope.launch {
                    analyzeTextInRealTime(text, nodeInfo, packageName)
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
                 serviceScope.launch {
                     analyzeTextInRealTime(text, event.source, packageName)
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
            serviceScope.launch {
                analyzeTextInRealTime(text, event.source, packageName)
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
            
            val analysis = languageModelService.analyzePromptWithLLM(text)
            
            if (analysis.isMisuse && analysis.confidence > 0.75) {
                Log.w(TAG, "🚨 DETECTED MISUSE in $packageName: ${analysis.category} (confidence: ${analysis.confidence})")
                
                // Store detected activity in background
                storeDetectedActivity(text, analysis.category.name, packageName, analysis.confidence)
                
                // Show sliding notification
                showSlidingNotification(analysis)
                
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
    
         private fun showSlidingNotification(analysis: PromptAnalysisResponse) {
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
                 
                 createNotificationView(analysis)
                 animateNotificationIn()
             } catch (e: Exception) {
                 Log.e(TAG, "Error showing notification", e)
                 // Reset state on error
                 isNotificationShowing = false
                 notificationView = null
             }
         }
     }
    
         private fun createNotificationView(analysis: PromptAnalysisResponse) {
         val inflater = LayoutInflater.from(this)
         notificationView = inflater.inflate(R.layout.sliding_notification, null)
         
         // Set up notification content
         val categoryIcon = notificationView?.findViewById<TextView>(R.id.categoryIcon)
         val categoryTitle = notificationView?.findViewById<TextView>(R.id.categoryTitle)
         val categoryDescription = notificationView?.findViewById<TextView>(R.id.categoryDescription)
         val suggestionText = notificationView?.findViewById<TextView>(R.id.suggestionText)
         
         // Set category-specific content
         val category = analysis.category
         val color = categoryColors[category] ?: Color.GRAY
         val icon = categoryIcons[category] ?: "⚠️"
         
         categoryIcon?.text = icon
         categoryTitle?.text = getCategoryTitle(category)
         categoryDescription?.text = analysis.reason
         suggestionText?.text = analysis.suggestions.firstOrNull() ?: "Please review your content"
         
         // Set up close button functionality
         val closeButton = notificationView?.findViewById<ImageButton>(R.id.closeButton)
         closeButton?.setOnClickListener {
             Log.d(TAG, "Close button clicked")
             progressAnimator?.cancel()
             animateNotificationOut()
         }
         
         // Set background color with modern card design
         val background = notificationView?.findViewById<View>(R.id.notificationBackground)
         background?.background = createModernCardBackground(color)
         
         // Set up window parameters for center positioning
         val params = WindowManager.LayoutParams().apply {
             type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
             } else {
                 WindowManager.LayoutParams.TYPE_PHONE
             }
             flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                     WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
             format = PixelFormat.TRANSLUCENT
             width = WindowManager.LayoutParams.WRAP_CONTENT
             height = WindowManager.LayoutParams.WRAP_CONTENT
             gravity = Gravity.CENTER
             x = 0
             y = 0
         }
         
         // Add view to window
         windowManager?.addView(notificationView, params)
         isNotificationShowing = true
         lastNotificationTime = System.currentTimeMillis()
         
         // Auto-hide after exactly 3 seconds with progress animation
         serviceScope.launch {
             val progressBar = notificationView?.findViewById<ProgressBar>(R.id.autoHideProgress)
             
             // Animate progress bar from 100 to 0 over 3 seconds
             progressAnimator = ValueAnimator.ofInt(100, 0)
             progressAnimator?.duration = 3000 // Exactly 3 seconds
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
                // Get app name from package manager
                val appName = try {
                    val packageManager = packageManager
                    val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(applicationInfo).toString()
                } catch (e: Exception) {
                    // Fallback to package name if app name not found
                    packageName
                }
                
                // Create detected activity object
                val detectedActivity = DetectedActivity(
                    content = content,
                    category = category,
                    appPackage = packageName,
                    appName = appName,
                    timestamp = System.currentTimeMillis(),
                    confidence = confidence,
                    isCleared = false // Will be updated if clearing is successful
                )
                
                // Store in activity storage
                activityStorage.insertActivity(detectedActivity)
                
                Log.d(TAG, "📝 Stored detected activity: $category in $appName - '${content.take(30)}...'")
                
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
}
