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
        existingImageBytes: ByteArray? = null,
        date: java.time.LocalDate,
        tags: List<String>,
        isFavorite: Boolean
    ): Long {
        val imageBytes = if (imageUri != null) {
            imageStorage.readBytes(imageUri)
        } else {
            existingImageBytes
        }
        val memory = Memory(
            id = id ?: 0L,
            title = title,
            description = description,
            imagePath = null,
            imageBytes = imageBytes,
            date = date,
            tags = tags,
            isFavorite = isFavorite
        )
        return repo.add(memory)
    }
}
