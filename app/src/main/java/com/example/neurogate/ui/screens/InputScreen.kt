package com.example.neurogate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search

import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import com.example.neurogate.ui.components.CustomIcons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neurogate.ui.AIDetectionViewModel
import com.example.neurogate.ui.components.FloatingAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    viewModel: AIDetectionViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val flaggedCount by viewModel.flaggedInteractions.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var promptText by remember { mutableStateOf("") }
    var shouldClearInput by remember { mutableStateOf(false) }
    
    // Optimize scroll state
    val scrollState = rememberScrollState()
    
    // Memoize examples to avoid recreation
    val examples = remember {
        listOf(
            "Create a beautiful sunset landscape" to "Safe",
            "Replace my face with Tom Cruise" to "Misuse",
            "Generate a cute cat picture" to "Safe",
            "Create deepfake video of celebrity" to "Misuse",
            "Make me look like Brad Pitt" to "Misuse",
            "Swap my face with a celebrity" to "Misuse",
            "Transform into Leonardo DiCaprio" to "Misuse"
        )
    }
    
    // Handle input clearing
    LaunchedEffect(uiState.shouldClearInput) {
        if (uiState.shouldClearInput) {
            promptText = ""
            shouldClearInput = true
            viewModel.clearInputHandled()
        }
    }
    
    // Reset clear flag
    LaunchedEffect(shouldClearInput) {
        if (shouldClearInput) {
            shouldClearInput = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NeuroGate AI Protection") },
                actions = {
                    // Dashboard button with badge
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = flaggedCount.size.toString(),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(
                            imageVector = CustomIcons.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    }
                    

                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                HeaderCard()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Input Section
                InputSection(
                    promptText = promptText,
                    onPromptChange = { 
                        promptText = it
                        // Real-time analysis with debouncing
                        if (it.length > 5) {
                            viewModel.analyzePrompt(it)
                        }
                    },
                    onAnalyzeClick = { 
                        if (promptText.isNotBlank()) {
                            viewModel.analyzePrompt(promptText)
                        }
                    },
                    onClearClick = { promptText = "" },
                    isAnalyzing = uiState.isAnalyzing
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status Section
                StatusSection(uiState = uiState)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Quick Examples
                ExamplesSection(
                    examples = examples,
                    onExampleClick = { example ->
                        promptText = example
                        viewModel.analyzePrompt(example)
                    }
                )
            }
            
            // Floating Alert
            FloatingAlert(
                detectionResult = if (uiState.showAlert) uiState.lastDetectionResult else null,
                onDismiss = { viewModel.dismissAlert() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = CustomIcons.Security,
                contentDescription = "Security",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "AI Misuse Detection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Text(
                text = "Real-time protection against harmful AI usage",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun InputSection(
    promptText: String,
    onPromptChange: (String) -> Unit,
    onAnalyzeClick: () -> Unit,
    onClearClick: () -> Unit,
    isAnalyzing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Enter your AI prompt:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("e.g., 'Create a beautiful landscape' or 'Replace my face with Tom Cruise'")
                },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onAnalyzeClick,
                    enabled = promptText.isNotBlank() && !isAnalyzing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Analyze"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Prompt")
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                OutlinedButton(
                    onClick = onClearClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
private fun StatusSection(uiState: com.example.neurogate.ui.AIDetectionUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Detection Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            uiState.lastDetectionResult?.let { result ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (result.isMisuse) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = "Status",
                        tint = if (result.isMisuse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = if (result.isMisuse) "Misuse Detected" else "No Issues Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (result.isMisuse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = result.reason,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        Text(
                            text = "Confidence: ${(result.confidence * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } ?: run {
                Text(
                    text = "Enter a prompt to start analysis",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ExamplesSection(
    examples: List<Pair<String, String>>,
    onExampleClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Examples",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            examples.forEach { (example, status) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onExampleClick(example) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• $example",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Surface(
                        color = if (status == "Safe") 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = status,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
