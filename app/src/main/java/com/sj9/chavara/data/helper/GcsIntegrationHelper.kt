package com.sj9.chavara.data.helper

import android.content.Context
import android.util.Log
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.sj9.chavara.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date

/**
 * Helper class for integrating Google Sheets data fetching with Google Cloud Storage uploads.
 * Manages the complete workflow of fetching student data from spreadsheets,
 * downloading associated media files, and organizing them in GCS by month.
 */
class GcsIntegrationHelper(private val context: Context) {

    companion object {
        private const val TAG = "GcsIntegrationHelper"
        private const val GCS_BUCKET_NAME = "yourapp-student-data" // Replace with your bucket
        private const val SHEETS_APP_NAME = "Chavara Youth Sheets Integration"
        private const val CONNECTION_TIMEOUT = 30000
        private const val READ_TIMEOUT = 60000
    }

    // Service instances - avoiding delegated properties to prevent smart cast issues
    private var _sheetsService: Sheets? = null
    private var _storageService: Storage? = null
    private var _servicesInitialized = false

    // Safe getters for services
    private fun getSheetsService(): Sheets? {
        if (!_servicesInitialized) {
            initializeServices()
        }
        return _sheetsService
    }

    private fun getStorageService(): Storage? {
        if (!_servicesInitialized) {
            initializeServices()
        }
        return _storageService
    }

    private fun initializeServices() {
        if (_servicesInitialized) return

        try {
            // Initialize Sheets service
            val sheetsCredentials = loadSheetsCredentials()
            _sheetsService = sheetsCredentials?.let { createSheetsService(it) }

            // Initialize Storage service
            val gcsCredentials = loadGcsCredentials()
            _storageService = gcsCredentials?.let { createStorageService(it) }

            _servicesInitialized = true
            Log.d(TAG, "Services initialization completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize services", e)
        }
    }

    /**
     * Data class representing a student record from the spreadsheet
     */
    data class StudentRecord(
        val name: String,
        val date: String,
        val photoLink: String,
        val videoLink: String,
        val month: String,
        val studentId: String
    )

    /**
     * Check if both services are available
     */
    private fun areServicesAvailable(): Boolean {
        val sheets = getSheetsService()
        val storage = getStorageService()
        return sheets != null && storage != null
    }

    /**
     * Initialize and validate services
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing GcsIntegrationHelper...")

            // Force initialization
            initializeServices()

            val sheets = getSheetsService()
            val storage = getStorageService()

            if (sheets != null && storage != null) {
                Log.d(TAG, "Both Google services initialized successfully")
                true
            } else {
                Log.e(TAG, "Failed to initialize one or both Google services")
                if (sheets == null) Log.e(TAG, "Sheets service is null - check sheets_key.json")
                if (storage == null) Log.e(TAG, "Storage service is null - check gcs_key.json")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GcsIntegrationHelper", e)
            false
        }
    }

    /**
     * Main public function to fetch data from spreadsheet URL and upload to GCS
     */
    suspend fun fetchAndUploadFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting fetch and upload process for URL: $url")

            // Check if services are available
            if (!areServicesAvailable()) {
                val initSuccess = initialize()
                if (!initSuccess) {
                    return@withContext Result.failure(
                        Exception("Failed to initialize Google services. Check service account files.")
                    )
                }
            }

            // Verify services are still available after initialization
            if (!areServicesAvailable()) {
                return@withContext Result.failure(
                    Exception("Google services are not available. Check service account files.")
                )
            }

            // Extract spreadsheet ID from URL
            val spreadsheetId = extractSpreadsheetId(url)
                ?: return@withContext Result.failure(Exception("Invalid Google Sheets URL"))

            Log.d(TAG, "Extracted spreadsheet ID: $spreadsheetId")

            // Fetch data from spreadsheet
            val studentRecords = fetchStudentDataFromSheets(spreadsheetId)
            if (studentRecords.isEmpty()) {
                return@withContext Result.failure(Exception("No student data found in spreadsheet"))
            }

            Log.d(TAG, "Fetched ${studentRecords.size} student records")

            // Process each student record
            var successCount = 0
            var errorCount = 0

            for (student in studentRecords) {
                try {
                    val uploadSuccess = processStudentRecord(student)
                    if (uploadSuccess) {
                        successCount++
                        Log.d(TAG, "Successfully processed student: ${student.name}")
                    } else {
                        errorCount++
                        Log.w(TAG, "Failed to process student: ${student.name}")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "Error processing student: ${student.name}", e)
                }
            }

            val resultMessage = "Processing completed. Success: $successCount, Errors: $errorCount"
            Log.d(TAG, resultMessage)

