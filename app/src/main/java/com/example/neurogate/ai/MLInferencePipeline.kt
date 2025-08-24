package com.example.neurogate.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.*

/**
 * Machine Learning Inference Pipeline for Real-time AI Misuse Detection
 * Simulates a lightweight transformer model for on-device inference
 */
class MLInferencePipeline(private val context: Context) {
    
    private val vocabularySize = 10000
    private val embeddingDim = 128
    private val maxSequenceLength = 64
    private val numClasses = 6 // NONE, DEEPFAKE, CELEBRITY, HARMFUL, COPYRIGHT, PRIVACY
    
    // Simulated model weights (in real implementation, these would be loaded from a trained model)
    private lateinit var embeddingMatrix: Array<FloatArray>
    private lateinit var transformerWeights: Array<Array<FloatArray>>
    private lateinit var classifierWeights: Array<FloatArray>
    private lateinit var vocabulary: Map<String, Int>
    
    init {
        initializeModel()
    }
    
    /**
     * Real-time inference using lightweight transformer architecture
     */
    suspend fun predict(text: String): MLPredictionResult = withContext(Dispatchers.Default) {
        android.util.Log.d("NeuroGate-ML", "Starting ML inference for: $text")
        
        val startTime = System.currentTimeMillis()
        
        // Tokenization and preprocessing
        val tokens = tokenize(text)
        val inputIds = convertToIds(tokens)
        val paddedInput = padSequence(inputIds, maxSequenceLength)
        
        // Forward pass through the model
        val embeddings = embedTokens(paddedInput)
        val contextualEmbeddings = transformerEncoder(embeddings)
        val pooledOutput = globalAveragePooling(contextualEmbeddings)
        val logits = classifier(pooledOutput)
        val probabilities = softmax(logits)
        
        val inferenceTime = System.currentTimeMillis() - startTime
        
        // Parse results
        val predictedClass = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[predictedClass].toDouble()
        val category = indexToCategory(predictedClass)
        
        android.util.Log.d("NeuroGate-ML", """
            ML Inference Results:
            Predicted Class: $predictedClass ($category)
            Confidence: $confidence
            Inference Time: ${inferenceTime}ms
            Probabilities: ${probabilities.contentToString()}
        """.trimIndent())
        
        MLPredictionResult(
            category = category,
            confidence = confidence,
            probabilities = probabilities.toList(),
            inferenceTimeMs = inferenceTime,
            tokens = tokens,
            isMisuse = predictedClass != 0, // 0 is NONE class
            riskScore = calculateRiskScore(probabilities)
        )
    }
    
    /**
     * Advanced tokenization with subword handling
     */
    private fun tokenize(text: String): List<String> {
        val normalizedText = text.lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        
        val words = normalizedText.split(" ")
        val tokens = mutableListOf<String>()
        
        for (word in words) {
            if (word.length <= 3) {
                tokens.add(word)
            } else {
                // Subword tokenization for better handling of variations
                tokens.addAll(subwordTokenize(word))
            }
        }
        
        return tokens.take(maxSequenceLength - 2).let { 
            listOf("[CLS]") + it + listOf("[SEP]")
        }
    }
    
    /**
     * Subword tokenization to handle spelling variations
     */
    private fun subwordTokenize(word: String): List<String> {
        if (word.length <= 4) return listOf(word)
        
        val subwords = mutableListOf<String>()
        var start = 0
        
        while (start < word.length) {
            var end = min(start + 4, word.length)
            var foundSubword = false
            
            // Look for longest matching subword in vocabulary
            while (end > start + 1) {
                val subword = word.substring(start, end)
                if (vocabulary.containsKey(subword)) {
                    subwords.add(subword)
                    start = end
                    foundSubword = true
                    break
                }
                end--
            }
            
            if (!foundSubword) {
                // Add character-level token for unknown subwords
                subwords.add(word.substring(start, start + 1))
                start++
            }
        }
        
        return subwords
    }
    
