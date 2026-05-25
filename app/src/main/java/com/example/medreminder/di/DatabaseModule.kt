package com.example.medreminder.di

import android.content.Context
import androidx.room.Room
import com.example.medreminder.data.local.MedicationDatabase
import com.example.medreminder.data.local.dao.MedicationDao
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
    ): MedicationDatabase {
        return Room.databaseBuilder(
            context,
            MedicationDatabase::class.java,
            "medication_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideMedicationDao(db: MedicationDatabase): MedicationDao {
        return db.medicationDao()
    }
}
