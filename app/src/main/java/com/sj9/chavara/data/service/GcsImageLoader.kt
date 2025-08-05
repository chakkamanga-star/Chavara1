package com.sj9.chavara.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for loading images from Google Cloud Storage
 */
class GcsImageLoader(private val context: Context) {

    private val gcsService = GoogleCloudStorageService(context)

    /**
     * Load image bitmap from GCS URL
     */
    suspend fun loadImageFromGcs(gcsUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!gcsUrl.startsWith("gs://")) {
                return@withContext null
            }

            // Extract bucket and object path from GCS URL
            val urlParts = gcsUrl.removePrefix("gs://").split("/", limit = 2)
            if (urlParts.size != 2) {
                return@withContext null
            }

            val bucketName = urlParts[0]
            val objectPath = urlParts[1]

            // Get image data from GCS
            val imageData = gcsService.downloadFile(bucketName, objectPath)

            if (imageData != null) {
                BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get public URL for GCS object (if bucket allows public access)
     */
    fun getPublicGcsUrl(gcsUrl: String): String? {
        return try {
            if (!gcsUrl.startsWith("gs://")) {
                return null
            }

            val urlParts = gcsUrl.removePrefix("gs://").split("/", limit = 2)
            if (urlParts.size != 2) {
                return null
            }

            val bucketName = urlParts[0]
            val objectPath = urlParts[1]

            // Return public HTTP URL
            "https://storage.googleapis.com/$bucketName/$objectPath"
        } catch (e: Exception) {
            null
        }
    }
}