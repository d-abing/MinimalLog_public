package com.aube.minimallog.domain.usecase

import com.aube.minimallog.domain.model.Memory
import com.aube.minimallog.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ObserveMemoryByIdUseCase(private val repo: MemoryRepository) {
    operator fun invoke(id: Long?): Flow<Memory?> = if (id == null) emptyFlow() else repo.observeById(id)
}