    private fun convertToIds(tokens: List<String>): IntArray {
        return tokens.map { vocabulary[it] ?: vocabulary["[UNK]"] ?: 1 }.toIntArray()
    }
    
    private fun padSequence(sequence: IntArray, maxLength: Int): IntArray {
        val padded = IntArray(maxLength)
        val copyLength = min(sequence.size, maxLength)
        System.arraycopy(sequence, 0, padded, 0, copyLength)
        return padded
    }
    
    /**
     * Token embedding layer
     */
    private fun embedTokens(inputIds: IntArray): Array<FloatArray> {
        return Array(inputIds.size) { i ->
            val tokenId = inputIds[i]
            if (tokenId < embeddingMatrix.size) {
                embeddingMatrix[tokenId].copyOf()
            } else {
                FloatArray(embeddingDim) // Zero embedding for unknown tokens
            }
        }
    }
    
    /**
     * Simplified transformer encoder with multi-head attention
     */
    private fun transformerEncoder(embeddings: Array<FloatArray>): Array<FloatArray> {
        val sequenceLength = embeddings.size
        val output = Array(sequenceLength) { FloatArray(embeddingDim) }
        
        // Multi-head self-attention (simplified)
        for (i in embeddings.indices) {
            val attended = FloatArray(embeddingDim)
            
            for (j in embeddings.indices) {
                // Calculate attention weight
                val score = dotProduct(embeddings[i], embeddings[j])
                val weight = exp(score / sqrt(embeddingDim.toDouble())).toFloat()
                
                // Apply attention
                for (k in attended.indices) {
                    attended[k] += weight * embeddings[j][k]
                }
            }
            
            // Layer normalization and residual connection
            val normalized = layerNorm(attended)
            for (k in output[i].indices) {
                output[i][k] = embeddings[i][k] + normalized[k]
            }
            
            // Feed-forward network
            output[i] = feedForward(output[i])
        }
        
        return output
    }
    
    /**
     * Global average pooling to get fixed-size representation
     */
    private fun globalAveragePooling(embeddings: Array<FloatArray>): FloatArray {
        val pooled = FloatArray(embeddingDim)
        
        for (embedding in embeddings) {
            for (i in pooled.indices) {
                pooled[i] += embedding[i]
            }
        }
        
        for (i in pooled.indices) {
            pooled[i] /= embeddings.size
        }
        
        return pooled
    }
    
    /**
     * Classification head
     */
    private fun classifier(pooledOutput: FloatArray): FloatArray {
        val logits = FloatArray(numClasses)
        
        for (i in logits.indices) {
            logits[i] = dotProduct(pooledOutput, classifierWeights[i])
        }
        
        return logits
    }
    
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val shifted = logits.map { it - maxLogit }
        val expSum = shifted.sumOf { exp(it.toDouble()) }
        
