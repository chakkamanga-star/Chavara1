package com.sj9.chavara.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

open class FamilyMembersViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    val familyMembers: StateFlow<List<FamilyMember>> = repository.familyMembers
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val userProfile: StateFlow<FamilyMember?> = repository.userProfile

    val todaysBirthdays: StateFlow<List<FamilyMember>> = repository.familyMembers.map {
        repository.getTodaysBirthdayMembers()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getMembersByMonth(): Map<Int, List<FamilyMember>> {
        return repository.getMembersByMonth()
    }

    fun saveFamilyMember(member: FamilyMember) {
        viewModelScope.launch {
            if (member.id <= 0) {
                val newId = repository.getNewFamilyMemberId()
                repository.saveFamilyMember(member.copy(id = newId))
            } else {
                repository.saveFamilyMember(member)
            }
        }
    }
    fun deleteFamilyMember(memberId: Int) {
        viewModelScope.launch {
            repository.deleteFamilyMember(memberId)
        }
    }

    fun getMemberById(id: Int): FamilyMember? {
        return repository.getMemberById(id)
    }

    fun uploadMemberPhoto(memberId: Int, imageBytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            repository.uploadMemberPhoto(memberId, imageBytes, mimeType)
        }
    }
    suspend fun getAuthenticatedImageUrl(gcsUrl: String): String? {
        return repository.getAuthenticatedImageUrl(gcsUrl)
    }
}