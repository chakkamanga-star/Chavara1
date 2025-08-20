package com.sj9.chavara.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.service.GoogleCloudStorageService
import com.sj9.chavara.data.service.GoogleSheetsService
import com.sj9.chavara.data.service.ImageDownloadService
import com.sj9.chavara.data.service.SheetRowData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Collections


class ChavaraRepository(private val context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("chavara_prefs", Context.MODE_PRIVATE)
    private val imageDownloadService = ImageDownloadService()

    private val googleSheetsService = try {
        GoogleSheetsService(context)
    } catch (e: Exception) {
        Log.e("ChavaraRepo", "Failed to initialize GoogleSheetsService", e)
        null
    }

    private val googleCloudStorageService = try {
        GoogleCloudStorageService(context)
    } catch (e: Exception) {
        Log.e("ChavaraRepo", "Failed to initialize GoogleCloudStorageService", e)
        null
    }

    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    private val _userProfile = MutableStateFlow<FamilyMember?>(null)
    val userProfile: StateFlow<FamilyMember?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val isInitialized = Collections.synchronizedSet(mutableSetOf<Boolean>())

    suspend fun initialize() {
        if (isInitialized.contains(true)) {
            Log.d("ChavaraRepo", "initialize: Already initialized. Skipping.")
            return
        }

        _isLoading.value = true
        Log.d("ChavaraRepo", "initialize: Starting repository initialization.")
        try {
            if (googleCloudStorageService == null) {
                Log.e("ChavaraRepo", "initialize: GoogleCloudStorageService is null. Aborting.")
                return
            }

            val membersList = mutableListOf<FamilyMember>()
            googleCloudStorageService.loadFamilyMembersFlow().collect { member ->
                membersList.add(member)
            }
            _familyMembers.value = membersList
            Log.i("ChavaraRepo", "initialize: Finished loading ${_familyMembers.value.size} members.")

            val profile = googleCloudStorageService.loadUserProfile()
            _userProfile.value = profile

            isInitialized.add(true)
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "initialize: Error during initialization", e)
        } finally {
            _isLoading.value = false
            Log.d("ChavaraRepo", "initialize: Initialization process finished.")
        }
    }

    fun triggerDataSyncFromSpreadsheet(spreadsheetUrl: String) {
        Log.d("ChavaraRepo", "Triggering background data sync.")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dataSyncRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .setInputData(workDataOf(DataSyncWorker.KEY_SPREADSHEET_URL to spreadsheetUrl))
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DataSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            dataSyncRequest
        )
    }

    suspend fun fetchDataFromSpreadsheet(
        spreadsheetUrl: String,
        onProgress: suspend (String) -> Unit = {}
    ): Result<String> {
        _isLoading.value = true
        return try {
            if (googleSheetsService == null || googleCloudStorageService == null) {
                return Result.failure(Exception("Google services not available."))
            }

            if (!googleSheetsService.validateSpreadsheetUrl(spreadsheetUrl)) {
                return Result.failure(Exception("Invalid or inaccessible spreadsheet URL"))
            }

            onProgress("Fetching data from spreadsheet...")
            val rawSheetData = googleSheetsService.fetchRawSheetData(spreadsheetUrl) { progressMessage ->
                // Handle progress in coroutine context
                CoroutineScope(Dispatchers.IO).launch {
                    onProgress(progressMessage)
                }
            }

            var newMembers = transformSheetDataToFamilyMembers(rawSheetData)

            if (newMembers.isNotEmpty()) {
                onProgress("Downloading and saving member photos...")
                newMembers = newMembers.map { member ->
                    if (imageDownloadService.isValidImageUrl(member.photoUrl)) {
                        try {
                            val imageData = imageDownloadService.downloadImage(member.photoUrl)
                            if (imageData != null) {
                                // Use the correct method name - likely uploadImage or saveImage
                                // With this:
                                val uploadedUrl: String? = googleCloudStorageService.uploadMediaFile(
                                    "member_${member.id}_photo.jpg",
                                    imageData.data,
                                    imageData.mimeType
                                )
                                if (uploadedUrl != null) {
                                    member.copy(photoUrl = uploadedUrl)
                                } else {
                                    member
                                }
                            } else {
                                member
                            }
                        } catch (e: Exception) {
                            Log.e("ChavaraRepo", "Error during image download/upload for ${member.name}", e)
                            member
                        }
                    } else {
                        member
                    }
                }

                onProgress("Saving members to cloud storage...")
                val saveResults = newMembers.map { googleCloudStorageService.saveFamilyMember(it) }

                if (saveResults.all { it }) {
                    _familyMembers.value = newMembers
                    sharedPrefs.edit {
                        putString("last_spreadsheet_url", spreadsheetUrl)
                        putLong("last_sync_time", System.currentTimeMillis())
                    }
                    Result.success("Successfully loaded and saved ${newMembers.size} members")
                } else {
                    Result.failure(Exception("Failed to save all members to cloud storage."))
                }
            } else {
                Result.failure(Exception("No data found in the spreadsheet"))
            }
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error during spreadsheet fetch", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    // Add these methods to ChavaraRepository class

    suspend fun uploadMemberPhoto(memberId: Int, imageData: ByteArray, mimeType: String): String? {
        return try {
            val fileName = "member_${memberId}_photo_${System.currentTimeMillis()}.${
                when(mimeType) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "image/gif" -> "gif"
                    else -> "jpg"
                }
            }"
            googleCloudStorageService?.uploadMediaFile(fileName, imageData, mimeType)
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error uploading photo for member $memberId", e)
            null
        }
    }

    suspend fun getAuthenticatedImageUrl(gcsUrl: String): String? {
        return googleCloudStorageService?.getAuthenticatedImageUrl(gcsUrl)
    }

    suspend fun validateSpreadsheetUrl(url: String): Boolean {
        return googleSheetsService?.validateSpreadsheetUrl(url) ?: false
    }

    fun getTodaysBirthdayMembers(): List<FamilyMember> = _familyMembers.value.filter { it.isBirthdayToday() }

    fun getMembersByMonth(): Map<Int, List<FamilyMember>> = _familyMembers.value.groupBy { it.getBirthMonth() }
    suspend fun searchMembersByName(query: String): List<FamilyMember> {
        return _familyMembers.value.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.course.contains(query, ignoreCase = true) ||
                    it.residence.contains(query, ignoreCase = true)
        }
    }
    suspend fun saveFamilyMember(member: FamilyMember): Boolean {
        _isLoading.value = true
        return try {
            val success = googleCloudStorageService?.saveFamilyMember(member) ?: false
            if (success) {
                val currentMembers = _familyMembers.value.toMutableList()
                val existingIndex = currentMembers.indexOfFirst { it.id == member.id }
                if (existingIndex != -1) {
                    currentMembers[existingIndex] = member
                } else {
                    currentMembers.add(member)
                }
                _familyMembers.value = currentMembers
            }
            success
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error saving member: ${member.id}", e)
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deleteFamilyMember(memberId: Int): Boolean {
        return try {
            val deleted = googleCloudStorageService?.deleteFamilyMember(memberId) ?: false
            if (deleted) {
                _familyMembers.value = _familyMembers.value.filter { it.id != memberId }
            }
            deleted
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error deleting member: $memberId", e)
            false
        }
    }

    suspend fun saveUserProfile(profile: FamilyMember): Boolean {
        return try {
            val saved = googleCloudStorageService?.saveUserProfile(profile.copy(isCurrentUserProfile = true)) ?: false
            if (saved) {
                _userProfile.value = profile
            }
            saved
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error saving user profile", e)
            false
        }
    }

    suspend fun resetAppData(): Boolean {
        return try {
            val cloudReset = googleCloudStorageService?.resetAllData() ?: false
            if (cloudReset) {
                _familyMembers.value = emptyList()
                _userProfile.value = null
                sharedPrefs.edit { clear() }
            }
            cloudReset
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Exception during app data reset", e)
            false
        }
    }

    fun getMemberById(id: Int): FamilyMember? = _familyMembers.value.find { it.id == id }

    private fun transformSheetDataToFamilyMembers(rawSheetData: List<SheetRowData>): List<FamilyMember> {
        return rawSheetData.mapIndexed { index, sheetRow ->
            FamilyMember(
                id = index + 1,
                name = sheetRow.name,
                course = sheetRow.course,
                birthday = sheetRow.birthday,
                phoneNumber = sheetRow.phoneNumber,
                residence = sheetRow.residence,
                emailAddress = sheetRow.emailAddress,
                chavaraPart = sheetRow.chavaraPart,
                photoUrl = sheetRow.originalPhotoUrl,
                videoUrl = sheetRow.originalVideoUrl,
                submissionDate = sheetRow.submissionDate
            )
        }
    }

    fun getLastSyncInfo(): Pair<String?, Long> {
        val url = sharedPrefs.getString("last_spreadsheet_url", null)
        val time = sharedPrefs.getLong("last_sync_time", 0L)
        return url to time
    }

    fun getNewFamilyMemberId(): Int = (_familyMembers.value.maxByOrNull { it.id }?.id ?: 0) + 1
}

/**
 * This CoroutineWorker is now part of the repository file.
 * It handles fetching data from Google Sheets in the background.
 */
class DataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "data_sync_work"
        const val KEY_SPREADSHEET_URL = "spreadsheet_url"
        const val KEY_PROGRESS = "progress"
        const val KEY_RESULT = "result"
        const val KEY_ERROR = "error"
        private const val TAG = "DataSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "DataSyncWorker started")

        val spreadsheetUrl = inputData.getString(KEY_SPREADSHEET_URL)
        if (spreadsheetUrl.isNullOrBlank()) {
            Log.e(TAG, "No spreadsheet URL provided")
            return Result.failure(
                workDataOf(KEY_ERROR to "No spreadsheet URL provided")
            )
        }

        return try {
            Log.d(TAG, "Creating repository and starting sync for URL: $spreadsheetUrl")
            val repository = ChavaraRepository(applicationContext)

            // Report initial progress - this is already in a coroutine context
            setProgress(workDataOf(KEY_PROGRESS to "Starting data sync..."))

            val result = repository.fetchDataFromSpreadsheet(spreadsheetUrl) { progressMessage ->
                Log.d(TAG, "Progress update: $progressMessage")
                // Since onProgress is now suspend, we can call setProgress directly
                setProgress(workDataOf(KEY_PROGRESS to progressMessage))
            }

            result.fold(
                onSuccess = { successMessage ->
                    Log.i(TAG, "Data sync completed successfully: $successMessage")
                    Result.success(workDataOf(KEY_RESULT to successMessage))
                },
                onFailure = { exception ->
                    val errorMessage = exception.message ?: "Unknown error occurred during sync"
                    Log.e(TAG, "Data sync failed: $errorMessage", exception)
                    Result.failure(workDataOf(KEY_ERROR to errorMessage))
                }
            )
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Worker execution failed"
            Log.e(TAG, "Exception in DataSyncWorker: $errorMessage", e)
            Result.failure(workDataOf(KEY_ERROR to errorMessage))
        }
    }
}