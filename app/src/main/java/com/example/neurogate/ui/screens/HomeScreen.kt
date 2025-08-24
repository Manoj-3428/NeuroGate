package com.example.neurogate.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History

import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.neurogate.data.DetectedActivity
import com.example.neurogate.data.ActivityStorage
import java.util.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// Data structure for weekly data
data class WeeklyData(
    val date: LocalDate,
    val flags: Int,
    val activities: List<DetectedActivity>
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    activityStorage: ActivityStorage,
    onNavigateToActivityHistory: () -> Unit
) {
    val activities by activityStorage.getAllActivities().collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        TopAppBar(
            title = { 
                Text(
                    "NeuroGate", 
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ) 
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2196F3),
                navigationIconContentColor = Color.Black,
                actionIconContentColor = Color.Black
            )
        )
        
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                StatusCard(activityStorage = activityStorage)
            }
            
            // Today's Activity Distribution
            item {
                Text(
                    text = "Today's Activity Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
            
            // Pie Chart
            item {
                PieChartCard(activities = activities)
            }
            
            // View All Button
            item {
                Button(
                    onClick = onNavigateToActivityHistory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3) // Blue
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View All Activities")
                }
            }
        }
    }
}

@Composable
fun StatusCard(activityStorage: ActivityStorage) {
    val activities by activityStorage.getAllActivities().collectAsState(initial = emptyList())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Protection Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Active - Monitoring all apps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2196F3) // Blue
                    )
                }
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Protection Active",
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF2196F3) // Blue
                )
            }
            
            HorizontalDivider(color = Color(0xFFE0E0E0))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Total Detections",
                    value = activities.size.toString(),
                    icon = Icons.Default.Warning
                )
                StatItem(
                    label = "Today",
                    value = activities.count { 
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        it.timestamp >= today
                    }.toString(),
                    icon = Icons.Default.Today
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF2196F3) // Blue
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PieChartCard(activities: List<DetectedActivity>) {
    var showBarChart by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    
    val todayActivities = activities.filter { 
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        it.timestamp >= today
    }
    
    // Generate real data for past 7 days from local storage
    val weeklyData = remember(activities) {
            (0..6).map { dayOffset ->
                val date = LocalDate.now().minusDays(6L - dayOffset)
                val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                
                val dayActivities = activities.filter { activity ->
                    activity.timestamp >= startOfDay && activity.timestamp < endOfDay
                }
                
                WeeklyData(
                    date = date,
                    flags = dayActivities.size,
                    activities = dayActivities
                )
            }
        }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = "Activity Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (todayActivities.isEmpty()) {
                EmptyStateCard()
            } else {
                if (!showBarChart) {
                    // Pie Chart View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showBarChart = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedPieChart(
                            activities = todayActivities,
                            modifier = Modifier.size(180.dp)
                        )
                        
                        // Click hint
                        Text(
                            text = "Click to view weekly data",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                    
                    // Legend
                    CategoryLegend(activities = todayActivities)
                } else {
                    // Bar Chart View
                    WeeklyBarChartView(
                        weeklyData = weeklyData,
                        selectedDay = selectedDay,
                        onDaySelected = { dayIndex ->
                            selectedDay = if (selectedDay == dayIndex) null else dayIndex
                        },
                        onBackToPieChart = {
                            showBarChart = false
                            selectedDay = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedPieChart(
    activities: List<DetectedActivity>,
    modifier: Modifier = Modifier
) {
    // Trigger animations when the composable is first launched
    LaunchedEffect(Unit) {
        // This ensures animations start immediately
    }
    val categoryCounts = activities.groupBy { it.category }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
    
    val total = categoryCounts.sumOf { it.second }
    
    if (total == 0) return
    
    // Enhanced animations for better visual appeal
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1800, easing = EaseOutCubic),
        label = "pie_chart_animation"
    )
    
    // Scale animation with bounce effect
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )
    
    // Rotation animation for dynamic movement - continuous 360 degrees
    val rotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )
    
    // Pulse animation for the chart
    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_animation"
    )
    
    // Entrance animation - starts from 0 and scales up with rotation
    val entranceScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, delayMillis = 200, easing = EaseOutCubic),
        label = "entrance_scale"
    )
    
    val entranceRotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(1200, delayMillis = 200, easing = EaseOutCubic),
        label = "entrance_rotation"
    )
    
    Box(
        modifier = modifier
            .scale(entranceScale * scale * (0.95f + pulse * 0.05f))
            .graphicsLayer(
                rotationZ = entranceRotation + rotation,
                alpha = entranceScale * (0.9f + pulse * 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 * 0.9f // Slightly larger without center circle
            

            
            var currentAngle = 0f
            
            // Draw all pie chart segments with enhanced colors
            categoryCounts.forEach { (category, count) ->
                val sweepAngle = (count.toFloat() / total) * 360f * animatedProgress
                var color = getCategoryColor(category)
                val percentage = (count.toFloat() / total * 100).toInt()
                
                // Add gradient effect to colors with bounce
                val enhancedColor = color.copy(alpha = (0.95f + pulse * 0.05f) * entranceScale)
                
                // Draw the pie segment with shadow effect
                drawArc(
                    color = enhancedColor,
                    startAngle = currentAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2)
                )
                
                // Draw percentage text directly on the segment (like sample image)
                if (percentage > 5) {
                    val textAngle = currentAngle + (sweepAngle / 2)
                    val textRadius = radius * 0.5f
                    val textX = center.x + (textRadius * kotlin.math.cos(Math.toRadians(textAngle.toDouble()))).toFloat()
                    val textY = center.y + (textRadius * kotlin.math.sin(Math.toRadians(textAngle.toDouble()))).toFloat()
                    
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = Color.White
                            textAlign = Paint.Align.CENTER
                            textSize = 24f * animatedProgress
                            isFakeBoldText = true
                        }
                        canvas.nativeCanvas.drawText(
                            "${percentage}%",
                            textX,
                            textY + 12f,
                            paint
                        )
                    }
                }
                

                
                currentAngle += sweepAngle
            }
        }
        

    }
}

