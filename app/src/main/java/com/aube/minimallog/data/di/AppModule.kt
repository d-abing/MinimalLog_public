package com.aube.minimallog.data.di

import android.content.Context
import androidx.room.Room
import com.aube.minimallog.data.database.ImageStorage
import com.aube.minimallog.data.database.MinimalLogDatabase
import com.aube.minimallog.data.database.dao.MemoryDao
import com.aube.minimallog.data.repository.DriveBackupRepository
import com.aube.minimallog.data.repository.MemoryRepositoryImpl
import com.aube.minimallog.domain.repository.MemoryRepository
import com.aube.minimallog.domain.usecase.AddMemoryUseCase
import com.aube.minimallog.domain.usecase.DeleteMemoryUseCase
import com.aube.minimallog.domain.usecase.ObserveFavoritesUseCase
import com.aube.minimallog.domain.usecase.ObserveMemoriesUseCase
import com.aube.minimallog.domain.usecase.ObserveMemoryByIdUseCase
import com.aube.minimallog.domain.usecase.ObserveSearchUseCase
import com.aube.minimallog.domain.usecase.ObserveTodayMemoryUseCase
import com.aube.minimallog.domain.usecase.ToggleFavoriteUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): MinimalLogDatabase =
        Room.databaseBuilder(ctx, MinimalLogDatabase::class.java, "minimallog.db").build()

    @Provides
    fun provideDao(db: MinimalLogDatabase): MemoryDao = db.memoryDao()

    @Provides
    @Singleton
    fun provideImageStorage(@ApplicationContext ctx: Context) = ImageStorage(ctx)

    @Provides
    @Singleton
    fun provideMemoryRepository(dao: MemoryDao, imageStorage: ImageStorage): MemoryRepository = MemoryRepositoryImpl(dao, imageStorage)

    @Provides
    @Singleton
    fun provideAddUseCase(repo: MemoryRepository, imageStorage: ImageStorage) =
        AddMemoryUseCase(repo, imageStorage)

    @Provides
    @Singleton
    fun provideObserveToday(repo: MemoryRepository) = ObserveTodayMemoryUseCase(repo)

    @Provides
    @Singleton
    fun provideObserveMemories(repo: MemoryRepository) = ObserveMemoriesUseCase(repo)

    @Provides
    @Singleton
    fun provideObserveFavorites(repo: MemoryRepository) = ObserveFavoritesUseCase(repo)

    @Provides
    @Singleton
    fun provideObserveSearch(repo: MemoryRepository) = ObserveSearchUseCase(repo)

    @Provides
    @Singleton
    fun provideToggleFavorite(repo: MemoryRepository) = ToggleFavoriteUseCase(repo)

    @Provides @Singleton
    fun provideObserveMemoryById(repo: MemoryRepository) = ObserveMemoryByIdUseCase(repo)

    @Provides @Singleton
    fun provideDeleteMemory(repo: MemoryRepository) = DeleteMemoryUseCase(repo)

    @Provides @Singleton
    fun provideDriveBackupRepository(@ApplicationContext context: Context) = DriveBackupRepository(context)

}