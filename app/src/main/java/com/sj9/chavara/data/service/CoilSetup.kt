package com.sj9.chavara.data.service

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.intercept.Interceptor
import coil.request.ImageResult

object CoilSetup {
    private var imageLoader: ImageLoader? = null
    private var gcsService: GoogleCloudStorageService? = null

    fun getSharedImageLoader(context: Context, googleCloudStorageService: GoogleCloudStorageService? = null): ImageLoader {
        // Store the GCS service for URL conversion
        if (googleCloudStorageService != null) {
            gcsService = googleCloudStorageService
        }

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
                // Important for GCS URLs - Add the interceptor
                .components {
                    add(GcsUrlInterceptor())
                }
                .build()
        }
        return imageLoader!!
    }

    /**
     * Custom Coil interceptor that converts gs:// URLs to signed HTTP URLs
     */
    private class GcsUrlInterceptor : Interceptor {
        override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
            val request = chain.request
            val data = request.data

            Log.d("ImageDebug", "[COIL] Processing image request for: $data")

            // Check if this is a GCS URL that needs conversion
            if (data is String && data.startsWith("gs://")) {
                Log.d("ImageDebug", "[COIL] Detected GCS URL, attempting conversion...")

                if (gcsService != null) {
                    try {
                        val signedUrl = gcsService!!.getAuthenticatedImageUrl(data)
                        if (signedUrl != null) {
                            Log.i("ImageDebug", "[COIL] Successfully converted GCS URL to signed URL")
                            // Create a new request with the signed URL
                            val newRequest = request.newBuilder()
                                .data(signedUrl)
                                .build()
                            return chain.proceed(newRequest)
                        } else {
                            Log.e("ImageDebug", "[COIL] Failed to get signed URL for: $data")
                        }
                    } catch (e: Exception) {
                        Log.e("ImageDebug", "[COIL] Exception while converting GCS URL: $data", e)
                    }
                } else {
                    Log.e("ImageDebug", "[COIL] GCS service is null! Cannot convert GCS URL: $data")
                }
            }

            // Proceed with original request if not a GCS URL or conversion failed
            return chain.proceed(request)
        }
    }
}