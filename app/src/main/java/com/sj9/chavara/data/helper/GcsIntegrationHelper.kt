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
            Result.failure(Exception("Critical