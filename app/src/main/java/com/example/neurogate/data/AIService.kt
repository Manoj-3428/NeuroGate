package com.example.neurogate.data

import android.content.Context
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.Url
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.max

interface AIService {
    @POST("analyze-prompt")
    suspend fun analyzePrompt(
        @Header("Authorization") apiKey: String,
        @Body request: PromptAnalysisRequest
    ): PromptAnalysisResponse
}

/**
 * Real AI Service using Language Models and other reliable services
 * Provides dynamic, context-aware misuse detection using LLMs
 */
class RealAIService(private val context: Context) : AIService {
    
    private val languageModelService = com.example.neurogate.ai.LanguageModelService(context)
    private val openAIClient = createOpenAIClient()
    private val perspectiveClient = createPerspectiveClient()
    private val contentModerationClient = createContentModerationClient()
    
    override suspend fun analyzePrompt(
        apiKey: String,
        request: PromptAnalysisRequest
    ): PromptAnalysisResponse = coroutineScope {
        
        val startTime = System.currentTimeMillis()
        android.util.Log.d("NeuroGate-RealAI", "Starting LLM-powered analysis for: ${request.prompt}")
        
        try {
            // Primary analysis using Language Models
            val llmResult = languageModelService.analyzePromptWithLLM(request.prompt)
            
            val totalTime = System.currentTimeMillis() - startTime
            
            android.util.Log.d("NeuroGate-RealAI", """
                LLM-Powered Analysis Complete:
                Total Time: ${totalTime}ms
                LLM Result: ${llmResult.isMisuse} (${llmResult.category})
                Confidence: ${llmResult.confidence}
                Analysis Method: ${llmResult.metadata["analysis_method"] ?: "llm"}
            """.trimIndent())
            
            llmResult
            
        } catch (e: Exception) {
            android.util.Log.e("NeuroGate-RealAI", "LLM analysis failed, falling back to multi-service analysis", e)
            
            // Fallback to original multi-service analysis
            val openAIAnalysisDeferred = async { 
                analyzeWithOpenAI(request.prompt, apiKey) 
            }
            val perspectiveAnalysisDeferred = async { 
                analyzeWithPerspective(request.prompt) 
            }
            val contentModerationDeferred = async { 
                analyzeWithContentModeration(request.prompt) 
            }
            
            val openAIResult = openAIAnalysisDeferred.await()
            val perspectiveResult = perspectiveAnalysisDeferred.await()
            val contentModerationResult = contentModerationDeferred.await()
            
            // Combine results using ensemble method
            val finalResult = ensembleResults(openAIResult, perspectiveResult, contentModerationResult)
            
            val totalTime = System.currentTimeMillis() - startTime
            
            android.util.Log.d("NeuroGate-RealAI", """
                Fallback Analysis Complete:
                Total Time: ${totalTime}ms
                OpenAI Score: ${openAIResult.confidence}
                Perspective Score: ${perspectiveResult.confidence}
                Content Moderation Score: ${contentModerationResult.confidence}
                Final Result: ${finalResult.isMisuse} (${finalResult.category})
            """.trimIndent())
            
            finalResult
        }
    }
    
    /**
     * Analyze using OpenAI's Content Moderation API
     */
    private suspend fun analyzeWithOpenAI(prompt: String, apiKey: String): PromptAnalysisResponse {
        return try {
            // Use BuildConfig API key if available, otherwise use provided key
            val actualApiKey = if (apiKey == "your-api-key-here") {
                com.example.neurogate.BuildConfig.OPENAI_API_KEY
            } else {
                apiKey
            }
            
            // If no valid API key, fall back to local analysis
            if (actualApiKey == "your-openai-api-key-here") {
                android.util.Log.w("NeuroGate-RealAI", "OpenAI API key not configured, using fallback analysis")
                return fallbackAnalysis(prompt)
            }
            
            val request = OpenAIRequest(
                input = prompt,
                model = "text-moderation-latest"
            )
            
            val response = openAIClient.create(OpenAIService::class.java)
                .moderateContent("Bearer $actualApiKey", request)
            
            // Parse OpenAI's moderation results
            val categories = response.results.firstOrNull()?.categories ?: OpenAICategories()
            val scores = response.results.firstOrNull()?.categoryScores ?: OpenAICategoryScores()
            
            val isMisuse = categories.harassment || categories.hate || categories.selfHarm || 
                          categories.sexual || categories.violence || categories.sexualMinors
            
            val confidence = max(
                scores.harassment ?: 0.0,
                max(
                    scores.hate ?: 0.0,
                    max(
                        scores.selfHarm ?: 0.0,
                        max(
                            scores.sexual ?: 0.0,
                            max(scores.violence ?: 0.0, scores.sexualMinors ?: 0.0)
                        )
                    )
                )
            )
            
            val category = when {
                categories.sexual || categories.sexualMinors -> MisuseCategory.HARMFUL_CONTENT
                categories.violence -> MisuseCategory.HARMFUL_CONTENT
                categories.harassment || categories.hate -> MisuseCategory.HARMFUL_CONTENT
                categories.selfHarm -> MisuseCategory.HARMFUL_CONTENT
                else -> MisuseCategory.NONE
            }
            
            PromptAnalysisResponse(
                isMisuse = isMisuse,
                confidence = confidence,
                reason = "OpenAI Content Moderation: ${if (isMisuse) "Detected inappropriate content" else "Content appears safe"}",
                category = category,
                suggestions = generateSuggestions(category, confidence),
                metadata = mapOf(
                    "openai_flagged" to isMisuse.toString(),
                    "openai_harassment" to (scores.harassment ?: 0.0).toString(),
                    "openai_hate" to (scores.hate ?: 0.0).toString(),
                    "openai_sexual" to (scores.sexual ?: 0.0).toString(),
                    "openai_violence" to (scores.violence ?: 0.0).toString()
                )
            )
            
        } catch (e: Exception) {
            android.util.Log.e("NeuroGate-RealAI", "OpenAI analysis failed", e)
            // Fallback to local analysis
            fallbackAnalysis(prompt)
        }
    }
    
