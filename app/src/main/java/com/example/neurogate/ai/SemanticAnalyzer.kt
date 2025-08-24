package com.example.neurogate.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.*

/**
 * Advanced Semantic Analyzer for AI Misuse Detection
 * Uses semantic similarity, fuzzy matching, and context analysis
 */
class SemanticAnalyzer(private val context: Context) {
    
    // Pre-computed word embeddings for common misuse patterns
    private val misuseEmbeddings = mutableMapOf<String, FloatArray>()
    private val safeEmbeddings = mutableMapOf<String, FloatArray>()
    
    // Fuzzy matching threshold
    private val fuzzyThreshold = 0.75
    
    // Semantic similarity threshold
    private val semanticThreshold = 0.65
    
    init {
        initializeEmbeddings()
    }
    
    /**
     * Advanced semantic analysis with multiple detection methods
     */
    suspend fun analyzePrompt(prompt: String): SemanticAnalysisResult = withContext(Dispatchers.Default) {
        val normalizedPrompt = normalizeText(prompt)
        
        // Multi-layered analysis
        val fuzzyScore = calculateFuzzyMisuseScore(normalizedPrompt)
        val semanticScore = calculateSemanticMisuseScore(normalizedPrompt)
        val contextScore = calculateContextualRisk(normalizedPrompt)
        val intentScore = calculateIntentAnalysis(normalizedPrompt)
        
        // Weighted ensemble scoring
        val finalScore = (
            fuzzyScore * 0.25 +
            semanticScore * 0.35 +
            contextScore * 0.25 +
            intentScore * 0.15
        )
        
        val category = determineCategory(normalizedPrompt, finalScore)
        val confidence = calculateConfidence(fuzzyScore, semanticScore, contextScore, intentScore)
        
        android.util.Log.d("NeuroGate-AI", """
            Semantic Analysis Results:
            Prompt: $prompt
            Fuzzy Score: $fuzzyScore
            Semantic Score: $semanticScore
            Context Score: $contextScore
            Intent Score: $intentScore
            Final Score: $finalScore
            Category: $category
            Confidence: $confidence
        """.trimIndent())
        
        SemanticAnalysisResult(
            isMisuse = finalScore > 0.5,
            confidence = confidence,
            misuseScore = finalScore,
            category = category,
            detectionMethod = getDetectionMethod(fuzzyScore, semanticScore, contextScore),
            explanation = generateExplanation(normalizedPrompt, finalScore, category)
        )
    }
    
    /**
     * Fuzzy matching with Levenshtein distance and phonetic similarity
     */
    private fun calculateFuzzyMisuseScore(prompt: String): Double {
        val words = prompt.split(" ")
        var maxScore = 0.0
        
        // Deepfake and face manipulation patterns
        val deepfakePatterns = listOf(
            "deepfake", "deep fake", "face swap", "faceswap", "replace face",
            "swap face", "put my face", "celebrity face", "face replacement",
            "face morph", "face change", "face transform", "face edit"
        )
        
        // Celebrity and public figure patterns
        val celebrityPatterns = listOf(
            "tom cruise", "brad pitt", "angelina jolie", "leonardo dicaprio",
            "robert downey", "scarlett johansson", "ryan reynolds", "jennifer lawrence",
            "will smith", "dwayne johnson", "chris evans", "margot robbie",
            "celebrity", "famous person", "movie star", "actor", "actress"
        )
        
        // Action patterns
        val actionPatterns = listOf(
            "replace with", "swap with", "make me look like", "transform into",
            "put on", "overlay", "superimpose", "blend with", "merge with"
        )
        
        val allPatterns = deepfakePatterns + celebrityPatterns + actionPatterns
        
        for (pattern in allPatterns) {
            for (word in words) {
                val similarity = calculateLevenshteinSimilarity(word, pattern)
                val phoneticSimilarity = calculatePhoneticSimilarity(word, pattern)
                val combinedSimilarity = max(similarity, phoneticSimilarity)
                
                if (combinedSimilarity > fuzzyThreshold) {
                    maxScore = max(maxScore, combinedSimilarity)
                }
            }
            
            // Check for phrase-level similarity
            val phraseSimilarity = calculatePhraseSimilarity(prompt, pattern)
            if (phraseSimilarity > fuzzyThreshold) {
                maxScore = max(maxScore, phraseSimilarity)
            }
        }
        
        return maxScore
    }
    
