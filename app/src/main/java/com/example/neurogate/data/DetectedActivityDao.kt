package com.example.neurogate.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectedActivityDao {
    
    @Query("SELECT * FROM detected_activities WHERE isNotificationClosed = 1 ORDER BY timestamp DESC")
    fun getAllClosedActivities(): Flow<List<DetectedActivity>>
    
    @Query("SELECT * FROM detected_activities WHERE isNotificationClosed = 1 AND isUndoUsed = 0 ORDER BY timestamp DESC")
    fun getFinalActivities(): Flow<List<DetectedActivity>>
    
    @Query("SELECT * FROM detected_activities WHERE packageName = :packageName AND isNotificationClosed = 1 ORDER BY timestamp DESC")
    fun getActivitiesByPackage(packageName: String): Flow<List<DetectedActivity>>
    
    @Query("SELECT COUNT(*) FROM detected_activities WHERE isNotificationClosed = 1 AND isUndoUsed = 0")
    fun getFinalActivityCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM detected_activities WHERE category = :category AND isNotificationClosed = 1 AND isUndoUsed = 0")
    fun getCategoryCount(category: String): Flow<Int>
    
    @Insert
    suspend fun insertActivity(activity: DetectedActivity): Long
    
    @Update
    suspend fun updateActivity(activity: DetectedActivity)
    
    @Query("UPDATE detected_activities SET isUndoUsed = 1 WHERE id = :activityId")
    suspend fun markAsUndoUsed(activityId: Long)
    
    @Query("UPDATE detected_activities SET isNotificationClosed = 1 WHERE id = :activityId")
    suspend fun markAsNotificationClosed(activityId: Long)
    
    @Query("DELETE FROM detected_activities WHERE id = :activityId")
    suspend fun deleteActivity(activityId: Long)
    
    @Query("DELETE FROM detected_activities WHERE isNotificationClosed = 0")
    suspend fun deletePendingActivities()
    
    @Query("DELETE FROM detected_activities")
    suspend fun deleteAllActivities()
}
