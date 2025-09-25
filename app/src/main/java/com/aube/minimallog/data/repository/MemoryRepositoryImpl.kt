package com.aube.minimallog.data.repository

import com.aube.minimallog.data.database.ImageStorage
import com.aube.minimallog.data.database.converter.EntityMapper
import com.aube.minimallog.data.database.dao.MemoryDao
import com.aube.minimallog.data.model.MemoryEntity
import com.aube.minimallog.domain.model.Memory
import com.aube.minimallog.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class MemoryRepositoryImpl(
    private val dao: MemoryDao,
    private val imageStorage: ImageStorage
) : MemoryRepository {

    override suspend fun add(memory: Memory): Long {
        val entity = MemoryEntity(
            id = memory.id,
            title = memory.title,
            description = memory.description,
            imagePath = memory.imagePath,
            epochDay = memory.date.toEpochDay(),
            tagsCsv = memory.tags.joinToString(","),
            isFavorite = memory.isFavorite
        )
        return dao.insert(entity)
    }

    override fun observeAll(): Flow<List<Memory>> =
        dao.observeAll().map { it.map(EntityMapper::toDomain) }

    override fun observeById(id: Long): Flow<Memory?> =
        dao.observeById(id).map { it?.let(EntityMapper::toDomain) }

    override fun observeToday(date: LocalDate): Flow<Memory?> {
        val mm = "%02d".format(date.monthValue)
        val dd = "%02d".format(date.dayOfMonth)
        return dao.observeToday(mm, dd).map { it?.let(EntityMapper::toDomain) }
    }

    override fun observeFavorites(): Flow<List<Memory>> =
        dao.observeFavorites().map { it.map(EntityMapper::toDomain) }

    override fun observeSearch(query: String): Flow<List<Memory>> {
        val q = query.trim()
        val like = "%$q%"
        return dao.observeSearch(q, like).map { it.map(EntityMapper::toDomain) }
    }

    override suspend fun toggleFavorite(id: Long): Boolean {
        dao.toggleFavorite(id)
        return dao.getFavoriteState(id) ?: false
    }

    override suspend fun delete(id: Long) {
        val entity = dao.getByIdOnce(id)
        // 이미지 먼저 정리(실패해도 진행)
        val success = imageStorage.deleteFile(entity?.imagePath)
        // DB 삭제
        dao.deleteById(id)
    }
}