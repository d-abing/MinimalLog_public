package com.aube.minimallog.domain.usecase

import com.aube.minimallog.domain.model.Memory
import com.aube.minimallog.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow

class ObserveSearchUseCase(private val repo: MemoryRepository) {
    operator fun invoke(query: String): Flow<List<Memory>> = repo.observeSearch(query)
}