    /**
     * Analyze using Google's Perspective API for toxicity detection
     */
    private suspend fun analyzeWithPerspective(prompt: String): PromptAnalysisResponse {
        return try {
            // Check if Perspective API key is available
            val perspectiveApiKey = com.example.neurogate.BuildConfig.PERSPECTIVE_API_KEY
            if (perspectiveApiKey == "your-perspective-api-key-here") {
                android.util.Log.w("NeuroGate-RealAI", "Perspective API key not configured, using fallback analysis")
                return fallbackAnalysis(prompt)
            }
            
            val request = PerspectiveRequest(
                comment = PerspectiveComment(text = prompt),
                languages = listOf("en"),
                requestedAttributes = mapOf(
                    "TOXICITY" to PerspectiveAttribute(),
                    "SEVERE_TOXICITY" to PerspectiveAttribute(),
                    "IDENTITY_ATTACK" to PerspectiveAttribute(),
                    "INSULT" to PerspectiveAttribute(),
                    "PROFANITY" to PerspectiveAttribute(),
                    "THREAT" to PerspectiveAttribute()
                )
            )
            
            val response = perspectiveClient.create(PerspectiveService::class.java)
                .analyzeText(request)
            
            val toxicityScore = response.attributeScores?.get("TOXICITY")?.summaryScore?.value ?: 0.0
            val severeToxicityScore = response.attributeScores?.get("SEVERE_TOXICITY")?.summaryScore?.value ?: 0.0
            val threatScore = response.attributeScores?.get("THREAT")?.summaryScore?.value ?: 0.0
            
            val isMisuse = toxicityScore > 0.7 || severeToxicityScore > 0.5 || threatScore > 0.6
            
            PromptAnalysisResponse(
                isMisuse = isMisuse,
                confidence = max(toxicityScore, max(severeToxicityScore, threatScore)),
                reason = "Perspective API: ${if (isMisuse) "Detected toxic or threatening content" else "Content appears safe"}",
                category = if (isMisuse) MisuseCategory.HARMFUL_CONTENT else MisuseCategory.NONE,
                suggestions = if (isMisuse) listOf(
                    "Consider using more respectful language",
                    "Avoid harmful or threatening content",
                    "Follow community guidelines"
                ) else emptyList(),
                metadata = mapOf(
                    "perspective_toxicity" to toxicityScore.toString(),
                    "perspective_severe_toxicity" to severeToxicityScore.toString(),
                    "perspective_threat" to threatScore.toString()
                )
            )
            
        } catch (e: Exception) {
            android.util.Log.e("NeuroGate-RealAI", "Perspective analysis failed", e)
            fallbackAnalysis(prompt)
        }
    }
    
    /**
     * Analyze using Azure Content Moderator or similar service
     */
    private suspend fun analyzeWithContentModeration(prompt: String): PromptAnalysisResponse {
        return try {
            // Check if Azure API key is available
            val azureApiKey = com.example.neurogate.BuildConfig.AZURE_CONTENT_MODERATOR_KEY
            if (azureApiKey == "your-azure-key-here") {
                android.util.Log.w("NeuroGate-RealAI", "Azure Content Moderator key not configured, using fallback analysis")
                return fallbackAnalysis(prompt)
            }
            
            // Simulate Azure Content Moderator analysis
            val request = ContentModerationRequest(text = prompt)
            
            val response = contentModerationClient.create(ContentModerationService::class.java)
                .moderateContent(request)
            
            val isMisuse = response.terms?.isNotEmpty() == true || 
                          response.pII?.isNotEmpty() == true ||
                          response.classification?.reviewRecommended == true
            
            PromptAnalysisResponse(
                isMisuse = isMisuse,
                confidence = response.classification?.reviewRecommended?.let { if (it) 0.8 else 0.1 } ?: 0.0,
                reason = "Content Moderation: ${if (isMisuse) "Content requires review" else "Content appears appropriate"}",
                category = if (isMisuse) MisuseCategory.HARMFUL_CONTENT else MisuseCategory.NONE,
                suggestions = if (isMisuse) listOf(
                    "Content may violate community guidelines",
                    "Consider revising your prompt",
                    "Ensure content is appropriate for all audiences"
                ) else emptyList(),
                metadata = mapOf(
                    "moderation_terms" to (response.terms?.size?.toString() ?: "0"),
                    "moderation_pii" to (response.pII?.size?.toString() ?: "0"),
                    "moderation_review_recommended" to (response.classification?.reviewRecommended?.toString() ?: "false")
                )
            )
            
        } catch (e: Exception) {
            android.util.Log.e("NeuroGate-RealAI", "Content moderation analysis failed", e)
            fallbackAnalysis(prompt)
        }
    }
    
