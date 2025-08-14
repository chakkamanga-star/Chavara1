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


class ChavaraRepository(private val context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("chavara_prefs", Context.MODE_PRIVATE)
    private val imageDownloadService = ImageDownloadService()

    private val googleSheetsService = try {
        GoogleSheetsService(context)
    } catch (_: Exception) {
        null
    }

    private val googleCloudStorageService = try {
        GoogleCloudStorageService(context)
    } catch (_: Exception) {
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
            // FIX: Log if the service is null to make debugging easier
            if (googleCloudStorageService == null) {
                Log.e("ChavaraRepo", "GoogleCloudStorageService is null, cannot initialize.")
                return
            }
            val members = googleCloudStorageService.loadFamilyMembers()
            _familyMembers.value = members
            val profile = googleCloudStorageService.loadUserProfile()
            _userProfile.value = profile
        } catch (e: Exception) {
            // FIX: Log the exception instead of ignoring it
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
                    if (imageDownloadService.isValidImageUrl(member.photoUrl)) {
                        val imageData = imageDownloadService.downloadImage(member.photoUrl)
                        if (imageData != null) {
                            val fileName = imageDownloadService.generateImageFileName(member.photoUrl, member.id)
                            val contentType = imageDownloadService.getMimeType(member.photoUrl)
                            val gcsUrl = googleCloudStorageService.uploadMediaFile(fileName, imageData, contentType)

                            if (gcsUrl != null) {
                                member.copy(photoUrl = gcsUrl)
                            } else {
                                onProgress("Failed to upload photo for ${member.name}")
                                member
                            }
                        } else {
                            onProgress("Failed to download photo for ${member.name}")
                            member
                        }
                    } else {
                        member
                    }
                }

                // First, save the main list file as before
                val listSaved = googleCloudStorageService.saveFamilyMembers(newMembers)

                // **THE FIX: Now, loop and save each member individually**
                var individualSavesSucceeded = true
                onProgress("Saving individual member records...")
                for (member in newMembers) {
                    val success = googleCloudStorageService.saveFamilyMember(member)
                    if (!success) {
                        individualSavesSucceeded = false
                        Log.e("ChavaraRepo", "Failed to save individual record for ${member.name}")
                        // Decide if you want to stop or continue on failure
                    }
                }

                if (listSaved && individualSavesSucceeded) {
                    _familyMembers.value = newMembers
                    sharedPrefs.edit {
                        putString("last_spreadsheet_url", spreadsheetUrl)
                        putLong("last_sync_time", System.currentTimeMillis())
                    }
                    Result.success("Successfully loaded and saved ${newMembers.size} members")
                } else {
                    Result.failure(Exception("Failed to save all data to cloud storage. List save: $listSaved, Individual saves: $individualSavesSucceeded"))
                }
            } else {
                Result.failure(Exception("No data found in the spreadsheet"))
            }
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error during spreadsheet fetch: ${e.message}")
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
        // FIX: Remove the "double save" and update the local state more safely
        _isLoading.value = true
        return try {
            val googleCloudStorageService = this.googleCloudStorageService ?: return false
            // First, save the individual member file
            val savedIndividual = googleCloudStorageService.saveFamilyMember(member)
            if (savedIndividual) {
                // If successful, then update the local list
                val currentMembers = _familyMembers.value.toMutableList()
                val existingIndex = currentMembers.indexOfFirst { it.id == member.id }
                if (existingIndex >= 0) {
                    currentMembers[existingIndex] = member
                } else {
                    currentMembers.add(member)
                }
                // And then save the entire updated list
                val savedList = googleCloudStorageService.saveFamilyMembers(currentMembers)
                if(savedList) {
                    // Finally, update the state flow
                    _familyMembers.value = currentMembers
                    true
                } else {
                    // Rollback or error handling for list save failure
                    Log.e("ChavaraRepo", "Failed to save the updated members list.")
                    false
                }
            } else {
                Log.e("ChavaraRepo", "Failed to save individual member file.")
                false
            }
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error saving member: ${e.message}")
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
                googleCloudStorageService.saveFamilyMembers(currentMembers)
            }
            deleted
        } catch (e: Exception) {
            Log.e("ChavaraRepo", "Error deleting member: ${e.message}")
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
            Log.e("ChavaraRepo", "Error saving user profile: ${e.message}")
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