package com.sj9.chavara.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.ui.theme.ChavaraTheme
import com.sj9.chavara.data.repository.ChavaraRepository
import com.sj9.chavara.ui.theme.ris
import kotlinx.coroutines.launch

@Composable
fun AppResetScreen(
    modifier: Modifier = Modifier
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

    var isResetting by remember { mutableStateOf(false) }
    var isReset by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    // Background gradient matching the design
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF433042), // #433042
            Color(0xFF5E3762), // #5E3762
            Color(0xFF5E205D), // #5E205D
            Color(0xFF521652)  // #521652
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 200f),
        end = androidx.compose.ui.geometry.Offset(300f, 800f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isReset) {
                // Reset completion message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .alpha(0.8f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50), // Green gradient for success
                                    Color(0xFF2E7D32)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "The App Is Reset",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ris
                    )
                }
            } else if (showConfirmation) {
                // Confirmation dialog
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Are you sure you want to reset all app data?",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = ris,
                        modifier = Modifier.padding(16.dp)
                    )

                    Text(
                        text = "This action cannot be undone.",
                        color = Color.Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = ris
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Cancel button
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.Gray)
                                .clickable { showConfirmation = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = ris
                            )
                        }

                        // Confirm button
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.Red)
                                .clickable {
                                    if (!isResetting && repository != null) {
                                        coroutineScope.launch {
                                            isResetting = true
                                            try {
                                                val success = repository.resetAppData()
                                                if (success) {
                                                    isReset = true
                                                    showConfirmation = false
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            isResetting = false
                                        }
                                    }
                                }
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isResetting) "..." else "Reset",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = ris
                            )
                        }
                    }
                }
            } else {
                // Initial reset button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .alpha(0.8f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF5722), // Red gradient for warning
                                    Color(0xFFD32F2F)
                                )
                            )
                        )
                        .clickable { showConfirmation = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset App Data",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ris
                    )
                }
            }
        }
    }
}

@Preview(
    name = "App Reset Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
fun AppResetScreenPreview() {
    ChavaraTheme {
        AppResetScreen()
    }
}
