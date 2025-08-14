package com.sj9.chavara.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val userProfile: StateFlow<FamilyMember?> = repository.userProfile

    fun saveUserProfile(profile: FamilyMember) {
        viewModelScope.launch {
            repository.initialize()
            _uiState.value = ProfileUiState.Loading
            val success = repository.saveUserProfile(profile)
            _uiState.value = if (success) {
                ProfileUiState.Success("Profile saved successfully")
            } else {
                ProfileUiState.Error("Failed to save profile")
            }
        }
    }

    fun resetAppData() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val success = repository.resetAppData()
            _uiState.value = if (success) {
                ProfileUiState.Success("App data has been reset.")
            } else {
                ProfileUiState.Error("Failed to reset app data")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = ProfileUiState.Idle
    }
}

sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    data class Success(val message: String) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}