    /**
     * Fallback analysis using improved local rules with comprehensive deepfake detection
     */
    private suspend fun fallbackAnalysis(prompt: String): PromptAnalysisResponse {
        val lowerPrompt = prompt.lowercase()
        
        // Comprehensive deepfake and face manipulation patterns with variations
        val deepfakePatterns = listOf(
            // Direct deepfake terms
            "deepfake", "deep fake", "deep-fake", "deepfak", "deepfke", "deepfae",
            "face swap", "faceswap", "face-swap", "face swp", "face swaping",
            "face replacement", "face replacment", "face replac", "face replce",
            
            // Face manipulation actions (require context)
            "replace my face", "replac my face", "replace my fas", "replce my face",
            "swap my face", "swp my face", "swap my fas", "swaping my face",
            "put my face", "put my fas", "put my face on", "put my fas on",
            "make me look like", "make me lok like", "make me look lik",
            "transform me into", "transform me in", "transform me int",
            "change my face", "change my fas", "chng my face", "chage my face",
            "face morph", "face morf", "face morphing", "face morfing",
            "face edit", "face edt", "face editing", "face edting",
            "face blend", "face blnd", "face blending", "face blnding",
            "face overlay", "face overla", "face overlaying", "face overlaing",
            "face superimpose", "face superimpse", "face superposing",
            "face merge", "face mrg", "face merging", "face mrging",
            "face composite", "face composit", "face compositing",
            "face manipulation", "face maniplation", "face manpulation",
            
            // Video manipulation
            "fake video", "fak video", "fake vid", "fak vid",
            "deepfake video", "deepfak video", "deep fake vid",
            "face swap video", "faceswap vid", "face swp vid",
            "video manipulation", "vid manipulation", "video maniplation",
            "video editing", "vid editing", "video edting",
            
            // Impersonation terms
            "impersonate", "impersonat", "impersnate", "impersont",
            "pretend to be", "pretend to b", "pretnd to be",
            "act as", "act as if", "act like", "act lik",
            "look like", "lok like", "look lik", "lok lik",
            "appear as", "appear like", "appear lik",
            
            // Celebrity and public figure patterns
            "tom cruise", "tom cruse", "tom cruse", "tom cruse",
            "brad pitt", "brad pit", "brad ptt", "brad ptt",
            "angelina jolie", "angelina joli", "angelina jlie",
            "leonardo dicaprio", "leonardo dicapri", "leonardo dicapri",
            "robert downey", "robert dwny", "robert doney",
            "scarlett johansson", "scarlett johansn", "scarlett johansn",
            "ryan reynolds", "ryan reynlds", "ryan reynlds",
            "jennifer lawrence", "jennifer lawrenc", "jennifer lawrenc",
            "will smith", "will smth", "will smt",
            "dwayne johnson", "dwayne jhnsn", "dwayne jhnsn",
            "chris evans", "chris evns", "chris evns",
            "margot robbie", "margot robb", "margot robb",
            "celebrity", "celebrty", "celebrty", "celbrity",
            "famous person", "famous persn", "famous prsn",
            "movie star", "movie str", "movie str",
            "actor", "actr", "actr",
            "actress", "actres", "actres",
            "hollywood", "hollywod", "hollywod",
            "famous", "famos", "famos",
            "public figure", "public figr", "public figr",
            
            // Context variations (require additional context)
            "make me", "make me look", "make me appear",
            "turn me into", "turn me in", "turn me int",
            "become", "becom", "becom",
            "transform into", "transform in", "transform int",
            "change into", "change in", "change int",
            "switch with", "switch wth", "switch wth",
            "replace with", "replac wth", "replace wth",
            "swap with", "swp wth", "swap wth",
            
            // Intent indicators (require additional context)
            "i want to", "i want t", "i want t",
            "can you make", "can u make", "can you mak",
            "help me", "help me", "help me",
            "show me how", "show me hw", "show me hw",
            "teach me", "teach me", "teach me",
            "give me", "giv me", "giv me",
            "create for me", "create for me", "create for me",
            "generate for me", "generate for me", "generate for me"
        )
        
        // Check for deepfake patterns with context requirements
        val hasDeepfakePattern = checkDeepfakeContext(lowerPrompt, deepfakePatterns)
        
        // Check for celebrity patterns
        val celebrityPatterns = listOf(
            "tom cruise", "brad pitt", "angelina jolie", "leonardo dicaprio",
            "robert downey", "scarlett johansson", "ryan reynolds", "jennifer lawrence",
            "will smith", "dwayne johnson", "chris evans", "margot robbie",
            "celebrity", "famous person", "movie star", "actor", "actress", "hollywood"
        )
        
        val hasCelebrityPattern = celebrityPatterns.any { pattern ->
            lowerPrompt.contains(pattern) || 
            calculateFuzzySimilarity(lowerPrompt, pattern) > 0.7
        }
        
        // Check for harmful patterns (for completeness)
        val harmfulPatterns = listOf(
            "nude", "naked", "explicit", "porn", "sexual", "violence", "blood",
            "gore", "weapon", "gun", "bomb", "terrorist", "hate speech",
            "kill", "murder", "suicide", "self harm"
        )
        
        val hasHarmfulPattern = harmfulPatterns.any { lowerPrompt.contains(it) }
        
        // Determine category and confidence based on patterns
        return when {
            hasDeepfakePattern && hasCelebrityPattern -> {
                PromptAnalysisResponse(
                    isMisuse = true,
                    confidence = 0.95,
                    reason = "Detected potential deepfake/celebrity impersonation request with high confidence",
                    category = MisuseCategory.CELEBRITY_IMPERSONATION,
                    suggestions = listOf(
                        "Avoid impersonating celebrities without consent",
                        "Create original characters instead",
                        "Consider the legal and ethical implications",
                        "Respect privacy and personal rights"
                    )
                )
            }
            hasDeepfakePattern -> {
                PromptAnalysisResponse(
                    isMisuse = true,
                    confidence = 0.9,
                    reason = "Detected potential deepfake or face manipulation request",
                    category = MisuseCategory.DEEPFAKE,
                    suggestions = listOf(
                        "Consider using original photos or artwork",
                        "Respect privacy and consent",
                        "Explore ethical creative alternatives",
                        "Avoid manipulating real people's images"
                    )
                )
            }
            hasHarmfulPattern -> {
                PromptAnalysisResponse(
                    isMisuse = true,
                    confidence = 0.95,
                    reason = "Detected potentially harmful content",
                    category = MisuseCategory.HARMFUL_CONTENT,
                    suggestions = listOf(
                        "Avoid generating harmful or explicit content",
                        "Consider the impact on others",
                        "Follow community guidelines"
                    )
                )
            }
            else -> {
                PromptAnalysisResponse(
                    isMisuse = false,
                    confidence = 0.9,
                    reason = "Content appears safe for AI generation",
                    category = MisuseCategory.NONE,
                    suggestions = listOf(
                        "Your prompt appears to be safe",
                        "Continue exploring creative possibilities",
                        "Consider adding more specific details"
                    )
                )
            }
        }
    }
    
