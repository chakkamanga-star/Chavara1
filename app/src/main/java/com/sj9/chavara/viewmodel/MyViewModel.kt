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

class MyViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {

    val familyMembers = repository.familyMembers
    val isLoading = repository.isLoading
    val userProfile = repository.userProfile

    // Today's birthday members for home screen
    val todaysBirthdayMembers = repository.familyMembers.map {
        repository.getTodaysBirthdayMembers()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Recent members (for home screen highlights)
    val recentMembers = repository.familyMembers.map { members ->
        members.take(6) // Show first 6 members as recent
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Members with upcoming birthdays (next 7 days)
    val upcomingBirthdays = repository.familyMembers.map { members ->
        val today = java.util.Calendar.getInstance()
        val nextWeek = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 7)
        }

        members.filter { member ->
            try {
                val birthDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    .parse(member.birthday)
                val birthCalendar = java.util.Calendar.getInstance().apply {
                    time = birthDate
                    set(java.util.Calendar.YEAR, today.get(java.util.Calendar.YEAR))
                }

                birthCalendar.after(today) && birthCalendar.before(nextWeek)
            } catch (e: Exception) {
                false
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun initializeRepository() {
        viewModelScope.launch {
            repository.initialize()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.initialize()
        }
    }
    suspend fun getAuthenticatedImageUrl(gcsUrl: String): String? {
        return repository.getAuthenticatedImageUrl(gcsUrl)
    }
    fun getTotalMembersCount(): Int {
        return familyMembers.value.size
    }

    fun getMembersWithPhotosCount(): Int {
        return familyMembers.value.count { !it.photoUrl.isNullOrEmpty() }
    }

    fun getLastSyncInfo() = repository.getLastSyncInfo()
}