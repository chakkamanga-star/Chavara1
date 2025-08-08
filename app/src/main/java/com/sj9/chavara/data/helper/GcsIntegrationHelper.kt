package com.sj9.chavara.data.helper

import android.content.Context
import android.util.Log
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.auth.oauth2.ServiceAccountCredentials
import com.sj9.chavara.R
import com.sj9.chavara.data.service.GoogleSheetsService
import com.sj9.chavara.data.service.SheetRowData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

/**
 * Helper class for integrating Google Sheets data fetching with Google Cloud Storage uploads.
 * Uses GoogleSheetsService for sheet operations and focuses on GCS upload workflow.
 * Manages downloading media files and organizing them in GCS by month.
 */
class GcsIntegrationHelper(private val context: Context) {

    companion object {
        private const val TAG = "GcsIntegrationHelper"
        private const val GCS_BUCKET_NAME = "chakka" // Your bucket name
        private const val CONNECTION_TIMEOUT = 30000 // milliseconds
        private const val READ_TIMEOUT = 60000 // milliseconds
    }

    // Use GoogleSheetsService for all sheet operations
    private val googleSheetsService = GoogleSheetsService(context)

    // Only handle GCS operations here
    private var _storageService: Storage? = null
    private var _storageInitialized = false

    private fun getStorageService(): Storage? {
        if (!_storageInitialized) {
            Log.d(TAG, "getStorageService: Storage not initialized. Initializing...")
            initializeStorage()
        }
        return _storageService
    }

    @Synchronized
    private fun initializeStorage() {
        if (_storageInitialized) {
            Log.d(TAG, "initializeStorage: Storage already initialized, skipping.")
            return
        }
        Log.i(TAG, "initializeStorage: Attempting to initialize GCS storage...")

        try {
            Log.d(TAG, "initializeStorage: Attempting to load GCS credentials...")
            val gcsCredentials = loadGcsCredentials()
            if (gcsCredentials == null) {
                Log.e(TAG, "initializeStorage: GCS credentials failed to load. _storageService will be null.")
                _storageService = null
            } else {
                Log.d(TAG, "initializeStorage: GCS credentials loaded. Creating Storage service...")
                _storageService = StorageOptions.newBuilder()
                    .setCredentials(gcsCredentials)
                    .build()
                    .service
                Log.d(TAG, "initializeStorage: Storage service creation attempted. Success: ${_storageService != null}")
            }

            _storageInitialized = true
            Log.i(TAG, "initializeStorage: Storage initialization completed. Success: ${_storageService != null}")

        } catch (e: Exception) {
            Log.e(TAG, "initializeStorage: CRITICAL FAILURE during storage initialization", e)
            _storageService = null
            _storageInitialized = true
        }
    }

    data class StudentRecord(
        // Essential fields from your spreadsheet
        val emailAddress: String,
        val name: String,
        val course: String,
        val phoneNumber: String,
        val birthday: String,
        val imageUrl: String,
        val residenceInBangalore: String?,
        val chavaraParticipation: String?,

        // Internal fields for processing
        val month: String,           // Derived from 'birthday'
        val studentId: String        // Generated
    )

    private fun isStorageAvailable(): Boolean {
        val storage = _storageService
        val available = storage != null
        if (!available) {
            Log.w(TAG, "isStorageAvailable: Storage service is null.")
        }
        return available
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "initialize: Attempting to initialize GCS storage...")
        if (!_storageInitialized) {
            initializeStorage()
        }

