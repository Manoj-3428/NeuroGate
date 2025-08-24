package com.example.neurogate.data

import com.google.gson.annotations.SerializedName

// API Models for LLM Integration
data class PromptAnalysisRequest(
    @SerializedName("prompt")
    val prompt: String,
    @SerializedName("context")
    val context: String = "AI image generation",
    @SerializedName("model")
    val model: String = "gpt-3.5-turbo"
)

data class PromptAnalysisResponse(
    @SerializedName("is_misuse")
    val isMisuse: Boolean,
    @SerializedName("confidence")
    val confidence: Double,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("category")
    val category: MisuseCategory,
    @SerializedName("suggestions")
    val suggestions: List<String> = emptyList(),
    @SerializedName("metadata")
    val metadata: Map<String, String> = emptyMap()
)

enum class MisuseCategory {
    DEEPFAKE,
    CELEBRITY_IMPERSONATION,
    HARMFUL_CONTENT,
    COPYRIGHT_VIOLATION,
    PRIVACY_VIOLATION,
    NONE
}

// Detection Result Model
data class DetectionResult(
    val isMisuse: Boolean,
    val confidence: Double,
    val reason: String,
    val category: MisuseCategory,
    val timestamp: Long = System.currentTimeMillis(),
    val prompt: String,
    val suggestions: List<String> = emptyList()
)

// Flagged Interaction for Dashboard
data class FlaggedInteraction(
    val id: String,
    val prompt: String,
    val detectionResult: DetectionResult,
    val timestamp: Long,
    val userAction: UserAction = UserAction.NONE
)

enum class UserAction {
    NONE,
    ACKNOWLEDGED,
    MODIFIED_PROMPT,
    PROCEEDED_ANYWAY
}

// App Settings
data class AppSettings(
    val enableRealTimeDetection: Boolean = true,
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.MEDIUM,
    val autoClearInput: Boolean = true,
    val showFloatingAlerts: Boolean = true
)

enum class SensitivityLevel {
    LOW,
    MEDIUM,
    HIGH
}
