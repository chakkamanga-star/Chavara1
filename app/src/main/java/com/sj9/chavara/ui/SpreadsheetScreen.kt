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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.sj9.chavara.R
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.ui.theme.ris
import com.sj9.chavara.ui.utils.*
import com.sj9.chavara.data.repository.ChavaraRepository
import kotlinx.coroutines.launch

@Composable
fun SpreadsheetScreen(
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember {
        try {
            ChavaraRepository(context)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    val coroutineScope = rememberCoroutineScope()

    var spreadsheetUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    // Exact gradient from the design: linear-gradient(192deg, #80EF6F 14.35%, #1DA313 37.97%, #10810C 60.19%, #0E4808 79.64%)
    // 192deg = 192 * π / 180 = 3.35 radians, converted to offset coordinates
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF80EF6F), // #80EF6F at 14.35%
            Color(0xFF1DA313), // #1DA313 at 37.97%
            Color(0xFF10810C), // #10810C at 60.19%
            Color(0xFF0E4808)  // #0E4808 at 79.64%
        ),
        start = androidx.compose.ui.geometry.Offset(350f, 0f),
        end = androidx.compose.ui.geometry.Offset(-50f, 800f)
    )

    // Button gradient: linear-gradient(180deg, #4EB154 27.88%, #30A038 61.06%, #338538 89.91%)
    val buttonGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4EB154), // #4EB154 at 27.88%
            Color(0xFF30A038), // #30A038 at 61.06%
            Color(0xFF338538)  // #338538 at 89.91%
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, 400f)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        val dimensions = getResponsiveDimensions()
        // Background image with opacity 0.2
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

        // URL Input Field (larger one)
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
                onValueChange = { spreadsheetUrl = it },
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

        // Save Button (smaller one)
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
                .clickable {
                    if (!isLoading && spreadsheetUrl.isNotEmpty() && repository != null) {
                        coroutineScope.launch {
                            isLoading = true
                            message = "Fetching data..."

                            try {
                                val result = repository.fetchDataFromSpreadsheet(
                                    spreadsheetUrl = spreadsheetUrl,
                                    onProgress = { progressMessage ->
                                        message = progressMessage
                                    }
                                )

                                if (result.isSuccess) {
                                    message = result.getOrNull() ?: "Data and images saved successfully!"
                                    onSaveClick()
                                } else {
                                    message = "Error: ${result.exceptionOrNull()?.message}"
                                }
                            } catch (e: Exception) {
                                message = "Error: ${e.message}"
                                e.printStackTrace()
                            }

                            isLoading = false
                        }
                    } else if (repository == null) {
                        message = "Service unavailable. Please restart the app."
                    }
                },
            contentAlignment = Alignment.Center // Note: If you don't need centering for an empty box, you can remove this
        ) {
            // Empty box as the text is positioned separately
        }

        // "Paste Spreadsheet URL" text
        Text(
            text = "Paste Spreadsheet ",
            color = Color.White,
            fontFamily = ris,
            fontSize = responsiveFontSize(27f),
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .offset(
                    x = dimensions.horizontalPadding * 3.5f,
                    y = dimensions.screenHeight * 0.345f
                )
        )
        Text(
            text = "URL",
            color = Color.White,
            fontFamily = ris,
            fontSize = responsiveFontSize(22f),
            fontWeight = FontWeight.Normal,
            lineHeight = 40.sp,
            modifier = Modifier
                .offset(
                    x = dimensions.horizontalPadding * 8f,
                    y = dimensions.screenHeight * 0.375f
                )
        )

        // "Save" text
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

        // Message display
        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = if (message.startsWith("Error:")) Color.Red else Color.White,
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
    }
}

@Preview(showBackground = true)
@Composable
fun SpreadsheetScreenPreview() {
    ChavaraTheme {
        SpreadsheetScreen()
    }
}