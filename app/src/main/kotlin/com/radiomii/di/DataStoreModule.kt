@file:Suppress("unused")

package com.radiomii.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.radiomii.data.prefs.FavoritesData
import com.radiomii.data.prefs.favoritesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused", "UnusedDeclaration")
object DataStoreModule {

    // Using a Context extension so tests can provide their own DataStore without a real Context.
    @Provides
    @Singleton
    @Suppress("unused", "UnusedDeclaration")
    fun provideFavoritesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<FavoritesData> = context.favoritesDataStore
}
