package com.sj9.chavara.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * A data class to hold the downloaded image data and its MIME type.
 */
data class ImageData(val data: ByteArray, val mimeType: String)

/**
 * Service to handle downloading images from URLs.
 */
class ImageDownloadService {

    companion object {
        private const val TAG = "ImageDownloadService"
    }

    /**
     * Checks if a given URL string is a valid and accessible image URL.
     * It performs a HEAD request to check the content type without downloading the full image.
     */
    fun isValidImageUrl(url: String?): Boolean {
        Log.d(TAG, "Validating image URL: $url")

        if (url.isNullOrBlank()) {
            Log.w(TAG, "URL is null or blank")
            return false
        }

        if (!url.startsWith("http")) {
            Log.w(TAG, "URL does not start with http/https: $url")
            return false
        }

        // Check for Google Drive URLs (they don't have file extensions)
        if (url.contains("drive.google.com")) {
            Log.d(TAG, "Google Drive URL detected, allowing download")
            return true
        }

        // Simple check for common image extensions as a first pass.
        val isValid = url.endsWith(".jpg", true) ||
                url.endsWith(".jpeg", true) ||
                url.endsWith(".png", true) ||
                url.endsWith(".gif", true)

        Log.d(TAG, "URL validation result for $url: $isValid")
        return isValid
    }

    /**
     * Converts Google Drive sharing URLs to direct download URLs
     */
    private fun convertGoogleDriveUrl(url: String): String {
        when {
            // Handle drive.google.com/open?id= format
            url.contains("drive.google.com/open?id=") -> {
                val fileId = url.substringAfter("id=").substringBefore("&")
                // Try the newer format first
                val directUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
                Log.d(TAG, "Converted Google Drive URL (open): $url -> $directUrl")
                return directUrl
            }
            // Handle drive.google.com/file/d/{id}/view format
            url.contains("drive.google.com/file/d/") && url.contains("/view") -> {
                val fileId = url.substringAfter("/file/d/").substringBefore("/view")
                val directUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
                Log.d(TAG, "Converted Google Drive URL (file/d): $url -> $directUrl")
                return directUrl
            }
            // Handle drive.google.com/uc?id= format - add confirm parameter
            url.contains("drive.google.com/uc?") && !url.contains("confirm=") -> {
                val directUrl = "$url&confirm=t"
                Log.d(TAG, "Added confirm parameter to Google Drive URL: $url -> $directUrl")
                return directUrl
            }
            else -> {
                Log.d(TAG, "URL not converted (not a Google Drive sharing URL): $url")
                return url
            }
        }
    }

