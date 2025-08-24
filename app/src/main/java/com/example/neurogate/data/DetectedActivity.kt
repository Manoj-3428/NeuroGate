package com.example.neurogate.data

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class DetectedActivity(
    val id: String = UUID.randomUUID().toString(),
    val content: String,                    // The flagged sentence/text
    val category: String,                   // Misuse category (PERSONAL_DATA, HACKING, etc.)
    val appPackage: String,                 // Package name (e.g., com.whatsapp)
    val appName: String,                    // App name (e.g., WhatsApp, Chrome)
    val timestamp: Long,                    // When it was detected (millis)
    val confidence: Double,                 // Detection confidence score
    val isCleared: Boolean = false          // Whether input was successfully cleared
) {
    fun getFormattedTime(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    fun getFormattedCategory(): String {
        return category.replace("_", " ")
    }
}