package com.sj9.chavara.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.flow.*

import java.text.SimpleDateFormat
import java.util.*

class GalleryViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {

    private val _galleryFilter = MutableStateFlow(GalleryFilter.ALL)
    val galleryFilter: StateFlow<GalleryFilter> = _galleryFilter.asStateFlow()

    val familyMembers: StateFlow<List<FamilyMember>> = repository.familyMembers

    // This is the primary property your UI will use. It automatically filters photos/videos.
    val filteredGalleryItems: StateFlow<List<GalleryItem>> =
        combine(familyMembers, galleryFilter) { members, filter ->
            when (filter) {
                GalleryFilter.PHOTOS -> {
                    members
                        .filter { it.photoUrl.isNotEmpty() }
                        .map { GalleryItem.Photo(it.photoUrl, it) }
                }
                GalleryFilter.VIDEOS -> {
                    members
                        .filter { it.videoUrl.isNotEmpty() }
                        .map { GalleryItem.Video(it.videoUrl, it) }
                }
                GalleryFilter.ALL -> {
                    val photos = members
                        .filter { it.photoUrl.isNotEmpty() }
                        .map { GalleryItem.Photo(it.photoUrl, it) }
                    val videos = members
                        .filter { it.videoUrl.isNotEmpty() }
                        .map { GalleryItem.Video(it.videoUrl, it) }
                    (photos + videos).sortedBy { it.member.name }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // This property organizes members with media into a map of Month Name -> Member List
    val membersWithMediaByMonth: StateFlow<Map<String, List<FamilyMember>>> =
        familyMembers.map { members ->
            members
                .filter { it.photoUrl.isNotEmpty() || it.videoUrl.isNotEmpty() }
                .sortedBy { getDayOfMonth(it.birthday) }
                .groupBy { getMonthName(it.birthday) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())


    fun setFilter(filter: GalleryFilter) {
        _galleryFilter.value = filter
    }

    // Helper functions that were in GalleryManager
    private val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private fun getMonthName(birthday: String): String {
        return try {
            val monthIndex = birthday.split("/")[1].toInt()
            if (monthIndex in 1..12) monthNames[monthIndex - 1] else "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getDayOfMonth(birthday: String): Int {
        return try {
            birthday.split("/")[0].toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getFormattedDateForTab(member: FamilyMember): String {
        return try {
            val parts = member.birthday.split("/")
            if (parts.size >= 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2]
                val calendar = Calendar.getInstance().apply {
                    set(year.toInt(), month.toInt() - 1, day.toInt())
                }
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
            } else {
                member.birthday
            }
        } catch (e: Exception) {
            member.birthday
        }
    }

    sealed class GalleryItem(val member: FamilyMember) {
        data class Photo(val url: String, val fm: FamilyMember) : GalleryItem(fm)
        data class Video(val url: String, val fm: FamilyMember) : GalleryItem(fm)
    }

    enum class GalleryFilter {
        ALL, PHOTOS, VIDEOS
    }
}