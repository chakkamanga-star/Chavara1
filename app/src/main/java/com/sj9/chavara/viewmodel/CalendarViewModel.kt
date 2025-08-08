package com.sj9.chavara.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.ui.utils.SpreadsheetUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val familyMembers = repository.familyMembers.asStateFlow()

    // Birthday events grouped by month for calendar display
    val birthdayEventsByMonth = repository.familyMembers.map { members ->
        members.groupBy { it.getBirthMonth() }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Get all birthday events for calendar view
    val allBirthdayEvents = repository.familyMembers.map { members ->
        members.map { member ->
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val birthDate = dateFormat.parse(member.birthday)
                val calendar = Calendar.getInstance().apply {
                    time = birthDate ?: Date()
                    set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                }
                Pair(calendar.time, member)
            } catch (e: Exception) {
                null
            }
        }.filterNotNull()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Get upcoming birthdays for next 30 days
    val upcomingBirthdays = repository.familyMembers.map { members ->
        val today = Calendar.getInstance()
        val thirtyDaysLater = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 30)
        }

        members.filter { member ->
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val birthDate = dateFormat.parse(member.birthday)
                val birthCalendar = Calendar.getInstance().apply {
                    time = birthDate ?: Date()
                    set(Calendar.YEAR, today.get(Calendar.YEAR))
                }

                // If birthday already passed this year, check next year
                if (birthCalendar.before(today)) {
                    birthCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR) + 1)
                }

                birthCalendar.after(today) && birthCalendar.before(thirtyDaysLater)
            } catch (e: Exception) {
                false
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun getEventsForDate(date: String): List<FamilyMember> {
        return familyMembers.value.filter { it.birthday == date }
    }

    fun getEventsForMonth(month: Int): List<FamilyMember> {
        return familyMembers.value.filter { it.getBirthMonth() == month }
    }

    fun getTodaysBirthdays(): List<FamilyMember> {
        return repository.getTodaysBirthdayMembers()
    }

    fun refreshCalendarData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.initialize()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

