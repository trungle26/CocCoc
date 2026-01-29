package com.example.coccoc.di

import android.content.Context
import androidx.room.Room
import com.example.coccoc.data.local.AppDatabase
import com.example.coccoc.data.local.dao.ArticleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "coccoc_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideArticleDao(database: AppDatabase): ArticleDao {
        return database.articleDao()
    }
}
