package com.example.neurogate.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AIRepository(
    private val context: android.content.Context,
    private val aiService: AIService = RealAIService(context),
    private val apiKey: String = "your-api-key-here"
) {
    private val _flaggedInteractions = MutableStateFlow<List<FlaggedInteraction>>(emptyList())
    val flaggedInteractions: Flow<List<FlaggedInteraction>> = _flaggedInteractions.asStateFlow()
    
    private val _settings = MutableStateFlow(AppSettings())
    val settings: Flow<AppSettings> = _settings.asStateFlow()
    
    suspend fun analyzePrompt(prompt: String): DetectionResult {
        val request = PromptAnalysisRequest(prompt = prompt)
        val response = aiService.analyzePrompt(apiKey, request)
        
        val detectionResult = DetectionResult(
            isMisuse = response.isMisuse,
            confidence = response.confidence,
            reason = response.reason,
            category = response.category,
            prompt = prompt,
            suggestions = response.suggestions
        )
        
        // If misuse detected, add to flagged interactions
        if (response.isMisuse) {
            val flaggedInteraction = FlaggedInteraction(
                id = UUID.randomUUID().toString(),
                prompt = prompt,
                detectionResult = detectionResult,
                timestamp = System.currentTimeMillis()
            )
            
            val currentList = _flaggedInteractions.value.toMutableList()
            currentList.add(0, flaggedInteraction) // Add to beginning
            _flaggedInteractions.value = currentList
        }
        
        return detectionResult
    }
    
    fun updateUserAction(interactionId: String, action: UserAction) {
        val currentList = _flaggedInteractions.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == interactionId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(userAction = action)
            _flaggedInteractions.value = currentList
        }
    }
    
    fun updateSettings(settings: AppSettings) {
        _settings.value = settings
    }
    
    fun clearFlaggedInteractions() {
        _flaggedInteractions.value = emptyList()
    }
    
    fun getFlaggedInteractionsCount(): Int {
        return _flaggedInteractions.value.size
    }
    
    fun getRecentFlaggedInteractions(limit: Int = 10): List<FlaggedInteraction> {
        return _flaggedInteractions.value.take(limit)
    }
}
