package com.example.neurogate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.neurogate.ui.components.ActivityCard
import com.example.neurogate.ui.viewmodels.ActivityViewModel
import com.example.neurogate.utils.AppNameUtils
import getCategoryDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    viewModel: ActivityViewModel,
    onBackClick: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedApp by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    val activities by viewModel.activities.collectAsState()
    val context = LocalContext.current
    
    // Optimize list state
    val listState = rememberLazyListState()
    
    // Filter activities based on selected filters
    val filteredActivities = remember(activities, selectedCategory, selectedApp) {
        activities.filter { activity ->
            val categoryMatch = selectedCategory == null || activity.category == selectedCategory
            val appMatch = selectedApp == null || activity.packageName == selectedApp
            categoryMatch && appMatch
        }
    }
    
    // Memoize filter chips
    val filterChips = remember(selectedCategory, selectedApp) {
        buildList {
            if (selectedCategory != null) {
                add(FilterChipData(getCategoryDisplayName(selectedCategory!!), FilterType.CATEGORY))
            }
            if (selectedApp != null) {
                add(FilterChipData(AppNameUtils.getAppName(context, selectedApp!!), FilterType.APP))
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
        if (filteredActivities.isEmpty()) {
            EmptyStateContent()
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredActivities,
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
            viewModel = viewModel
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
    viewModel: ActivityViewModel
) {
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var apps by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        // Get unique categories and apps from current activities
        val currentActivities = viewModel.activities.value
        categories = currentActivities.map { it.category }.distinct().sorted()
        apps = currentActivities.map { it.packageName }.distinct().sorted()
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
                            text = getCategoryDisplayName(category),
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
                            text = AppNameUtils.getAppName(context, app),
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


