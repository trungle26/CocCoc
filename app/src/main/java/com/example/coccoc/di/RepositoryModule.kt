package com.example.coccoc.di

import com.example.coccoc.data.repository.ArticleRepository
import com.example.coccoc.domain.repository.IArticleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindArticleRepository(
        articleRepository: ArticleRepository
    ): IArticleRepository
}
