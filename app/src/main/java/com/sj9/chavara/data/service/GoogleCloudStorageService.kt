package com.sj9.chavara.data.service

import android.content.Context
import android.util.Log
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

import com.sj9.chavara.data.model.FamilyMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service class for interacting with Google Cloud Storage
 * Handles low-level storage operations
 */
class GoogleCloudStorageService(private val context: Context) {

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
                val credentials = ServiceAccountManager.getGcsCredentials(context)
                if (credentials != null) {
                    _storage = StorageOptions.newBuilder().setCredentials(credentials).build().service
                    Log.d(TAG, "GCS service initialized successfully.")
                } else {
                    Log.e(TAG, "GCS credentials are null.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GCS", e)
                _storage = null
            } finally {
                _storageInitialized = true
            }
        }
        return _storage
    }

    suspend fun testBucketAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Test if bucket allows public access
            val testUrl = "https://storage.googleapis.com/chakka/test-file.txt"
            val connection = java.net.URL(testUrl).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            Log.d(TAG, "Public access test response code: $responseCode")

            when (responseCode) {
                200, 404 -> {
                    Log.d(TAG, "Bucket is publicly accessible")
                    true
                }
                403 -> {
                    Log.e(TAG, "Bucket is NOT publicly accessible - need to enable public access")
                    false
                }
                else -> {
                    Log.w(TAG, "Unexpected response code: $responseCode")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing bucket access", e)
            false
        }
    }

    suspend fun getAuthenticatedImageUrl(gcsUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!gcsUrl.startsWith("gs://")) return@withContext null

            val urlParts = gcsUrl.removePrefix("gs://").split("/", limit = 2)
            if (urlParts.size != 2) return@withContext null

            val bucketName = urlParts[0]
            val objectPath = urlParts[1]

            getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, objectPath)
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    val signedUrl = blob.signUrl(1, java.util.concurrent.TimeUnit.HOURS)
                    signedUrl.toString()
                } else {
                    Log.w(TAG, "Blob does not exist: $gcsUrl")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating signed URL for: $gcsUrl", e)
            null
        }
    }

    suspend fun saveFamilyMembers(familyMembers: List<FamilyMember>): Boolean = withContext(Dispatchers.IO) {
        try {
            getStorage()?.let { storage ->
                val json = gson.toJson(familyMembers)
                val blobId = BlobId.of(BUCKET_NAME, "family-members/family_members.json")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
                storage.create(blobInfo, json.toByteArray())
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save family members list", e)
            false
        }
    }

    suspend fun loadFamilyMembers(): List<FamilyMember> = withContext(Dispatchers.IO) {
        try {
            getStorage()?.let { storage ->
                val members = mutableListOf<FamilyMember>()
                Log.d(TAG, "Starting to list blobs with prefix: 'family-members/member_'")
                val blobs = storage.list(BUCKET_NAME, Storage.BlobListOption.prefix("family-members/member_")).iterateAll()

                for (blob in blobs) {
                    if (blob.name.endsWith("/family_members.json")) {
                        Log.d(TAG, "Skipping main list file: ${blob.name}")
                        continue
                    }
                    Log.d(TAG, "Found blob: ${blob.name}")
                    var content: String? = null
                    try {
                        content = String(blob.getContent())
                        val member = gson.fromJson(content, FamilyMember::class.java)
                        members.add(member)
                        Log.d(TAG, "Successfully parsed member: ${member.name} from ${blob.name}")
                    } catch (e: JsonSyntaxException) {
                        Log.e(TAG, "Failed to parse member JSON for blob: ${blob.name}", e)
                        Log.e(TAG, "Corrupted content was: $content")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process blob: ${blob.name}", e)
                    }
                }
                Log.d(TAG, "Loaded ${members.size} individual member files.")
                return@withContext members
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load family members with a critical error.", e)
            emptyList()
        }
    }

    suspend fun saveFamilyMember(member: FamilyMember): Boolean = withContext(Dispatchers.IO) {
        try {
            getStorage()?.let { storage ->
                val json = gson.toJson(member)
                val blobId = BlobId.of(BUCKET_NAME, "family-members/member_${member.id}.json")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
                storage.create(blobInfo, json.toByteArray())
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save individual family member", e)
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
