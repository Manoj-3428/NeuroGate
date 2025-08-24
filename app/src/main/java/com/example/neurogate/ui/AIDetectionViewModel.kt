package com.example.neurogate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurogate.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AIDetectionViewModel(
    private val repository: AIRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AIDetectionUiState())
    val uiState: StateFlow<AIDetectionUiState> = _uiState.asStateFlow()
    
    // Optimize data flows with caching
    val flaggedInteractions = repository.flaggedInteractions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val settings = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )
    
    // Debouncing mechanism with improved performance
    private var analysisJob: kotlinx.coroutines.Job? = null
    
    init {
        viewModelScope.launch {
            settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }
    
    fun analyzePrompt(prompt: String) {
        if (prompt.isBlank()) return
        
        android.util.Log.d("NeuroGate", "Starting analysis for prompt: $prompt")
        
        // Cancel previous analysis job for debouncing
        analysisJob?.cancel()
        
        analysisJob = viewModelScope.launch {
            // Add a small delay for debouncing
            kotlinx.coroutines.delay(300)
            
            _uiState.update { it.copy(isAnalyzing = true) }
            
            try {
                val result = repository.analyzePrompt(prompt)
                
                android.util.Log.d("NeuroGate", "Analysis result - isMisuse: ${result.isMisuse}, category: ${result.category}, reason: ${result.reason}")
                
                _uiState.update { currentState -> 
                    currentState.copy(
                        isAnalyzing = false,
                        lastDetectionResult = result,
                        showAlert = result.isMisuse && currentState.settings.showFloatingAlerts
                    )
                }
                
                // Auto-clear input if misuse detected and setting is enabled
                if (result.isMisuse && _uiState.value.settings.autoClearInput) {
                    _uiState.update { it.copy(shouldClearInput = true) }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("NeuroGate", "Analysis failed", e)
                _uiState.update { 
                    it.copy(
                        isAnalyzing = false,
                        error = "Analysis failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun dismissAlert() {
        _uiState.update { it.copy(showAlert = false) }
    }
    
    fun clearInputHandled() {
        _uiState.update { it.copy(shouldClearInput = false) }
    }
    
    fun updateUserAction(interactionId: String, action: UserAction) {
        repository.updateUserAction(interactionId, action)
    }
    
    fun updateSettings(settings: AppSettings) {
        repository.updateSettings(settings)
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearFlaggedInteractions() {
        repository.clearFlaggedInteractions()
    }
}

data class AIDetectionUiState(
    val isAnalyzing: Boolean = false,
    val lastDetectionResult: DetectionResult? = null,
    val showAlert: Boolean = false,
    val shouldClearInput: Boolean = false,
    val error: String? = null,
    val settings: AppSettings = AppSettings()
)

// Extension function to format timestamp with caching
private val timestampFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

fun Long.formatTimestamp(): String {
    val date = Date(this)
    return timestampFormatter.format(date)
}
