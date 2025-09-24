package com.aube.minimallog.domain.usecase

import com.aube.minimallog.domain.model.Memory
import com.aube.minimallog.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ObserveTodayMemoryUseCase(private val repo: MemoryRepository) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<Memory?> = repo.observeToday(date)
}