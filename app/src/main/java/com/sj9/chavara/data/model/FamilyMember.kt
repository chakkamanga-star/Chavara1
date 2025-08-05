package com.sj9.chavara.data.model

import java.util.Date

data class FamilyMember(
    val id: Int = 0,
    val name: String = "",
    val course: String = "",
    val birthday: String = "", // Format: DD/MM/YYYY
    val phoneNumber: String = "",
    val residence: String = "",
    val emailAddress: String = "",
    val chavaraPart: String = "", // "How do you want to be part of Chavara Youth?"
    val photoUrl: String = "",
    val videoUrl: String = "",
    val submissionDate: String = "", // When they submitted the form
    val isCurrentUserProfile: Boolean = false
) {
    // Helper function to get birth month for organizing by month
    fun getBirthMonth(): Int {
        return try {
            val parts = birthday.split("/")
            if (parts.size >= 2) parts[1].toInt() else 0
        } catch (e: Exception) {
            0
        }
    }

    // Helper function to check if today is birthday
    fun isBirthdayToday(): Boolean {
        return try {
            val parts = birthday.split("/")
            if (parts.size >= 2) {
                val currentDate = java.util.Calendar.getInstance()
                val birthDay = parts[0].toInt()
                val birthMonth = parts[1].toInt()
                
                currentDate.get(java.util.Calendar.DAY_OF_MONTH) == birthDay &&
                (currentDate.get(java.util.Calendar.MONTH) + 1) == birthMonth
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // Helper function to get formatted birthday for display
    fun getFormattedBirthday(): String {
        return try {
            val parts = birthday.split("/")
            if (parts.size >= 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2]
                "$day/$month/$year"
            } else birthday
        } catch (e: Exception) {
            birthday
        }
    }
}
