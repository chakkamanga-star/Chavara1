package com.sj9.chavara.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.ui.utils.SpreadsheetUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpreadsheetViewModel(private val chavaraRepository: ChavaraRepository) : ViewModel() {

    // You already have an instance of the repository here. Use this one!


    private val _uiState = MutableStateFlow<SpreadsheetUiState>(SpreadsheetUiState.Idle)
    val uiState: StateFlow<SpreadsheetUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<Boolean>(false)
    val navigationEvent: StateFlow<Boolean> = _navigationEvent.asStateFlow()

    companion object {
        private const val TAG = "SpreadsheetViewModel"
    }

    fun processSpreadsheetUrl(url: String) {
        if (url.isBlank()) {
            _uiState.update { SpreadsheetUiState.Error("Spreadsheet URL cannot be empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { SpreadsheetUiState.Loading("Fetching data from URL...") }
            Log.d(TAG, "Attempting to process URL: $url")

            // The repository function handles its own loading state.
            // No need to call a separate initialize() here.
            val result = chavaraRepository.fetchDataFromSpreadsheet(url) { progressMessage ->
                _uiState.value = SpreadsheetUiState.Loading(progressMessage)
            }

            result.fold(
                onSuccess = { successMessage ->
                    Log.i(TAG, "Processing successful: $successMessage")
                    _uiState.update { SpreadsheetUiState.Success(successMessage) }
                    _navigationEvent.update { true } // Trigger navigation
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
        // No cleanup() method is defined in the repository, so this call is removed.
        // The ViewModel and its repository instance will be garbage collected automatically.
        Log.d(TAG, "SpreadsheetViewModel cleared.")
    }
}