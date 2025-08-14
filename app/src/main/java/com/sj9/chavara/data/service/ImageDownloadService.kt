package com.sj9.chavara.data.service


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Service for downloading images from URLs (including Google Drive links)
 */
class ImageDownloadService {

    /**
     * Download image from URL and return byte array
     */
    suspend fun downloadImage(imageUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val processedUrl = processImageUrl(imageUrl)
            val url = URL(processedUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("User-Agent", "Chavara Youth App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val buffer = ByteArray(1024)
                val byteArrayOutputStream = ByteArrayOutputStream()

                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }

                inputStream.close()
                connection.disconnect()

                byteArrayOutputStream.toByteArray()
            } else {
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Process different types of image URLs (especially Google Drive links)
     */
    private fun processImageUrl(imageUrl: String): String {
        return when {
            // Google Drive sharing link format: https://drive.google.com/file/d/FILE_ID/view?usp=sharing
            imageUrl.contains("drive.google.com/file/d/") -> {
                val fileId = extractGoogleDriveFileId(imageUrl)
                "https://drive.google.com/uc?export=download&id=$fileId"
            }

            // Google Drive direct link format: https://drive.google.com/open?id=FILE_ID
            imageUrl.contains("drive.google.com/open?id=") -> {
                val fileId = imageUrl.substringAfter("id=").substringBefore("&")
                "https://drive.google.com/uc?export=download&id=$fileId"
            }

            // Already a direct download link or other image URL
            else -> imageUrl
        }
    }

    /**
     * Extract file ID from Google Drive URL
     */
    private fun extractGoogleDriveFileId(url: String): String {
        return try {
            when {
                url.contains("/file/d/") -> {
                    url.substringAfter("/file/d/").substringBefore("/")
                }
                url.contains("id=") -> {
                    url.substringAfter("id=").substringBefore("&")
                }
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Generate a unique filename for the downloaded image
     */
    fun generateImageFileName(originalUrl: String, memberId: Int): String {
        val fileExtension = getFileExtension(originalUrl)
        val uniqueId = UUID.randomUUID().toString().take(8)
        return "member_${memberId}_${uniqueId}.$fileExtension"
    }

    /**
     * Get file extension from URL or default to jpg
     */
    private fun getFileExtension(url: String): String {
        return try {
            when {
                url.contains(".png", ignoreCase = true) -> "png"
                url.contains(".gif", ignoreCase = true) -> "gif"
                url.contains(".webp", ignoreCase = true) -> "webp"
                url.contains(".jpeg", ignoreCase = true) -> "jpeg"
                else -> "jpg"
            }
        } catch (_: Exception) {
            "jpg"
        }
    }

    /**
     * FIX: Added the missing getMimeType function
     * Determines the MIME type from a URL string.
     */
    fun getMimeType(url: String): String {
        val extension = getFileExtension(url)
        return when (extension.lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "jpeg", "jpg" -> "image/jpeg"
            else -> "application/octet-stream" // Default binary stream type
        }
    }


    /**
     * Validate if URL is likely an image
     */
    fun isValidImageUrl(url: String): Boolean {
        return url.isNotEmpty() && (
                url.contains("drive.google.com") ||
                        url.contains(".jpg", ignoreCase = true) ||
                        url.contains(".jpeg", ignoreCase = true) ||
                        url.contains(".png", ignoreCase = true) ||
                        url.contains(".gif", ignoreCase = true) ||
                        url.contains(".webp", ignoreCase = true) ||
                        url.contains("image", ignoreCase = true)
                )
    }
}