package com.example.neurogate.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neurogate.data.DetectionResult
import com.example.neurogate.data.MisuseCategory

@Composable
fun FloatingAlert(
    detectionResult: DetectionResult?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = detectionResult != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        detectionResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = getAlertColor(result.category)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = getAlertTitle(result.category),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = result.reason,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                        
                        if (result.suggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Suggestion: ${result.suggestions.first()}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getAlertColor(category: MisuseCategory): Color {
    return when (category) {
        MisuseCategory.DEEPFAKE -> Color(0xFFE74C3C) // Red
        MisuseCategory.CELEBRITY_IMPERSONATION -> Color(0xFFE67E22) // Orange
        MisuseCategory.HARMFUL_CONTENT -> Color(0xFFC0392B) // Dark Red
        MisuseCategory.COPYRIGHT_VIOLATION -> Color(0xFF8E44AD) // Purple
        MisuseCategory.PRIVACY_VIOLATION -> Color(0xFF2980B9) // Blue
        MisuseCategory.NONE -> Color(0xFF27AE60) // Green
    }
}

@Composable
private fun getAlertTitle(category: MisuseCategory): String {
    return when (category) {
        MisuseCategory.DEEPFAKE -> "Deepfake Detection"
        MisuseCategory.CELEBRITY_IMPERSONATION -> "Celebrity Impersonation"
        MisuseCategory.HARMFUL_CONTENT -> "Harmful Content"
        MisuseCategory.COPYRIGHT_VIOLATION -> "Copyright Violation"
        MisuseCategory.PRIVACY_VIOLATION -> "Privacy Violation"
        MisuseCategory.NONE -> "No Issues Detected"
    }
}
