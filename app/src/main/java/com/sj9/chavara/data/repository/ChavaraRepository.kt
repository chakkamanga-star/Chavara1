package com.sj9.chavara.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.service.GoogleCloudStorageService
import com.sj9.chavara.data.service.GoogleSheetsService
import com.sj9.chavara.data.service.ImageDownloadService
import com.sj9.chavara.data.service.SheetRowData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class ChavaraRepository(context: Context) {
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

    suspend fun fetchDataFromSpreadsheet(
        spreadsheetUrl: String,
        onProgress: (String) -> Unit = {}
    ): Result<String> {
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
                    Log.d("ChavaraRepo", "Checking image URL for ${member.name}: '${member.photoUrl}'")
                    if (imageDownloadService.isValidImageUrl(member.photoUrl)) {
                        Log.d("ChavaraRepo", "Attempting to download image for ${member.name} from URL: ${member.photoUrl}")
                        try {
                            val imageData = imageDownloadService.downloadImage(member.photoUrl)
                            if (imageData != null) {
                                // Log the MIME type and size to confirm what was downloaded
                                Log.d("ChavaraRepo", "Download successful for ${member.name}. File size: ${imageData.data.size} bytes. MIME Type: ${imageData.mimeType}")

                                // Pass the MIME type to the filename generator to preserve the extension
                                val fileName = imageDownloadService.generateImageFileName(member.id, imageData.mimeType)
                                Log.d("ChavaraRepo", "Uploading image for ${member.name} to GCS as '$fileName'")
                                // Pass the correct MIME type to the upload service
                                val gcsUrl = googleCloudStorageService.uploadMediaFile(fileName, imageData.data, imageData.mimeType)

                                if (gcsUrl != null) {
                                    Log.d("ChavaraRepo", "Upload successful. GCS URL: $gcsUrl")
                                    member.copy(photoUrl = gcsUrl)
                                } else {
                                    Log.e("ChavaraRepo", "Failed to upload photo for ${member.name} to GCS.")
                                    onProgress("Failed to upload photo for ${member.name} to GCS.")
                                    member
                                }
                            } else {
                                Log.e("ChavaraRepo", "Failed to download image for ${member.name}. The download service returned null.")
                                onProgress("Failed to download photo for ${member.name}. URL may be invalid.")
                                member
                            }
                        } catch (e: Exception) {
                            onProgress("Error processing photo for ${member.name}")
                            Log.e("ChavaraRepo", "Error during image download/upload for ${member.name}: ${e.message}", e)
                            member
                        }
                    } else {
                        Log.d("ChavaraRepo", "Skipping image download for ${member.name} as URL is not valid.")
                        member
                    }
                }

                val saveListResult = newMembers.map { member ->
                    googleCloudStorageService.saveFamilyMember(member)
                }

                if (saveListResult.all { it }) {
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
            Log.e("ChavaraRepo", "Error during spreadsheet fetch: ${e.message}", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    fun getTodaysBirthdayMembers(): List<FamilyMember> {
        return _familyMembers.value.filter { member -> member.isBirthdayToday() }
    }

    fun getMembersByMonth(): Map<Int, List<FamilyMember>> {
        return _familyMembers.value.groupBy { member -> member.getBirthMonth() }
    }

    suspend fun saveFamilyMember(member: FamilyMember): Boolean {
        _isLoading.value = true
        return try {
            val googleCloudStorageService = this.googleCloudStorageService ?: return false
            val savedIndividual = googleCloudStorageService.saveFamilyMember(member)
            if (savedIndividual) {
                val currentMembers = _familyMembers.value.toMutableList()
                val existingIndex = currentMembers.indexOfFirst { it.id == member.id }
                if (existingIndex >= 0) {
                    currentMembers[existingIndex] = member
                } else {
                    currentMembers.add(member)
                }
                _familyMembers.value = currentMembers
                true
            } else {
                Log.e("ChavaraRepo", "Failed to save individual member file.")
                false
            }
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error saving member: ${e.message}", e)
            false
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deleteFamilyMember(memberId: Int): Boolean {
        return try {
            val googleCloudStorageService = this.googleCloudStorageService ?: return false
            val deleted = googleCloudStorageService.deleteFamilyMember(memberId)
            if (deleted) {
                val currentMembers = _familyMembers.value.toMutableList()
                currentMembers.removeAll { it.id == memberId }
                _familyMembers.value = currentMembers
            }
            deleted
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error deleting member: ${e.message}", e)
            false
        }
    }

    suspend fun saveUserProfile(profile: FamilyMember): Boolean {
        return try {
            val googleCloudStorageService = this.googleCloudStorageService ?: return false
            val saved = googleCloudStorageService.saveUserProfile(profile.copy(isCurrentUserProfile = true))
            if (saved) {
                _userProfile.value = profile
            }
            saved
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error saving user profile: ${e.message}", e)
            false
        }
    }

    suspend fun resetAppData(): Boolean {
        return try {
            val cloudReset = googleCloudStorageService?.resetAllData() ?: true
            if (cloudReset) {
                _familyMembers.value = emptyList()
                _userProfile.value = null
                sharedPrefs.edit { clear() }
                Log.d("ChavaraRepo", "Local data reset successfully.")
            } else {
                Log.w("ChavaraRepo", "Cloud data reset failed. Local data NOT reset to prevent inconsistency.")
            }
            cloudReset
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Exception during resetAppData", e)
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
                submissionDate = sheetRow.submissionDate,
                isCurrentUserProfile = false
            )
        }
    }

    fun getLastSyncInfo(): Pair<String?, Long> {
        val url = sharedPrefs.getString("last_spreadsheet_url", null)
        val time = sharedPrefs.getLong("last_sync_time", 0L)
        return Pair(url, time)
    }

    fun getNewFamilyMemberId(): Int {
        val currentMaxId = _familyMembers.value.maxByOrNull { it.id }?.id ?: 0
        return currentMaxId + 1
    }}
