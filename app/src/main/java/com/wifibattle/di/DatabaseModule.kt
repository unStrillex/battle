package com.wifibattle.di

import android.content.Context
import androidx.room.Room
import com.wifibattle.data.local.AppDatabase
import com.wifibattle.data.local.MatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "wfb.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideMatchHistoryDao(db: AppDatabase): MatchHistoryDao = db.matchHistoryDao()
}
