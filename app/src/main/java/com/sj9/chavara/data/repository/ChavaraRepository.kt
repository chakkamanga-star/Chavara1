package com.sj9.chavara.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.service.DataSyncWorker
import com.sj9.chavara.data.service.GoogleCloudStorageService
import com.sj9.chavara.data.service.GoogleSheetsService
import com.sj9.chavara.data.service.ImageDownloadService
import com.sj9.chavara.data.service.SheetRowData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    suspend fun initialize() {
        _isLoading.value = true
        try {
            if (googleCloudStorageService == null) {
                Log.e("ChavaraRepo", "GoogleCloudStorageService is null, cannot initialize.")
                return
            }
            val members = googleCloudStorageService.loadFamilyMembers()
            _familyMembers.value = members
            val profile = googleCloudStorageService.loadUserProfile()
            _userProfile.value = profile
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error during initialization", e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Enqueues a background worker to fetch data from the spreadsheet.
     * This operation will continue even if the app is backgrounded.
     */
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


    // This function is now intended to be called from the background worker
    suspend fun fetchDataFromSpreadsheet(
        spreadsheetUrl: String,
        onProgress: (String) -> Unit = {}
    ): Result<String> {
        // This function remains largely the same but will be executed by the Worker
        _isLoading.value = true
        return try {
            if (googleSheetsService == null || googleCloudStorageService == null) {
                return Result.failure(Exception("Google services not available."))
            }

            if (!googleSheetsService.validateSpreadsheetUrl(spreadsheetUrl)) {
                return Result.failure(Exception("Invalid or inaccessible spreadsheet URL"))
            }

            val rawSheetData = googleSheetsService.fetchRawSheetData(spreadsheetUrl, onProgress)
            var newMembers = transformSheetDataToFamilyMembers(rawSheetData)

            if (newMembers.isNotEmpty()) {
                onProgress("Downloading and saving member photos...")
                newMembers = newMembers.map { member ->
                    if (imageDownloadService.isValidImageUrl(member.photoUrl)) {
                        Log.d("ChavaraRepo", "Attempting to download image for ${member.name} from URL: ${member.photoUrl}")
                        try {
                            val imageData = imageDownloadService.downloadImage(member.photoUrl)
                            if (imageData != null) {
                                val fileName = imageDownloadService.generateImageFileName(member.id, imageData.mimeType)
                                val gcsUrl = googleCloudStorageService.uploadMediaFile(fileName, imageData.data, imageData.mimeType)

                                if (gcsUrl != null) {
                                    Log.d("ChavaraRepo", "Upload successful. GCS URL: $gcsUrl")
                                    member.copy(photoUrl = gcsUrl)
                                } else {
                                    Log.e("ChavaraRepo", "Failed to upload photo for ${member.name} to GCS.")
                                    onProgress("Failed to upload photo for ${member.name}.")
                                    member
                                }
                            } else {
                                Log.e("ChavaraRepo", "Failed to download image for ${member.name}.")
                                onProgress("Failed to download photo for ${member.name}.")
                                member
                            }
                        } catch (e: Exception) {
                            onProgress("Error processing photo for ${member.name}")
                            Log.e("ChavaraRepo", "Error during image download/upload for ${member.name}", e)
                            member
                        }
                    } else {
                        member
                    }
                }

                val saveResults = newMembers.map { member ->
                    googleCloudStorageService.saveFamilyMember(member)
                }

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

    fun getTodaysBirthdayMembers(): List<FamilyMember> {
        return _familyMembers.value.filter { it.isBirthdayToday() }
    }

    fun getMembersByMonth(): Map<Int, List<FamilyMember>> {
        return _familyMembers.value.groupBy { it.getBirthMonth() }
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

    fun getMemberById(id: Int): FamilyMember? {
        return _familyMembers.value.find { it.id == id }
    }

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

    fun getNewFamilyMemberId(): Int {
        return (_familyMembers.value.maxByOrNull { it.id }?.id ?: 0) + 1
    }
}
