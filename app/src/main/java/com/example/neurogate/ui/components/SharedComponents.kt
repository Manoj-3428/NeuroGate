package com.example.neurogate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neurogate.data.DetectedActivity

@Composable
fun ActivityCard(activity: DetectedActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                CategoryChip(category = activity.category)
            }
            
            // Content
            Text(
                text = activity.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            
            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.getFormattedTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(activity.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activity.confidence > 0.8) Color.Red else Color(0xFF2196F3)
                    )
                    
                    if (activity.isCleared) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Cleared",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Green
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(category: String) {
    val (backgroundColor, textColor) = when (category) {
        "PERSONAL_DATA" -> Color(0xFFFF5722) to Color.White
        "HACKING" -> Color(0xFFE91E63) to Color.White
        "IMAGE_VIDEO_MISUSE" -> Color(0xFF9C27B0) to Color.White
        "EXPLOSIVES" -> Color(0xFFF44336) to Color.White
        "DRUGS" -> Color(0xFFFF9800) to Color.White
        else -> Color(0xFF607D8B) to Color.White
    }
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = category.replace("_", " "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
