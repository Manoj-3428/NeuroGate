package com.example.neurogate.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.neurogate.data.Converters
import java.util.Date

@Entity(tableName = "detected_activities")
@TypeConverters(Converters::class)
data class DetectedActivity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val category: String,
    val packageName: String,
    val confidence: Double,
    val timestamp: Date,
    val isUndoUsed: Boolean = false,
    val isNotificationClosed: Boolean = false
)