package com.sj9.chavara.ui.utils

 // Or your preferred UI package

sealed interface SpreadsheetUiState {
    data object Idle : SpreadsheetUiState // Initial state
    data class Loading(val loadingMessage: String = "Processing...") : SpreadsheetUiState
    data class Success(val successMessage: String) : SpreadsheetUiState
    data class Error(val errorMessage: String) : SpreadsheetUiState
}