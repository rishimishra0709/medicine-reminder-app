package com.example.medreminder.di

import com.example.medreminder.data.repository.MedicationRepositoryImpl
import com.example.medreminder.domain.repository.AlarmScheduler
import com.example.medreminder.domain.repository.MedicationRepository
import com.example.medreminder.reminder.AndroidAlarmScheduler
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
    abstract fun bindMedicationRepository(
        impl: MedicationRepositoryImpl
    ): MedicationRepository

    @Binds
    @Singleton
    abstract fun bindAlarmScheduler(
        impl: AndroidAlarmScheduler
    ): AlarmScheduler
}
