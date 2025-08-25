package com.sj9.chavara.data.service

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.ImageResult
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ImageLoaderModule {

    fun create(context: Context): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .components {
                // Pass the application context to the interceptor
                add(GcsUrlInterceptor(context.applicationContext))
            }
            .build()
    }

    private class GcsUrlInterceptor(private val context: Context) : Interceptor {
        // Lazily initialize the GCS service to avoid holding a static context reference
        private val gcsService by lazy { GoogleCloudStorageService(context) }

        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val request = chain.request
            val data = request.data
            val logTag = "ImageDebug"

            Log.d(logTag, "[COIL] Processing image request for: $data")

            if (data is String && data.startsWith("gs://")) {
                Log.d(logTag, "[COIL] Detected GCS URL, attempting conversion...")
                try {
                    val signedUrl = gcsService.getAuthenticatedImageUrl(data)
                    if (signedUrl != null) {
                        Log.i(logTag, "[COIL] Successfully converted GCS URL to signed URL")
                        val newRequest = request.newBuilder()
                            .data(signedUrl)
                            .build()
                        return chain.proceed(newRequest)
                    } else {
                        Log.e(logTag, "[COIL] Failed to get signed URL for: $data")
                    }
                } catch (e: Exception) {
                    Log.e(logTag, "[COIL] Exception while converting GCS URL: $data", e)
                }
            }
            return chain.proceed(request)
        }
    }
}