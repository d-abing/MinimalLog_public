package com.aube.minimallog.domain.usecase

import com.aube.minimallog.domain.repository.MemoryRepository

class DeleteMemoryUseCase(private val repo: MemoryRepository) {
    suspend operator fun invoke(id: Long) = repo.delete(id)
}