    /**
     * Check for deepfake context with multiple requirements
     */
    private fun checkDeepfakeContext(prompt: String, patterns: List<String>): Boolean {
        val words = prompt.split(" ")
        
        // High-confidence patterns (direct matches)
        val highConfidencePatterns = listOf(
            "deepfake", "deep fake", "deep-fake", "deepfak", "deepfke", "deepfae",
            "face swap", "faceswap", "face-swap", "face swp", "face swaping",
            "face replacement", "face replacment", "face replac", "face replce",
            "replace my face", "replac my face", "replace my fas", "replce my face",
            "swap my face", "swp my face", "swap my fas", "swaping my face",
            "put my face", "put my fas", "put my face on", "put my fas on",
            "make me look like", "make me lok like", "make me look lik",
            "transform me into", "transform me in", "transform me int",
            "change my face", "change my fas", "chng my face", "chage my face",
            "impersonate", "impersonat", "impersnate", "impersont",
            "pretend to be", "pretend to b", "pretnd to be"
        )
        
        // Check for high-confidence patterns first
        if (highConfidencePatterns.any { prompt.contains(it) }) {
            return true
        }
        
        // Medium-confidence patterns (require context)
        val mediumConfidencePatterns = listOf(
            "face morph", "face morf", "face morphing", "face morfing",
            "face edit", "face edt", "face editing", "face edting",
            "face blend", "face blnd", "face blending", "face blnding",
            "face overlay", "face overla", "face overlaying", "face overlaing",
            "face superimpose", "face superimpse", "face superposing",
            "face merge", "face mrg", "face merging", "face mrging",
            "face composite", "face composit", "face compositing",
            "face manipulation", "face maniplation", "face manpulation",
            "fake video", "fak video", "fake vid", "fak vid",
            "deepfake video", "deepfak video", "deep fake vid",
            "face swap video", "faceswap vid", "face swp vid"
        )
        
        // Check for medium-confidence patterns
        if (mediumConfidencePatterns.any { prompt.contains(it) }) {
            return true
        }
        
        // Low-confidence patterns (require multiple keywords or context)
        val lowConfidenceKeywords = listOf(
            "replace", "replac", "replce",
            "swap", "swp", "swaping",
            "transform", "transform",
            "change", "chng", "chage",
            "make", "mak",
            "turn", "turn",
            "become", "becom",
            "act", "act",
            "look", "lok",
            "appear", "appear"
        )
        
        // Count how many low-confidence keywords are present
        val keywordCount = lowConfidenceKeywords.count { keyword ->
            words.any { word -> 
                word.contains(keyword) || calculateLevenshteinSimilarity(word, keyword) > 0.7
            }
        }
        
        // Require at least 2 keywords or specific context
        if (keywordCount >= 2) {
            return true
        }
        
        // Check for specific context patterns
        val contextPatterns = listOf(
            "with", "wth",
            "into", "in", "int",
            "like", "lik",
            "as", "as",
            "to", "to"
        )
        
        // If we have at least one keyword and context, it might be suspicious
        if (keywordCount >= 1 && contextPatterns.any { prompt.contains(it) }) {
            // Additional check: look for personal pronouns or intent words
            val intentWords = listOf("me", "my", "i", "myself", "want", "want", "help", "show", "teach", "give")
            if (intentWords.any { prompt.contains(it) }) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Calculate fuzzy similarity between two strings
     */
    private fun calculateFuzzySimilarity(text: String, pattern: String): Double {
        val words = text.split(" ")
        val patternWords = pattern.split(" ")
        
        var maxSimilarity = 0.0
        
        for (word in words) {
            for (patternWord in patternWords) {
                val similarity = calculateLevenshteinSimilarity(word, patternWord)
                maxSimilarity = maxOf(maxSimilarity, similarity)
            }
        }
        
        return maxSimilarity
    }
    
    /**
     * Calculate Levenshtein distance similarity
     */
    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        return if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)
    }
    
