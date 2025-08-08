package com.sj9.chavara.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.helper.GcsIntegrationHelper
import com.sj9.chavara.ui.utils.SpreadsheetUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Or your preferred UI package

class SpreadsheetViewModel(application: Application) : AndroidViewModel(application) {

    private val gcsIntegrationHelper = GcsIntegrationHelper(application.applicationContext)

    private val _uiState = MutableStateFlow<SpreadsheetUiState>(SpreadsheetUiState.Idle)
    val uiState: StateFlow<SpreadsheetUiState> = _uiState.asStateFlow()

    // Optional: If you want to signal a navigation event specifically
    private val _navigationEvent = MutableStateFlow<Boolean>(false)
    val navigationEvent: StateFlow<Boolean> = _navigationEvent.asStateFlow()

    companion object {
        private const val TAG = "SpreadsheetViewModel"
    }

    init {
        viewModelScope.launch {
            Log.d(TAG, "Initializing GcsIntegrationHelper...")
            // We initialize the helper lazily when needed, or you can initialize eagerly here.
            // For now, let's rely on GcsIntegrationHelper's internal initialization.
            // If initialize() itself has user-visible side effects or takes long,
            // you might want to expose its state too.
            // For simplicity, we assume fetchAndUploadFromUrl handles its own initialization checks.
        }
    }

    fun processSpreadsheetUrl(url: String) {
        if (url.isBlank()) {
            _uiState.update { SpreadsheetUiState.Error("Spreadsheet URL cannot be empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { SpreadsheetUiState.Loading("Fetching data from URL...") }
            Log.d(TAG, "Attempting to process URL: $url")

            // Initialize GCS helper first if it has a separate initialization step that can fail
            // and needs to be communicated to the UI.
            // For this example, let's assume `fetchAndUploadFromUrl` handles internal init.
            // Or you can call gcsIntegrationHelper.initialize() and check its result first.
            if (!gcsIntegrationHelper.initialize()) { // Assuming initialize() is suspend and returns Boolean
                _uiState.update { SpreadsheetUiState.Error("Failed to initialize Google services. Check setup.") }
                return@launch
            }


            val result = gcsIntegrationHelper.fetchAndUploadFromUrl(url)

            result.fold(
                onSuccess = { successMessage ->
                    Log.i(TAG, "Processing successful: $successMessage")
                    _uiState.update { SpreadsheetUiState.Success(successMessage) }
                    _navigationEvent.update { true } // Trigger navigation event
                },
                onFailure = { exception ->
                    val errorMessage = exception.message ?: "An unknown error occurred."
                    Log.e(TAG, "Processing failed: $errorMessage", exception)
                    _uiState.update { SpreadsheetUiState.Error("Error: $errorMessage") }
                }
            )
        }
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
        gcsIntegrationHelper.cleanup()
        Log.d(TAG, "SpreadsheetViewModel cleared and GcsIntegrationHelper cleaned up.")
    }
}