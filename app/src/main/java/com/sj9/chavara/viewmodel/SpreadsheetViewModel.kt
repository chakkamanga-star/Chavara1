package com.sj9.chavara.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.ui.utils.SpreadsheetUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpreadsheetViewModel(
    application: Application, // Changed to just 'application' for convention
    private val chavaraRepository: ChavaraRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SpreadsheetUiState>(SpreadsheetUiState.Idle)
    val uiState: StateFlow<SpreadsheetUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<Boolean>(false)
    val navigationEvent: StateFlow<Boolean> = _navigationEvent.asStateFlow()

    private val workManager = WorkManager.getInstance(application)

    companion object {
        private const val TAG = "SpreadsheetViewModel"
    }

    fun processSpreadsheetUrl(url: String) {
        if (url.isBlank()) {
            _uiState.update { SpreadsheetUiState.Error("Spreadsheet URL cannot be empty.") }
            return
        }
        processSpreadsheetWithWorker(url)
    }

    private fun processSpreadsheetWithWorker(url: String) {
        _uiState.update { SpreadsheetUiState.Loading("Starting background sync...") }
        Log.d(TAG, "Triggering background sync for URL: $url")

        // Trigger the background worker via the repository
        chavaraRepository.triggerDataSyncFromSpreadsheet(url)

        // Monitor the worker's progress and result
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkLiveData("data_sync_work")
                .observeForever { workInfos ->
                    val workInfo = workInfos?.firstOrNull()
                    workInfo?.let { info ->
                        when (info.state) {
                            WorkInfo.State.ENQUEUED -> {
                                _uiState.update {
                                    SpreadsheetUiState.Loading("Sync queued, waiting for network...")
                                }
                            }
                            WorkInfo.State.RUNNING -> {
                                val progress = info.progress.getString("progress")
                                _uiState.update {
                                    SpreadsheetUiState.Loading(progress ?: "Syncing data...")
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                val successMessage = info.outputData.getString("result")
                                    ?: "Data sync completed successfully"
                                Log.i(TAG, "Background sync successful: $successMessage")
                                _uiState.update { SpreadsheetUiState.Success(successMessage) }
                                _navigationEvent.update { true }
                            }
                            WorkInfo.State.FAILED -> {
                                val errorMessage = info.outputData.getString("error")
                                    ?: "Background sync failed"
                                Log.e(TAG, "Background sync failed: $errorMessage")
                                _uiState.update { SpreadsheetUiState.Error(errorMessage) }
                            }
                            WorkInfo.State.CANCELLED -> {
                                _uiState.update {
                                    SpreadsheetUiState.Error("Sync was cancelled")
                                }
                            }
                            else -> { // BLOCKED state
                                _uiState.update {
                                    SpreadsheetUiState.Loading("Waiting for network connection...")
                                }
                            }
                        }
                    }
                }
        }
    }

    fun cancelSync() {
        workManager.cancelUniqueWork("data_sync_work")
        _uiState.update { SpreadsheetUiState.Idle }
    }

    fun resetNavigationEvent() {
        _navigationEvent.update { false }
    }

    fun clearMessage() {
        if (_uiState.value is SpreadsheetUiState.Success || _uiState.value is SpreadsheetUiState.Error) {
            _uiState.update { SpreadsheetUiState.Idle }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // It's good practice to remove the observer to prevent memory leaks,
        // though with observeForever it's especially important.
        workManager.getWorkInfosForUniqueWorkLiveData("data_sync_work").removeObserver { }
        Log.d(TAG, "SpreadsheetViewModel cleared.")
    }
}
