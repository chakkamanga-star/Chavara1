package com.sj9.chavara.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.model.FamilyMember
import com.sj9.chavara.data.model.getBirthMonth  // Add this import
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val repository: ChavaraRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow<Int?>(null)
    val selectedMonth: StateFlow<Int?> = _selectedMonth.asStateFlow()

    private val _galleryFilter = MutableStateFlow(GalleryFilter.ALL)
    val galleryFilter: StateFlow<GalleryFilter> = _galleryFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val familyMembers = repository.familyMembers.asStateFlow()

    // All photos from members
    val allPhotos = repository.familyMembers.map { members ->
        members.mapNotNull { member ->
            member.photoUrl.takeIf { it.isNotEmpty() }?.let { url ->
                GalleryItem.Photo(url, member)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // All videos from members
    val allVideos = repository.familyMembers.map { members ->
        members.mapNotNull { member ->
            member.videoUrl.takeIf { it.isNotEmpty() }?.let { url ->
                GalleryItem.Video(url, member)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // All media combined
    val allMedia = repository.familyMembers.map { members ->
        val photos = members.mapNotNull { member ->
            member.photoUrl.takeIf { it.isNotEmpty() }?.let { url ->
                GalleryItem.Photo(url, member)
            }
        }
        val videos = members.mapNotNull { member ->
            member.videoUrl.takeIf { it.isNotEmpty() }?.let { url ->
                GalleryItem.Video(url, member)
            }
        }
        (photos + videos).sortedBy { it.member.name }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Members grouped by month for gallery navigation
    val membersByMonth = repository.familyMembers.map { members ->
        repository.getMembersByMonth()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Members with media grouped by month
    val membersWithMediaByMonth = repository.familyMembers.map { members ->
        members.filter { member ->
            member.photoUrl.isNotEmpty() || member.videoUrl.isNotEmpty()
        }.groupBy { member ->
            member.getBirthMonth()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // Filtered gallery items based on current filter and selected month
    val filteredGalleryItems = repository.familyMembers.map { members ->
        val filteredByType = when (_galleryFilter.value) {
            GalleryFilter.PHOTOS -> {
                members.mapNotNull { member ->
                    member.photoUrl.takeIf { it.isNotEmpty() }?.let { url ->
                        GalleryItem.Photo(url, member)
                    }
                }
            }
            GalleryFilter.VIDEOS -> {
                members.mapNotNull { member ->
                    member.videoUrl.takeIf { it.isNotEmpty() }?.let { url ->
                        GalleryItem.Video(url, member)
                    }
                }
            }
            GalleryFilter.ALL -> {
                val photos = members.mapNotNull { member ->
                    member.photoUrl.takeIf { it.isNotEmpty() }?.let { url ->
                        GalleryItem.Photo(url, member)
                    }
                }
                val videos = members.mapNotNull { member ->
                    member.videoUrl.takeIf { it.isNotEmpty() }?.let { url ->
                        GalleryItem.Video(url, member)
                    }
                }
                photos + videos
            }
        }

        // Filter by selected month if any
        _selectedMonth.value?.let { month ->
            filteredByType.filter { item ->
                item.member.getBirthMonth() == month
            }
        } ?: filteredByType
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectMonth(month: Int?) {
        _selectedMonth.value = month
    }

    fun setFilter(filter: GalleryFilter) {
        _galleryFilter.value = filter
    }

    fun getMembersWithMediaForMonth(month: Int): List<FamilyMember> {
        return familyMembers.value.filter { member ->
            member.getBirthMonth() == month &&
                    (member.photoUrl.isNotEmpty() || member.videoUrl.isNotEmpty())
        }
    }

    fun getMediaCountByMonth(): Map<Int, MediaCount> {
        return familyMembers.value.groupBy { member ->
            member.getBirthMonth()
        }.mapValues { (_, members) ->
            MediaCount(
                photos = members.count { member ->
                    member.photoUrl.isNotEmpty()
                },
                videos = members.count { member ->
                    member.videoUrl.isNotEmpty()
                }
            )
        }
    }

    fun refreshGallery() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.initialize()
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class GalleryItem(val member: FamilyMember) {
        class Photo(val url: String, member: FamilyMember) : GalleryItem(member)
        class Video(val url: String, member: FamilyMember) : GalleryItem(member)
    }

    enum class GalleryFilter {
        ALL, PHOTOS, VIDEOS
    }

    data class MediaCount(
        val photos: Int = 0,
        val videos: Int = 0
    ) {
        val total: Int get() = photos + videos
    }
}