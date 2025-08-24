package com.example.neurogate.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurogate.data.ActivityRepository
import com.example.neurogate.data.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityViewModel(private val repository: ActivityRepository) : ViewModel() {
    
    private val _activities = MutableStateFlow<List<DetectedActivity>>(emptyList())
    val activities: StateFlow<List<DetectedActivity>> = _activities.asStateFlow()
    
    private val _activityCount = MutableStateFlow(0)
    val activityCount: StateFlow<Int> = _activityCount.asStateFlow()
    
    private val _categoryCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryCounts: StateFlow<Map<String, Int>> = _categoryCounts.asStateFlow()
    
    init {
        loadActivities()
        loadActivityCount()
    }
    
    private fun loadActivities() {
        viewModelScope.launch {
            repository.getFinalActivities().collect { activities ->
                _activities.value = activities
            }
        }
    }
    
    private fun loadActivityCount() {
        viewModelScope.launch {
            repository.getActivityCount().collect { count ->
                _activityCount.value = count
            }
        }
    }
    
    fun loadActivitiesByPackage(packageName: String) {
        viewModelScope.launch {
            repository.getActivitiesByPackage(packageName).collect { activities ->
                _activities.value = activities
            }
        }
    }
    
    fun loadCategoryCounts() {
        viewModelScope.launch {
            val categories = listOf(
                "PRIVACY_VIOLATION",
                "HARMFUL_CONTENT", 
                "DEEPFAKE",
                "CELEBRITY_IMPERSONATION",
                "COPYRIGHT_VIOLATION"
            )
            
            val counts = mutableMapOf<String, Int>()
            categories.forEach { category ->
                repository.getCategoryCount(category).collect { count ->
                    counts[category] = count
                    _categoryCounts.value = counts.toMap()
                }
            }
        }
    }
    
    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
            loadActivities() // Reload the list
        }
    }
    
    fun deleteAllActivities() {
        viewModelScope.launch {
            repository.deleteAllActivities()
            loadActivities() // Reload the list
        }
    }
    
    fun refresh() {
        loadActivities()
        loadActivityCount()
        loadCategoryCounts()
    }
}