        return shifted.map { (exp(it.toDouble()) / expSum).toFloat() }.toFloatArray()
    }
    
    private fun dotProduct(a: FloatArray, b: FloatArray): Float {
        return a.zip(b) { x, y -> x * y }.sum()
    }
    
    private fun layerNorm(input: FloatArray): FloatArray {
        val mean = input.average().toFloat()
        val variance = input.map { (it - mean).pow(2) }.average().toFloat()
        val std = sqrt(variance + 1e-6f)
        
        return input.map { (it - mean) / std }.toFloatArray()
    }
    
    private fun feedForward(input: FloatArray): FloatArray {
        // Simple feed-forward network: Linear -> ReLU -> Linear
        val hidden = input.map { max(0f, it * 2f) }.toFloatArray() // ReLU activation
        return hidden.map { it * 0.5f }.toFloatArray() // Output projection
    }
    
    private fun indexToCategory(index: Int): String {
        return when (index) {
            0 -> "NONE"
            1 -> "DEEPFAKE"
            2 -> "CELEBRITY_IMPERSONATION"
            3 -> "HARMFUL_CONTENT"
            4 -> "COPYRIGHT_VIOLATION"
            5 -> "PRIVACY_VIOLATION"
            else -> "UNKNOWN"
        }
    }
    
    private fun calculateRiskScore(probabilities: FloatArray): Double {
        // Risk score is 1 - probability of NONE class
        return (1.0 - probabilities[0]).coerceIn(0.0, 1.0)
    }
    
    /**
     * Initialize the model with simulated pre-trained weights
     */
    private fun initializeModel() {
        android.util.Log.d("NeuroGate-ML", "Initializing ML model...")
        
        // Initialize vocabulary
        vocabulary = buildVocabulary()
        
        // Initialize embedding matrix
        embeddingMatrix = Array(vocabularySize) { 
            FloatArray(embeddingDim) { (Random().nextGaussian() * 0.1).toFloat() }
        }
        
        // Initialize transformer weights
        transformerWeights = Array(4) { // 4 layers
            Array(embeddingDim) { 
                FloatArray(embeddingDim) { (Random().nextGaussian() * 0.02).toFloat() }
            }
        }
        
        // Initialize classifier weights
        classifierWeights = Array(numClasses) {
            FloatArray(embeddingDim) { (Random().nextGaussian() * 0.1).toFloat() }
        }
        
        // Adjust weights based on domain knowledge
        adjustWeightsForMisuseDetection()
        
        android.util.Log.d("NeuroGate-ML", "ML model initialized successfully")
    }
    
    private fun buildVocabulary(): Map<String, Int> {
        val vocab = mutableMapOf<String, Int>()
        
        // Special tokens
        vocab["[PAD]"] = 0
        vocab["[UNK]"] = 1
        vocab["[CLS]"] = 2
        vocab["[SEP]"] = 3
        
        // Common words and misuse-related terms
        val misuseTerms = listOf(
            "deepfake", "deep", "fake", "face", "swap", "replace", "celebrity",
            "famous", "actor", "actress", "person", "put", "make", "create",
            "generate", "transform", "change", "edit", "morph", "blend",
            "overlay", "superimpose", "merge", "composite", "manipulation",
            "my", "me", "i", "myself", "with", "into", "like", "as",
            "tom", "cruise", "brad", "pitt", "leonardo", "dicaprio",
            "will", "smith", "ryan", "reynolds", "scarlett", "johansson"
        )
        
        val commonWords = listOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to",
            "for", "of", "with", "by", "from", "up", "about", "into",
            "through", "during", "before", "after", "above", "below",
            "is", "are", "was", "were", "be", "been", "being", "have",
            "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "can", "this", "that",
            "these", "those", "some", "any", "all", "no", "not"
        )
        
        var index = 4
        for (term in misuseTerms + commonWords) {
            if (!vocab.containsKey(term)) {
                vocab[term] = index++
            }
        }
        
        // Add character-level tokens for handling unknown words
        for (c in 'a'..'z') {
            vocab[c.toString()] = index++
        }
        for (c in '0'..'9') {
            vocab[c.toString()] = index++
        }
        
        return vocab
    }
    
    private fun adjustWeightsForMisuseDetection() {
        // Boost weights for misuse-related terms
        val misuseTermIndices = listOf(
            vocabulary["deepfake"], vocabulary["face"], vocabulary["swap"],
            vocabulary["replace"], vocabulary["celebrity"], vocabulary["famous"]
        ).filterNotNull()
        
        // Increase classifier weights for misuse detection
        for (classIndex in 1 until numClasses) { // Skip NONE class
            for (termIndex in misuseTermIndices) {
                if (termIndex < embeddingMatrix.size) {
                    for (i in embeddingMatrix[termIndex].indices) {
                        classifierWeights[classIndex][i] += embeddingMatrix[termIndex][i] * 0.5f
                    }
                }
            }
        }
    }
}

/**
 * Result of ML inference
 */
data class MLPredictionResult(
    val category: String,
    val confidence: Double,
    val probabilities: List<Float>,
    val inferenceTimeMs: Long,
    val tokens: List<String>,
    val isMisuse: Boolean,
    val riskScore: Double
)
