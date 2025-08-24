package com.example.neurogate.ui.screens

import EmptyStateCard
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.example.neurogate.data.*
import com.example.neurogate.ui.AIDetectionViewModel
import com.example.neurogate.ui.components.CustomIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AIDetectionViewModel,
    onNavigateBack: () -> Unit
) {
    val flaggedInteractions by viewModel.flaggedInteractions.collectAsState(initial = emptyList())
    
    val statistics = remember(flaggedInteractions) {
        Statistics(
            totalFlagged = flaggedInteractions.size,
            deepfakes = flaggedInteractions.count { it.detectionResult.category == MisuseCategory.DEEPFAKE },
            harmful = flaggedInteractions.count { it.detectionResult.category == MisuseCategory.HARMFUL_CONTENT }
        )
    }
    
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Flagged Interactions",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                },

                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearFlaggedInteractions() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3), // Blue
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item(key = "statistics_header") {
                StatisticsHeader(statistics = statistics)
            }
            
            if (flaggedInteractions.isEmpty()) {
                item(key = "empty_state") {
                    EmptyStateCard()
                }
            } else {
                items(
                    items = flaggedInteractions,
                    key = { interaction -> interaction.id }
                ) { interaction ->
                    FlaggedInteractionCard(
                        interaction = interaction,
                        onUpdateAction = { action ->
                            viewModel.updateUserAction(interaction.id, action)
                        }
                    )
                }
            }
        }
    }
}

// Statistics data class
private data class Statistics(
    val totalFlagged: Int,
    val deepfakes: Int,
    val harmful: Int
)

@Composable
private fun StatisticsHeader(statistics: Statistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Detection Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3) // Blue
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "Total Flagged",
                    value = statistics.totalFlagged.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFF2196F3) // Blue
                )
                
                StatCard(
                    title = "Deepfakes",
                    value = statistics.deepfakes.toString(),
                    icon = Icons.Default.Person,
                    color = Color(0xFF2196F3) // Blue
                )
                
                StatCard(
                    title = "Harmful",
                    value = statistics.harmful.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFF2196F3) // Blue
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(32.dp),
            tint = color
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFF2196F3).copy(alpha = 0.7f) // Blue with transparency
        )
    }
}



@Composable
private fun FlaggedInteractionCard(
    interaction: FlaggedInteraction,
    onUpdateAction: (UserAction) -> Unit
) {
    val categoryIcon = getCategoryIcon(interaction.detectionResult.category)
    val categoryColor = getCategoryColor(interaction.detectionResult.category)
    val categoryTitle = getCategoryTitle(interaction.detectionResult.category)
    val formattedTimestamp = formatTimestamp(interaction.timestamp)
    val userActionText = getUserActionText(interaction.userAction)
    
    // Get user action color within Composable context
    val userActionColor = when (interaction.userAction) {
        UserAction.NONE -> Color(0xFFF44336) // Red for pending
        else -> Color(0xFF2196F3) // Blue for others
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 1. Category Name - Horizontal Row at the top
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = "Category",
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = categoryTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )
                
                // Status Tag
                Surface(
                    color = userActionColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = userActionText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 2. Content Column - Where we detect and what we detected
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp) // Indent to align with category text
            ) {
                // Where we are detecting
                Text(
                    text = "Where we are detecting:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3).copy(alpha = 0.7f) // Blue with transparency
                )
                
                Text(
                    text = getAppName(interaction.detectionResult.prompt),
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // What we detected
                Text(
                    text = "What we detected:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3).copy(alpha = 0.7f) // Blue with transparency
                )
                
                Text(
                    text = interaction.prompt,
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Timestamp
                Text(
                    text = "Detected on:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3).copy(alpha = 0.7f) // Blue with transparency
                )
                
                Text(
                    text = formattedTimestamp,
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                // Additional Details (Reason)
                if (interaction.detectionResult.reason.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Reason:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2196F3).copy(alpha = 0.7f) // Blue with transparency
                    )
                    
                    Text(
                        text = interaction.detectionResult.reason,
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            // Action Buttons for pending interactions
            if (interaction.userAction == UserAction.NONE) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onUpdateAction(UserAction.ACKNOWLEDGED) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3) // Blue
                        )
                    ) {
                        Text("Acknowledge", fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    OutlinedButton(
                        onClick = { onUpdateAction(UserAction.MODIFIED_PROMPT) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3) // Blue
                        )
                    ) {
                        Text("Modified", fontSize = 12.sp)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    OutlinedButton(
                        onClick = { onUpdateAction(UserAction.PROCEEDED_ANYWAY) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3) // Blue
                        )
                    ) {
                        Text("Proceeded", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// Helper functions
private fun getCategoryIcon(category: MisuseCategory): ImageVector {
    return when (category) {
        MisuseCategory.DEEPFAKE -> Icons.Default.Person
        MisuseCategory.CELEBRITY_IMPERSONATION -> Icons.Default.Star
        MisuseCategory.HARMFUL_CONTENT -> Icons.Default.Warning
        MisuseCategory.COPYRIGHT_VIOLATION -> Icons.Default.Warning
        MisuseCategory.PRIVACY_VIOLATION -> CustomIcons.Security
        MisuseCategory.NONE -> Icons.Default.CheckCircle
    }
}

private fun getCategoryColor(category: MisuseCategory): Color {
    return when (category) {
        MisuseCategory.DEEPFAKE -> Color(0xFFE74C3C)
        MisuseCategory.CELEBRITY_IMPERSONATION -> Color(0xFFE67E22)
        MisuseCategory.HARMFUL_CONTENT -> Color(0xFFC0392B)
        MisuseCategory.COPYRIGHT_VIOLATION -> Color(0xFF8E44AD)
        MisuseCategory.PRIVACY_VIOLATION -> Color(0xFF2980B9)
        MisuseCategory.NONE -> Color(0xFF27AE60)
    }
}

private fun getCategoryTitle(category: MisuseCategory): String {
    return when (category) {
        MisuseCategory.DEEPFAKE -> "Deepfake"
        MisuseCategory.CELEBRITY_IMPERSONATION -> "Celebrity Impersonation"
        MisuseCategory.HARMFUL_CONTENT -> "Harmful Content"
        MisuseCategory.COPYRIGHT_VIOLATION -> "Copyright"
        MisuseCategory.PRIVACY_VIOLATION -> "Privacy Violation"
        MisuseCategory.NONE -> "No Issues"
    }
}

private fun getUserActionText(userAction: UserAction): String {
    return when (userAction) {
        UserAction.NONE -> "Pending"
        UserAction.ACKNOWLEDGED -> "Acknowledged"
        UserAction.MODIFIED_PROMPT -> "Modified"
        UserAction.PROCEEDED_ANYWAY -> "Proceeded"
    }
}



private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun getAppName(prompt: String): String {
    // Split package name by '.' and take the last element as app name
    return if (prompt.contains(".")) {
        val parts = prompt.split(".")
        if (parts.isNotEmpty()) {
            parts.last().capitalize()
        } else {
            "AI Text Generation Interface"
        }
    } else {
        "AI Text Generation Interface"
    }
}

