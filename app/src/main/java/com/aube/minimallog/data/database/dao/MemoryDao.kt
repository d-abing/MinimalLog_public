package com.aube.minimallog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aube.minimallog.data.model.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MemoryEntity): Long

    // 전체 최신순
    @Query("SELECT * FROM memories ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<MemoryEntity?>

    // 오늘(특정 날짜) 레코드 중 가장 최신 1개
    @Query("""
        SELECT * FROM memories
        ORDER BY 
            CASE 
              WHEN strftime('%m', date(epochDay*86400, 'unixepoch')) = :mm 
               AND strftime('%d', date(epochDay*86400, 'unixepoch')) = :dd 
              THEN 1 ELSE 0 
            END DESC,
            epochDay DESC,
            id DESC
        LIMIT 1
    """)
    fun observeToday(mm: String, dd: String): Flow<MemoryEntity?>

    // 즐겨찾기만
    @Query("SELECT * FROM memories WHERE isFavorite = 1 ORDER BY epochDay DESC, id DESC")
    fun observeFavorites(): Flow<List<MemoryEntity>>

    // 간단 검색(제목/본문/태그)
    @Query("""
        SELECT * FROM memories 
        WHERE (:q == '' OR title LIKE :qLike OR description LIKE :qLike OR tagsCsv LIKE :qLike)
        ORDER BY epochDay DESC, id DESC
    """)
    fun observeSearch(q: String, qLike: String): Flow<List<MemoryEntity>>

    @Query("""
        UPDATE memories 
        SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END
        WHERE id = :id
    """)
    suspend fun toggleFavorite(id: Long): Int

    @Query("SELECT isFavorite FROM memories WHERE id = :id")
    suspend fun getFavoriteState(id: Long): Boolean?

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): MemoryEntity?

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}