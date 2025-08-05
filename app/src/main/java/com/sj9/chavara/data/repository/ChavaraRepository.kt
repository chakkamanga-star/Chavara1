package com.sj9.chavara.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.service.GoogleCloudStorageService
import com.sj9.chavara.data.service.GoogleSheetsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.Result // Added import for Result

/**
 * Repository class for managing app data and synchronization with cloud services
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
            if (googleCloudStorageService != null) {
                val members = googleCloudStorageService.loadFamilyMembers()
                _familyMembers.value = members

                val profile = googleCloudStorageService.loadUserProfile()
                _userProfile.value = profile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Fetch data from Google Sheets and sync with cloud storage
     */
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
            val newMembers = googleSheetsService.fetchFamilyMembers(spreadsheetUrl, onProgress)

            if (newMembers.isNotEmpty()) {
                // Save to cloud storage
                val saved = googleCloudStorageService.saveFamilyMembers(newMembers)

                if (saved) {
                    // Update local state
                    _familyMembers.value = newMembers

                    // Save spreadsheet URL for future reference
                    sharedPrefs.edit()
                        .putString("last_spreadsheet_url", spreadsheetUrl)
                        .putLong("last_sync_time", System.currentTimeMillis())
                        .apply()

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
     * Add or update family member
     */
    suspend fun saveFamilyMember(member: FamilyMember): Boolean {
        return try {
            // Save to cloud storage if available
            if (googleCloudStorageService == null) return false

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
     * Delete family member
     */
    suspend fun deleteFamilyMember(memberId: Int): Boolean {
        return try {
            // Delete from cloud storage if available
            if (googleCloudStorageService == null) return false

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
     * Save user profile
     */
    suspend fun saveUserProfile(profile: FamilyMember): Boolean {
        return try {
            if (googleCloudStorageService == null) return false

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
     * Reset all app data
     */
    suspend fun resetAppData(): Boolean {
        return try {
            // Reset cloud storage if available
            val cloudReset = if (googleCloudStorageService != null) {
                googleCloudStorageService.resetAllData()
            } else {
                true // If no cloud service, just reset local
            }

            if (cloudReset) {
                // Clear local state
                _familyMembers.value = emptyList()
                _userProfile.value = null

                // Clear shared preferences
                sharedPrefs.edit().clear().apply()
            }

            cloudReset
        } catch (e: Exception) {
            e.printStackTrace()
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
     * Get last sync information
     */
    fun getLastSyncInfo(): Pair<String?, Long> {
        val url = sharedPrefs.getString("last_spreadsheet_url", null)
        val time = sharedPrefs.getLong("last_sync_time", 0L)
        return Pair(url, time)
    }
}