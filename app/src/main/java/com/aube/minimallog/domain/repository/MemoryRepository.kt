package com.aube.minimallog.domain.repository

import com.aube.minimallog.domain.model.Memory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MemoryRepository {
    suspend fun add(memory: Memory): Long
    fun observeAll(): Flow<List<Memory>>
    fun observeById(id: Long): Flow<Memory?>
    fun observeToday(date: LocalDate): Flow<Memory?>
    fun observeFavorites(): Flow<List<Memory>>
    fun observeSearch(query: String): Flow<List<Memory>>
    suspend fun toggleFavorite(id: Long): Boolean
    suspend fun delete(id: Long)
}