package com.sj9.chavara.data.service

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sj9.chavara.R
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Singleton class to manage service account credentials for Google APIs
 */
object ServiceAccountManager {
    private var sheetsCredentials: GoogleCredentials? = null
    private var gcsCredentials: GoogleCredentials? = null

    /**
     * Get Google Sheets credentials from the raw resource
     */
    fun getSheetsCredentials(context: Context): GoogleCredentials? {
        return try {
            if (sheetsCredentials == null) {
                sheetsCredentials = loadCredentialsFromResource(
                    context = context,
                    resourceId = R.raw.sheets_key,
                    scopes = listOf("https://www.googleapis.com/auth/spreadsheets.readonly")
                )
            }
            sheetsCredentials
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get Google Cloud Storage credentials from the raw resource
     */
    fun getGcsCredentials(context: Context): GoogleCredentials? {
        return try {
            if (gcsCredentials == null) {
                gcsCredentials = loadCredentialsFromResource(
                    context = context,
                    resourceId = R.raw.gcs_key,
                    scopes = listOf("https://www.googleapis.com/auth/cloud-platform")
                )
            }
            gcsCredentials
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Load credentials from a raw resource file
     */
    private fun loadCredentialsFromResource(
        context: Context,
        resourceId: Int,
        scopes: List<String>
    ): GoogleCredentials {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        // Parse JSON to ensure it's valid
        val gson = Gson()
        val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)

        // Convert back to InputStream for GoogleCredentials
        val credentialsStream = ByteArrayInputStream(jsonString.toByteArray())

        return ServiceAccountCredentials
            .fromStream(credentialsStream)
            .createScoped(scopes)
    }

    /**
     * Clear cached credentials (useful for testing or when credentials change)
     */
    fun clearCredentials() {
        sheetsCredentials = null
        gcsCredentials = null
    }
}