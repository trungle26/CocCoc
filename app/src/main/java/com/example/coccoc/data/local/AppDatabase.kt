package com.example.coccoc.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.coccoc.data.local.dao.ArticleDao
import com.example.coccoc.data.local.entity.ArticleEntity

@Database(
    entities = [ArticleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}
