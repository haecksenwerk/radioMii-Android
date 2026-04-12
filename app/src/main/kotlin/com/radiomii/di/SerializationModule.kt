package com.radiomii.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SerializationModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // ignoreUnknownKeys: safe to add new fields to persisted models without breaking old data.
        // coerceInputValues: invalid enum values fall back to defaults instead of throwing.
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
}