            Result.success(resultMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch and upload from URL", e)
            Result.failure(e)
        }
    }

    /**
     * Load Google Sheets service account credentials from raw resources
     */
    private fun loadSheetsCredentials(): GoogleCredentials? {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.sheets_key)
            val credentialsJson = inputStream.bufferedReader().use { it.readText() }

            ServiceAccountCredentials
                .fromStream(ByteArrayInputStream(credentialsJson.toByteArray()))
                .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets.readonly"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Sheets credentials", e)
            null
        }
    }

    /**
     * Load Google Cloud Storage service account credentials from raw resources
     */
    private fun loadGcsCredentials(): GoogleCredentials? {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.gcs_key)
            val credentialsJson = inputStream.bufferedReader().use { it.readText() }

            ServiceAccountCredentials
                .fromStream(ByteArrayInputStream(credentialsJson.toByteArray()))
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load GCS credentials", e)
            null
        }
    }

    /**
     * Create Google Sheets service instance
     */
    private fun createSheetsService(credentials: GoogleCredentials): Sheets {
        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            { request ->
                credentials.refresh()
                request.headers.authorization = "Bearer ${credentials.accessToken.tokenValue}"
            }
        )
            .setApplicationName(SHEETS_APP_NAME)
            .build()
    }

    /**
     * Create Google Cloud Storage service instance
     */
    private fun createStorageService(credentials: GoogleCredentials): Storage {
        return StorageOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .service
    }

    /**
     * Extract spreadsheet ID from Google Sheets URL
     */
    private fun extractSpreadsheetId(url: String): String? {
        return try {
            val regex = "/spreadsheets/d/([a-zA-Z0-9-_]+)".toRegex()
            regex.find(url)?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract spreadsheet ID from URL: $url", e)
            null
        }
    }

    /**
     * Fetch student data from Google Sheets
     */
    private suspend fun fetchStudentDataFromSheets(spreadsheetId: String): List<StudentRecord> = withContext(Dispatchers.IO) {
        try {
            return@withContext getSheetsService()?.let { sheets ->
                // Fetch data from the first sheet, assuming columns: Name, Date, Photo Link, Video Link
                val range = "Sheet1!A:D"
                val response = sheets.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute()

                val values = response.values ?: return@let emptyList() // Fixed with property

                val studentRecords = mutableListOf<StudentRecord>()

                // Skip header row (index 0) and process data rows
                for (i in 1 until values.size) {
                    val row = values[i]
                    if (row.size >= 4) {
                        val name = row[0]?.toString()?.trim() ?: continue
                        val date = row[1]?.toString()?.trim() ?: continue
                        val photoLink = row[2]?.toString()?.trim() ?: ""
                        val videoLink = row[3]?.toString()?.trim() ?: ""

                        // Extract month from date
                        val month = extractMonthFromDate(date)
                        val studentId = generateStudentId(name, i)

                        studentRecords.add(
                            StudentRecord(
                                name = name,
                                date = date,
                                photoLink = photoLink,
                                videoLink = videoLink,
                                month = month,
                                studentId = studentId
                            )
                        )
                    }
                }

                studentRecords
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch student data from sheets", e)
            emptyList()
        }
    }

    /**
     * Extract month from date string (supports various formats)
     */
    private fun extractMonthFromDate(dateString: String): String {
        return try {
            val formats = arrayOf(
                "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd",
                "dd-MM-yyyy", "MM-dd-yyyy", "yyyy/MM/dd"
            )

            for (format in formats) {
                try {
                    val date = SimpleDateFormat(format, Locale.getDefault()).parse(dateString)
                    if (date != null) {
                        val calendar = Calendar.getInstance().apply { time = date }
                        val monthNames = arrayOf(
                            "january", "february", "march", "april", "may", "june",
                            "july", "august", "september", "october", "november", "december"
                        )
                        return monthNames[calendar.get(Calendar.MONTH)]
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }

            // Fallback: try to extract month from string directly
            dateString.lowercase().let { lowerDate ->
                when {
                    lowerDate.contains("jan") -> "january"
                    lowerDate.contains("feb") -> "february"
                    lowerDate.contains("mar") -> "march"
                    lowerDate.contains("apr") -> "april"
                    lowerDate.contains("may") -> "may"
                    lowerDate.contains("jun") -> "june"
                    lowerDate.contains("jul") -> "july"
                    lowerDate.contains("aug") -> "august"
                    lowerDate.contains("sep") -> "september"
                    lowerDate.contains("oct") -> "october"
                    lowerDate.contains("nov") -> "november"
                    lowerDate.contains("dec") -> "december"
                    else -> "unknown"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract month from date: $dateString", e)
            "unknown"
        }
    }

    /**
     * Generate unique student ID
     */
    private fun generateStudentId(name: String, index: Int): String {
        val cleanName = name.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        return "${cleanName}_${index}_${System.currentTimeMillis()}"
    }

    /**
     * Process individual student record: download files and upload to GCS
     */
    private suspend fun processStudentRecord(student: StudentRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing student: ${student.name} (Month: ${student.month})")

            var success = true

            // Process photo if link exists
            if (student.photoLink.isNotEmpty()) {
                val photoSuccess = downloadAndUploadFile(
                    fileUrl = student.photoLink,
                    gcsPath = "students/${student.month}/${student.studentId}/photo.jpg",
                    fileType = "photo"
                )
                if (!photoSuccess) {
                    Log.w(TAG, "Failed to process photo for student: ${student.name}")
                    success = false
                }
            }

            // Process video if link exists
            if (student.videoLink.isNotEmpty()) {
                val videoSuccess = downloadAndUploadFile(
                    fileUrl = student.videoLink,
                    gcsPath = "students/${student.month}/${student.studentId}/video.mp4",
                    fileType = "video"
                )
                if (!videoSuccess) {
                    Log.w(TAG, "Failed to process video for student: ${student.name}")
                    success = false
                }
            }

            // Upload student metadata
            val metadataSuccess = uploadStudentMetadata(student)
            if (!metadataSuccess) {
                Log.w(TAG, "Failed to upload metadata for student: ${student.name}")
                success = false
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Error processing student record: ${student.name}", e)
            false
        }
    }

    /**
     * Download file from URL and upload to GCS
     */
    private suspend fun downloadAndUploadFile(
        fileUrl: String,
        gcsPath: String,
        fileType: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading $fileType from: $fileUrl")

            // Process Google Drive URLs
            val downloadUrl = processGoogleDriveUrl(fileUrl)

            // Download file to memory
            val fileData = downloadFileToMemory(downloadUrl)
            if (fileData == null) {
                Log.e(TAG, "Failed to download file from: $downloadUrl")
                return@withContext false
            }

            Log.d(TAG, "Downloaded ${fileData.size} bytes, uploading to GCS: $gcsPath")

            // Upload to GCS
            val uploadSuccess = uploadToGcs(fileData, gcsPath, getContentType(fileType))

            if (uploadSuccess) {
                Log.d(TAG, "Successfully uploaded $fileType to: $gcsPath")
            } else {
                Log.e(TAG, "Failed to upload $fileType to: $gcsPath")
            }

            uploadSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading and uploading file: $fileUrl", e)
            false
        }
    }

    /**
     * Process Google Drive URLs to direct download links
     */
    private fun processGoogleDriveUrl(url: String): String {
        return when {
            url.contains("drive.google.com/file/d/") -> {
                val fileId = url.substringAfter("/file/d/").substringBefore("/")
                "https://drive.google.com/uc?export=download&id=$fileId"
            }
            url.contains("drive.google.com/open?id=") -> {
                val fileId = url.substringAfter("id=").substringBefore("&")
                "https://drive.google.com/uc?export=download&id=$fileId"
            }
            else -> url
        }
    }

    /**
     * Download file from URL to memory
     */
    private suspend fun downloadFileToMemory(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECTION_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("User-Agent", "Chavara Youth App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream: InputStream = connection.inputStream
                val buffer = ByteArray(8192)
                val outputStream = ByteArrayOutputStream()

                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                inputStream.close()
                connection.disconnect()

                outputStream.toByteArray()
            } else {
                Log.e(TAG, "HTTP error: $responseCode for URL: $url")
                connection.disconnect()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file from URL: $url", e)
            null
        }
    }

    /**
     * Upload data to Google Cloud Storage
     */
    private suspend fun uploadToGcs(
        data: ByteArray,
        gcsPath: String,
        contentType: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorageService()?.let { storage ->
                val blobId = BlobId.of(GCS_BUCKET_NAME, gcsPath)
                val blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build()

                storage.create(blobInfo, data)
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload to GCS: $gcsPath", e)
            false
        }
    }

    /**
     * Upload student metadata as JSON to GCS
     */
    private suspend fun uploadStudentMetadata(student: StudentRecord): Boolean = withContext(Dispatchers.IO) {
        try {
            val metadata = """
                {
                    "name": "${student.name}",
                    "date": "${student.date}",
                    "month": "${student.month}",
                    "studentId": "${student.studentId}",
                    "photoLink": "${student.photoLink}",
                    "videoLink": "${student.videoLink}",
                    "uploadedAt": "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"
                }
            """.trimIndent()

            val gcsPath = "students/${student.month}/${student.studentId}/metadata.json"
            uploadToGcs(metadata.toByteArray(), gcsPath, "application/json")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload metadata for student: ${student.name}", e)
            false
        }
    }

    /**
     * Get content type based on file type
     */
    private fun getContentType(fileType: String): String {
        return when (fileType.lowercase()) {
            "photo" -> "image/jpeg"
            "video" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        // Note: lazy properties can't be reset, but they will be garbage collected
        // when this instance is no longer referenced
        Log.d(TAG, "GcsIntegrationHelper cleaned up")
    }
}