package com.sj9.chavara.data.service

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * A singleton object to provide a highly optimized and shared ImageLoader instance.
 * This setup is configured for performance, with aggressive memory and disk caching
 * to provide an experience similar to social media apps like Instagram.
 */
object ImageLoaderModule {

    private var imageLoader: ImageLoader? = null

    fun getSharedImageLoader(context: Context): ImageLoader {
        // Return the existing instance if it's already been created (singleton pattern)
        if (imageLoader == null) {
            // Configure a custom OkHttpClient for better network performance
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            imageLoader = ImageLoader.Builder(context)
                // Use our custom OkHttpClient
                .okHttpClient(okHttpClient)
                // **Memory Cache Configuration**
                .memoryCache {
                    MemoryCache.Builder(context)
                        // Use 25% of the app's available memory for the memory cache.
                        // This is aggressive and good for apps that display many images.
                        .maxSizePercent(0.25)
                        .build()
                }
                // **Disk Cache Configuration**
                .diskCache {
                    DiskCache.Builder()
                        // Store the cache in a dedicated "image_cache" directory
                        .directory(context.cacheDir.resolve("image_cache"))
                        // Use 5% of available disk space for the disk cache.
                        // This is a generous amount for holding many images.
                        .maxSizePercent(0.05)
                        .build()
                }
                .build()
        }
        return imageLoader!!
    }
}