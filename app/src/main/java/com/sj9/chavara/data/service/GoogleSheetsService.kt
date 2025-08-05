package com.sj9.chavara.data.service

import android.content.Context
import android.util.Log
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.auth.oauth2.GoogleCredentials
import com.sj9.chavara.data.model.FamilyMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleSheetsService(private val context: Context) {

    private val imageDownloadService = ImageDownloadService(context)
    private val gcsService = GoogleCloudStorageService(context)

    private var _sheetsService: Sheets? = null
    private var _sheetsInitialized = false

    @Suppress("GetterSetterNames")
    private fun getSheetsService(): Sheets? {
        if (!_sheetsInitialized) {
            try {
                val credentials = ServiceAccountManager.getSheetsCredentials(context)
                _sheetsService = credentials?.let { creds ->
                    Sheets.Builder(
                        NetHttpTransport(),
                        GsonFactory.getDefaultInstance()
                    ) { request ->
                        creds.refresh()
                        request.headers.authorization = "Bearer ${creds.accessToken.tokenValue}"
                        request
                    }
                        .setApplicationName("Chavara Youth App")
                        .build()
                }
                _sheetsInitialized = true
            } catch (e: Exception) {
                Log.e("GoogleSheetsService", "Failed to initialize Sheets service", e)
                _sheetsService = null
                _sheetsInitialized = true
            }
        }
        return _sheetsService
    }

    @Suppress("NewApi")
    private fun extractSpreadsheetId(url: String): String? {
        val regex = "/spreadsheets/d/([a-zA-Z0-9-_]+)".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    suspend fun fetchFamilyMembers(
        spreadsheetUrl: String,
        onProgress: (String) -> Unit = {}
    ): List<FamilyMember> = withContext(Dispatchers.IO) {
        try {
            val sheets = getSheetsService()
                ?: throw IllegalStateException("Google Sheets service not available. Check sheets_key.json file.")

            val spreadsheetId = extractSpreadsheetId(spreadsheetUrl)
                ?: throw IllegalArgumentException("Invalid Google Sheets URL")

            val range = "Sheet1!A:I"

            val response = sheets.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()

            val values = response.values ?: emptyList()

            val familyMembers = mutableListOf<FamilyMember>()
            val totalRows = values.size - 1

            onProgress("Processing $totalRows members...")

            for (i in 1 until values.size) {
                val row = values[i]
                if (row.size >= 8) {
                    val originalPhotoUrl = getValueAt(row, 7)
                    val originalVideoUrl = if (row.size > 8) getValueAt(row, 8) else ""

                    val memberName = getValueAt(row, 0)
                    onProgress("Processing $memberName... ($i/$totalRows)")

                    var storedPhotoUrl = originalPhotoUrl
                    if (imageDownloadService.isValidImageUrl(originalPhotoUrl)) {
                        onProgress("Downloading image for $memberName...")
                        val imageData = imageDownloadService.downloadImage(originalPhotoUrl)
                        if (imageData != null) {
                            onProgress("Uploading image for $memberName...")
                            val fileName = imageDownloadService.generateImageFileName(originalPhotoUrl, i)
                            val uploadedUrl = gcsService.uploadMediaFile(fileName, imageData, "image/jpeg")
                            if (uploadedUrl != null) {
                                storedPhotoUrl = uploadedUrl
                            }
                        }
                    }

                    var storedVideoUrl = originalVideoUrl
                    if (originalVideoUrl.isNotEmpty() && (originalVideoUrl.contains("drive.google.com") ||
                                originalVideoUrl.contains(".mp4", ignoreCase = true))) {
                        storedVideoUrl = originalVideoUrl
                    } else if (originalVideoUrl.isNotEmpty()) {
                        Log.w("GoogleSheetsService", "Invalid video URL for $memberName: $originalVideoUrl")
                    }

                    val member = FamilyMember(
                        id = i,
                        name = getValueAt(row, 0),
                        course = getValueAt(row, 1),
                        birthday = getValueAt(row, 2),
                        phoneNumber = getValueAt(row, 3),
                        residence = getValueAt(row, 4),
                        emailAddress = getValueAt(row, 5),
                        chavaraPart = getValueAt(row, 6),
                        photoUrl = storedPhotoUrl,
                        videoUrl = storedVideoUrl,
                        submissionDate = getCurrentTimestamp()
                    )
                    familyMembers.add(member)
                } else {
                    Log.w("GoogleSheetsService", "Skipping row $i: Insufficient columns (${row.size} < 8)")
                }
            }

            familyMembers
        } catch (e: Exception) {
            Log.e("GoogleSheetsService", "Failed to fetch family members", e)
            emptyList()
        }
    }

    private fun getValueAt(row: List<Any>, index: Int): String {
        return if (index < row.size) row[index].toString().trim() else ""
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
    }

    suspend fun validateSpreadsheetUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val sheets = getSheetsService() ?: return@withContext false

            val spreadsheetId = extractSpreadsheetId(url) ?: return@withContext false

            val response = sheets.spreadsheets()
                .get(spreadsheetId)
                .execute()

            response != null
        } catch (e: Exception) {
            Log.e("GoogleSheetsService", "Spreadsheet validation failed", e)
            false
        }
    }
}