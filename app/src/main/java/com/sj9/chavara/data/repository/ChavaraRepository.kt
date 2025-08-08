package com.sj9.chavara.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit // Add for KTX extension
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.service.GoogleCloudStorageService
import com.sj9.chavara.data.service.GoogleSheetsService
import com.sj9.chavara.data.service.SheetRowData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository class for managing app data and synchronization with cloud services
 * Handles business logic and state management
 */
class ChavaraRepository(private val context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("chavara_prefs", Context.MODE_PRIVATE)

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

    // State flows for reactive data
    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    private val _userProfile = MutableStateFlow<FamilyMember?>(null)
    val userProfile: StateFlow<FamilyMember?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Load initial data
    suspend fun initialize() {
        _isLoading.value = true
        try {
            // Load data from cloud storage if available
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
            // Check if services are available
            if (googleSheetsService == null || googleCloudStorageService == null) {
                return Result.failure(Exception("Google services not available. Please check your service account files."))
            }

            // Validate URL first
            if (!googleSheetsService.validateSpreadsheetUrl(spreadsheetUrl)) {
                return Result.failure(Exception("Invalid or inaccessible spreadsheet URL"))
            }

            // Fetch data from sheets with progress updates
            val rawSheetData = googleSheetsService.fetchRawSheetData(spreadsheetUrl, onProgress)
            val newMembers = transformSheetDataToFamilyMembers(rawSheetData)

            if (newMembers.isNotEmpty()) {
                // Save to cloud storage
                val saved = googleCloudStorageService.saveFamilyMembers(newMembers)

                if (saved) {
                    // Update local state
                    _familyMembers.value = newMembers

                    // Save spreadsheet URL for future reference (using KTX)
                    sharedPrefs.edit {
                        putString("last_spreadsheet_url", spreadsheetUrl)
                        putLong("last_sync_time", System.currentTimeMillis())
                    }

                    Result.success("Successfully loaded ${newMembers.size} members")
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

    /**
     * Get members celebrating birthday today
     */
    fun getTodaysBirthdayMembers(): List<FamilyMember> {
        return _familyMembers.value.filter { it.isBirthdayToday() }
    }

    /**
     * Get members organized by birth month
     */
    fun getMembersByMonth(): Map<Int, List<FamilyMember>> {
        return _familyMembers.value.groupBy { it.getBirthMonth() }
    }

    /**
     * Add or update family member with state management
     */
    suspend fun saveFamilyMember(member: FamilyMember): Boolean {
        return try {
            // Save to cloud storage if available
            val googleCloudStorageService = this.googleCloudStorageService ?: return false

            val saved = googleCloudStorageService.saveFamilyMember(member)

            if (saved) {
                // Update local state
                val currentMembers = _familyMembers.value.toMutableList()
                val existingIndex = currentMembers.indexOfFirst { it.id == member.id }

                if (existingIndex >= 0) {
                    currentMembers[existingIndex] = member
                } else {
                    currentMembers.add(member)
                }

                _familyMembers.value = currentMembers

                // Also save the complete list
                googleCloudStorageService.saveFamilyMembers(currentMembers)
            }

            saved
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete family member with state management
     */
    suspend fun deleteFamilyMember(memberId: Int): Boolean {
        return try {
            // Delete from cloud storage if available
            val googleCloudStorageService = this.googleCloudStorageService ?: return false

            val deleted = googleCloudStorageService.deleteFamilyMember(memberId)

            if (deleted) {
                // Update local state
                val currentMembers = _familyMembers.value.toMutableList()
                currentMembers.removeAll { it.id == memberId }
                _familyMembers.value = currentMembers

                // Save updated list
                googleCloudStorageService.saveFamilyMembers(currentMembers)
            }

            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Save user profile with state management
     */
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

    /**
     * Reset all app data with state management
     */
    suspend fun resetAppData(): Boolean {
        return try {
            val cloudReset = googleCloudStorageService?.resetAllData() ?: true

            if (cloudReset) {
                // Clear local state
                _familyMembers.value = emptyList()
                _userProfile.value = null

                // Clear SharedPreferences using KTX
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

    /**
     * Get member by ID
     */
    fun getMemberById(id: Int): FamilyMember? {
        return _familyMembers.value.find { it.id == id }
    }

    /**
     * Transform raw sheet data to FamilyMember objects
     */
    private fun transformSheetDataToFamilyMembers(rawSheetData: List<SheetRowData>): List<FamilyMember> {
        return rawSheetData.mapIndexed { index, sheetRow ->
            FamilyMember(
                id = index + 1, // Generate sequential IDs
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

    /**
     * Get last sync information
     */
    fun getLastSyncInfo(): Pair<String?, Long> {
        val url = sharedPrefs.getString("last_spreadsheet_url", null)
        val time = sharedPrefs.getLong("last_sync_time", 0L)
        return Pair(url, time)
    }
}