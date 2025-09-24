package com.aube.minimallog.domain.usecase

import com.aube.minimallog.domain.model.Memory
import com.aube.minimallog.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoritesUseCase(private val repo: MemoryRepository) {
    operator fun invoke(): Flow<List<Memory>> = repo.observeFavorites()
}