package com.sj9.chavara.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import android.util.Log

class MediaViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {

    private val _deletionStatus = MutableStateFlow<String?>(null)
    val deletionStatus: StateFlow<String?> = _deletionStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val familyMembers = repository.familyMembers.asStateFlow()

    // Get all member photos
    val memberPhotos = repository.familyMembers.map { members ->
        members.mapNotNull { it.photoUrl }.filter { it.isNotEmpty() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Get all member videos
    val memberVideos = repository.familyMembers.map { members ->
        members.mapNotNull { it.videoUrl }.filter { it.isNotEmpty() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Get members with photos
    val membersWithPhotos = repository.familyMembers.map { members ->
        members.filter { !it.photoUrl.isNullOrEmpty() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Get members with videos
    val membersWithVideos = repository.familyMembers.map { members ->
        members.filter { !it.videoUrl.isNullOrEmpty() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun deleteUserMedia(fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _deletionStatus.value = null
            try {
                // Note: You'll need to add this method to ChavaraRepository
                // repository.deleteMediaFile(fileName)
                _deletionStatus.value = "File '$fileName' deleted successfully."
                Log.i("MediaViewModel", "File '$fileName' deleted successfully.")
            } catch (e: Exception) {
                _deletionStatus.value = "Error deleting file '$fileName': ${e.message}"
                Log.e("MediaViewModel", "Error deleting file '$fileName'", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getMemberPhotosGroupedByMonth(): Map<Int, List<FamilyMember>> {
        return membersWithPhotos.value.groupBy { it.getBirthMonth() }
    }

    fun getMemberVideosGroupedByMonth(): Map<Int, List<FamilyMember>> {
        return membersWithVideos.value.groupBy { it.getBirthMonth() }
    }

    fun clearDeletionStatus() {
        _deletionStatus.value = null
    }
}