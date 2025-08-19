package com.sj9.chavara.data.service


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import android.util.Log // Added Log import for debugging

/**
 * Service for downloading images from URLs (including Google Drive links)
 */
class ImageDownloadService {

    /**
     * Data class to hold downloaded image data and its MIME type.
     */
    data class DownloadedImage(
        val data: ByteArray,
        val mimeType: String
    )

    /**
     * Download image from URL and return its data and MIME type.
     */
    suspend fun downloadImage(imageUrl: String): DownloadedImage? = withContext(Dispatchers.IO) {
        val processedUrl = processImageUrl(imageUrl)
        try {
            Log.d("ImageDownloadService", "Attempting to download from processed URL: $processedUrl")
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
                val contentType = connection.contentType ?: "application/octet-stream"
                Log.d("ImageDownloadService", "Download successful. Content type from header: $contentType")

                val inputStream = connection.inputStream
                val buffer = ByteArray(1024)
                val byteArrayOutputStream = ByteArrayOutputStream()
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }

                inputStream.close()
                connection.disconnect()

                val originalData = byteArrayOutputStream.toByteArray()
                Log.d("ImageDownloadService", "File downloaded successfully. Size: ${originalData.size} bytes.")

                return@withContext DownloadedImage(originalData, contentType)
            } else {
                Log.e("ImageDownloadService", "HTTP error response code: $responseCode")
                connection.disconnect()
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("ImageDownloadService", "Error during image download from URL: $processedUrl", e)
            return@withContext null
        }
    }

    /**
     * Process different types of image URLs (especially Google Drive links)
     */
    private fun processImageUrl(imageUrl: String): String {
        return when {
            imageUrl.contains("drive.google.com/file/d/") -> {
                val fileId = extractGoogleDriveFileId(imageUrl)
                "https://drive.google.com/uc?export=download&id=$fileId"
            }
            imageUrl.contains("drive.google.com/open?id=") -> {
                val fileId = imageUrl.substringAfter("id=").substringBefore("&")
                "https://drive.google.com/uc?export=download&id=$fileId"
            }
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
     * Generate a unique filename for the downloaded image based on its mime type.
     */
    fun generateImageFileName(memberId: Int, mimeType: String): String {
        val extension = when {
            mimeType.contains("jpeg") -> "jpeg"
            mimeType.contains("png") -> "png"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("heic") -> "heic"
            mimeType.contains("heif") -> "heif"
            else -> "jpg" // Fallback to jpg
        }
        val uniqueId = UUID.randomUUID().toString().take(8)
        return "member_${memberId}_${uniqueId}.$extension"
    }

    /**
     * Validate if URL is likely an image.
     */
    fun isValidImageUrl(url: String): Boolean {
        return !url.isNullOrEmpty() && url.contains("drive.google.com")
    }
}
