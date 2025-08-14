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

    // FIX: Make bucket name a constant for clarity
    companion object {
        private const val BUCKET_NAME = "chakka"
        private const val TAG = "GcsService"
    }
    private val gson = Gson()

    private var _storage: Storage? = null
    private var _storageInitialized = false

    private fun getStorage(): Storage? {
        if (!_storageInitialized) {
            try {
                Log.d(TAG, "Initializing Google Cloud Storage service...")
                val credentials = ServiceAccountManager.getGcsCredentials(context)
                if (credentials != null) {
                    _storage = StorageOptions.newBuilder()
                        .setCredentials(credentials)
                        .build()
                        .service
                    Log.d(TAG, "Google Cloud Storage service initialized successfully.")
                } else {
                    Log.e(TAG, "GCS credentials are null. Service not initialized.")
                }
            } catch (e: Exception) {
                // FIX: Log the exception instead of silently failing
                Log.e(TAG, "Failed to initialize Google Cloud Storage service", e)
                _storage = null
            } finally {
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
                    val blobId = BlobId.of(BUCKET_NAME, "family-members/family_members.json")
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
                val blobId = BlobId.of(BUCKET_NAME, "family-members/family_members.json")
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
                val blobId = BlobId.of(BUCKET_NAME, "family-members/member_${member.id}.json")
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
                val blobId = BlobId.of(BUCKET_NAME, "family-members/member_$memberId.json")
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
                val blobId = BlobId.of(BUCKET_NAME, "user-profile/profile.json")
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
                val blobId = BlobId.of(BUCKET_NAME, "user-profile/profile.json")
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
     * Upload media file (photo/video) to Cloud Storage
     */
    suspend fun uploadMediaFile(
        fileName: String,
        content: ByteArray,
        contentType: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            return@withContext getStorage()?.let { storage ->
                val blobId = BlobId.of(BUCKET_NAME, "media/$fileName")
                val blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build()

                storage.create(blobInfo, content)
                "gs://$BUCKET_NAME/media/$fileName" // Return GCS URL
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
                val blobId = BlobId.of(BUCKET_NAME, "media/$fileName")
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
                    val blobId = BlobId.of(BUCKET_NAME, objectPath) // Using the class property
                    val blob = storage.get(blobId)

                    if (blob != null && blob.exists()) {
                        blob.getContent()
                    } else {
                        Log.w(
                            "GCS_Service",
                            "File not found or does not exist: gs://$BUCKET_NAME/$objectPath"
                        )
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "GCS_Service",
                    "Error downloading file: gs://$BUCKET_NAME/$objectPath",
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
                val blobs = storage.list(BUCKET_NAME).iterateAll()

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