package com.sj9.chavara.data.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A CoroutineWorker dedicated to fetching data from Google Sheets,
 * downloading images, and saving everything to Google Cloud Storage.
 * This worker ensures the process continues even if the app is in the background.
 */
class DataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Manually create an instance of the repository.
    // In a production app with dependency injection (e.g., Hilt), this would be injected.
    private val repository = ChavaraRepository(applicationContext)

    companion object {
        const val WORK_NAME = "com.sj9.chavara.data.service.DataSyncWorker"
        const val KEY_SPREADSHEET_URL = "SPREADSHEET_URL"
        private const val TAG = "DataSyncWorker"
    }

    override suspend fun doWork(): Result {
        // Retrieve the spreadsheet URL passed to the worker
        val spreadsheetUrl = inputData.getString(KEY_SPREADSHEET_URL)
        if (spreadsheetUrl.isNullOrBlank()) {
            Log.e(TAG, "Spreadsheet URL is missing. Cannot perform sync.")
            return Result.failure()
        }

        Log.i(TAG, "Worker started for URL: $spreadsheetUrl")

        return withContext(Dispatchers.IO) {
            try {
                // Use the repository to fetch data. The repository already contains
                // the logic for fetching from sheets, downloading images, and saving to GCS.
                val result = repository.fetchDataFromSpreadsheet(spreadsheetUrl) { progressMessage ->
                    // Here you could update a notification to show progress,
                    // but for now, we'll just log it.
                    Log.d(TAG, "Sync Progress: $progressMessage")
                }

                result.fold(
                    onSuccess = {
                        Log.i(TAG, "Worker finished successfully: $it")
                        Result.success()
                    },
                    onFailure = {
                        Log.e(TAG, "Worker failed with error.", it)
                        Result.failure()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected exception occurred in DataSyncWorker.", e)
                Result.failure()
            }
        }
    }
}
