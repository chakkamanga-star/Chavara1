package com.sj9.chavara.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FamilyMembersViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {

    val familyMembers = repository.familyMembers.asStateFlow()
    val isLoading = repository.isLoading.asStateFlow()
    val userProfile = repository.userProfile.asStateFlow()

    val todaysBirthdays = repository.familyMembers.map {
        repository.getTodaysBirthdayMembers()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadDataFromSpreadsheet(url: String) {
        viewModelScope.launch {
            repository.fetchDataFromSpreadsheet(url) { progress ->
                // Handle progress updates if needed
            }
        }
    }

    fun getMembersByMonth() = repository.getMembersByMonth()

    fun saveFamilyMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.saveFamilyMember(member)
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

    fun getLastSyncInfo() = repository.getLastSyncInfo()
}