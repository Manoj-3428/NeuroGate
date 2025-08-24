package com.example.neurogate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neurogate.data.DetectedActivity
import com.example.neurogate.data.ActivityStorage
import com.example.neurogate.ui.components.ActivityCard
import com.example.neurogate.ui.components.CategoryChip
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    activityStorage: ActivityStorage,
    onBackClick: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedApp by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    // Memoize the activities flow to prevent recreation
    val activitiesFlow = remember(selectedCategory, selectedApp) {
        if (selectedCategory != null) {
            activityStorage.getActivitiesByCategory(selectedCategory!!)
        } else if (selectedApp != null) {
            activityStorage.getActivitiesByApp(selectedApp!!)
        } else {
            activityStorage.getAllActivities()
        }
    }
    
    val activities by activitiesFlow.collectAsState(initial = emptyList())
    
    // Optimize list state
    val listState = rememberLazyListState()
    
    // Memoize filter chips
    val filterChips = remember(selectedCategory, selectedApp) {
        buildList {
            if (selectedCategory != null) {
                add(FilterChipData(selectedCategory!!, FilterType.CATEGORY))
            }
            if (selectedApp != null) {
                add(FilterChipData(selectedApp!!, FilterType.APP))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Activity History", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
        )
        
        // Filter Chips
        if (filterChips.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterChips.forEach { chipData ->
                    FilterChip(
                        selected = true,
                        onClick = { 
                            when (chipData.type) {
                                FilterType.CATEGORY -> selectedCategory = null
                                FilterType.APP -> selectedApp = null
                            }
                        },
                        label = { Text(chipData.label) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear") }
                    )
                }
            }
        }
        
        // Activities List
        if (activities.isEmpty()) {
            EmptyStateContent()
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = activities,
                    key = { it.id }
                ) { activity ->
                    ActivityCard(activity = activity)
                }
            }
        }
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            onDismiss = { showFilterDialog = false },
            onCategorySelected = { category ->
                selectedCategory = category
                selectedApp = null
                showFilterDialog = false
            },
            onAppSelected = { app ->
                selectedApp = app
                selectedCategory = null
                showFilterDialog = false
            },
            activityStorage = activityStorage
        )
    }
}

// Data classes for better performance
private data class FilterChipData(
    val label: String,
    val type: FilterType
)

private enum class FilterType {
    CATEGORY, APP
}

@Composable
private fun EmptyStateContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                "No activities found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun FilterDialog(
    onDismiss: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onAppSelected: (String) -> Unit,
    activityStorage: ActivityStorage
) {
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var apps by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        categories = activityStorage.getAllCategories()
        apps = activityStorage.getAllApps()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Activities") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Filter by Category:", fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = categories,
                        key = { it }
                    ) { category ->
                        Text(
                            text = category.replace("_", " "),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategorySelected(category) }
                                .padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                HorizontalDivider()
                
                Text("Filter by App:", fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = apps,
                        key = { it }
                    ) { app ->
                        Text(
                            text = app,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
