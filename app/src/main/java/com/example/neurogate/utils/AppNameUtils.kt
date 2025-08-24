package com.example.neurogate.utils

import android.content.Context
import android.content.pm.PackageManager

object AppNameUtils {
    
    /**
     * Get app name from package name
     */
    fun getAppName(context: Context, packageName: String): String {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            // If package not found, return a formatted version of the package name
            formatPackageName(packageName)
        } catch (e: Exception) {
            // Fallback to formatted package name
            formatPackageName(packageName)
        }
    }
    
    /**
     * Format package name to be more readable
     */
    private fun formatPackageName(packageName: String): String {
        return when {
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("telegram") -> "Telegram"
            packageName.contains("instagram") -> "Instagram"
            packageName.contains("facebook") -> "Facebook"
            packageName.contains("twitter") -> "Twitter"
            packageName.contains("gmail") -> "Gmail"
            packageName.contains("chrome") -> "Chrome"
            packageName.contains("firefox") -> "Firefox"
            packageName.contains("safari") -> "Safari"
            packageName.contains("edge") -> "Edge"
            packageName.contains("chatgpt") -> "ChatGPT"
            packageName.contains("deepseek") -> "DeepSeek"
            packageName.contains("bard") -> "Google Bard"
            packageName.contains("claude") -> "Claude"
            packageName.contains("discord") -> "Discord"
            packageName.contains("slack") -> "Slack"
            packageName.contains("linkedin") -> "LinkedIn"
            packageName.contains("youtube") -> "YouTube"
            packageName.contains("tiktok") -> "TikTok"
            packageName.contains("snapchat") -> "Snapchat"
            packageName.contains("reddit") -> "Reddit"
            packageName.contains("pinterest") -> "Pinterest"
            packageName.contains("spotify") -> "Spotify"
            packageName.contains("netflix") -> "Netflix"
            packageName.contains("amazon") -> "Amazon"
            packageName.contains("ebay") -> "eBay"
            packageName.contains("uber") -> "Uber"
            packageName.contains("lyft") -> "Lyft"
            packageName.contains("doordash") -> "DoorDash"
            packageName.contains("grubhub") -> "GrubHub"
            packageName.contains("airbnb") -> "Airbnb"
            packageName.contains("booking") -> "Booking.com"
            packageName.contains("expedia") -> "Expedia"
            packageName.contains("hotels") -> "Hotels.com"
            packageName.contains("tripadvisor") -> "TripAdvisor"
            packageName.contains("yelp") -> "Yelp"
            packageName.contains("google") -> "Google"
            packageName.contains("microsoft") -> "Microsoft"
            packageName.contains("apple") -> "Apple"
            packageName.contains("samsung") -> "Samsung"
            packageName.contains("huawei") -> "Huawei"
            packageName.contains("xiaomi") -> "Xiaomi"
            packageName.contains("oneplus") -> "OnePlus"
            packageName.contains("oppo") -> "OPPO"
            packageName.contains("vivo") -> "vivo"
            packageName.contains("realme") -> "realme"
            packageName.contains("nokia") -> "Nokia"
            packageName.contains("motorola") -> "Motorola"
            packageName.contains("lg") -> "LG"
            packageName.contains("sony") -> "Sony"
            packageName.contains("htc") -> "HTC"
            packageName.contains("blackberry") -> "BlackBerry"
            packageName.contains("alcatel") -> "Alcatel"
            packageName.contains("zte") -> "ZTE"
            packageName.contains("meizu") -> "Meizu"
            packageName.contains("honor") -> "Honor"
            packageName.contains("iqoo") -> "iQOO"
            packageName.contains("poco") -> "POCO"
            packageName.contains("redmi") -> "Redmi"
            packageName.contains("mi") -> "Xiaomi"
            packageName.contains("samsung") -> "Samsung"
            packageName.contains("galaxy") -> "Samsung Galaxy"
            packageName.contains("note") -> "Samsung Note"
            packageName.contains("tab") -> "Samsung Tab"
            packageName.contains("watch") -> "Samsung Watch"
            packageName.contains("buds") -> "Samsung Buds"
            packageName.contains("gear") -> "Samsung Gear"
            packageName.contains("bixby") -> "Bixby"
            packageName.contains("samsung") -> "Samsung"
            packageName.contains("galaxy") -> "Samsung Galaxy"
            packageName.contains("note") -> "Samsung Note"
            packageName.contains("tab") -> "Samsung Tab"
            packageName.contains("watch") -> "Samsung Watch"
            packageName.contains("buds") -> "Samsung Buds"
            packageName.contains("gear") -> "Samsung Gear"
            packageName.contains("bixby") -> "Bixby"
            else -> {
                // Extract the last part of the package name and capitalize it
                val parts = packageName.split(".")
                if (parts.isNotEmpty()) {
                    parts.last().replaceFirstChar { it.uppercase() }
                } else {
                    packageName
                }
            }
        }
    }
}
