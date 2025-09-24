package com.aube.minimallog.domain.usecase

import android.net.Uri
import com.aube.minimallog.data.database.ImageStorage
import com.aube.minimallog.domain.model.Memory
import com.aube.minimallog.domain.repository.MemoryRepository

class AddMemoryUseCase(
    private val repo: MemoryRepository,
    private val imageStorage: ImageStorage
) {
    suspend operator fun invoke(
        id: Long? = null,
        title: String,
        description: String,
        imageUri: Uri?,
        date: java.time.LocalDate,
        tags: List<String>,
        isFavorite: Boolean
    ): Long {
        val path: String? = if (imageUri != null) imageStorage.persist(imageUri) else null
        val memory = Memory(
            id = id ?: 0L,
            title = title,
            description = description,
            imagePath = path,
            date = date,
            tags = tags,
            isFavorite = isFavorite
        )
        return repo.add(memory)
    }
}