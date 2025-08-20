package com.sj9.chavara.data.service

import android.util.Log

/**
 * Service for loading images from Google Cloud Storage
 */
class GcsImageLoader() {

    companion object {
        private const val TAG = "GcsImageLoader"
    }

    /**
     * Get public URL for GCS object (if bucket allows public access)
     */
    fun getPublicGcsUrl(gcsUrl: String): String? {
        Log.d(TAG, "Attempting to convert GCS URL: $gcsUrl")
        return try {
            if (!gcsUrl.startsWith("gs://")) {
                Log.w(TAG, "URL is not a valid GCS URL.")
                return null
            }

            val urlParts = gcsUrl.removePrefix("gs://").split("/", limit = 2)
            if (urlParts.size != 2) {
                Log.e(TAG, "Invalid GCS URL format. Expected gs://<bucket>/<object-path>")
                return null
            }

            val bucketName = urlParts[0]
            val objectPath = urlParts[1]

            val publicUrl = "https://storage.googleapis.com/$bucketName/$objectPath"
            Log.d(TAG, "Successfully converted GCS URL to public URL: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert GCS URL.", e)
            null
        }
    }
}