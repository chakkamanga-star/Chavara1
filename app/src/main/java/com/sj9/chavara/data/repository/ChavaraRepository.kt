package com.sj9.chavara.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.service.GoogleCloudStorageService
import com.sj9.chavara.data.service.GoogleSheetsService
import com.sj9.chavara.data.service.ImageDownloadService // Import the service
import com.sj9.chavara.data.service.SheetRowData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChavaraRepository(private val context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("chavara_prefs", Context.MODE_PRIVATE)

    // Instantiate the ImageDownloadService
    private val imageDownloadService = ImageDownloadService()

    private val googleSheetsService = try {
        GoogleSheetsService(context)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private val googleCloudStorageService = try {
        GoogleCloudStorageService(context)
    } catch (e: Exception) {
        e.printStackTrace()
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
            googleCloudStorageService?.let { service ->
                val members = service.loadFamilyMembers()
                _familyMembers.value = members
                val profile = service.loadUserProfile()
                _userProfile.value = profile
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                // --- NEW LOGIC TO DOWNLOAD AND UPLOAD IMAGES ---
                onProgress("Downloading and saving member photos...")
                newMembers = newMembers.map { member ->
                    if (imageDownloadService.isValidImageUrl(member.photoUrl)) {
                        val imageData = imageDownloadService.downloadImage(member.photoUrl)
                        if (imageData != null) {
                            val fileName = imageDownloadService.generateImageFileName(member.photoUrl, member.id)
                            val gcsUrl = googleCloudStorageService.uploadMediaFile(fileName, imageData, "image/jpeg")
                            if (gcsUrl != null) {
                                // Return a new FamilyMember object with the updated GCS URL
                                member.copy(photoUrl = gcsUrl)
                            } else {
                                member // Return original member if upload fails
                            }
                        } else {
                            member // Return original member if download fails
                        }
                    } else {
                        member // Return original member if URL is not valid
                    }
                }
                // --- END OF NEW LOGIC ---

                val saved = googleCloudStorageService.saveFamilyMembers(newMembers)
                if (saved) {
                    _familyMembers.value = newMembers
                    sharedPrefs.edit {
                        putString("last_spreadsheet_url", spreadsheetUrl)
                        putLong("last_sync_time", System.currentTimeMillis())
                    }
                    Result.success("Successfully loaded and saved ${newMembers.size} members")
                } else {
                    Result.failure(Exception("Failed to save data to cloud storage"))
                }
            } else {
                Result.failure(Exception("No data found in the spreadsheet"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        return try {
            val googleCloudStorageService = this.googleCloudStorageService ?: return false
            val saved = googleCloudStorageService.saveFamilyMember(member)
            if (saved) {
                val currentMembers = _familyMembers.value.toMutableList()
                val existingIndex = currentMembers.indexOfFirst { it.id == member.id }
                if (existingIndex >= 0) {
                    currentMembers[existingIndex] = member
                } else {
                    currentMembers.add(member)
                }
                _familyMembers.value = currentMembers
                googleCloudStorageService.saveFamilyMembers(currentMembers)
            }
            saved
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
            e.printStackTrace()
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
            e.printStackTrace()
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
}