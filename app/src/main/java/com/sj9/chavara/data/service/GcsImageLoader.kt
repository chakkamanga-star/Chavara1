package com.sj9.chavara.data.service

/**
 * Service for loading images from Google Cloud Storage
 */
class GcsImageLoader() {




    /**
     * Load image bitmap from GCS URL
     */


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
        } catch (_: Exception) {
            null
        }
    }
}