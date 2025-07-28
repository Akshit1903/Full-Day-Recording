package com.akshit.fulldayrecording.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val duration: Long, // in milliseconds
    val fileSize: Long, // in bytes
    val isSynced: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) 