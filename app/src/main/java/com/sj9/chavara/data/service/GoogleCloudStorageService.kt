package com.sj9.chavara.data.service

import android.content.Context
import android.util.Log
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sj9.chavara.data.model.FamilyMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service class for interacting with Google Cloud Storage
 * Handles low-level storage operations
 */
class GoogleCloudStorageService(private val context: Context) {

    private val bucketName = "chakka" // Replace with your actual bucket name
    private val gson = Gson()

    private var _storage: Storage? = null
    private var _storageInitialized = false

    private fun getStorage(): Storage? {
        if (!_storageInitialized) {
            try {
                val credentials = ServiceAccountManager.getGcsCredentials(context)
                _storage = credentials?.let { creds ->
                    StorageOptions.newBuilder()
                        .setCredentials(creds)
                        .build()
                        .service
                }
                _storageInitialized = true
            } catch (e: Exception) {
                e.printStackTrace()
                _storage = null
                _storageInitialized = true
            }
        }
        return _storage
    }

    /**
     * Save family members data to Cloud Storage
     */
    suspend fun saveFamilyMembers(familyMembers: List<FamilyMember>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                return@withContext getStorage()?.let { storage ->
                    val json = gson.toJson(familyMembers)
                    val blobId = BlobId.of(bucketName, "family-members/family_members.json")
                    val blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType("application/json")
                        .build()

                    storage.create(blobInfo, json.toByteArray())
                    true
                } ?: false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    /**
     * Load family members data from Cloud Storage
     */
    suspend fun loadFamilyMembers(): List<FamilyMember> = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, "family-members/family_members.json")
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    val content = String(blob.getContent())
                    val type = object : TypeToken<List<FamilyMember>>() {}.type
                    gson.fromJson<List<FamilyMember>>(content, type)
                } else {
                    emptyList()
                }
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Save individual family member data
     */
    suspend fun saveFamilyMember(member: FamilyMember): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val json = gson.toJson(member)
                val blobId = BlobId.of(bucketName, "family-members/member_${member.id}.json")
                val blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("application/json")
                    .build()

                storage.create(blobInfo, json.toByteArray())
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete family member data
     */
    suspend fun deleteFamilyMember(memberId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, "family-members/member_$memberId.json")
                storage.delete(blobId)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Save user profile data
     */
    suspend fun saveUserProfile(userProfile: FamilyMember): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val json = gson.toJson(userProfile)
                val blobId = BlobId.of(bucketName, "user-profile/profile.json")
                val blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("application/json")
                    .build()

                storage.create(blobInfo, json.toByteArray())
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Load user profile data
     */
    suspend fun loadUserProfile(): FamilyMember? = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, "user-profile/profile.json")
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    val content = String(blob.getContent())
                    gson.fromJson(content, FamilyMember::class.java)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Save app settings/configuration
     */
    @Suppress("UNUSED")  // Suppress if not used; remove if truly unused
    suspend fun saveAppSettings(settings: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val json = gson.toJson(settings)
                val blobId = BlobId.of(bucketName, "app-settings/settings.json")
                val blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("application/json")
                    .build()

                storage.create(blobInfo, json.toByteArray())
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Load app settings/configuration
     */
    @Suppress("UNUSED")  // Suppress if not used; remove if truly unused
    suspend fun loadAppSettings(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, "app-settings/settings.json")
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    val content = String(blob.getContent())
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    gson.fromJson<Map<String, Any>>(content, type)
                } else {
                    emptyMap()
                }
            } ?: emptyMap()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Upload media file (photo/video) to Cloud Storage
     */
    suspend fun uploadMediaFile(
        fileName: String,
        content: ByteArray,
        contentType: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, "media/$fileName")
                val blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build()

                storage.create(blobInfo, content)
                "gs://$bucketName/media/$fileName" // Return GCS URL
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete media file from Cloud Storage
     */
    suspend fun deleteMediaFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, "media/$fileName")
                storage.delete(blobId)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Download file from Cloud Storage with custom bucket and path
     */
    suspend fun downloadFile(bucketName: String, objectPath: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, objectPath)
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    blob.getContent()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Download file from default bucket
     */
    suspend fun downloadFileFromDefaultBucket(objectPath: String): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                return@withContext getStorage()?.let { storage ->
                    val blobId = BlobId.of(bucketName, objectPath) // Using the class property
                    val blob = storage.get(blobId)

                    if (blob != null && blob.exists()) {
                        blob.getContent()
                    } else {
                        Log.w(
                            "GCS_Service",
                            "File not found or does not exist: gs://$bucketName/$objectPath"
                        )
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "GCS_Service",
                    "Error downloading file: gs://$bucketName/$objectPath",
                    e
                )
                null
            }
        }

    /**
     * Reset all app data (for app reset functionality)
     */
    suspend fun resetAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                // List all objects and delete them
                val blobs = storage.list(bucketName).iterateAll()

                for (blob in blobs) {
                    storage.delete(blob.blobId)
                }

                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}