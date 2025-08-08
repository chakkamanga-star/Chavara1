package com.sj9.chavara.data.service

import android.content.Context
import android.util.Log
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents a raw row of data fetched from the Google Sheet.
 * This service will transform sheet rows into this structure.
 */
data class SheetRowData(
    val rowIndex: Int, // Original row index in the sheet (for reference/logging)
    val name: String,
    val course: String,
    val birthday: String,
    val phoneNumber: String,
    val residence: String,
    val emailAddress: String,
    val chavaraPart: String,
    val originalPhotoUrl: String, // The URL as found in the sheet
    val originalVideoUrl: String, // The URL as found in the sheet
)

class GoogleSheetsService(private val context: Context) {

    companion object {
        private const val TAG = "GoogleSheetsService"
    }

    private var _sheetsService: Sheets? = null
    private var _sheetsInitialized = false

    @Synchronized
    private fun getSheetsService(): Sheets? {
        if (!_sheetsInitialized) {
            try {
                Log.d(TAG, "Initializing Sheets service...")
                val credentials = ServiceAccountManager.getSheetsCredentials(context)

                if (credentials == null) {
                    Log.e(TAG, "Failed to load credentials from ServiceAccountManager. Sheets service will be null.")
                    _sheetsService = null
                } else {
                    Log.d(TAG, "Credentials loaded successfully, creating Sheets service...")
                    _sheetsService = Sheets.Builder(
                        NetHttpTransport(),
                        GsonFactory.getDefaultInstance(),
                        HttpCredentialsAdapter(credentials)
                    )
                        .setApplicationName("Chavara Youth App")
                        .build()
                    Log.d(TAG, "Sheets service created successfully.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Sheets service", e)
                _sheetsService = null
            } finally {
                _sheetsInitialized = true
            }
        }
        return _sheetsService
    }

    private fun extractSpreadsheetId(url: String): String? {
        return try {
            Log.d(TAG, "Extracting spreadsheet ID from URL: $url")
            val regex = "/spreadsheets/d/([a-zA-Z0-9-_]+)".toRegex()
            val result = regex.find(url)?.groupValues?.get(1)
            if (result == null) {
                Log.w(TAG, "Could not extract spreadsheet ID from URL: $url")
            } else {
                Log.d(TAG, "Extracted spreadsheet ID: $result")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract spreadsheet ID from URL: $url", e)
            null
        }
    }

    /**
     * Auto-detects the number of columns and sheet name, then fetches data accordingly
     */
    private suspend fun detectAndFetchSheetData(
        sheets: Sheets,
        spreadsheetId: String,
        onProgress: (String) -> Unit
    ): ValueRange? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "detectAndFetchSheetData: Getting spreadsheet metadata...")
            onProgress("Analyzing spreadsheet structure...")

            // Get spreadsheet metadata to find sheet names
            val spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute()
            val sheetsList = spreadsheet.sheets

            if (sheetsList.isNullOrEmpty()) {
                Log.e(TAG, "detectAndFetchSheetData: No sheets found in spreadsheet")
                return@withContext null
            }

            // Try the first sheet (usually the main data sheet)
            val firstSheet = sheetsList[0]
            val sheetName = firstSheet.properties.title
            Log.d(TAG, "detectAndFetchSheetData: Using first sheet: '$sheetName'")

            // Get the dimensions to determine column range
            val sheetProperties = firstSheet.properties
            val columnCount = sheetProperties.gridProperties?.columnCount ?: 26 // Default to Z if unknown
            Log.d(TAG, "detectAndFetchSheetData: Sheet has $columnCount columns")

            // Convert column count to letter range (A, B, C... Z, AA, AB...)
            val endColumn = getColumnLetter(columnCount - 1) // -1 because it's 0-indexed
            val range = "'$sheetName'!A:$endColumn"

            Log.d(TAG, "detectAndFetchSheetData: Using auto-detected range: $range")
            onProgress("Fetching data using range: $range")

            // Fetch data with the detected range
            val response = sheets.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()

            Log.d(TAG, "detectAndFetchSheetData: Successfully fetched data with auto-detected range")
            return@withContext response

        } catch (e: Exception) {
            Log.e(TAG, "detectAndFetchSheetData: Failed to auto-detect and fetch", e)
            // Fallback to simple range
            Log.d(TAG, "detectAndFetchSheetData: Falling back to simple A:Z range")
            return@withContext try {
                sheets.spreadsheets().values()
                    .get(spreadsheetId, "A:Z")
                    .execute()
            } catch (fallbackException: Exception) {
                Log.e(TAG, "detectAndFetchSheetData: Even fallback failed", fallbackException)
                null
            }
        }
    }

    /**
     * Converts column index to Excel-style column letters (A, B, C... Z, AA, AB...)
     */
    private fun getColumnLetter(columnIndex: Int): String {
        var index = columnIndex
        var result = ""

        while (index >= 0) {
            result = ('A' + (index % 26)) + result
            index = index / 26 - 1
        }

        return result
    }

    suspend fun fetchRawSheetData(
        spreadsheetUrl: String,
        onProgress: (String) -> Unit = {}
    ): List<SheetRowData> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting fetchRawSheetData for URL: $spreadsheetUrl")
            onProgress("Initializing Google Sheets connection...")

            val sheets = getSheetsService()
            if (sheets == null) {
                val errorMsg = "Google Sheets service not available. Check credentials and ServiceAccountManager."
                Log.e(TAG, errorMsg)
                onProgress("Error: $errorMsg")
                return@withContext emptyList<SheetRowData>()
            }
            Log.d(TAG, "Sheets service obtained.")

            val spreadsheetId = extractSpreadsheetId(spreadsheetUrl)
            if (spreadsheetId == null) {
                val errorMsg = "Invalid Google Sheets URL format: $spreadsheetUrl"
                Log.e(TAG, errorMsg)
                onProgress("Error: $errorMsg")
                return@withContext emptyList<SheetRowData>()
            }
            Log.d(TAG, "Spreadsheet ID: $spreadsheetId")

            Log.d(TAG, "Auto-detecting sheet structure and fetching data...")
            onProgress("Auto-detecting sheet structure...")

            val response: ValueRange? = detectAndFetchSheetData(sheets, spreadsheetId, onProgress)

            if (response == null) {
                Log.e(TAG, "Failed to fetch data from sheet after trying auto-detection and fallback")
                onProgress("Error: Failed to fetch data from spreadsheet")
                return@withContext emptyList<SheetRowData>()
            }

            Log.d(TAG, "Raw response received")

            val values = response.getValues()
            if (values.isNullOrEmpty()) {
                Log.w(TAG, "No data found in spreadsheet.")
                onProgress("No data found in the spreadsheet.")
                return@withContext emptyList<SheetRowData>()
            }

            Log.d(TAG, "Found ${values.size} raw rows of data.")
            if (values.size <= 1) { // Only header or empty
                Log.w(TAG, "Spreadsheet contains no data rows (only header or empty).")
                onProgress("Spreadsheet contains no data rows.")
                return@withContext emptyList<SheetRowData>()
            }
            onProgress("Processing ${values.size - 1} data rows...")

            val rawDataList = mutableListOf<SheetRowData>()

            for (i in 1 until values.size) { // Start from 1 to skip header
                val row = values[i]
                Log.d(TAG, "Processing sheet row ${i + 1} (index $i): $row")

                if (row.isNullOrEmpty()) {
                    Log.w(TAG, "Skipping empty row at sheet row ${i + 1}")
                    continue
                }

                val name = getValueAt(row, 0)
                if (name.isEmpty()) {
                    Log.w(TAG, "Skipping row at sheet row ${i + 1}: Name is empty.")
                    onProgress("Skipping row ${i + 1}: Empty name.")
                    continue
                }

                onProgress("Reading $name... (Row ${i + 1}/${values.size})")

                val sheetRow = SheetRowData(
                    rowIndex = i,
                    name = name,
                    course = getValueAt(row, 1),
                    birthday = getValueAt(row, 2),
                    phoneNumber = getValueAt(row, 3),
                    residence = getValueAt(row, 4),
                    emailAddress = getValueAt(row, 5),
                    chavaraPart = getValueAt(row, 6),
                    originalPhotoUrl = getValueAt(row, 7),
                    originalVideoUrl = getValueAt(row, 8)
                )
                rawDataList.add(sheetRow)
                Log.d(TAG, "Added SheetRowData for: $name")
            }

            Log.i(TAG, "Successfully processed ${rawDataList.size} rows into SheetRowData.")
            onProgress("Sheet data processing complete. Found ${rawDataList.size} valid entries.")
            return@withContext rawDataList

        } catch (e: Exception) {
            val errorMsg = "Failed to fetch and process sheet data: ${e.localizedMessage}"
            Log.e(TAG, errorMsg, e)
            onProgress("Error: ${e.message ?: "Unknown error while processing sheet."}")
            return@withContext emptyList<SheetRowData>()
        }
    }

    private fun getValueAt(row: List<Any>, index: Int): String {
        return try {
            if (index < row.size && row[index] != null) {
                row[index].toString().trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting value at index $index from row $row: ${e.message}")
            ""
        }
    }

    suspend fun validateSpreadsheetUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Validating spreadsheet URL: $url")

            val sheets = getSheetsService()
            if (sheets == null) {
                Log.e(TAG, "Sheets service not available for URL validation.")
                return@withContext false
            }

            val spreadsheetId = extractSpreadsheetId(url)
            if (spreadsheetId == null) {
                Log.e(TAG, "Invalid URL format for validation: $url")
                return@withContext false
            }

            sheets.spreadsheets()
                .get(spreadsheetId)
                .setFields("spreadsheetId")
                .execute()

            Log.d(TAG, "Spreadsheet URL validation successful for ID: $spreadsheetId")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Spreadsheet URL validation failed for URL: $url", e)
            return@withContext false
        }
    }
}