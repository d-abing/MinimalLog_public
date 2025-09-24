package com.aube.minimallog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val imagePath: String?,        // 앱 내부 저장소 경로 (nullable)
    val epochDay: Long,            // LocalDate -> epochDay 로 저장
    val tagsCsv: String,           // "tag1,tag2,tag3"
    val isFavorite: Boolean
)