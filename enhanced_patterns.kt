
    // Enhanced patterns based on training data analysis
    val ENHANCED_MALICIOUS_PATTERNS = mapOf(
        "high_confidence" to listOf(
            // Hacking patterns
            "hack into", "break into", "bypass", "crack", "steal",
            "password", "account", "system", "network", "security",
            "disable", "jailbreak", "hotwire", "pick lock",
            
            // Violence patterns
            "kill", "murder", "poison", "bomb", "explosive", "detonate",
            "weapon", "gun", "knife", "attack", "assault", "hurt",
            
            // Fraud patterns
            "fake", "forge", "counterfeit", "cheat", "scam", "fraud",
            "credit card", "money", "steal", "clone", "impersonate",
            
            // Drug patterns
            "drug", "cannabis", "heroin", "cocaine", "meth", "ecstasy",
            "smuggle", "hide", "police", "illegal",
            
            // Cyber attack patterns
            "malware", "virus", "trojan", "ransomware", "spyware",
            "phishing", "email", "website", "crash", "disable"
        ),
        "medium_confidence" to listOf(
            // Variations and related terms
            "hack", "hacking", "hacker", "steal", "stealing", "theft",
            "rob", "robbing", "break", "breaking", "crack", "cracking",
            "bypass", "bypassing", "disable", "disabling", "jailbreak",
            "hotwire", "pick", "picking", "lock", "locks",
            "kill", "killing", "murder", "murdering", "poison", "poisoning",
            "bomb", "bombs", "explosive", "explosives", "detonate", "detonating",
            "weapon", "weapons", "gun", "guns", "knife", "knives",
            "attack", "attacking", "assault", "assaulting", "hurt", "hurting",
            "fake", "faking", "forge", "forging", "counterfeit", "counterfeiting",
            "cheat", "cheating", "scam", "scamming", "fraud", "fraudulent",
            "credit", "card", "cards", "money", "clone", "cloning",
            "impersonate", "impersonating", "drug", "drugs", "cannabis",
            "heroin", "cocaine", "meth", "ecstasy", "smuggle", "smuggling",
            "hide", "hiding", "police", "illegal", "malware", "virus",
            "viruses", "trojan", "ransomware", "spyware", "phishing",
            "email", "emails", "website", "websites", "crash", "crashing"
        )
    )

    /**
     * Ultra-fast pattern matching optimized for minimal delay
     * Uses pre-compiled patterns for instant detection
     */
    fun fastAnalyzePatterns(prompt: String): PatternAnalysisResult {
        val lowerPrompt = prompt.lowercase()
        
        // Pre-compiled high-confidence patterns for instant matching
        val highConfidencePatterns = setOf(
            "hack into", "break into", "kill", "murder", "bomb", "explosive",
            "steal", "fake", "forge", "cheat", "scam", "poison", "detonate",
            "weapon", "gun", "knife", "attack", "assault", "hurt", "disable",
            "jailbreak", "hotwire", "pick lock", "bypass", "counterfeit",
            "credit card", "clone", "impersonate", "drug", "cannabis",
            "heroin", "cocaine", "meth", "ecstasy", "smuggle", "malware",
            "virus", "trojan", "ransomware", "spyware", "phishing",
            "blackmail", "sabotage", "escape", "sneak", "run away",
            "get caught", "without permission", "illegally", "anonymously",
            "secretly", "quietly"
        )
        
        // Check for high-confidence patterns first (fastest)
        val highConfidenceMatches = highConfidencePatterns.count { lowerPrompt.contains(it) }
        
        if (highConfidenceMatches > 0) {
            return PatternAnalysisResult(
                isMisuse = true,
                category = MisuseCategory.OTHER_MISUSE,
                confidence = minOf(0.9 + (highConfidenceMatches * 0.05), 1.0),
                scores = mapOf(
                    MisuseCategory.OTHER_MISUSE to minOf(0.9 + (highConfidenceMatches * 0.05), 1.0),
                    MisuseCategory.HARMFUL_CONTENT to 0.7,
                    MisuseCategory.PERSONAL_DETAILS to 0.3,
                    MisuseCategory.DEEPFAKE_IMPERSONATION to 0.2,
                    MisuseCategory.NONE to 0.1
                )
            )
        }
        
        // Fallback to regular analysis for edge cases
        return analyzePatterns(prompt)
    }
