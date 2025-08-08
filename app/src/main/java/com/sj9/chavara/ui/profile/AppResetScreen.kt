package com.sj9.chavara.ui.profile

import androidx.compose.runtime.*
import com.sj9.chavara.viewmodel.ProfileViewModel
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import com.sj9.chavara.viewmodel.ProfileUiState

@Composable
fun AppResetScreen(
    viewModel: ProfileViewModel,
    onResetComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            onResetComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Are you sure you want to reset all app data?")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.resetAppData() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Confirm Reset")
        }

        if (uiState is ProfileUiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
        if (uiState is ProfileUiState.Error) {
            Text(
                text = (uiState as ProfileUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}