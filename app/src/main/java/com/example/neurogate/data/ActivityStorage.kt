package com.example.neurogate.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class ActivityStorage private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("detected_activities", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _activities = MutableStateFlow<List<DetectedActivity>>(emptyList())
    val activities: StateFlow<List<DetectedActivity>> = _activities.asStateFlow()
    
    companion object {
        @Volatile
        private var INSTANCE: ActivityStorage? = null
        
        fun getInstance(context: Context): ActivityStorage {
            return INSTANCE ?: synchronized(this) {
                val instance = ActivityStorage(context.applicationContext)
                INSTANCE = instance
                instance.loadActivities()
                instance
            }
        }
    }
    
    private fun loadActivities() {
        val activitiesJson = prefs.getString("activities_list", "[]") ?: "[]"
        try {
            val activities = json.decodeFromString<List<DetectedActivity>>(activitiesJson)
            _activities.value = activities.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            _activities.value = emptyList()
        }
    }
    
    suspend fun insertActivity(activity: DetectedActivity) = withContext(Dispatchers.IO) {
        val currentActivities = _activities.value.toMutableList()
        currentActivities.add(0, activity) // Add at beginning for newest first
        
        // Keep only last 1000 activities to prevent excessive storage
        val limitedActivities = currentActivities.take(1000)
        
        saveActivities(limitedActivities)
        _activities.value = limitedActivities
    }
    
    fun getAllActivities(): Flow<List<DetectedActivity>> = activities
    
    fun getActivitiesByCategory(category: String): Flow<List<DetectedActivity>> {
        return MutableStateFlow(_activities.value.filter { it.category == category }).asStateFlow()
    }
    
    fun getActivitiesByApp(appPackage: String): Flow<List<DetectedActivity>> {
        return MutableStateFlow(_activities.value.filter { it.appPackage == appPackage }).asStateFlow()
    }
    
    fun getActivitiesFromDate(startDate: Date): Flow<List<DetectedActivity>> {
        return MutableStateFlow(_activities.value.filter { it.timestamp >= startDate.time }).asStateFlow()
    }
    
    suspend fun getCategoryCount(category: String): Int = withContext(Dispatchers.IO) {
        _activities.value.count { it.category == category }
    }
    
    suspend fun getTotalCount(): Int = withContext(Dispatchers.IO) {
        _activities.value.size
    }
    
    suspend fun getAllCategories(): List<String> = withContext(Dispatchers.IO) {
        _activities.value.map { it.category }.distinct().sorted()
    }
    
    suspend fun getAllApps(): List<String> = withContext(Dispatchers.IO) {
        _activities.value.map { it.appName }.distinct().sorted()
    }
    
    suspend fun deleteActivity(activity: DetectedActivity) = withContext(Dispatchers.IO) {
        val currentActivities = _activities.value.toMutableList()
        currentActivities.removeAll { it.id == activity.id }
        saveActivities(currentActivities)
        _activities.value = currentActivities
    }
    
    suspend fun deleteOlderThan(date: Date) = withContext(Dispatchers.IO) {
        val currentActivities = _activities.value.filter { it.timestamp >= date.time }
        saveActivities(currentActivities)
        _activities.value = currentActivities
    }
    
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        saveActivities(emptyList())
        _activities.value = emptyList()
    }
    
    private fun saveActivities(activities: List<DetectedActivity>) {
        val activitiesJson = json.encodeToString(activities)
        prefs.edit().putString("activities_list", activitiesJson).apply()
    }
}
