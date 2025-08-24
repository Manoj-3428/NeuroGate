package com.example.neurogate.ai

import android.content.Context
import com.example.neurogate.BuildConfig
import com.example.neurogate.data.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlin.math.max

/**
 * Dynamic Language Model Service using real LLMs
 * Supports Hugging Face, OpenAI, Cohere, and Anthropic
 * Falls back to enhanced local analysis when APIs are unavailable
 */
class LanguageModelService(private val context: Context) {
    
    private val huggingFaceClient = createHuggingFaceClient()
    private val openAIClient = createOpenAIClient()
    private val cohereClient = createCohereClient()
    private val anthropicClient = createAnthropicClient()
    
    suspend fun analyzePromptWithLLM(prompt: String): PromptAnalysisResponse = coroutineScope {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("NeuroGate-LLM", "Starting LLM analysis for: $prompt")
        
        // Check if any API keys are configured
        val hasConfiguredAPIs = listOf(
            BuildConfig.HUGGINGFACE_API_KEY,
            BuildConfig.OPENAI_API_KEY,
            BuildConfig.COHERE_API_KEY
        ).any { it != "your-huggingface-api-key-here" && it != "your-openai-api-key-here" && it != "your-cohere-api-key-here" }
        
        if (!hasConfiguredAPIs) {
            android.util.Log.i("NeuroGate-LLM", "No API keys configured, using enhanced fallback analysis")
            return@coroutineScope enhancedFallbackAnalysis(prompt)
        }
        
        try {
            // Try multiple LLM services in parallel with timeout
            val huggingFaceDeferred = async { 
                withTimeout(5000) { analyzeWithHuggingFace(prompt) }
            }
            val openAIDeferred = async { 
                withTimeout(5000) { analyzeWithOpenAI(prompt) }
            }
            val cohereDeferred = async { 
                withTimeout(5000) { analyzeWithCohere(prompt) }
            }
            
            // Wait for the first successful response
            val results = listOf(
                runCatching { huggingFaceDeferred.await() },
                runCatching { openAIDeferred.await() },
                runCatching { cohereDeferred.await() }
            ).mapNotNull { it.getOrNull() }
            
            val totalTime = System.currentTimeMillis() - startTime
            
            if (results.isNotEmpty()) {
                // Use ensemble of successful results
                val finalResult = ensembleLLMResults(results)
                android.util.Log.d("NeuroGate-LLM", """
                    LLM Analysis Complete:
                    Total Time: ${totalTime}ms
                    Services Used: ${results.size}
                    Final Result: ${finalResult.isMisuse} (${finalResult.category})
                    Confidence: ${finalResult.confidence}
                """.trimIndent())
                
                finalResult
            } else {
                // All LLM services failed, use enhanced fallback
                android.util.Log.w("NeuroGate-LLM", "All LLM services failed, using enhanced fallback")
                enhancedFallbackAnalysis(prompt)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("NeuroGate-LLM", "LLM analysis failed", e)
            enhancedFallbackAnalysis(prompt)
        }
    }
    
    /**
     * Analyze using Hugging Face Transformers API
     */
    private suspend fun analyzeWithHuggingFace(prompt: String): PromptAnalysisResponse {
        val apiKey = BuildConfig.HUGGINGFACE_API_KEY
        if (apiKey == "your-huggingface-api-key-here") {
            throw Exception("Hugging Face API key not configured")
        }
        
        val request = HuggingFaceRequest(
            inputs = buildPromptForAnalysis(prompt)
        )
        
        val response = huggingFaceClient.create(HuggingFaceService::class.java)
            .classifyText("Bearer $apiKey", request)
        
        return parseHuggingFaceResponse(response, prompt)
    }
    
    /**
     * Analyze using OpenAI GPT for context understanding
     */
    private suspend fun analyzeWithOpenAI(prompt: String): PromptAnalysisResponse {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey == "your-openai-api-key-here") {
            throw Exception("OpenAI API key not configured")
        }
        
        val request = OpenAICompletionRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = buildSystemPrompt()
                ),
                OpenAIMessage(
                    role = "user",
                    content = buildUserPrompt(prompt)
                )
            ),
            temperature = 0.1,
            maxTokens = 150
        )
        
        val response = openAIClient.create(OpenAICompletionService::class.java)
            .createCompletion("Bearer $apiKey", request)
        
        return parseOpenAIResponse(response, prompt)
    }
    
    /**
     * Analyze using Cohere for semantic understanding
     */
    private suspend fun analyzeWithCohere(prompt: String): PromptAnalysisResponse {
        val apiKey = BuildConfig.COHERE_API_KEY
        if (apiKey == "your-cohere-api-key-here") {
            throw Exception("Cohere API key not configured")
        }
        
        val request = CohereClassifyRequest(
            texts = listOf(prompt),
            examples = getCohereTrainingExamples()
        )
        
        val response = cohereClient.create(CohereService::class.java)
            .classifyText("Bearer $apiKey", request)
        
        return parseCohereResponse(response, prompt)
    }
    
    /**
     * Build system prompt for GPT analysis
     */
    private fun buildSystemPrompt(): String {
        return """
        You are an AI safety classifier. Analyze user prompts to detect potential misuse for:
        1. Deepfake creation or face manipulation
        2. Celebrity impersonation without consent
        3. Privacy violations or personal data misuse
        4. Creating harmful, toxic, or inappropriate content
        5. Bypassing AI safety measures
        
        Consider context, intent, and potential harm. Some educational or creative uses may be acceptable.
        
        Respond ONLY with a JSON object in this exact format:
        {
            "is_misuse": boolean,
            "confidence": number (0.0-1.0),
            "category": "DEEPFAKE|CELEBRITY_IMPERSONATION|HARMFUL_CONTENT|PRIVACY_VIOLATION|NONE",
            "reason": "brief explanation",
            "suggestions": ["suggestion1", "suggestion2"]
        }
        """.trimIndent()
    }
    
    /**
     * Build user prompt for analysis
     */
    private fun buildUserPrompt(prompt: String): String {
        return "Analyze this prompt for potential AI misuse: \"$prompt\""
    }
    
    /**
     * Build prompt for Hugging Face classification
     */
    private fun buildPromptForAnalysis(prompt: String): String {
        return """
        Classify this text for AI misuse detection:
        
        Text: "$prompt"
        
        Categories:
        - SAFE: Normal, creative, or educational content
        - DEEPFAKE: Face manipulation, deepfake creation
        - CELEBRITY: Celebrity impersonation without consent
        - HARMFUL: Toxic, inappropriate, or dangerous content
        - PRIVACY: Personal data or privacy violations
        """.trimIndent()
    }
    
    /**
     * Get training examples for Cohere
     */
    private fun getCohereTrainingExamples(): List<CohereExample> {
        return listOf(
            // Safe examples
            CohereExample("Generate a beautiful landscape", "SAFE"),
            CohereExample("Create a cute cat picture", "SAFE"),
            CohereExample("Design a logo for my business", "SAFE"),
            CohereExample("Write a poem about nature", "SAFE"),
            
            // Deepfake examples
            CohereExample("Replace my face with Tom Cruise", "DEEPFAKE"),
            CohereExample("Make me look like Brad Pitt", "DEEPFAKE"),
            CohereExample("Create a deepfake video", "DEEPFAKE"),
            CohereExample("Face swap with celebrity", "DEEPFAKE"),
            
            // Celebrity impersonation
            CohereExample("Transform me into Leonardo DiCaprio", "CELEBRITY"),
            CohereExample("Make me appear as Angelina Jolie", "CELEBRITY"),
            CohereExample("Impersonate famous actor", "CELEBRITY"),
            
            // Harmful content
            CohereExample("Generate violent images", "HARMFUL"),
            CohereExample("Create explicit content", "HARMFUL"),
            CohereExample("Make dangerous weapons", "HARMFUL"),
            
            // Privacy violations
            CohereExample("My password is 123456", "PRIVACY"),
            CohereExample("Here's my credit card number", "PRIVACY"),
            CohereExample("Share personal information", "PRIVACY")
        )
    }
    
    /**
     * Parse Hugging Face response
     */
    private fun parseHuggingFaceResponse(response: List<HuggingFaceResult>, prompt: String): PromptAnalysisResponse {
        val result = response.firstOrNull() ?: throw Exception("No classification result")
        
        val isMisuse = result.label != "SAFE"
        val confidence = result.score
        
        val category = when (result.label) {
            "DEEPFAKE" -> MisuseCategory.DEEPFAKE
            "CELEBRITY" -> MisuseCategory.CELEBRITY_IMPERSONATION
            "HARMFUL" -> MisuseCategory.HARMFUL_CONTENT
            "PRIVACY" -> MisuseCategory.PRIVACY_VIOLATION
            else -> MisuseCategory.NONE
        }
        
        return PromptAnalysisResponse(
            isMisuse = isMisuse,
            confidence = confidence,
            reason = "Hugging Face classification: ${result.label} with ${(confidence * 100).toInt()}% confidence",
            category = category,
            suggestions = generateLLMSuggestions(category, confidence),
            metadata = mapOf(
                "model" to "huggingface",
                "label" to result.label,
                "score" to confidence.toString()
            )
        )
    }
    
    /**
     * Parse OpenAI response
     */
    private fun parseOpenAIResponse(response: OpenAICompletionResponse, prompt: String): PromptAnalysisResponse {
        val content = response.choices.firstOrNull()?.message?.content 
            ?: throw Exception("No completion result")
        
        return try {
            // Parse JSON response from GPT
            val jsonStart = content.indexOf("{")
            val jsonEnd = content.lastIndexOf("}") + 1
            
            if (jsonStart == -1 || jsonEnd <= jsonStart) {
                throw Exception("Invalid JSON response")
            }
            
            val jsonContent = content.substring(jsonStart, jsonEnd)
            val gson = com.google.gson.Gson()
            val result = gson.fromJson(jsonContent, GPTAnalysisResult::class.java)
            
            val category = when (result.category) {
                "DEEPFAKE" -> MisuseCategory.DEEPFAKE
                "CELEBRITY_IMPERSONATION" -> MisuseCategory.CELEBRITY_IMPERSONATION
                "HARMFUL_CONTENT" -> MisuseCategory.HARMFUL_CONTENT
                "PRIVACY_VIOLATION" -> MisuseCategory.PRIVACY_VIOLATION
                else -> MisuseCategory.NONE
            }
            
            PromptAnalysisResponse(
                isMisuse = result.isMisuse,
                confidence = result.confidence,
                reason = "GPT analysis: ${result.reason}",
                category = category,
                suggestions = result.suggestions,
                metadata = mapOf(
                    "model" to "gpt-3.5-turbo",
                    "raw_response" to content
                )
            )
            
        } catch (e: Exception) {
            android.util.Log.e("NeuroGate-LLM", "Failed to parse GPT response: $content", e)
            throw Exception("Failed to parse GPT response")
        }
    }
    
    /**
     * Parse Cohere response
     */
    private fun parseCohereResponse(response: CohereClassifyResponse, prompt: String): PromptAnalysisResponse {
        val result = response.classifications.firstOrNull() 
            ?: throw Exception("No classification result")
        
        val isMisuse = result.prediction != "SAFE"
        val confidence = result.confidence
        
        val category = when (result.prediction) {
            "DEEPFAKE" -> MisuseCategory.DEEPFAKE
            "CELEBRITY" -> MisuseCategory.CELEBRITY_IMPERSONATION
            "HARMFUL" -> MisuseCategory.HARMFUL_CONTENT
            "PRIVACY" -> MisuseCategory.PRIVACY_VIOLATION
            else -> MisuseCategory.NONE
        }
        
        return PromptAnalysisResponse(
            isMisuse = isMisuse,
            confidence = confidence,
            reason = "Cohere classification: ${result.prediction} with ${(confidence * 100).toInt()}% confidence",
            category = category,
            suggestions = generateLLMSuggestions(category, confidence),
            metadata = mapOf(
                "model" to "cohere",
                "prediction" to result.prediction,
                "confidence" to confidence.toString()
            )
        )
    }
    
    /**
     * Ensemble multiple LLM results
     */
    private fun ensembleLLMResults(results: List<PromptAnalysisResponse>): PromptAnalysisResponse {
        val misuseCount = results.count { it.isMisuse }
        val totalResults = results.size
        
        val isMisuse = misuseCount > totalResults / 2 // Majority vote
        
        val avgConfidence = results.map { it.confidence }.average()
        
        // Choose category from most confident misuse detection
        val category = if (isMisuse) {
            results.filter { it.isMisuse }
                .maxByOrNull { it.confidence }?.category 
                ?: MisuseCategory.NONE
        } else {
            MisuseCategory.NONE
        }
        
        val reasons = results.map { it.reason }
        val allSuggestions = results.flatMap { it.suggestions }.distinct()
        
        return PromptAnalysisResponse(
            isMisuse = isMisuse,
            confidence = avgConfidence,
            reason = "Ensemble LLM analysis (${misuseCount}/${totalResults} detected misuse): ${reasons.joinToString("; ")}",
            category = category,
            suggestions = allSuggestions.take(3), // Limit suggestions
            metadata = mapOf(
                "ensemble_size" to totalResults.toString(),
                "misuse_votes" to misuseCount.toString(),
                "models_used" to results.mapNotNull { it.metadata["model"] }.joinToString(",")
            )
        )
    }
    
    /**
     * Enhanced fallback when all LLMs fail
     */
    private fun enhancedFallbackAnalysis(prompt: String): PromptAnalysisResponse {
        // Use the improved static analysis as fallback
        android.util.Log.w("NeuroGate-LLM", "Using enhanced fallback analysis")
        
        val lowerPrompt = prompt.lowercase()
        
        // COMPREHENSIVE PATTERN MATCHING FOR 5 CATEGORIES
        
        // 1. PERSONAL DATA PATTERNS
        val personalDataPatterns = listOf(
            "my name is", "my date of birth", "my phone number is", "my email is",
            "my password is", "my bank account number", "my credit card", "my passport number",
            "my student id", "aadhaar number", "ifsc code", "cvv", "expiry", "otp",
            "personal details", "private information", "confidential data", "my address is",
            "i live at", "my account number", "social security", "passport", "student id"
        )
        
        // 2. HACKING PATTERNS
        val hackingPatterns = listOf(
            "hack into", "break into", "bypass password", "steal details", "plant a keylogger",
            "hack into email", "disable cctv", "crack login", "hack into phone", "change grades",
            "unauthorized access", "system breach", "password crack", "security bypass",
            "hacking guide", "cyber attack", "social engineering", "phishing", "malware",
            "exploit", "vulnerability", "root access", "admin access", "data breach",
            "keylogger", "cctv", "grades", "system", "security", "cyber", "breach"
        )
        
        // 2.5. HARMFUL CONTENT PATTERNS (Self-harm, Violence, etc.)
        val harmfulContentPatterns = listOf(
            "how to kill", "kill myself", "kill yourself", "commit suicide", "end my life",
            "take my life", "self harm", "self-harm", "cut myself", "hurt myself",
            "harm myself", "die", "death", "murder", "assassinate", "shoot", "stab",
            "poison", "overdose", "hang myself", "jump off", "crash car", "drive into",
            "violent", "violence", "attack", "fight", "beat up", "punch", "kick",
            "threaten", "threat", "harm", "hurt", "injure", "wound", "bleed", "blood"
        )
        
        // 3. IMAGE/VIDEO MISUSE PATTERNS
        val imageVideoPatterns = listOf(
            "replace face", "swap face", "face swap", "face replacement", "make a video where",
            "generate a video where", "deepfake", "face manipulation", "put face on", "swap face with",
            "replace face with", "face editing", "face morph", "face blend", "face overlay",
            "face merge", "face composite", "face superimpose", "impersonate", "pretend to be",
            "prank", "cartoon character", "celebrity body", "movie actor", "action scene",
            "instagram reel", "deepfake", "manipulation", "create deepfake", "generate deepfake",
            "make deepfake", "create fake video", "generate fake video", "make fake video",
            "create fake photo", "generate fake photo", "make fake photo", "face swap with",
            "replace face with", "swap face with", "put face on", "face manipulation"
        )
        
        // 4. EXPLOSIVES/WEAPONS PATTERNS
        val explosivesPatterns = listOf(
            "make a bomb", "build a bomb", "create an explosive", "assemble a bomb",
            "hide a bomb", "detonate ied", "formula for making tnt", "turn into a bomb",
            "plant a bomb", "gunpowder", "pipe bomb", "time bomb", "homemade bomb",
            "chemicals explosive", "blow up", "explosive", "detonation", "bomb circuit",
            "ied", "tnt", "bomb", "explosive", "detonate", "gunpowder", "chemicals"
        )
        
        // 5. DRUGS/SUBSTANCES PATTERNS
        val drugsPatterns = listOf(
            "take lsd", "inject heroin", "snort cocaine", "smoke meth", "mix ecstasy",
            "take tablets", "cannabis edibles", "roll a joint", "dosage of mushrooms",
            "crush pills", "drug", "heroin", "cocaine", "meth", "ecstasy", "lsd",
            "mushrooms", "cannabis", "marijuana", "weed", "oxycodone", "prescription pills",
            "hallucinate", "get high", "feel better", "inject", "snort", "smoke", "mix"
        )
        
        // Check for patterns - 6 CATEGORIES
        val hasPersonalData = personalDataPatterns.any { lowerPrompt.contains(it) }
        val hasHacking = hackingPatterns.any { lowerPrompt.contains(it) }
        val hasHarmfulContent = harmfulContentPatterns.any { lowerPrompt.contains(it) }
        val hasImageVideo = imageVideoPatterns.any { lowerPrompt.contains(it) }
        val hasExplosives = explosivesPatterns.any { lowerPrompt.contains(it) }
        val hasDrugs = drugsPatterns.any { lowerPrompt.contains(it) }
        
        // Intent indicators that boost detection confidence
        val intentIndicators = listOf(
            "how to", "how can i", "give me", "show me", "teach me",
            "what is the best", "best way to", "help me", "i want to",
            "tutorial", "guide", "instructions", "steps", "method"
        )
        
        val hasIntent = intentIndicators.any { lowerPrompt.contains(it) }
        
        return when {
            hasPersonalData -> PromptAnalysisResponse(
                isMisuse = true,
                confidence = if (hasIntent) 0.95 else 0.85,
                reason = "Potential personal data/privacy violation detected",
                category = MisuseCategory.PRIVACY_VIOLATION,
                suggestions = listOf(
                    "Protect personal and sensitive information",
                    "Avoid sharing confidential data like passwords, bank details, or IDs",
                    "Respect privacy rights and data protection"
                ),
                metadata = mapOf("analysis_method" to "enhanced_fallback")
            )
            hasHacking -> PromptAnalysisResponse(
                isMisuse = true,
                confidence = if (hasIntent) 0.95 else 0.85,
                reason = "Potential hacking/security bypass request detected",
                category = MisuseCategory.HARMFUL_CONTENT,
                suggestions = listOf(
                    "Avoid requesting hacking tutorials or security bypass methods",
                    "Consider legitimate cybersecurity learning resources",
                    "Respect network security and privacy"
                ),
                metadata = mapOf("analysis_method" to "enhanced_fallback")
            )
            hasHarmfulContent -> PromptAnalysisResponse(
                isMisuse = true,
                confidence = if (hasIntent) 0.98 else 0.9,
                reason = "Potential harmful content or self-harm detected",
                category = MisuseCategory.HARMFUL_CONTENT,
                suggestions = listOf(
                    "If you're having thoughts of self-harm, please seek help immediately",
                    "Contact a mental health professional or crisis hotline",
                    "You're not alone - help is available 24/7"
                ),
                metadata = mapOf("analysis_method" to "enhanced_fallback")
            )
            hasImageVideo -> PromptAnalysisResponse(
                isMisuse = true,
                confidence = if (hasIntent) 0.95 else 0.85,
                reason = "Potential image/video manipulation request detected",
                category = MisuseCategory.DEEPFAKE,
                suggestions = listOf(
                    "Avoid unauthorized face swapping or video manipulation",
                    "Respect people's privacy and consent",
                    "Create original content instead"
                ),
                metadata = mapOf("analysis_method" to "enhanced_fallback")
            )
            hasExplosives -> PromptAnalysisResponse(
                isMisuse = true,
                confidence = if (hasIntent) 0.98 else 0.9,
                reason = "Potential explosives/weapons request detected",
                category = MisuseCategory.HARMFUL_CONTENT,
                suggestions = listOf(
                    "Avoid requesting information about explosives or weapons",
                    "Consider legal and safe alternatives",
                    "Follow laws and regulations"
                ),
                metadata = mapOf("analysis_method" to "enhanced_fallback")
            )
            hasDrugs -> PromptAnalysisResponse(
                isMisuse = true,
                confidence = if (hasIntent) 0.95 else 0.85,
                reason = "Potential drug/substance abuse request detected",
                category = MisuseCategory.HARMFUL_CONTENT,
                suggestions = listOf(
                    "Avoid requesting information about illegal drugs or substances",
                    "Consider legal and healthy alternatives",
                    "Follow laws and regulations"
                ),
                metadata = mapOf("analysis_method" to "enhanced_fallback")
            )
            else -> PromptAnalysisResponse(
                isMisuse = false,
                confidence = 0.9,
                reason = "No significant misuse patterns found",
                category = MisuseCategory.NONE,
                suggestions = listOf("Content appears safe", "Continue creating"),
                metadata = mapOf("analysis_method" to "enhanced_fallback")
            )
        }
    }
    
    /**
     * Generate contextual suggestions based on LLM analysis
     */
    private fun generateLLMSuggestions(category: MisuseCategory, confidence: Double): List<String> {
        return when (category) {
            MisuseCategory.DEEPFAKE -> listOf(
                "Consider using original artwork instead",
                "Explore ethical digital art creation",
                "Respect consent and privacy rights"
            )
            MisuseCategory.CELEBRITY_IMPERSONATION -> listOf(
                "Create original characters inspired by but not copying celebrities",
                "Consider the legal implications of impersonation",
                "Respect public figures' rights"
            )
            MisuseCategory.HARMFUL_CONTENT -> listOf(
                "Focus on positive and constructive content",
                "Consider the impact on others",
                "Follow community guidelines"
            )
            MisuseCategory.PRIVACY_VIOLATION -> listOf(
                "Protect personal and sensitive information",
                "Be mindful of data privacy",
                "Avoid sharing confidential details"
            )
            MisuseCategory.NONE -> listOf(
                "Your content appears appropriate",
                "Continue exploring creative possibilities",
                "Consider adding more details for better results"
            )
            else -> emptyList()
        }
    }
    
    private fun createHuggingFaceClient(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        
        return Retrofit.Builder()
            .baseUrl("https://api-inference.huggingface.co/models/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private fun createOpenAIClient(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private fun createCohereClient(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        
        return Retrofit.Builder()
            .baseUrl("https://api.cohere.ai/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private fun createAnthropicClient(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        
        return Retrofit.Builder()
            .baseUrl("https://api.anthropic.com/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

// API Interfaces and Data Classes

// Hugging Face API
interface HuggingFaceService {
    @POST("facebook/bart-large-mnli")
    suspend fun classifyText(
        @Header("Authorization") authorization: String,
        @Body request: HuggingFaceRequest
    ): List<HuggingFaceResult>
}

data class HuggingFaceRequest(
    val inputs: String
)

data class HuggingFaceResult(
    val label: String,
    val score: Double
)

// OpenAI Completion API
interface OpenAICompletionService {
    @POST("chat/completions")
    suspend fun createCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAICompletionRequest
    ): OpenAICompletionResponse
}

data class OpenAICompletionRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double,
    val maxTokens: Int
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

data class OpenAICompletionResponse(
    val choices: List<OpenAIChoice>
)

data class OpenAIChoice(
    val message: OpenAIMessage
)

data class GPTAnalysisResult(
    val isMisuse: Boolean,
    val confidence: Double,
    val category: String,
    val reason: String,
    val suggestions: List<String>
)

// Cohere API
interface CohereService {
    @POST("classify")
    suspend fun classifyText(
        @Header("Authorization") authorization: String,
        @Body request: CohereClassifyRequest
    ): CohereClassifyResponse
}

data class CohereClassifyRequest(
    val texts: List<String>,
    val examples: List<CohereExample>
)

data class CohereExample(
    val text: String,
    val label: String
)

data class CohereClassifyResponse(
    val classifications: List<CohereClassification>
)

data class CohereClassification(
    val prediction: String,
    val confidence: Double
)
