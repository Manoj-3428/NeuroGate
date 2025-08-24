package com.example.neurogate.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Date

class ActivityRepository(private val dao: DetectedActivityDao) {
    
    // Get all final activities (notification closed and no undo used)
    fun getFinalActivities(): Flow<List<DetectedActivity>> {
        return dao.getFinalActivities()
            .catch { e ->
                // Log error and return empty list
                println("Error fetching final activities: ${e.message}")
            }
    }
    
    // Get activities by package name
    fun getActivitiesByPackage(packageName: String): Flow<List<DetectedActivity>> {
        return dao.getActivitiesByPackage(packageName)
            .catch { e ->
                println("Error fetching activities for $packageName: ${e.message}")
            }
    }
    
    // Get activity count
    fun getActivityCount(): Flow<Int> {
        return dao.getFinalActivityCount()
            .catch { e ->
                println("Error fetching activity count: ${e.message}")
            }
    }
    
    // Get category count
    fun getCategoryCount(category: String): Flow<Int> {
        return dao.getCategoryCount(category)
            .catch { e ->
                println("Error fetching category count: ${e.message}")
            }
    }
    
    // Insert new activity (pending - notification not closed yet)
    suspend fun insertPendingActivity(
        text: String,
        category: String,
        packageName: String,
        confidence: Double
    ): Long {
        return try {
            val activity = DetectedActivity(
                text = text,
                category = category,
                packageName = packageName,
                confidence = confidence,
                timestamp = Date(),
                isUndoUsed = false,
                isNotificationClosed = false
            )
            dao.insertActivity(activity)
        } catch (e: Exception) {
            println("Error inserting pending activity: ${e.message}")
            -1L
        }
    }
    
    // Mark activity as notification closed (final storage)
    suspend fun markAsNotificationClosed(activityId: Long) {
        try {
            dao.markAsNotificationClosed(activityId)
        } catch (e: Exception) {
            println("Error marking activity as notification closed: ${e.message}")
        }
    }
    
    // Mark activity as undo used
    suspend fun markAsUndoUsed(activityId: Long) {
        try {
            dao.markAsUndoUsed(activityId)
        } catch (e: Exception) {
            println("Error marking activity as undo used: ${e.message}")
        }
    }
    
    // Delete pending activities (cleanup)
    suspend fun deletePendingActivities() {
        try {
            dao.deletePendingActivities()
        } catch (e: Exception) {
            println("Error deleting pending activities: ${e.message}")
        }
    }
    
    // Delete specific activity
    suspend fun deleteActivity(activityId: Long) {
        try {
            dao.deleteActivity(activityId)
        } catch (e: Exception) {
            println("Error deleting activity: ${e.message}")
        }
    }
    
    // Delete all activities
    suspend fun deleteAllActivities() {
        try {
            dao.deleteAllActivities()
        } catch (e: Exception) {
            println("Error deleting all activities: ${e.message}")
        }
    }
}