@Composable
fun CategoryLegend(activities: List<DetectedActivity>) {
    val categoryCounts = activities.groupBy { it.category }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
    
    val total = categoryCounts.sumOf { it.second }
    
    // Animation for legend appearance
    val legendAnimation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, delayMillis = 300, easing = EaseOutCubic),
        label = "legend_animation"
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.graphicsLayer(
            alpha = legendAnimation,
            scaleX = 0.8f + legendAnimation * 0.2f,
            scaleY = 0.8f + legendAnimation * 0.2f
        )
    ) {
        Text(
            text = "Category Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        categoryCounts.forEachIndexed { index, (category, count) ->
            val percentage = if (total > 0) (count.toFloat() / total * 100) else 0f
            val delay = index * 100
            
            LegendItem(
                category = category,
                count = count,
                percentage = percentage,
                color = getCategoryColor(category),
                delay = delay
            )
        }
    }
}

@Composable
fun LegendItem(
    category: String,
    count: Int,
    percentage: Float,
    color: Color,
    delay: Int = 0
) {
    // Enhanced animation for legend items
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, delayMillis = delay, easing = EaseOutCubic),
        label = "legend_animation"
    )
    
    val pulseAnimation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_legend"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = animatedAlpha,
                scaleX = 0.9f + animatedAlpha * 0.1f,
                scaleY = 0.9f + animatedAlpha * 0.1f
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.8f + pulseAnimation * 0.2f))
                    .graphicsLayer(
                        scaleX = 0.8f + pulseAnimation * 0.2f,
                        scaleY = 0.8f + pulseAnimation * 0.2f
                    )
            )
            Text(
                text = getCategoryDisplayName(category),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.8f + pulseAnimation * 0.2f),
                modifier = Modifier.graphicsLayer(
                    scaleX = 0.9f + pulseAnimation * 0.1f,
                    scaleY = 0.9f + pulseAnimation * 0.1f
                )
            )
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray.copy(alpha = 0.6f + pulseAnimation * 0.2f)
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    // Animation for empty state
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = EaseOutBack),
        label = "empty_state_animation"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF2196F3) // Blue
        )
        Text(
            text = "No Detections Today",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        Text(
            text = "Great job! No flagged activities detected today.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "PERSONAL_DATA" -> Color(0xFF4FC3F7) // Vibrant Blue (like the sample)
        "HACKING" -> Color(0xFFFF9800) // Orange (like the sample)
        "IMAGE_VIDEO_MISUSE" -> Color(0xFF4CAF50) // Green (like the sample)
        "EXPLOSIVES" -> Color(0xFFF44336) // Red (like the sample)
        "DRUGS" -> Color(0xFF9C27B0) // Purple
        "DEEPFAKE" -> Color(0xFF2196F3) // Blue
        "CELEBRITY_IMPERSONATION" -> Color(0xFFE91E63) // Pink
        "HARMFUL_CONTENT" -> Color(0xFFFF5722) // Deep Orange
        "COPYRIGHT_VIOLATION" -> Color(0xFF673AB7) // Deep Purple
        "PRIVACY_VIOLATION" -> Color(0xFF00BCD4) // Cyan
        else -> Color(0xFF607D8B) // Blue Grey
    }
}

