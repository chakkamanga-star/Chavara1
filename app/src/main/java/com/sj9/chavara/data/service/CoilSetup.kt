package com.sj9.chavara.data.service

// Create a new file: app/src/main/java/com/sj9/chavara/data/service/CoilSetup.kt



import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

object CoilSetup {
    private var imageLoader: ImageLoader? = null

    fun getSharedImageLoader(context: Context): ImageLoader {
        // Use the existing instance if we have one
        if (imageLoader == null) {
            imageLoader = ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder(context)
                        .maxSizePercent(0.25) // Use 25% of available memory for in-memory cache
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02) // Use 2% of available disk space for disk cache
                        .build()
                }
               // Important for GCS URLs
                .build()
        }
        return imageLoader!!
    }
}