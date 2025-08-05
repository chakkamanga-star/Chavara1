package com.sj9.chavara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sj9.chavara.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.min
@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun LoginScreen(
    onNameEntered: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember { mutableStateOf("") }
    // Animation state has been removed


        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val widthScale = maxWidth / 390.dp
            val heightScale = maxHeight / 844.dp
            val minScale = min(widthScale, heightScale)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    ,
                // Center the content on the screen
                contentAlignment = Alignment.Center
            ) {
                // Use a Column to arrange the card and button vertically
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Main card Box (offset removed)
                    Box(
                        modifier = Modifier
                            .width(364.dp * minScale)
                            .height(270.dp * minScale),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(324.dp * minScale)
                                .height(203.dp * minScale)
                                .clip(RoundedCornerShape(50.dp * minScale))
                                .border(
                                    1.dp,
                                    WhiteTransparent47,
                                    RoundedCornerShape(50.dp * minScale)
                                )
                                .background(
                                    brush = Brush.radialGradient(
                                        colorStops = arrayOf(
                                            0.0f to GlassmorphicPrimary,
                                            0.0f to GlassmorphicSecondary.copy(alpha = 0.65f),
                                            1.0f to GlassmorphicTertiary.copy(alpha = 0.30f)
                                        ),
                                        center = androidx.compose.ui.geometry.Offset(0.4442f, 0.2496f),
                                        radius = 600f
                                    )
                                )
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(fontSize = 34.sp * minScale, fontWeight = FontWeight.Bold, fontFamily = ris, color = Color.Black)) { append("Hi!\n") }
                                    withStyle(style = SpanStyle(fontSize = 23.sp * minScale, fontWeight = FontWeight.Bold, fontFamily = ris, color = Color.Black)) { append(" What's Your Name?") }
                                },
                                modifier = Modifier
                                    .padding(start = 42.dp * minScale, top = 15.dp * minScale) // Using padding instead of offset
                                    .width(304.dp * minScale)
                                    .height(105.dp * minScale),
                                lineHeight = (40.sp * minScale)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter) // Align inside the card
                                    .padding(bottom = 15.dp * minScale) // Using padding instead of offset
                                    .width(260.dp * minScale)
                                    .height(61.dp * minScale)
                                    .clip(RoundedCornerShape(50.dp * minScale))
                                    .border(1.dp, WhiteTransparent47, RoundedCornerShape(50.dp * minScale))
                                    .background(
                                        brush = Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0.0f to GlassmorphicPrimary,
                                                0.0f to GlassmorphicSecondary.copy(alpha = 0.65f),
                                                1.0f to GlassmorphicTertiary.copy(alpha = 0.30f)
                                            ),
                                            center = androidx.compose.ui.geometry.Offset(0.4442f, 0.2496f),
                                            radius = 350f
                                        )
                                    )
                                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(50.dp * minScale), ambientColor = Color.Black.copy(alpha = 0.29f)),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    modifier = Modifier.fillMaxSize(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = Color.Black,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    placeholder = { Text("Enter your name...", color = Color.Black.copy(alpha = 0.6f)) },
                                    textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 16.sp * minScale),
                                    singleLine = true,
                                    shape = RoundedCornerShape(50.dp * minScale)
                                )
                            }
                        }
                    }

                    // Space between card and button
                    Spacer(modifier = Modifier.height(30.dp * minScale))

                    // Button (offset removed)
                    Box(
                        modifier = Modifier
                            .width(221.dp * minScale)
                            .height(50.dp * minScale)
                            .clip(RoundedCornerShape(26.dp * minScale))
                            .border(1.dp, WhiteTransparent47, RoundedCornerShape(26.dp * minScale))
                            .background(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(0.0f to ButtonGlassPrimary, 1.0f to ButtonGlassSecondary),
                                    center = androidx.compose.ui.geometry.Offset(0.5f, 0.4375f),
                                    radius = 150f
                                )
                            )
                            .shadow(elevation = 20.dp, shape = RoundedCornerShape(26.dp * minScale), ambientColor = Color.Black.copy(alpha = 0.29f))
                            .clickable { onNameEntered(nameInput) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "yep!!",
                            color = Color.Black,
                            fontSize = 31.sp * minScale,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ris,
                            lineHeight = 40.sp * minScale,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                }
            }
        }


    LaunchedEffect(nameInput) {
        if (nameInput.isNotBlank() && nameInput.length > 2) {
            delay(1500)
            onNameEntered(nameInput)
        }
    }
}

@Preview(
    name = "Login Screen",
    showBackground = true,
    widthDp = 384,
    heightDp = 917
)
@Composable
fun LoginScreenPreview() {
    ChavaraTheme {
        LoginScreen(
            onNameEntered = {}
        )
    }
}