package com.example.neurogate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.neurogate.data.DetectedActivity
import com.example.neurogate.utils.AppNameUtils
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ActivityCard(activity: DetectedActivity) {
    val context = LocalContext.current
    val appName = AppNameUtils.getAppName(context, activity.packageName)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category at the top
            CategoryChip(category = activity.category)
            
            // App name
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            // Flagged data content
            Text(
                text = activity.text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // Footer with timestamp and confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(activity.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                // Confidence percentage
                Surface(
                    color = if (activity.confidence > 0.8) Color(0xFFFFEBEE) else Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(activity.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activity.confidence > 0.8) Color(0xFFD32F2F) else Color(0xFF1976D2)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: java.util.Date): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(timestamp)
}

@Composable
fun CategoryChip(category: String) {
    val backgroundColor = when (category) {
        "PERSONAL_DATA" -> Color(0xFF4FC3F7) // Vibrant Blue
        "HACKING" -> Color(0xFFFF9800) // Orange
        "IMAGE_VIDEO_MISUSE" -> Color(0xFF4CAF50) // Green
        "EXPLOSIVES" -> Color(0xFFF44336) // Red
        "DRUGS" -> Color(0xFF9C27B0) // Purple
        "DEEPFAKE" -> Color(0xFF4CAF50) // Green
        "CELEBRITY_IMPERSONATION" -> Color(0xFFE91E63) // Pink
        "HARMFUL_CONTENT" -> Color(0xFFFF5722) // Deep Orange
        "COPYRIGHT_VIOLATION" -> Color(0xFF673AB7) // Deep Purple
        "PRIVACY_VIOLATION" -> Color(0xFF00BCD4) // Cyan
        else -> Color(0xFF607D8B) // Blue Grey
    }
    val textColor = Color.White
    
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = getCategoryDisplayName(category),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getCategoryDisplayName(category: String): String {
    return when (category) {
        "PERSONAL_DATA" -> "Personal Data"
        "HACKING" -> "Hacking"
        "IMAGE_VIDEO_MISUSE" -> "Image/Video Misuse"
        "EXPLOSIVES" -> "Explosives"
        "DRUGS" -> "Drugs"
        "DEEPFAKE" -> "Deepfake"
        "CELEBRITY_IMPERSONATION" -> "Celebrity Impersonation"
        "HARMFUL_CONTENT" -> "Harmful Content"
        "COPYRIGHT_VIOLATION" -> "Copyright Violation"
        "PRIVACY_VIOLATION" -> "Privacy Violation"
        else -> category.replace("_", " ")
    }
}