    /**
     * Semantic similarity using word embeddings
     */
    private fun calculateSemanticMisuseScore(prompt: String): Double {
        val promptEmbedding = generateTextEmbedding(prompt)
        
        var maxMisuseScore = 0.0
        var maxSafeScore = 0.0
        
        // Calculate similarity with known misuse patterns
        for ((_, embedding) in misuseEmbeddings) {
            val similarity = cosineSimilarity(promptEmbedding, embedding)
            maxMisuseScore = max(maxMisuseScore, similarity)
        }
        
        // Calculate similarity with safe patterns
        for ((_, embedding) in safeEmbeddings) {
            val similarity = cosineSimilarity(promptEmbedding, embedding)
            maxSafeScore = max(maxSafeScore, similarity)
        }
        
        // Return relative misuse score
        return if (maxMisuseScore + maxSafeScore > 0) {
            maxMisuseScore / (maxMisuseScore + maxSafeScore)
        } else {
            0.0
        }
    }
    
    /**
     * Contextual risk analysis
     */
    private fun calculateContextualRisk(prompt: String): Double {
        var riskScore = 0.0
        
        // High-risk contexts
        val highRiskContexts = listOf(
            "social media", "profile picture", "dating app", "fake video",
            "prank", "revenge", "harassment", "impersonation", "fraud"
        )
        
        // Medium-risk contexts  
        val mediumRiskContexts = listOf(
            "entertainment", "movie", "fun", "joke", "meme", "art project"
        )
        
        // Low-risk contexts
        val lowRiskContexts = listOf(
            "education", "research", "analysis", "detection", "awareness",
            "study", "academic", "learning", "safety"
        )
        
        for (context in highRiskContexts) {
            if (calculatePhraseSimilarity(prompt, context) > 0.7) {
                riskScore += 0.8
            }
        }
        
        for (context in mediumRiskContexts) {
            if (calculatePhraseSimilarity(prompt, context) > 0.7) {
                riskScore += 0.5
            }
        }
        
        for (context in lowRiskContexts) {
            if (calculatePhraseSimilarity(prompt, context) > 0.7) {
                riskScore -= 0.3
            }
        }
        
        return max(0.0, min(1.0, riskScore))
    }
    
    /**
     * Intent analysis using linguistic patterns
     */
    private fun calculateIntentAnalysis(prompt: String): Double {
        var intentScore = 0.0
        
        // Imperative/command patterns (higher risk)
        val imperativePatterns = listOf("make", "create", "generate", "put", "replace", "swap")
        
        // Question patterns (lower risk)
        val questionWords = listOf("how", "what", "why", "when", "where", "can", "should")
        
        // Personal pronouns indicating self-modification
        val personalPronouns = listOf("my", "me", "i", "myself")
        
        val words = prompt.lowercase().split(" ")
        
        // Check for imperative mood
        if (words.any { it in imperativePatterns }) {
            intentScore += 0.6
        }
        
        // Check for questions (usually safer)
        if (words.any { it in questionWords }) {
            intentScore -= 0.3
        }
        
        // Check for personal modification intent
        if (words.any { it in personalPronouns }) {
            intentScore += 0.4
        }
        
        return max(0.0, min(1.0, intentScore))
    }
    
    private fun calculateLevenshteinSimilarity(s1: String, s2: String): Double {
        val distance = levenshteinDistance(s1.lowercase(), s2.lowercase())
        val maxLen = max(s1.length, s2.length)
        return if (maxLen == 0) 1.0 else 1.0 - (distance.toDouble() / maxLen)
    }
    
    private fun calculatePhoneticSimilarity(s1: String, s2: String): Double {
        // Simple phonetic similarity based on soundex-like algorithm
        val phonetic1 = generatePhoneticCode(s1)
        val phonetic2 = generatePhoneticCode(s2)
        return if (phonetic1 == phonetic2) 0.8 else 0.0
    }
    
