package com.aube.minimallog.presentation.model

import com.aube.minimallog.domain.model.Memory
import java.time.LocalDate

data class MemoryItem(
    val id: Long,
    val title: String,
    val description: String,
    val imageUrl: String?,   // File path도 String으로 전달
    val date: LocalDate,
    val tags: List<String>,
    val isFavorite: Boolean
)

object MemoryUiMapper {
    fun fromDomain(m: Memory) = MemoryItem(
        id = m.id,
        title = m.title,
        description = m.description,
        imageUrl = m.imagePath, // ← File path를 그대로 넘김(Coal이 File도 처리)
        date = m.date,
        tags = m.tags,
        isFavorite = m.isFavorite
    )
}