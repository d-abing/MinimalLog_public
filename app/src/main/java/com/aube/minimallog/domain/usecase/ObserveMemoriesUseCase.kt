package com.aube.minimallog.domain.usecase

import com.aube.minimallog.domain.model.Memory
import com.aube.minimallog.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveMemoriesUseCase(private val repo: MemoryRepository) {
    operator fun invoke(query: String = ""): Flow<List<Memory>> =
        if (query.isBlank()) repo.observeAll() else repo.observeSearch(query)
}