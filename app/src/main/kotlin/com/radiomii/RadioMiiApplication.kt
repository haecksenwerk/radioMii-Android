package com.radiomii

import android.app.Application
import android.content.ComponentCallbacks2
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import okio.Path.Companion.toPath
import javax.inject.Inject

@HiltAndroidApp
class RadioMiiApplication : Application(), SingletonImageLoader.Factory {

    // Injected by Hilt during onCreate() — available before any Composable calls newImageLoader()
    @Inject lateinit var okHttpClient: OkHttpClient

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                // Tighter than the 25% default to reduce GC pressure with many station icons.
                MemoryCache.Builder()
                    .maxSizePercent(context, percent = 0.10)
                    .build()
            }
            .diskCache {
                // Default is 250 MB — 50 MB is more than sufficient for ~32×32 favicons.
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_image_cache").absolutePath.toPath())
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .components {
                // Share the app-wide OkHttpClient (connection pool, interceptors, timeouts).
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .crossfade(true)
            .apply {
                if (BuildConfig.DEBUG) logger(DebugLogger())
            }
            .build()

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Clear memory cache when backgrounded or under pressure; disk cache repopulates it.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            SingletonImageLoader.get(this).memoryCache?.clear()
        }
    }
}