    private fun calculatePhraseSimilarity(phrase: String, pattern: String): Double {
        val phraseWords = phrase.lowercase().split(" ")
        val patternWords = pattern.lowercase().split(" ")
        
        var totalSimilarity = 0.0
        var matches = 0
        
        for (patternWord in patternWords) {
            var bestMatch = 0.0
            for (phraseWord in phraseWords) {
                val similarity = calculateLevenshteinSimilarity(phraseWord, patternWord)
                bestMatch = max(bestMatch, similarity)
            }
            if (bestMatch > 0.7) {
                totalSimilarity += bestMatch
                matches++
            }
        }
        
        return if (matches > 0) totalSimilarity / patternWords.size else 0.0
    }
    
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
    
    private fun generatePhoneticCode(word: String): String {
        // Simplified phonetic encoding
        return word.lowercase()
            .replace("[aeiou]".toRegex(), "")
            .replace("ph", "f")
            .replace("th", "t")
            .replace("sh", "s")
            .replace("ch", "k")
            .take(4)
    }
    
    private fun generateTextEmbedding(text: String): FloatArray {
        // Simplified embedding generation using word frequency and co-occurrence
        val words = normalizeText(text).split(" ")
        val embedding = FloatArray(100) // 100-dimensional embedding
        
        for (i in embedding.indices) {
            var value = 0.0f
            for ((index, word) in words.withIndex()) {
                val hash = (word.hashCode() + i).absoluteValue
                value += sin(hash.toDouble() / 1000.0).toFloat() * (1.0f / (index + 1))
            }
            embedding[i] = value / words.size
        }
        
        return embedding
    }
    
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return if (normA == 0.0 || normB == 0.0) 0.0 else dotProduct / (sqrt(normA) * sqrt(normB))
    }
    
    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
    
    private fun determineCategory(prompt: String, score: Double): String {
        return when {
            prompt.contains("deepfake") || prompt.contains("face") -> "DEEPFAKE"
            prompt.contains("celebrity") || prompt.contains("famous") -> "CELEBRITY_IMPERSONATION"
            prompt.contains("nude") || prompt.contains("explicit") -> "HARMFUL_CONTENT"
            prompt.contains("copyright") || prompt.contains("disney") -> "COPYRIGHT_VIOLATION"
            score > 0.7 -> "PRIVACY_VIOLATION"
            else -> "NONE"
        }
    }
    
    private fun calculateConfidence(fuzzy: Double, semantic: Double, context: Double, intent: Double): Double {
        val scores = listOf(fuzzy, semantic, context, intent)
        val variance = scores.map { (it - scores.average()).pow(2) }.average()
        val consistency = 1.0 - sqrt(variance)
        
        val avgScore = scores.average()
        return min(1.0, max(0.0, avgScore * consistency))
    }
    
    private fun getDetectionMethod(fuzzy: Double, semantic: Double, context: Double): String {
        return when {
            fuzzy > semantic && fuzzy > context -> "Fuzzy Pattern Matching"
            semantic > context -> "Semantic Analysis"
            else -> "Contextual Analysis"
        }
    }
    
    private fun generateExplanation(prompt: String, score: Double, category: String): String {
        return when {
            score > 0.8 -> "High confidence detection of AI misuse. Multiple indicators found."
            score > 0.6 -> "Moderate confidence detection. Some misuse patterns identified."
            score > 0.4 -> "Low confidence warning. Potential misuse detected."
            else -> "No significant misuse patterns detected."
        }
    }
    
    private fun initializeEmbeddings() {
        // Initialize with known misuse patterns
        val misusePatterns = listOf(
            "deepfake celebrity video creation",
            "face swap with famous person",
            "replace my face with actor",
            "create fake celebrity content",
            "impersonate public figure"
        )
        
        val safePatterns = listOf(
            "create beautiful landscape art",
            "generate abstract painting",
            "design logo for business",
            "make educational content",
            "artistic photo enhancement"
        )
        
        misusePatterns.forEach { pattern ->
            misuseEmbeddings[pattern] = generateTextEmbedding(pattern)
        }
        
        safePatterns.forEach { pattern ->
            safeEmbeddings[pattern] = generateTextEmbedding(pattern)
        }
    }
}

/**
 * Result of semantic analysis
 */
data class SemanticAnalysisResult(
    val isMisuse: Boolean,
    val confidence: Double,
    val misuseScore: Double,
    val category: String,
    val detectionMethod: String,
    val explanation: String
)