    /**
     * Calculate Levenshtein distance
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Check for contextual matches even with different grammar
     */
    private fun checkContextualMatch(text: String, pattern: String): Boolean {
        val textWords = text.split(" ").toSet()
        val patternWords = pattern.split(" ").toSet()
        
        // Check if key words from pattern exist in text
        val keyWords = patternWords.filter { it.length > 3 } // Focus on meaningful words
        val matches = keyWords.count { keyWord ->
            textWords.any { textWord ->
                calculateLevenshteinSimilarity(textWord, keyWord) > 0.6
            }
        }
        
        // If more than 50% of key words match, consider it a contextual match
        return keyWords.isNotEmpty() && (matches.toDouble() / keyWords.size) > 0.5
    }
    
    /**
     * Ensemble method to combine results from multiple AI services
     */
    private fun ensembleResults(
        openAIResult: PromptAnalysisResponse,
        perspectiveResult: PromptAnalysisResponse,
        contentModerationResult: PromptAnalysisResponse
    ): PromptAnalysisResponse {
        
        // Weight the results (OpenAI is most reliable)
        val openAIWeight = 0.5
        val perspectiveWeight = 0.3
        val contentModerationWeight = 0.2
        
        val weightedScore = (
            (if (openAIResult.isMisuse) openAIResult.confidence else 0.0) * openAIWeight +
            (if (perspectiveResult.isMisuse) perspectiveResult.confidence else 0.0) * perspectiveWeight +
            (if (contentModerationResult.isMisuse) contentModerationResult.confidence else 0.0) * contentModerationWeight
        )
        
        val isMisuse = weightedScore > 0.6 // Higher threshold for more accuracy
        
        // Choose the most confident category
        val finalCategory = when {
            openAIResult.isMisuse -> openAIResult.category
            perspectiveResult.isMisuse -> perspectiveResult.category
            contentModerationResult.isMisuse -> contentModerationResult.category
            else -> MisuseCategory.NONE
        }
        
        return PromptAnalysisResponse(
            isMisuse = isMisuse,
            confidence = weightedScore,
            reason = generateEnsembleReason(openAIResult, perspectiveResult, contentModerationResult, isMisuse),
            category = finalCategory,
            suggestions = generateSuggestions(finalCategory, weightedScore),
            metadata = openAIResult.metadata + perspectiveResult.metadata + contentModerationResult.metadata
        )
    }
    
    private fun generateEnsembleReason(
        openAI: PromptAnalysisResponse,
        perspective: PromptAnalysisResponse,
        contentModeration: PromptAnalysisResponse,
        isMisuse: Boolean
    ): String {
        return if (isMisuse) {
            "Multiple AI services detected potential misuse. Primary concerns: " +
            when {
                openAI.isMisuse -> "Content moderation flags"
                perspective.isMisuse -> "Toxicity detection"
                contentModeration.isMisuse -> "Policy violation"
                else -> "Combined analysis"
            }
        } else {
            "Content appears safe across multiple AI analysis services"
        }
    }
    
