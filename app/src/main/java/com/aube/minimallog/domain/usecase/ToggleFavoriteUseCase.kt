package com.aube.minimallog.domain.usecase

import com.aube.minimallog.domain.repository.MemoryRepository

class ToggleFavoriteUseCase(private val repo: MemoryRepository) {
    suspend operator fun invoke(id: Long): Boolean = repo.toggleFavorite(id)
}