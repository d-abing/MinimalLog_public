package com.aube.minimallog.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aube.minimallog.data.database.converter.Converters
import com.aube.minimallog.data.database.dao.MemoryDao
import com.aube.minimallog.data.model.MemoryEntity

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MinimalLogDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}