fun getCategoryDisplayName(category: String): String {
    return when (category) {
        "PERSONAL_DATA" -> "Personal Data"
        "HACKING" -> "Hacking"
        "IMAGE_VIDEO_MISUSE" -> "Image/Video Misuse"
        "EXPLOSIVES" -> "Explosives"
        "DRUGS" -> "Drugs"
        else -> category.replace("_", " ")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeeklyBarChartView(
    weeklyData: List<WeeklyData>,
    selectedDay: Int?,
    onDaySelected: (Int) -> Unit,
    onBackToPieChart: () -> Unit
) {
    Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToPieChart) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to pie chart",
                    tint = Color.Black
                )
            }
            Text(
                text = "Weekly Activity Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balance the layout
        }
        
        // Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activities Detected (Past 7 Days)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Total: ${weeklyData.sumOf { it.flags }} activities",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                // Simple Bar Chart
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyData.forEachIndexed { index, data ->
                        val isSelected = selectedDay == index
                        val barHeight = if (data.flags > 0) {
                            120.dp
                        } else 20.dp
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Count on top
                            Text(
                                text = "${data.flags}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color(0xFF2196F3) else Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                            
                            // Bar
                            val animatedScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1f,
                                animationSpec = tween(200, easing = EaseOutCubic),
                                label = "bar_scale_$index"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(barHeight)
                                    .background(
                                        color = if (isSelected) Color(0xFF2196F3) else Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                                    .clickable { onDaySelected(index) }
                                    .scale(animatedScale)
                                    .graphicsLayer(
                                        shadowElevation = if (isSelected) 8f else 2f,
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                        clip = true
                                    )
                            )
                            
                            // Day name
                            Text(
                                text = data.date.format(DateTimeFormatter.ofPattern("EEE")),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color(0xFF2196F3) else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
                }
            }
        }
        
        // Selected Day Details with Animation
        AnimatedVisibility(
            visible = selectedDay != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = EaseOutCubic)
            ) + fadeIn(
                animationSpec = tween(200)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(250, easing = EaseInCubic)
            ) + fadeOut(
                animationSpec = tween(150)
            )
        ) {
            selectedDay?.let { dayIndex ->
                val selectedData = weeklyData[dayIndex]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Flagged Activities - ${selectedData.date.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        
                        if (selectedData.activities.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No activities detected on this day",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = selectedData.activities,
                                    key = { it.id }
                                ) { activity ->
                                    FlaggedActivityItem(activity = activity)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlaggedActivityItem(activity: DetectedActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = getCategoryColor(activity.category),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activity.category.first().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Column {
                    Text(
                        text = getCategoryDisplayName(activity.category),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = activity.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            // Confidence indicator
            Text(
                text = "${(activity.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }
    }
}