    private fun generateSuggestions(category: MisuseCategory, confidence: Double): List<String> {
        return when (category) {
            MisuseCategory.DEEPFAKE -> listOf(
                "Consider using original photos or artwork instead",
                "Respect people's privacy and consent",
                "Explore ethical creative alternatives"
            )
            MisuseCategory.CELEBRITY_IMPERSONATION -> listOf(
                "Avoid impersonating real people without consent",
                "Create original characters instead",
                "Consider the legal and ethical implications"
            )
            MisuseCategory.HARMFUL_CONTENT -> listOf(
                "Avoid generating harmful or explicit content",
                "Consider the impact on others",
                "Follow community guidelines and laws"
            )
            MisuseCategory.COPYRIGHT_VIOLATION -> listOf(
                "Avoid using copyrighted characters or brands",
                "Create original content instead",
                "Respect intellectual property rights"
            )
            MisuseCategory.PRIVACY_VIOLATION -> listOf(
                "Respect privacy and personal boundaries",
                "Obtain proper consent before using someone's likeness",
                "Consider the ethical implications of your request"
            )
            MisuseCategory.NONE -> listOf(
                "Your prompt appears to be safe for AI generation",
                "Continue exploring creative possibilities",
                "Consider adding more specific details to your prompt"
            )
        }
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
    
    private fun createPerspectiveClient(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        
        return Retrofit.Builder()
            .baseUrl("https://commentanalyzer.googleapis.com/v1alpha1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private fun createContentModerationClient(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        
        return Retrofit.Builder()
            .baseUrl("https://api.cognitive.microsoft.com/contentmoderator/moderate/v1.0/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

// OpenAI API Models
interface OpenAIService {
    @POST("moderations")
    suspend fun moderateContent(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): OpenAIResponse
}

data class OpenAIRequest(
    val input: String,
    val model: String
)

data class OpenAIResponse(
    val id: String,
    val model: String,
    val results: List<OpenAIResult>
)

data class OpenAIResult(
    val flagged: Boolean,
    val categories: OpenAICategories,
    val categoryScores: OpenAICategoryScores
)

data class OpenAICategories(
    val harassment: Boolean = false,
    val hate: Boolean = false,
    val selfHarm: Boolean = false,
    val sexual: Boolean = false,
    val sexualMinors: Boolean = false,
    val violence: Boolean = false,
    val violenceGraphic: Boolean = false
)

data class OpenAICategoryScores(
    val harassment: Double? = null,
    val hate: Double? = null,
    val selfHarm: Double? = null,
    val sexual: Double? = null,
    val sexualMinors: Double? = null,
    val violence: Double? = null,
    val violenceGraphic: Double? = null
)

// Perspective API Models
interface PerspectiveService {
    @POST("comments:analyze")
    suspend fun analyzeText(@Body request: PerspectiveRequest): PerspectiveResponse
}

data class PerspectiveRequest(
    val comment: PerspectiveComment,
    val languages: List<String>,
    val requestedAttributes: Map<String, PerspectiveAttribute>
)

data class PerspectiveComment(
    val text: String
)

data class PerspectiveAttribute(
    val scoreType: String = "PROBABILITY",
    val scoreThreshold: Double = 0.5
)

data class PerspectiveResponse(
    val attributeScores: Map<String, PerspectiveScore>?
)

data class PerspectiveScore(
    val summaryScore: PerspectiveSummaryScore?
)

data class PerspectiveSummaryScore(
    val value: Double
)

// Content Moderation API Models
interface ContentModerationService {
    @POST("ProcessText/Screen")
    suspend fun moderateContent(@Body request: ContentModerationRequest): ContentModerationResponse
}

data class ContentModerationRequest(
    val text: String
)

data class ContentModerationResponse(
    val terms: List<String>? = null,
    val pII: List<String>? = null,
    val classification: ContentClassification? = null
)

data class ContentClassification(
    val reviewRecommended: Boolean? = null
)

// Mock Service with LLM simulation for development/testing
class MockAIService : AIService {
    override suspend fun analyzePrompt(
        apiKey: String,
        request: PromptAnalysisRequest
    ): PromptAnalysisResponse {
        // Simulate LLM processing time
        kotlinx.coroutines.delay(200)
        
        val prompt = request.prompt.lowercase()
        
        android.util.Log.d("NeuroGate-Mock", "Simulating LLM analysis for: $prompt")
        
        // Simulate intelligent context understanding
        return simulateLLMAnalysis(prompt)
    }
    
    /**
     * Simulate LLM-style context understanding
     */
    private fun simulateLLMAnalysis(prompt: String): PromptAnalysisResponse {
        // Comprehensive deepfake and face manipulation patterns with variations
        val deepfakePatterns = listOf(
            // Direct deepfake terms
            "deepfake", "deep fake", "deep-fake", "deepfak", "deepfke", "deepfae",
            "face swap", "faceswap", "face-swap", "face swp", "face swaping",
            "face replacement", "face replacment", "face replac", "face replce",
            
            // Face manipulation actions
            "replace my face", "replac my face", "replace my fas", "replce my face",
            "swap my face", "swp my face", "swap my fas", "swaping my face",
            "put my face", "put my fas", "put my face on", "put my fas on",
            "make me look like", "make me lok like", "make me look lik",
            "transform me into", "transform me in", "transform me int",
            "change my face", "change my fas", "chng my face", "chage my face",
            "face morph", "face morf", "face morphing", "face morfing",
            "face edit", "face edt", "face editing", "face edting",
            "face blend", "face blnd", "face blending", "face blnding",
            "face overlay", "face overla", "face overlaying", "face overlaing",
            "face superimpose", "face superimpse", "face superposing",
            "face merge", "face mrg", "face merging", "face mrging",
            "face composite", "face composit", "face compositing",
            "face manipulation", "face maniplation", "face manpulation",
            
            // Video manipulation
            "fake video", "fak video", "fake vid", "fak vid",
            "deepfake video", "deepfak video", "deep fake vid",
            "face swap video", "faceswap vid", "face swp vid",
            "video manipulation", "vid manipulation", "video maniplation",
            "video editing", "vid editing", "video edting",

            // Impersonation terms
            "impersonate", "impersonat", "impersnate", "impersont",
            "pretend to be", "pretend to b", "pretnd to be",
            "act as", "act as if", "act like", "act lik",
            "look like", "lok like", "look lik", "lok lik",
            "appear as", "appear like", "appear lik",
            
            // Celebrity and public figure patterns
            "tom cruise", "tom cruse", "tom cruse", "tom cruse",
            "brad pitt", "brad pit", "brad ptt", "brad ptt",
            "angelina jolie", "angelina joli", "angelina jlie",
            "leonardo dicaprio", "leonardo dicapri", "leonardo dicapri",
            "robert downey", "robert dwny", "robert doney",
            "scarlett johansson", "scarlett johansn", "scarlett johansn",
            "ryan reynolds", "ryan reynlds", "ryan reynlds",
            "jennifer lawrence", "jennifer lawrenc", "jennifer lawrenc",
            "will smith", "will smth", "will smt",
            "dwayne johnson", "dwayne jhnsn", "dwayne jhnsn",
            "chris evans", "chris evns", "chris evns",
            "margot robbie", "margot robb", "margot robb",
            "celebrity", "celebrty", "celebrty", "celbrity",
            "famous person", "famous persn", "famous prsn",
            "movie star", "movie str", "movie str",
            "actor", "actr", "actr",
            "actress", "actres", "actres",
            "hollywood", "hollywod", "hollywod",
            "famous", "famos", "famos",
            "public figure", "public figr", "public figr",
            
            // Context variations
            "make me", "make me look", "make me appear",
            "turn me into", "turn me in", "turn me int",
            "become", "becom", "becom",
            "transform into", "transform in", "transform int",
            "change into", "change in", "change int",
            "switch with", "switch wth", "switch wth",
            "replace with", "replac wth", "replace wth",
            "swap with", "swp wth", "swap wth",
            
            // Intent indicators
            "i want to", "i want t", "i want t",
            "can you make", "can u make", "can you mak",
            "help me", "help me", "help me",
            "show me how", "show me hw", "show me hw",
            "teach me", "teach me", "teach me",
            "give me", "giv me", "giv me",
            "create for me", "create for me", "create for me",
            "generate for me", "generate for me", "generate for me"
        )
        
        val celebrityPatterns = listOf(
            "tom cruise", "brad pitt", "angelina jolie", "leonardo dicaprio",
            "robert downey", "scarlett johansson", "ryan reynolds", "jennifer lawrence",
            "will smith", "dwayne johnson", "chris evans", "margot robbie",
            "celebrity", "famous person", "movie star", "actor", "actress", "hollywood"
        )
        
        val harmfulPatterns = listOf(
            "nude", "naked", "explicit", "porn", "sexual", "violence", "blood",
            "gore", "weapon", "gun", "bomb", "terrorist", "hate speech",
            "kill", "murder", "suicide", "self harm"
        )
        
        // Check for patterns with fuzzy matching
        val hasDeepfakePattern = checkDeepfakeContext(prompt, deepfakePatterns)
        
        val hasCelebrityPattern = celebrityPatterns.any { pattern ->
            prompt.contains(pattern) || 
            calculateFuzzySimilarity(prompt, pattern) > 0.7
        }
        
        val hasHarmfulPattern = harmfulPatterns.any { prompt.contains(it) }
        
        return when {
            hasDeepfakePattern && hasCelebrityPattern -> {
                PromptAnalysisResponse(
                    isMisuse = true,
                    confidence = 0.95,
                    reason = "Detected potential deepfake/celebrity impersonation request with high confidence",
                    category = MisuseCategory.CELEBRITY_IMPERSONATION,
                    suggestions = listOf(
                        "Avoid impersonating celebrities without consent",
                        "Create original characters instead",
                        "Consider the legal and ethical implications",
                        "Respect privacy and personal rights"
                    )
                )
            }
            hasDeepfakePattern -> {
                PromptAnalysisResponse(
                    isMisuse = true,
                    confidence = 0.9,
                    reason = "Detected potential deepfake or face manipulation request",
                    category = MisuseCategory.DEEPFAKE,
                    suggestions = listOf(
                        "Consider using original photos or artwork",
                        "Respect privacy and consent",
                        "Explore ethical creative alternatives",
                        "Avoid manipulating real people's images"
                    )
                )
            }
            hasHarmfulPattern -> {
                PromptAnalysisResponse(
                    isMisuse = true,
                    confidence = 0.95,
                    reason = "Detected potentially harmful content",
                    category = MisuseCategory.HARMFUL_CONTENT,
                    suggestions = listOf(
                        "Avoid generating harmful or explicit content",
                        "Consider the impact on others",
                        "Follow community guidelines"
                    )
                )
            }
            else -> {
                PromptAnalysisResponse(
                    isMisuse = false,
                    confidence = 0.9,
                    reason = "Content appears safe for AI generation",
                    category = MisuseCategory.NONE,
                    suggestions = listOf(
                        "Your prompt appears to be safe",
                        "Continue exploring creative possibilities",
                        "Consider adding more specific details"
                    )
                )
            }
        }
    }
    
    /**
     * Check for deepfake context with intelligent pattern matching
     */
    private fun checkDeepfakeContext(prompt: String, patterns: List<String>): Boolean {
        val words = prompt.lowercase().split(" ")
        
        // High-confidence patterns (direct matches)
        val highConfidencePatterns = listOf(
            "deepfake", "deep fake", "deep-fake", "deepfak", "deepfke", "deepfae",
            "face swap", "faceswap", "face-swap", "face swp", "face swaping",
            "face replacement", "face replacment", "face replac", "face replce",
            "replace my face", "replac my face", "replace my fas", "replce my face",
            "swap my face", "swp my face", "swap my fas", "swaping my face",
            "put my face", "put my fas", "put my face on", "put my fas on",
            "make me look like", "make me lok like", "make me look lik",
            "transform me into", "transform me in", "transform me int",
            "change my face", "change my fas", "chng my face", "chage my face",
            "impersonate", "impersonat", "impersnate", "impersont",
            "pretend to be", "pretend to b", "pretnd to be"
        )
        
        // Check for high-confidence patterns first
        if (highConfidencePatterns.any { prompt.lowercase().contains(it) }) {
            return true
        }
        
        // Medium-confidence patterns (require context)
        val mediumConfidencePatterns = listOf(
            "face morph", "face morf", "face morphing", "face morfing",
            "face edit", "face editing", "face manipulation", "face manipulat",
            "face change", "face chang", "face alter", "face alterat",
            "face transform", "face transformat", "face modify", "face modificat"
        )
        
        // Low-confidence keywords (require multiple or context)
        val lowConfidenceKeywords = listOf(
            "replace", "replac", "replce", "swap", "swp", "swaping",
            "change", "chang", "chng", "alter", "alt", "modify", "modif"
        )
        
        // Check for medium-confidence patterns
        if (mediumConfidencePatterns.any { prompt.lowercase().contains(it) }) {
            return true
        }
        
        // Check for low-confidence keywords with context requirements
        val lowConfidenceMatches = lowConfidenceKeywords.count { keyword ->
            prompt.lowercase().contains(keyword)
        }
        
        val contextWords = listOf("with", "into", "like", "as", "me", "my", "i", "want", "help", "make")
        val hasContext = contextWords.any { prompt.lowercase().contains(it) }
        
        // Require at least 2 low-confidence keywords OR 1 keyword with context
        return lowConfidenceMatches >= 2 || (lowConfidenceMatches >= 1 && hasContext)
    }
    
    /**
     * Calculate fuzzy similarity between two strings
     */
    private fun calculateFuzzySimilarity(text: String, pattern: String): Double {
        val words = text.split(" ")
        val patternWords = pattern.split(" ")
        
        var maxSimilarity = 0.0
        
        for (word in words) {
            for (patternWord in patternWords) {
                val similarity = calculateLevenshteinSimilarity(word, patternWord)
                maxSimilarity = maxOf(maxSimilarity, similarity)
            }
        }
        
        return maxSimilarity
    }
    
    /**
     * Calculate Levenshtein distance similarity
     */
    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        return if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)
    }
    
    /**
     * Calculate Levenshtein distance
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Check for contextual matches even with different grammar
     */
    private fun checkContextualMatch(text: String, pattern: String): Boolean {
        val textWords = text.split(" ").toSet()
        val patternWords = pattern.split(" ").toSet()
        
        // Check if key words from pattern exist in text
        val keyWords = patternWords.filter { it.length > 3 } // Focus on meaningful words
        val matches = keyWords.count { keyWord ->
            textWords.any { textWord ->
                calculateLevenshteinSimilarity(textWord, keyWord) > 0.6
            }
        }
        
        // If more than 50% of key words match, consider it a contextual match
        return keyWords.isNotEmpty() && (matches.toDouble() / keyWords.size) > 0.5
    }
}