    /**
     * Downloads an image from the given URL.
     *
     * @param urlString The URL of the image to download.
     * @return An [ImageData] object containing the byte array and MIME type, or null on failure.
     */
    suspend fun downloadImage(urlString: String): ImageData? = withContext(Dispatchers.IO) {
        val processedUrl = convertGoogleDriveUrl(urlString)
        Log.i(TAG, "Starting image download from: $processedUrl")
        if (processedUrl != urlString) {
            Log.i(TAG, "Original URL: $urlString")
        }

        try {
            Log.d(TAG, "Creating URL object for: $processedUrl")
            val url = URL(processedUrl)

            Log.d(TAG, "Opening HTTP connection...")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            Log.d(TAG, "Connection configured - timeout: 8000ms, method: GET")
            Log.d(TAG, "Connecting to server...")
            connection.connect()

            val responseCode = connection.responseCode
            Log.i(TAG, "Server response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Connection successful, processing response...")

                val contentType = connection.contentType ?: "image/jpeg"
                val contentLength = connection.contentLength
                Log.i(TAG, "Content-Type: $contentType")
                Log.i(TAG, "Content-Length: ${if (contentLength > 0) "$contentLength bytes" else "unknown"}")

                // Validate that we're getting actual image data, not HTML error pages
                if (contentType.startsWith("text/html")) {
                    Log.e(TAG, "❌ Received HTML instead of image data!")
                    Log.e(TAG, "This usually means:")
                    Log.e(TAG, "1. Google Drive file is not publicly accessible")
                    Log.e(TAG, "2. File requires authentication")
                    Log.e(TAG, "3. File ID is incorrect")
                    Log.e(TAG, "4. Google Drive is returning an error page")

                    connection.disconnect()
                    return@withContext null
                }

                // Additional validation for image content types
                if (!contentType.startsWith("image/")) {
                    Log.w(TAG, "⚠️ Content-Type is not an image: $contentType")
                    Log.w(TAG, "Proceeding anyway, but file may not be a valid image")
                }

                Log.d(TAG, "Opening input stream...")
                val inputStream = connection.inputStream
                val buffer = ByteArray(4096)
                val outputStream = ByteArrayOutputStream()

                var totalBytesRead = 0
                var bytesRead: Int
                var readCount = 0

                Log.d(TAG, "Starting data transfer...")
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    readCount++

                    // Log progress every 50 read operations to avoid spam
                    if (readCount % 50 == 0) {
                        Log.d(TAG, "Downloaded $totalBytesRead bytes so far...")
                    }
                }

                val imageData = outputStream.toByteArray()

                Log.d(TAG, "Closing streams...")
                inputStream.close()
                outputStream.close()
                connection.disconnect()

                Log.i(TAG, "✅ Image download SUCCESSFUL!")
                Log.i(TAG, "Final image size: ${imageData.size} bytes")
                Log.i(TAG, "MIME type: $contentType")
                Log.i(TAG, "Total read operations: $readCount")

                // Validate that we actually got image data
                if (imageData.isEmpty()) {
                    Log.e(TAG, "❌ Downloaded image data is empty!")
                    return@withContext null
                }

                return@withContext ImageData(data = imageData, mimeType = contentType)
            } else {
                Log.e(TAG, "❌ HTTP error - Response code: $responseCode")
                Log.e(TAG, "Response message: ${connection.responseMessage}")

                // Log additional error details for common HTTP errors
                when (responseCode) {
                    HttpURLConnection.HTTP_NOT_FOUND -> Log.e(TAG, "Image not found (404) at URL: $processedUrl")
                    HttpURLConnection.HTTP_FORBIDDEN -> {
                        Log.e(TAG, "Access forbidden (403) for URL: $processedUrl")
                        if (processedUrl.contains("drive.google.com")) {
                            Log.e(TAG, "Google Drive 403: File may not be publicly shared or requires authentication")
                        }
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> Log.e(TAG, "Unauthorized access (401) for URL: $processedUrl")
                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        Log.e(TAG, "Bad request (400) for URL: $processedUrl")
                        if (processedUrl.contains("drive.google.com")) {
                            Log.e(TAG, "Google Drive 400: File may not be publicly accessible or URL format is incorrect")
                            Log.e(TAG, "Make sure the Google Drive file is shared as 'Anyone with the link can view'")
                        }
                    }
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> Log.e(TAG, "Server error (500) for URL: $processedUrl")
                }

                connection.disconnect()
                return@withContext null
            }
        } catch (e: java.net.MalformedURLException) {
            Log.e(TAG, "❌ Invalid URL format: $processedUrl", e)
            return@withContext null
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "❌ Connection failed to $processedUrl - network issue or server unavailable", e)
            return@withContext null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Connection timeout for $processedUrl - server too slow or network issue", e)
            return@withContext null
        } catch (e: java.io.IOException) {
            Log.e(TAG, "❌ IO error during download from $processedUrl", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error downloading image from $processedUrl", e)
            return@withContext null
        }
    }

    /**
     * Generates a unique file name for a member's photo.
     *
     * @param memberId The ID of the family member.
     * @param mimeType The MIME type of the image (e.g., "image/jpeg").
     * @return A unique filename with the correct extension (e.g., "member_photo_123.jpg").
     */
    fun generateImageFileName(memberId: Int, mimeType: String): String {
        Log.d(TAG, "Generating filename for member ID: $memberId, MIME type: $mimeType")

        val extension = when (mimeType.lowercase(Locale.ROOT)) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            else -> {
                Log.w(TAG, "Unknown MIME type: $mimeType, defaulting to jpg")
                "jpg" // Default extension
            }
        }

        val fileName = "member_photo_${memberId}_${System.currentTimeMillis()}.$extension"
        Log.d(TAG, "Generated filename: $fileName")

        return fileName
    }
}