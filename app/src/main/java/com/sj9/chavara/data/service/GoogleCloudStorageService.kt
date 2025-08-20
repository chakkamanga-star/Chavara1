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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Service class for interacting with Google Cloud Storage
 * Handles low-level storage operations with enhanced, detailed logging.
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
                Log.d(TAG, "Attempting to get GCS credentials...")
                val credentials = ServiceAccountManager.getGcsCredentials(context)
                if (credentials != null) {
                    Log.d(TAG, "Credentials obtained successfully.")
                    _storage = StorageOptions.newBuilder()
                        .setCredentials(credentials)
                        .setProjectId("velvety-network-468011-t2") // Explicit project ID
                        .build()
                        .service
                    Log.i(TAG, "GCS service initialized successfully.")
                } else {
                    Log.e(TAG, "GCS credentials are null - check ServiceAccountManager implementation.")
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

    suspend fun getAuthenticatedImageUrl(gcsUrl: String): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "getAuthenticatedImageUrl called for: $gcsUrl")
        if (!gcsUrl.startsWith("gs://")) {
            Log.w(TAG, "Invalid GCS URL format: $gcsUrl")
            return@withContext null
        }

        try {
            val urlParts = gcsUrl.removePrefix("gs://").split("/", limit = 2)
            if (urlParts.size != 2) {
                Log.e(TAG, "Invalid GCS URL structure: $gcsUrl")
                return@withContext null
            }

            val bucketName = urlParts[0]
            val objectPath = urlParts[1]
            Log.d(TAG, "Parsed GCS URL -> Bucket: $bucketName, Object: $objectPath")

            getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, objectPath)
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    Log.d(TAG, "Blob found for: $gcsUrl. Generating signed URL.")
                    val signedUrl = blob.signUrl(1, TimeUnit.HOURS)
                    Log.i(TAG, "Successfully generated signed URL for $gcsUrl")
                    return@withContext signedUrl.toString()
                } else {
                    Log.w(TAG, "Blob does not exist at path: $gcsUrl")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating signed URL for: $gcsUrl", e)
            return@withContext null
        }
    }

    fun loadFamilyMembersFlow(): Flow<FamilyMember> = flow {
        Log.d(TAG, "loadFamilyMembersFlow: Initiating flow.")
        try {
            val storage = getStorage()
            if (storage == null) {
                Log.e(TAG, "loadFamilyMembersFlow: Storage is null, aborting.")
                return@flow
            }

            Log.d(TAG, "loadFamilyMembersFlow: Streaming blobs with prefix 'family-members/member_'.")
            val blobPage = storage.list(BUCKET_NAME, Storage.BlobListOption.prefix("family-members/member_"))
            val blobs = blobPage.iterateAll()

            for (blob in blobs) {
                try {
                    val content = String(blob.getContent())
                    val member = gson.fromJson(content, FamilyMember::class.java)
                    Log.d(TAG, "loadFamilyMembersFlow: Emitting member ${member.id} - ${member.name}")
                    emit(member)
                } catch (e: JsonSyntaxException) {
                    Log.e(TAG, "loadFamilyMembersFlow: JSON parsing failed for blob: ${blob.name}", e)
                }
            }
            Log.i(TAG, "loadFamilyMembersFlow: Finished streaming all member blobs.")
        } catch (e: Exception) {
            Log.e(TAG, "loadFamilyMembersFlow: A critical error occurred during the flow.", e)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveFamilyMember(member: FamilyMember): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "saveFamilyMember: Attempting to save member ${member.id} - ${member.name}")
        try {
            getStorage()?.let { storage ->
                val json = gson.toJson(member)
                val blobId = BlobId.of(BUCKET_NAME, "family-members/member_${member.id}.json")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
                storage.create(blobInfo, json.toByteArray())
                Log.i(TAG, "saveFamilyMember: Successfully saved member ${member.id}")
                return@withContext true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "saveFamilyMember: Failed to save member ${member.id}", e)
            return@withContext false
        }
    }

    suspend fun deleteFamilyMember(memberId: Int): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "deleteFamilyMember: Attempting to delete member $memberId")
        try {
            getStorage()?.let { storage ->
                val blobId = BlobId.of(BUCKET_NAME, "family-members/member_$memberId.json")
                storage.delete(blobId)
                Log.i(TAG, "deleteFamilyMember: Successfully deleted member $memberId")
                return@withContext true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "deleteFamilyMember: Failed to delete member $memberId", e)
            return@withContext false
        }
    }

    suspend fun saveUserProfile(userProfile: FamilyMember): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "saveUserProfile: Attempting to save user profile for ${userProfile.name}")
        try {
            getStorage()?.let { storage ->
                val json = gson.toJson(userProfile)
                val blobId = BlobId.of(BUCKET_NAME, "user-profile/profile.json")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
                storage.create(blobInfo, json.toByteArray())
                Log.i(TAG, "saveUserProfile: Successfully saved user profile.")
                return@withContext true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "saveUserProfile: Failed to save user profile", e)
            return@withContext false
        }
    }

    suspend fun loadUserProfile(): FamilyMember? = withContext(Dispatchers.IO) {
        Log.d(TAG, "loadUserProfile: Attempting to load user profile.")
        try {
            getStorage()?.let { storage ->
                val blobId = BlobId.of(BUCKET_NAME, "user-profile/profile.json")
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    val content = String(blob.getContent())
                    val profile = gson.fromJson(content, FamilyMember::class.java)
                    Log.i(TAG, "loadUserProfile: Successfully loaded user profile for ${profile.name}")
                    return@withContext profile
                } else {
                    Log.w(TAG, "loadUserProfile: User profile not found.")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadUserProfile: Failed to load user profile", e)
            return@withContext null
        }
    }

    suspend fun uploadMediaFile(fileName: String, content: ByteArray, contentType: String): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "uploadMediaFile: Attempting to upload '$fileName' ($contentType)")
        try {
            getStorage()?.let { storage ->
                val blobId = BlobId.of(BUCKET_NAME, "media/$fileName")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build()
                storage.create(blobInfo, content)
                val gcsUrl = "gs://$BUCKET_NAME/media/$fileName"
                Log.i(TAG, "uploadMediaFile: Successfully uploaded '$fileName' to $gcsUrl")
                return@withContext gcsUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadMediaFile: Failed to upload '$fileName'", e)
            return@withContext null
        }
    }

    suspend fun resetAllData(): Boolean = withContext(Dispatchers.IO) {
        Log.w(TAG, "resetAllData: Initiating complete data reset.")
        try {
            getStorage()?.let { storage ->
                val blobs = storage.list(BUCKET_NAME).iterateAll()
                var count = 0
                for (blob in blobs) {
                    storage.delete(blob.blobId)
                    count++
                    Log.d(TAG, "resetAllData: Deleted blob ${blob.name}")
                }
                Log.i(TAG, "resetAllData: Successfully deleted $count blobs.")
                return@withContext true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "resetAllData: Failed during data reset.", e)
            return@withContext false
        }
    }
}