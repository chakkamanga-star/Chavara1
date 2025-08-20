package com.sj9.chavara.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.ui.utils.*
import com.sj9.chavara.viewmodel.SpreadsheetViewModel
import com.sj9.chavara.ui.utils.SpreadsheetUiState
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun SpreadsheetScreen(
    modifier: Modifier = Modifier,
    viewModel: SpreadsheetViewModel, // ViewModel is now non-nullable and passed from navigation
    onProcessComplete: () -> Unit = {}
) {
    var spreadsheetUrl by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation event
    LaunchedEffect(key1 = viewModel.navigationEvent.collectAsState().value) {
        if (viewModel.navigationEvent.value) {
            onProcessComplete()
            viewModel.resetNavigationEvent()
        }
    }

    // Exact gradient from the design
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF80EF6F), Color(0xFF1DA313), Color(0xFF10810C), Color(0xFF0E4808)
        ),
        start = androidx.compose.ui.geometry.Offset(350f, 0f),
        end = androidx.compose.ui.geometry.Offset(-50f, 800f)
    )

    // Button gradient
    val buttonGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4EB154), Color(0xFF30A038), Color(0xFF338538)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, 400f)
    )

    var isLoading by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var messageColor by remember { mutableStateOf(Color.White) }

    // Update local isLoading and message based on uiState
    LaunchedEffect(uiState) {
        isLoading = uiState is SpreadsheetUiState.Loading
        when (val currentState = uiState) {
            is SpreadsheetUiState.Loading -> {
                messageText = currentState.loadingMessage
                messageColor = Color.White
            }
            is SpreadsheetUiState.Success -> {
                messageText = currentState.successMessage
                messageColor = Color(0xFF4CAF50) // Success green
            }
            is SpreadsheetUiState.Error -> {
                messageText = currentState.errorMessage
                messageColor = Color(0xFFFF5252) // Error red
            }
            SpreadsheetUiState.Idle -> {
                messageText = ""
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        val dimensions = getResponsiveDimensions()
        Image(
            painter = painterResource(id = R.drawable.jesus),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .height(dimensions.screenHeight * 0.75f)
                .offset(
                    x = dimensions.horizontalPadding,
                    y = dimensions.screenHeight * 0.16f
                )
                .alpha(1f),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(dimensions.screenHeight * 0.094f)
                .offset(
                    x = dimensions.horizontalPadding * 2.5f,
                    y = dimensions.screenHeight * 0.33f
                )
                .clip(RoundedCornerShape(dimensions.cornerRadius))
                .alpha(0.8f)
                .background(buttonGradient)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = spreadsheetUrl,
                onValueChange = {
                    spreadsheetUrl = it
                    if (uiState !is SpreadsheetUiState.Loading) {
                        viewModel.clearMessage()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = responsiveFontSize(16f),
                    fontFamily = ris
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (spreadsheetUrl.isEmpty()) {
                            Text(
                                text = "Paste Google Sheets URL here...",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = responsiveFontSize(14f),
                                fontFamily = ris
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.32f)
                .height(dimensions.screenHeight * 0.063f)
                .offset(
                    x = dimensions.horizontalPadding * 2.5f,
                    y = dimensions.screenHeight * 0.46f
                )
                .clip(RoundedCornerShape(dimensions.cornerRadius))
                .alpha(if (isLoading || spreadsheetUrl.isEmpty()) 0.5f else 0.8f)
                .background(buttonGradient)
                .clickable(enabled = !isLoading && spreadsheetUrl.isNotEmpty()) {
                    viewModel.processSpreadsheetUrl(spreadsheetUrl)
                },
            contentAlignment = Alignment.Center
        ) {
            // Empty box, text is positioned separately
        }

        Text(
            text = if (isLoading) "Loading..." else "Save",
            color = Color.White,
            fontFamily = ris,
            fontSize = responsiveFontSize(37f),
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .offset(
                    x = dimensions.horizontalPadding * 3.5f,
                    y = dimensions.screenHeight * 0.462f
                )
        )

        if (messageText.isNotEmpty()) {
            Text(
                text = messageText,
                color = messageColor,
                fontFamily = ris,
                fontSize = responsiveFontSize(20f),
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .offset(
                        x = dimensions.horizontalPadding * 2f,
                        y = dimensions.screenHeight * 0.55f
                    )
                    .fillMaxWidth(0.8f)
            )
        }

        // Add a cancel button when loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.25f)
                    .height(dimensions.screenHeight * 0.05f)
                    .offset(
                        x = dimensions.horizontalPadding * 5f,
                        y = dimensions.screenHeight * 0.47f
                    )
                    .clip(RoundedCornerShape(dimensions.cornerRadius))
                    .alpha(0.8f)
                    .background(Color(0xFFFF5252))
                    .clickable {
                        viewModel.cancelSync()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White,
                    fontFamily = ris,
                    fontSize = responsiveFontSize(14f),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpreadsheetScreenPreview() {
    ChavaraTheme {
        // This preview will not fully function without a real ViewModel instance.
        // For a more interactive preview, you would mock the ViewModel.
        SpreadsheetScreen(viewModel = viewModel())
    }
}
