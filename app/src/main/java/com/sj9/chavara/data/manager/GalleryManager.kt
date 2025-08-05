package com.sj9.chavara.data.manager

import com.sj9.chavara.data.model.FamilyMember
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager class for organizing gallery content by month and managing media
 */
object GalleryManager {
    
    private val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    /**
     * Organize family members by their birth month
     */
    fun organizeByMonth(members: List<FamilyMember>): Map<String, List<FamilyMember>> {
        val organized = mutableMapOf<String, MutableList<FamilyMember>>()
        
        members.forEach { member ->
            val monthIndex = member.getBirthMonth()
            if (monthIndex in 1..12) {
                val monthName = monthNames[monthIndex - 1]
                organized.getOrPut(monthName) { mutableListOf() }.add(member)
            }
        }
        
        // Sort members within each month by day
        organized.forEach { (_, membersList) ->
            membersList.sortBy { member ->
                try {
                    val parts = member.birthday.split("/")
                    if (parts.size >= 2) parts[0].toInt() else 0
                } catch (e: Exception) {
                    0
                }
            }
        }
        
        return organized.toMap()
    }
    
    /**
     * Get members with photos (non-empty photoUrl)
     */
    fun getMembersWithPhotos(members: List<FamilyMember>): List<FamilyMember> {
        return members.filter { it.photoUrl.isNotEmpty() }
    }
    
    /**
     * Get members with videos (non-empty videoUrl)
     */
    fun getMembersWithVideos(members: List<FamilyMember>): List<FamilyMember> {
        return members.filter { it.videoUrl.isNotEmpty() }
    }
    
    /**
     * Get formatted date for display in gallery tabs
     */
    fun getFormattedDateForTab(member: FamilyMember): String {
        return try {
            val parts = member.birthday.split("/")
            if (parts.size >= 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2]
                
                // Format as "DD MMM YYYY" (e.g., "15 Jan 1995")
                val calendar = Calendar.getInstance()
                calendar.set(year.toInt(), month.toInt() - 1, day.toInt())
                
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
            } else {
                member.birthday
            }
        } catch (e: Exception) {
            member.birthday
        }
    }
    
    /**
     * Get current month members for home screen birthday display
     */
    fun getCurrentMonthMembers(members: List<FamilyMember>): List<FamilyMember> {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        return members.filter { it.getBirthMonth() == currentMonth }
    }
    
    /**
     * Sort members chronologically by birthday (day and month)
     */
    fun sortMembersByBirthday(members: List<FamilyMember>): List<FamilyMember> {
        return members.sortedWith { member1, member2 ->
            try {
                val parts1 = member1.birthday.split("/")
                val parts2 = member2.birthday.split("/")
                
                if (parts1.size >= 2 && parts2.size >= 2) {
                    val month1 = parts1[1].toInt()
                    val day1 = parts1[0].toInt()
                    val month2 = parts2[1].toInt()
                    val day2 = parts2[0].toInt()
                    
                    // First compare by month, then by day
                    when {
                        month1 != month2 -> month1.compareTo(month2)
                        else -> day1.compareTo(day2)
                    }
                } else {
                    0
                }
            } catch (e: Exception) {
                0
            }
        }
    }
    
    /**
     * Get media type from URL
     */
    fun getMediaType(url: String): MediaType {
        return when {
            url.contains("video", ignoreCase = true) || 
            url.endsWith(".mp4", ignoreCase = true) ||
            url.endsWith(".avi", ignoreCase = true) ||
            url.endsWith(".mov", ignoreCase = true) -> MediaType.VIDEO
            
            url.contains("image", ignoreCase = true) ||
            url.endsWith(".jpg", ignoreCase = true) ||
            url.endsWith(".jpeg", ignoreCase = true) ||
            url.endsWith(".png", ignoreCase = true) ||
            url.endsWith(".gif", ignoreCase = true) -> MediaType.IMAGE
            
            else -> MediaType.UNKNOWN
        }
    }
    
    enum class MediaType {
        IMAGE, VIDEO, UNKNOWN
    }
}
