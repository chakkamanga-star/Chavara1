package com.sj9.chavara.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {

    private val _resetStatus = MutableStateFlow<String?>(null)
    val resetStatus: StateFlow<String?> = _resetStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val userProfile = repository.userProfile.asStateFlow()
    val familyMembers = repository.familyMembers.asStateFlow()

    fun saveUserProfile(profile: FamilyMember) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.saveUserProfile(profile)
                if (success) {
                    _resetStatus.value = "Profile saved successfully"
                } else {
                    _resetStatus.value = "Failed to save profile"
                }
            } catch (e: Exception) {
                _resetStatus.value = "Error saving profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetAppData() {
        viewModelScope.launch {
            _isLoading.value = true
            _resetStatus.value = null
            try {
                val success = repository.resetAppData()
                if (success) {
                    _resetStatus.value = "App data reset successfully"
                } else {
                    _resetStatus.value = "Failed to reset app data"
                }
            } catch (e: Exception) {
                _resetStatus.value = "Error resetting data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getLastSyncInfo() = repository.getLastSyncInfo()

    fun getTotalMembersCount(): Int {
        return familyMembers.value.size
    }

    fun getMembersWithPhotosCount(): Int {
        return familyMembers.value.count { !it.photoUrl.isNullOrEmpty() }
    }

    fun getMembersWithVideosCount(): Int {
        return familyMembers.value.count { !it.videoUrl.isNullOrEmpty() }
    }

    fun clearResetStatus() {
        _resetStatus.value = null
    }
}