        if (isStorageAvailable()) {
            Log.i(TAG, "initialize: GCS storage initialized successfully.")
            true
        } else {
            Log.e(TAG, "initialize: FAILED to initialize GCS storage. Check gcs_key.json and logs.")
            false
        }
    }

    suspend fun fetchAndUploadFromUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        Log.i(TAG, "--------------------------------------------------------------------")
        Log.i(TAG, "fetchAndUploadFromUrl: STARTING PROCESS FOR URL: $url")
        Log.i(TAG, "--------------------------------------------------------------------")

        try {
            Log.d(TAG, "fetchAndUploadFromUrl: [STEP 1] Ensuring storage is initialized...")
            if (!_storageInitialized) {
                val initSuccess = initialize()
                if (!initSuccess) {
                    Log.e(TAG, "fetchAndUploadFromUrl: [STEP 1a] FAILED to initialize GCS storage. Cannot proceed.")
                    return@withContext Result.failure(
                        Exception("Failed to initialize GCS storage. Check service account files and logs.")
                    )
                }
            }

            Log.d(TAG, "fetchAndUploadFromUrl: [STEP 2] Verifying storage is available...")
            if (!isStorageAvailable()) {
                Log.e(TAG, "fetchAndUploadFromUrl: [STEP 2a] CRITICAL: Storage is NOT available.")
                return@withContext Result.failure(
                    Exception("GCS storage is not available. Check logs for initialization errors.")
                )
            }

            Log.d(TAG, "fetchAndUploadFromUrl: [STEP 3] Validating spreadsheet URL...")
            if (!googleSheetsService.validateSpreadsheetUrl(url)) {
                Log.e(TAG, "fetchAndUploadFromUrl: [STEP 3a] Invalid or inaccessible spreadsheet URL: $url")
                return@withContext Result.failure(Exception("Invalid or inaccessible Google Sheets URL: $url"))
            }

            Log.d(TAG, "fetchAndUploadFromUrl: [STEP 4] Fetching raw sheet data...")
            val sheetRowDataList = googleSheetsService.fetchRawSheetData(url) { progress ->
                Log.d(TAG, "Sheet fetch progress: $progress")
            }

            if (sheetRowDataList.isEmpty()) {
                Log.w(TAG, "fetchAndUploadFromUrl: [STEP 4a] No sheet data returned. Processing 0 records.")
                return@withContext Result.success("No data found in the spreadsheet.")
            } else {
                Log.d(TAG, "fetchAndUploadFromUrl: [STEP 4b] Successfully fetched ${sheetRowDataList.size} sheet rows.")
            }

            Log.d(TAG, "fetchAndUploadFromUrl: [STEP 5] Converting sheet data to student records...")
            val studentRecords = sheetRowDataList.mapNotNull { sheetRow ->
                try {
                    convertSheetRowToStudentRecord(sheetRow)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to convert sheet row to student record: ${sheetRow.name}", e)
                    null
                }
            }

            Log.d(TAG, "fetchAndUploadFromUrl: [STEP 6] Starting to process ${studentRecords.size} student records...")
            var successCount = 0
            var errorCount = 0

            studentRecords.forEachIndexed { index, student ->
                Log.d(TAG, "fetchAndUploadFromUrl: [STEP 6.${index + 1}] Processing student record: ${student.name}")
                try {
                    val uploadSuccess = processStudentRecord(student)
                    if (uploadSuccess) {
                        successCount++
                        Log.d(TAG, "fetchAndUploadFromUrl: [STEP 6.${index + 1}a] Successfully processed student: ${student.name}")
                    } else {
                        errorCount++
                        Log.w(TAG, "fetchAndUploadFromUrl: [STEP 6.${index + 1}b] FAILED to fully process student: ${student.name}.")
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "fetchAndUploadFromUrl: [STEP 6.${index + 1}c] Exception while processing student: ${student.name}", e)
                }
            }

            val resultMessage = "Processing completed for URL. Total records fetched: ${studentRecords.size}. Successfully processed: $successCount, Errors: $errorCount."
            Log.i(TAG, "fetchAndUploadFromUrl: [STEP 7] FINISHED. Result: $resultMessage")

            if (errorCount > 0 && studentRecords.isNotEmpty()) {
                Result.failure(Exception(resultMessage + " Some records failed to process fully."))
            } else {
                Result.success(resultMessage)
            }

        } catch (e: Exception) {
            Log.e(TAG, "fetchAndUploadFromUrl: CRITICAL UNHANDLED EXCEPTION for URL: $url", e)
            Result.failure(Exception("Critical error during fetch/upload: ${e.localizedMessage}", e))
        } finally {
            Log.i(TAG, "--------------------------------------------------------------------")
            Log.i(TAG, "fetchAndUploadFromUrl: END OF PROCESS FOR URL: $url")
            Log.i(TAG, "--------------------------------------------------------------------")
        }
    }

    private fun loadGcsCredentials(): com.google.auth.oauth2.GoogleCredentials? {
        Log.d(TAG, "loadGcsCredentials: Attempting to load from R.raw.gcs_key")
        return try {
            context.resources.openRawResource(R.raw.gcs_key).use { inputStream ->
                ServiceAccountCredentials
                    .fromStream(inputStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
            }.also {
                Log.d(TAG, "loadGcsCredentials: Credentials loaded successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadGcsCredentials: FAILED to load GCS credentials", e)
            null
        }
    }

    private fun convertSheetRowToStudentRecord(sheetRow: SheetRowData): StudentRecord {
        val month = extractMonthFromDate(sheetRow.birthday)
        val studentId = generateStudentId(sheetRow.name, sheetRow.rowIndex)

        return StudentRecord(
            emailAddress = sheetRow.emailAddress,
            name = sheetRow.name,
            course = sheetRow.course,
            phoneNumber = sheetRow.phoneNumber,
            birthday = sheetRow.birthday,
            imageUrl = sheetRow.originalPhotoUrl,
            residenceInBangalore = sheetRow.residence,
            chavaraParticipation = sheetRow.chavaraPart,
            month = month,
            studentId = studentId
        )
    }

    private fun extractMonthFromDate(dateString: String): String {
        val formats = arrayOf(
            "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd",
            "dd-MM-yyyy", "MM-dd-yyyy", "yyyy/MM/dd",
            "d/M/yyyy", "M/d/yyyy"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US).apply { isLenient = false }
                val date = sdf.parse(dateString)
                if (date != null) {
                    val calendar = Calendar.getInstance().apply { time = date }
                    return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)?.lowercase(Locale.ENGLISH) ?: "unknown"
                }
            } catch (_: java.text.ParseException) { /* Try next format */ }
            catch (e: Exception) {
                Log.w(TAG, "extractMonthFromDate: Error parsing '$dateString' with '$format'", e)
            }
        }
        Log.w(TAG, "extractMonthFromDate: Could not parse '$dateString'. Fallback to string check.")
        return dateString.lowercase(Locale.ENGLISH).let {
            when {
                it.contains("jan") -> "january"; it.contains("feb") -> "february"
                it.contains("mar") -> "march"; it.contains("apr") -> "april"
                it.contains("may") -> "may"; it.contains("jun") -> "june"
                it.contains("jul") -> "july"; it.contains("aug") -> "august"
                it.contains("sep") -> "september"; it.contains("oct") -> "october"
                it.contains("nov") -> "november"; it.contains("dec") -> "december"
                else -> "unknown".also { Log.w(TAG, "Fallback failed for '$dateString'") }
            }
        }
    }

    private fun generateStudentId(name: String, index: Int): String {
        val cleanName = name.replace(Regex("[^a-zA-Z0-9]"), "").lowercase(Locale.ENGLISH)
        return "${cleanName}_${index}_${System.currentTimeMillis()}"
    }

    private suspend fun processStudentRecord(student: StudentRecord): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "processStudentRecord: START student: ${student.name} (Month: ${student.month}, ID: ${student.studentId})")
        var overallSuccess = true
        try {
            // Process image if available
            if (student.imageUrl.isNotBlank()) {
                Log.d(TAG, "processStudentRecord: Processing photo for ${student.name} from ${student.imageUrl}")
                val photoSuccess = downloadAndUploadFile(
                    student.imageUrl,
                    "students/${student.month}/${student.studentId}/photo.jpg",
                    "photo"
                )
                if (!photoSuccess) {
                    Log.w(TAG, "processStudentRecord: FAILED photo processing for ${student.name}")
                    overallSuccess = false
                } else {
                    Log.d(TAG, "processStudentRecord: Successfully processed photo for ${student.name}")
                }
            } else {
                Log.d(TAG, "processStudentRecord: No image URL for ${student.name}")
            }

            Log.d(TAG, "processStudentRecord: Uploading metadata for ${student.name}")
            val metadataSuccess = uploadStudentMetadata(student)
            if (!metadataSuccess) {
                Log.w(TAG, "processStudentRecord: FAILED metadata upload for ${student.name}")
                overallSuccess = false
            } else {
                Log.d(TAG, "processStudentRecord: Successfully uploaded metadata for ${student.name}")
            }

            Log.d(TAG, "processStudentRecord: FINISHED student: ${student.name}. Success: $overallSuccess")
            overallSuccess
        } catch (e: Exception) {
            Log.e(TAG, "processStudentRecord: CRITICAL error for student: ${student.name}", e)
            false
        }
    }

    private suspend fun downloadAndUploadFile(
        fileUrl: String,
        gcsPath: String,
        fileType: String
    ): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "downloadAndUploadFile: START $fileType. URL: $fileUrl, GCS: $gcsPath")
        try {
            Log.d(TAG, "downloadAndUploadFile: Processing GDrive URL: $fileUrl")
            val downloadUrl = processGoogleDriveUrl(fileUrl)
            Log.d(TAG, "downloadAndUploadFile: Effective download URL: $downloadUrl")

            Log.d(TAG, "downloadAndUploadFile: Downloading $fileType to memory from: $downloadUrl")
            val fileData = downloadFileToMemory(downloadUrl)
            if (fileData == null) {
                Log.e(TAG, "downloadAndUploadFile: FAILED download $fileType from: $downloadUrl")
                return@withContext false
            }
            Log.d(TAG, "downloadAndUploadFile: Downloaded ${fileData.size} bytes for $fileType.")

            Log.d(TAG, "downloadAndUploadFile: Uploading $fileType to GCS: $gcsPath")
            val contentType = getContentType(fileType, downloadUrl)
            val uploadSuccess = uploadToGcs(fileData, gcsPath, contentType)

            if (uploadSuccess) {
                Log.i(TAG, "downloadAndUploadFile: SUCCESS $fileType to GCS: $gcsPath")
            } else {
                Log.e(TAG, "downloadAndUploadFile: FAILED upload $fileType to GCS: $gcsPath")
            }
            uploadSuccess
        } catch (e: Exception) {
            Log.e(TAG, "downloadAndUploadFile: CRITICAL error $fileType. URL: $fileUrl, GCS: $gcsPath", e)
            false
        }
    }

    private fun downloadFileToMemory(urlString: String): ByteArray? {
        Log.d(TAG, "downloadFileToMemory: Attempting download from $urlString")
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        val outputStream = ByteArrayOutputStream()

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.inputStream
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                Log.d(TAG, "downloadFileToMemory: Successfully downloaded ${outputStream.size()} bytes from $urlString")
                return outputStream.toByteArray()
            } else {
                Log.e(TAG, "downloadFileToMemory: Download failed. HTTP Response Code: $responseCode from $urlString. Message: ${connection.responseMessage}")
                try {
                    connection.errorStream?.bufferedReader()?.use { Log.e(TAG, "Error stream: ${it.readText()}") }
                } catch (e: Exception) {
                    Log.w(TAG, "downloadFileToMemory: Could not read error stream.", e)
                }
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadFileToMemory: Exception during download from $urlString", e)
            return null
        } finally {
            try {
                outputStream.close()
                inputStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "downloadFileToMemory: Error closing streams/connection for $urlString", e)
            }
        }
    }

    private fun uploadToGcs(data: ByteArray, gcsPath: String, contentType: String): Boolean {
        Log.d(TAG, "uploadToGcs: Attempting to upload ${data.size} bytes to GCS path: $gcsPath with contentType: $contentType")
        val storage = getStorageService()
        if (storage == null) {
            Log.e(TAG, "uploadToGcs: Storage service is null. Cannot upload to $gcsPath.")
            return false
        }
        try {
            val blobId = BlobId.of(GCS_BUCKET_NAME, gcsPath)
            val blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build()
            storage.create(blobInfo, data)
            Log.i(TAG, "uploadToGcs: Successfully uploaded data to GCS: $gcsPath")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "uploadToGcs: FAILED to upload to GCS path $gcsPath", e)
            return false
        }
    }

    private fun uploadStudentMetadata(student: StudentRecord): Boolean {
        val gcsPath = "students/${student.month}/${student.studentId}/metadata.json"
        Log.d(TAG, "uploadStudentMetadata: Attempting to upload metadata for ${student.studentId} to $gcsPath")
        try {
            val metadataJson = """
            {
                "studentId": "${student.studentId}",
                "name": "${student.name}",
                "emailAddress": "${student.emailAddress}",
                "course": "${student.course}",
                "phoneNumber": "${student.phoneNumber}",
                "birthday": "${student.birthday}",
                "imageUrl": "${student.imageUrl}",
                "residenceInBangalore": "${student.residenceInBangalore ?: ""}",
                "chavaraParticipation": "${student.chavaraParticipation ?: ""}",
                "month": "${student.month}",
                "uploadTimestamp": "${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())}"
            }
        """.trimIndent()
            val metadataBytes = metadataJson.toByteArray(Charsets.UTF_8)
            return uploadToGcs(metadataBytes, gcsPath, "application/json")
        } catch (e: Exception) {
            Log.e(TAG, "uploadStudentMetadata: FAILED to create or upload metadata JSON for ${student.studentId}", e)
            return false
        }
    }

    private fun getContentType(fileType: String, url: String): String {
        return when (fileType.lowercase(Locale.ENGLISH)) {
            "photo" -> when {
                url.endsWith(".png", true) -> "image/png"
                url.endsWith(".gif", true) -> "image/gif"
                else -> "image/jpeg" // Default for photos
            }
            "video" -> when {
                url.endsWith(".mov", true) -> "video/quicktime"
                url.endsWith(".avi", true) -> "video/x-msvideo"
                else -> "video/mp4" // Default for videos
            }
            else -> "application/octet-stream" // Generic fallback
        }
    }

    private fun processGoogleDriveUrl(url: String): String {
        if (!url.contains("drive.google.com")) {
            Log.d(TAG, "processGoogleDriveUrl: URL is not a Google Drive URL: $url")
            return url
        }

        val fileIdRegexes = listOf(
            "drive\\.google\\.com/file/d/([^/?]+)".toRegex(),
            "drive\\.google\\.com/open\\?id=([^&/?]+)".toRegex()
        )

        var fileId: String? = null
        for (regex in fileIdRegexes) {
            val match = regex.find(url)
            if (match != null && match.groupValues.size > 1) {
                fileId = match.groupValues[1]
                Log.d(TAG, "processGoogleDriveUrl: Extracted File ID '$fileId' using regex $regex from URL $url")
                break
            }
        }

        return if (fileId != null) {
            if (url.contains("uc?export=download") && url.contains("id=$fileId")) {
                Log.d(TAG, "processGoogleDriveUrl: URL is already a direct download link: $url")
                url
            } else {
                val directDownloadUrl = "https://drive.google.com/uc?export=download&id=$fileId"
                Log.d(TAG, "processGoogleDriveUrl: Constructed direct download URL: $directDownloadUrl")
                directDownloadUrl
            }
        } else {
            Log.w(TAG, "processGoogleDriveUrl: Could not extract File ID from URL: $url. Returning original.")
            url
        }
    }

    fun cleanup() {
        Log.d(TAG, "GcsIntegrationHelper cleaned up")
    }
}