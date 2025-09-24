package com.aube.minimallog.data.database.converter

import com.aube.minimallog.data.model.MemoryEntity
import com.aube.minimallog.domain.model.Memory
import java.time.LocalDate

object EntityMapper {
    fun toDomain(e: MemoryEntity) = Memory(
        id = e.id,
        title = e.title,
        description = e.description,
        imagePath = e.imagePath,
        date = LocalDate.ofEpochDay(e.epochDay),
        tags = if (e.tagsCsv.isBlank()) emptyList() else e.tagsCsv.split(","),
        isFavorite = e.isFavorite
    )
}