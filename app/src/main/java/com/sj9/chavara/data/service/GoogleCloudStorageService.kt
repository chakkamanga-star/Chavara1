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
        private const val TAG = "ImageDebug" // Using this as the consistent log tag
    }
    private val gson = Gson()

    private var _storage: Storage? = null
    private var _storageInitialized = false

    private fun getStorage(): Storage? {
        Log.d(TAG, "[GCS] getStorage called. Initialized: $_storageInitialized")
        if (!_storageInitialized) {
            try {
                Log.d(TAG, "[GCS] Attempting to get GCS credentials...")
                val credentials = ServiceAccountManager.getGcsCredentials(context)
                if (credentials != null) {
                    Log.d(TAG, "[GCS] Credentials obtained successfully.")
                    _storage = StorageOptions.newBuilder()
                        .setCredentials(credentials)
                        .setProjectId("velvety-network-468011-t2") // Explicit project ID
                        .build()
                        .service
                    Log.i(TAG, "[GCS] GCS service initialized successfully.")
                } else {
                    Log.e(TAG, "[GCS] GCS credentials are null - check ServiceAccountManager implementation.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "[GCS] Failed to initialize GCS", e)
                _storage = null
            } finally {
                _storageInitialized = true
            }
        }
        return _storage
    }

    suspend fun getAuthenticatedImageUrl(gcsUrl: String): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "[GCS] Attempting to get signed URL for: $gcsUrl")
        if (!gcsUrl.startsWith("gs://")) {
            Log.w(TAG, "[GCS] Invalid GCS URL format. URL must start with 'gs://'.")
            return@withContext null
        }

        try {
            val urlParts = gcsUrl.removePrefix("gs://").split("/", limit = 2)
            if (urlParts.size != 2) {
                Log.e(TAG, "[GCS] Invalid GCS URL structure. Expected gs://<bucket>/<object-path>. URL: $gcsUrl")
                return@withContext null
            }

            val bucketName = urlParts[0]
            val objectPath = urlParts[1]
            Log.d(TAG, "[GCS] Parsed GCS URL -> Bucket: $bucketName, Object: $objectPath")

            getStorage()?.let { storage ->
                val blobId = BlobId.of(bucketName, objectPath)
                Log.d(TAG, "[GCS] Executing storage.get(blobId) for: $blobId")
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    // --- DETAILED LOGGING FOR SIGNED URL GENERATION ---
                    try {
                        Log.d(TAG, "[GCS] Blob found for '$gcsUrl'. ATTEMPTING to generate signed URL...")
                        val signedUrl = blob.signUrl(1, TimeUnit.HOURS)
                        Log.i(TAG, "[GCS] Successfully generated signed URL: $signedUrl")
                        return@withContext signedUrl.toString()
                    } catch (e: Exception) {
                        Log.e(TAG, "[GCS-PERMISSIONS-ERROR] FAILED to generate signed URL for '$gcsUrl'. This is likely a permissions issue. Please ensure the service account has the 'Service Account Token Creator' IAM role.", e)
                        return@withContext null
                    }
                    // --- END OF DETAILED LOGGING ---
                } else {
                    Log.w(TAG, "[GCS-ERROR] Blob does not exist at path: $gcsUrl")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[GCS-AUTH-URL-ERROR] A general error occurred while trying to get the image URL for: $gcsUrl", e)
        }
        return@withContext null
    }

    fun loadFamilyMembersFlow(): Flow<FamilyMember> = flow {
        Log.d(TAG, "[GCS] loadFamilyMembersFlow: Initiating flow.")
        try {
            val storage = getStorage()
            if (storage == null) {
                Log.e(TAG, "[GCS] loadFamilyMembersFlow: Storage is null, aborting.")
                return@flow
            }

            Log.d(TAG, "[GCS] loadFamilyMembersFlow: Streaming blobs with prefix 'family-members/member_'.")
            val blobPage = storage.list(BUCKET_NAME, Storage.BlobListOption.prefix("family-members/member_"))
            val blobs = blobPage.iterateAll()
            var memberCount = 0

            for (blob in blobs) {
                try {
                    Log.d(TAG, "[GCS] loadFamilyMembersFlow: Processing blob: ${blob.name}")
                    val content = String(blob.getContent())
                    Log.d(TAG, "[GCS] loadFamilyMembersFlow: Blob content: $content")
                    val member = gson.fromJson(content, FamilyMember::class.java)
                    Log.d(TAG, "[GCS] loadFamilyMembersFlow: Emitting member ${member.id} - ${member.name}")
                    emit(member)
                    memberCount++
                } catch (e: JsonSyntaxException) {
                    Log.e(TAG, "[GCS] loadFamilyMembersFlow: JSON parsing failed for blob: ${blob.name}", e)
                }
            }
            Log.i(TAG, "[GCS] loadFamilyMembersFlow: Finished streaming all member blobs. Total members loaded: $memberCount")
        } catch (e: Exception) {
            Log.e(TAG, "[GCS] loadFamilyMembersFlow: A critical error occurred during the flow.", e)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveFamilyMember(member: FamilyMember): Boolean = withContext(Dispatchers.IO) {
        var success = false
        Log.d(TAG, "[GCS] saveFamilyMember: Attempting to save member ${member.id} - ${member.name}")
        try {
            getStorage()?.let { storage ->
                val json = gson.toJson(member)
                val blobId = BlobId.of(BUCKET_NAME, "family-members/member_${member.id}.json")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
                storage.create(blobInfo, json.toByteArray())
                Log.i(TAG, "[GCS] saveFamilyMember: Successfully saved member ${member.id}")
                success = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "[GCS] saveFamilyMember: Failed to save member ${member.id}", e)
        }
        success
    }

    suspend fun deleteFamilyMember(memberId: Int): Boolean = withContext(Dispatchers.IO) {
        var success = false
        Log.d(TAG, "[GCS] deleteFamilyMember: Attempting to delete member $memberId")
        try {
            getStorage()?.let { storage ->
                val blobId = BlobId.of(BUCKET_NAME, "family-members/member_$memberId.json")
                storage.delete(blobId)
                Log.i(TAG, "[GCS] deleteFamilyMember: Successfully deleted member $memberId")
                success = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "[GCS] deleteFamilyMember: Failed to delete member $memberId", e)
        }
        success
    }

    suspend fun saveUserProfile(userProfile: FamilyMember): Boolean = withContext(Dispatchers.IO) {
        var success = false
        Log.d(TAG, "[GCS] saveUserProfile: Attempting to save user profile for ${userProfile.name}")
        try {
            getStorage()?.let { storage ->
                val json = gson.toJson(userProfile)
                val blobId = BlobId.of(BUCKET_NAME, "user-profile/profile.json")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/json").build()
                storage.create(blobInfo, json.toByteArray())
                Log.i(TAG, "[GCS] saveUserProfile: Successfully saved user profile.")
                success = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "[GCS] saveUserProfile: Failed to save user profile", e)
        }
        success
    }

    suspend fun loadUserProfile(): FamilyMember? = withContext(Dispatchers.IO) {
        Log.d(TAG, "[GCS] loadUserProfile: Attempting to load user profile.")
        try {
            getStorage()?.let { storage ->
                val blobId = BlobId.of(BUCKET_NAME, "user-profile/profile.json")
                val blob = storage.get(blobId)

                if (blob != null && blob.exists()) {
                    val content = String(blob.getContent())
                    val profile = gson.fromJson(content, FamilyMember::class.java)
                    Log.i(TAG, "[GCS] loadUserProfile: Successfully loaded user profile for ${profile.name}")
                    return@withContext profile
                } else {
                    Log.w(TAG, "[GCS] loadUserProfile: User profile not found.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[GCS] loadUserProfile: Failed to load user profile", e)
        }
        return@withContext null
    }

    suspend fun uploadMediaFile(fileName: String, content: ByteArray, contentType: String): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "[GCS] uploadMediaFile: Attempting to upload '$fileName' ($contentType)")
        try {
            getStorage()?.let { storage ->
                val blobId = BlobId.of(BUCKET_NAME, "media/$fileName")
                val blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build()
                storage.create(blobInfo, content)
                val gcsUrl = "gs://$BUCKET_NAME/media/$fileName"
                Log.i(TAG, "[GCS] uploadMediaFile: Successfully uploaded '$fileName' to $gcsUrl")
                return@withContext gcsUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "[GCS] uploadMediaFile: Failed to upload '$fileName'", e)
        }
        return@withContext null
    }

    suspend fun resetAllData(): Boolean = withContext(Dispatchers.IO) {
        var success = false
        Log.w(TAG, "[GCS] resetAllData: Initiating complete data reset.")
        try {
            getStorage()?.let { storage ->
                val blobs = storage.list(BUCKET_NAME).iterateAll()
                var count = 0
                for (blob in blobs) {
                    storage.delete(blob.blobId)
                    count++
                    Log.d(TAG, "[GCS] resetAllData: Deleted blob ${blob.name}")
                }
                Log.i(TAG, "[GCS] resetAllData: Successfully deleted $count blobs.")
                success = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "[GCS] resetAllData: Failed during data reset.", e)
        }
        success
    }
}